package com.catalyst.ProCounsellor.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.catalyst.ProCounsellor.dto.AppointmentBookingRequest;
import com.catalyst.ProCounsellor.model.AppointmentBooking;
import com.catalyst.ProCounsellor.model.Counsellor;
import com.catalyst.ProCounsellor.model.User;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;

@Service
public class AppointmentBookingService {
	
	@Autowired
    private Firestore firestore;
	
	private static final Logger logger = LoggerFactory.getLogger(AppointmentBookingService.class);
	
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");


	public String bookAppointment(AppointmentBookingRequest request) throws Exception {
	    logger.info("Attempting to book appointment: {}", request);
	    
	    // ✅ Check: Appointment date & time must be in the future
	    LocalDate appointmentDate = LocalDate.parse(request.getDate());
	    LocalTime appointmentStartTime = LocalTime.parse(request.getStartTime(), timeFormatter);
	    LocalDateTime appointmentDateTime = LocalDateTime.of(appointmentDate, appointmentStartTime);
	    LocalDateTime now = LocalDateTime.now();

	    if (appointmentDateTime.isBefore(now)) {
	        logger.warn("Attempted to book appointment in the past: {} {}", request.getDate(), request.getStartTime());
	        throw new RuntimeException("Cannot book an appointment in the past");
	    }
	
	    // Fetch counsellor details
	    logger.debug("Fetching counsellor details for ID: {}", request.getCounsellorId());
	    DocumentSnapshot counsellorSnapshot = firestore.collection("counsellors")
	            .document(request.getCounsellorId()).get().get();
	
	    if (!counsellorSnapshot.exists()) {
	        logger.warn("Counsellor not found with ID: {}", request.getCounsellorId());
	        throw new RuntimeException("Counsellor not found");
	    }
	
	    Counsellor counsellor = counsellorSnapshot.toObject(Counsellor.class);
	    logger.debug("Counsellor details retrieved: {}", counsellor.getUserName());
	
	    String weekday = LocalDate.parse(request.getDate()).getDayOfWeek().toString();
	    String formattedWeekday = weekday.substring(0,1).toUpperCase() + weekday.substring(1).toLowerCase();
	
	    if (!counsellor.getWorkingDays().contains(formattedWeekday)) {
	        logger.warn("Counsellor not available on selected day: {}", formattedWeekday);
	        throw new RuntimeException("Counsellor not available on selected day");
	    }
	
	    LocalTime start = LocalTime.parse(request.getStartTime(), timeFormatter);
	    LocalTime end = start.plusMinutes(30);
	    LocalTime officeStart = LocalTime.parse(counsellor.getOfficeStartTime());
	    LocalTime officeEnd = LocalTime.parse(counsellor.getOfficeEndTime());
	
	    if (start.isBefore(officeStart) || end.isAfter(officeEnd)) {
	        logger.warn("Requested time [{} - {}] is outside working hours [{} - {}]", start, end, officeStart, officeEnd);
	        throw new RuntimeException("Selected time is outside counsellor's working hours");
	    }
	
	    logger.debug("Checking if slot is already booked...");
	    ApiFuture<QuerySnapshot> query = firestore.collection("appointments")
	            .whereEqualTo("counsellorId", request.getCounsellorId())
	            .whereEqualTo("date", request.getDate())
	            .whereEqualTo("startTime", request.getStartTime())
	            .whereIn("status", List.of("pending", "confirmed"))
	            .get();
	
	    if (!query.get().isEmpty()) {
	        logger.warn("Slot already booked for counsellorId={}, date={}, startTime={}", 
	                    request.getCounsellorId(), request.getDate(), request.getStartTime());
	        throw new RuntimeException("Selected slot is already booked");
	    }
	
	    logger.debug("Checking if user has already booked a pending appointment with this counsellor...");
	    QuerySnapshot pendingUserAppointments = firestore.collection("appointments")
	            .whereEqualTo("userId", request.getUserId())
	            .whereEqualTo("counsellorId", request.getCounsellorId())
	            .whereEqualTo("status", "booked").get().get();
	
	    if (!pendingUserAppointments.isEmpty()) {
	        logger.warn("User {} already has a pending appointment with counsellor {}", 
	                    request.getUserId(), request.getCounsellorId());
	        throw new RuntimeException("You already have a pending appointment with this counsellor");
	    }
	
	    logger.info("Creating new appointment...");
	    AppointmentBooking appointment = new AppointmentBooking();
	    appointment.setUserId(request.getUserId());
	    appointment.setCounsellorId(request.getCounsellorId());
	    appointment.setDate(request.getDate());
	    appointment.setStartTime(request.getStartTime());
	    appointment.setEndTime(end.format(timeFormatter));
	    appointment.setMode(request.getMode());
	    appointment.setNotes(request.getNotes());
	    appointment.setStatus("pending");
	    appointment.setCreatedAt(Timestamp.now());
	    appointment.setUpdatedAt(Timestamp.now());
	
	    DocumentReference newDoc = firestore.collection("appointments").document();
	    appointment.setAppointmentId(newDoc.getId());
	
	    newDoc.set(appointment);
	    logger.info("Appointment booked with ID: {}", appointment.getAppointmentId());
	
	    logger.debug("Updating appointmentId in user and counsellor records...");
	
	    firestore.collection("users")
	        .document(request.getUserId())
	        .update("appointmentIds", FieldValue.arrayUnion(appointment.getAppointmentId()));
	
	    firestore.collection("counsellors")
	        .document(request.getCounsellorId())
	        .update("appointmentIds", FieldValue.arrayUnion(appointment.getAppointmentId()));
	
	    logger.info("Successfully updated appointment references in user and counsellor documents.");
	
	    return appointment.getAppointmentId();
	}

	public List<AppointmentBooking> getAppointmentsByCounsellorId(String counsellorId) throws Exception {
        logger.info("Fetching appointments for counsellor ID: {}", counsellorId);

        ApiFuture<QuerySnapshot> future = firestore.collection("appointments")
                .whereEqualTo("counsellorId", counsellorId)
                .get();

        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        List<AppointmentBooking> appointments = new ArrayList<>();

        for (QueryDocumentSnapshot doc : documents) {
            appointments.add(doc.toObject(AppointmentBooking.class));
        }

        logger.info("Total appointments found: {}", appointments.size());
        return appointments;
	}


	public AppointmentBooking getAppointmentById(String appointmentId) throws ExecutionException, InterruptedException {
	    logger.info("Fetching appointment by ID: {}", appointmentId);

	    DocumentSnapshot snapshot = firestore.collection("appointments").document(appointmentId).get().get();

	    if (snapshot.exists()) {
	        logger.info("Appointment found for ID: {}", appointmentId);
	        return snapshot.toObject(AppointmentBooking.class);
	    } else {
	        logger.warn("No appointment found with ID: {}", appointmentId);
	        return null;
	    }
	}
	
	public List<AppointmentBooking> getAppointmentsByUserId(String userId) throws Exception {
        logger.info("Fetching user document for userId: {}", userId);

        DocumentReference userRef = firestore.collection("users").document(userId);
        ApiFuture<DocumentSnapshot> userFuture = userRef.get();
        DocumentSnapshot userSnapshot = userFuture.get();

        if (!userSnapshot.exists()) {
            logger.warn("User not found for userId: {}", userId);
            return Collections.emptyList();
        }

        User user = userSnapshot.toObject(User.class);
        List<String> appointmentIds = user.getAppointmentIds();

        if (appointmentIds == null || appointmentIds.isEmpty()) {
            logger.info("No appointment IDs found for userId: {}", userId);
            return Collections.emptyList();
        }

        logger.info("Found {} appointment IDs for userId: {}", appointmentIds.size(), userId);

        List<AppointmentBooking> appointments = new ArrayList<>();
        for (String appointmentId : appointmentIds) {
            logger.debug("Fetching appointment with ID: {}", appointmentId);
            DocumentSnapshot doc = firestore.collection("appointments").document(appointmentId).get().get();
            if (doc.exists()) {
                appointments.add(doc.toObject(AppointmentBooking.class));
            } else {
                logger.warn("Appointment not found for ID: {}", appointmentId);
            }
        }

        logger.info("Returning {} appointments for userId: {}", appointments.size(), userId);
        return appointments;
    }
	
	public List<AppointmentBooking> getUpcomingAppointmentsByUserId(String userId) throws Exception {
        logger.info("Fetching upcoming appointments for userId: {}", userId);

        // Step 1: Fetch user document
        DocumentSnapshot userSnapshot = firestore.collection("users").document(userId).get().get();
        if (!userSnapshot.exists()) {
            logger.warn("User not found for userId: {}", userId);
            return Collections.emptyList();
        }

        User user = userSnapshot.toObject(User.class);
        List<String> appointmentIds = user.getAppointmentIds();

        if (appointmentIds == null || appointmentIds.isEmpty()) {
            logger.info("No appointment IDs found for userId: {}", userId);
            return Collections.emptyList();
        }

        logger.info("Found {} appointments for userId: {}", appointmentIds.size(), userId);

        List<AppointmentBooking> upcomingAppointments = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (String appointmentId : appointmentIds) {
            DocumentSnapshot doc = firestore.collection("appointments").document(appointmentId).get().get();
            if (!doc.exists()) {
                logger.warn("Appointment not found: {}", appointmentId);
                continue;
            }

            AppointmentBooking appointment = doc.toObject(AppointmentBooking.class);

            try {
                String dateStr = appointment.getDate();         // yyyy-MM-dd
                String startTimeStr = appointment.getStartTime(); // HH:mm

                LocalDateTime appointmentStart = LocalDateTime.parse(dateStr + "T" + startTimeStr);
                if (appointmentStart.isAfter(now)) {
                    logger.debug("Appointment {} is upcoming", appointmentId);
                    upcomingAppointments.add(appointment);
                } else {
                    logger.debug("Appointment {} has already started or ended", appointmentId);
                }
            } catch (Exception e) {
                logger.error("Failed to parse appointment date/time for ID: {}", appointmentId, e);
            }
        }

        logger.info("Returning {} upcoming appointments for userId: {}", upcomingAppointments.size(), userId);
        return upcomingAppointments;
    }
}
