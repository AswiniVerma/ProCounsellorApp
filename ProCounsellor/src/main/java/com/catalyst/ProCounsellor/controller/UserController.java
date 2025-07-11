package com.catalyst.ProCounsellor.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.catalyst.ProCounsellor.config.JwtUtil;
import com.catalyst.ProCounsellor.dto.AppointmentBookingRequest;
import com.catalyst.ProCounsellor.dto.CounsellorDataInUserDashboard;
import com.catalyst.ProCounsellor.exception.UserNotFoundException;
import com.catalyst.ProCounsellor.model.AppointmentBooking;
import com.catalyst.ProCounsellor.model.Counsellor;
import com.catalyst.ProCounsellor.model.User;
import com.catalyst.ProCounsellor.service.CounsellorService;
import com.catalyst.ProCounsellor.service.PhotoService;
import com.catalyst.ProCounsellor.service.UserService;
import com.google.api.core.ApiFuture;
import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/user")
public class UserController {

	@Autowired
    private UserService userService;
	
	@Autowired
    private CounsellorService counsellorService;
	
	@Autowired
	private PhotoService photoService;
	
	
	@PatchMapping("/{userId}")
	public ResponseEntity<?> updateUserFields(
	        @PathVariable String userId,
	        @RequestBody Map<String, Object> updates,
	        HttpServletRequest request) {

	    try {
	        User user = JwtUtil.getAuthenticatedUser(request);

	        if (!user.getUserName().equals(userId)) {
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized access");
	        }

	        User updatedUser = userService.updateUserFields(userId, updates);
	        return ResponseEntity.ok(updatedUser);

	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
	    }
	}
	
	@GetMapping("/{userId}")
	public ResponseEntity<?> getUserById(@PathVariable String userId, HttpServletRequest request) 
	        throws ExecutionException, InterruptedException {

		User authenticatedUser = JwtUtil.getAuthenticatedUser(request);

        if (!authenticatedUser.getUserName().equals(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized access");
        }

	    User user = userService.getUserById(userId);

	    if (user == null) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
	    }

	    return ResponseEntity.ok(user);
	}
	
	@GetMapping("/getCounsellorById")
	public ResponseEntity<?> getCounsellorById(@RequestParam String userId, @RequestParam String counsellorId, HttpServletRequest request) 
	        throws ExecutionException, InterruptedException {

		User user = JwtUtil.getAuthenticatedUser(request);
		
	    if (!user.getUserName().equals(userId)) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access denied: user ID mismatch");
	    }

	    Counsellor counsellor = counsellorService.getCounsellorById(counsellorId);

	    if (counsellor == null) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Counsellor not found");
	    }

	    return ResponseEntity.ok(counsellor);
	}

    @PostMapping("/book")
    public ResponseEntity<?> bookAppointment(@RequestBody AppointmentBookingRequest appointmentBookingRequest, HttpServletRequest request) {
        try {
	        User user = JwtUtil.getAuthenticatedUser(request);

	        if (!user.getUserName().equals(appointmentBookingRequest.getUserId())) {
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized access");
	        }
            String appointmentId = userService.bookAppointment(appointmentBookingRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Appointment booked successfully", "appointmentId", appointmentId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/getUserAllAppointments")
    public ResponseEntity<?> getAppointmentsByUserId(@RequestParam String userId, HttpServletRequest request) throws Exception {
    	User user = JwtUtil.getAuthenticatedUser(request);

        if (!user.getUserName().equals(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized access");
        }
        List<AppointmentBooking> appointments = userService.getAppointmentsByUserId(userId);
        return ResponseEntity.ok(appointments);
    }
    
    @GetMapping("/getUpcomingAppointments")
    public ResponseEntity<?> getUpcomingAppointments(@RequestParam String userId, HttpServletRequest request) throws Exception {
    	User user = JwtUtil.getAuthenticatedUser(request);

        if (!user.getUserName().equals(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized access");
        }
        
        List<AppointmentBooking> upcomingAppointments = userService.getUpcomingAppointmentsByUserId(userId);
        return ResponseEntity.ok(upcomingAppointments);
    }
    
	@GetMapping("/selectedCounsellorAppointments")
    public ResponseEntity<List<AppointmentBooking>> getAppointmentsByCounsellor(
    		@RequestParam String userId, @RequestParam String counsellorId, HttpServletRequest request) {
        try {
        	User user = JwtUtil.getAuthenticatedUser(request);

	        if (!user.getUserName().equals(userId)) {
	        	return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	        }
	        
            List<AppointmentBooking> appointments = counsellorService.getAppointmentsByCounsellorId(counsellorId);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
	}
	
	@GetMapping("/getAppointmentById")
	public ResponseEntity<AppointmentBooking> getAppointmentById(@RequestParam String userId, @RequestParam String appointmentId, HttpServletRequest request) {
	    try {
	    	User user = JwtUtil.getAuthenticatedUser(request);

	        if (!user.getUserName().equals(userId)) {
	        	return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	        }
	        
	        AppointmentBooking appointment = counsellorService.getAppointmentById(appointmentId);
	        if (appointment != null) {
	            return ResponseEntity.ok(appointment);
	        } else {
	            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
	        }
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
	    }
	}
    
    @GetMapping("/{userName}/counsellorsAccordingToInterestedCourse/all")
    public ResponseEntity<List<CounsellorDataInUserDashboard>> getCounsellorsByUserInterest(@PathVariable String userName, HttpServletRequest request) {
        try {
        	User user = JwtUtil.getAuthenticatedUser(request);

	        if (!user.getUserName().equals(userName)) {
	            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
	        }
	        
            User loggedInUser = userService.getUserById(userName);
            String interestedCourse = loggedInUser.getInterestedCourse();
            List<CounsellorDataInUserDashboard> matchingCounsellors = userService.getCounsellorsByCourse(interestedCourse);
            
            return new ResponseEntity<>(matchingCounsellors, HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (ExecutionException | InterruptedException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
	
	@GetMapping("/all-users")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }
	
	@PostMapping("/{userId}/subscribe/{counsellorId}")
	public ResponseEntity<String> subscribeToCounsellor(@PathVariable String userId, @PathVariable String counsellorId) {
	    try {
	    	boolean hasAlreadySubscribed = userService.isSubscribedToCounsellor(userId, counsellorId);
	    	if(!hasAlreadySubscribed) {
		        boolean result = userService.subscribeToCounsellor(userId, counsellorId);
		        if (result) {
		            return ResponseEntity.ok("Successfully subscribed to the counsellor.");
		        }
		        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		                             .body("Subscription failed. Either the user or counsellor does not exist.");
	    	}
	    	else {
	    		return ResponseEntity.ok("Already subscribed to the counsellor.");
	    	}
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                             .body("An error occurred while subscribing: " + e.getMessage());
	    }
	}

	@GetMapping("/{userId}/subscribed-counsellors")
	public ResponseEntity<?> getSubscribedCounsellors(@PathVariable String userId) {
	    try {
	        List<Counsellor> subscribedCounsellors = userService.getSubscribedCounsellors(userId);
	        if (subscribedCounsellors == null || subscribedCounsellors.isEmpty()) {
	            return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                                 .body("No counsellors subscribed by user with ID: " + userId);
	        }
	        return ResponseEntity.ok(subscribedCounsellors);
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                             .body("An error occurred while retrieving subscribed counsellors: " + e.getMessage());
	    }
	}
	
	
	@PostMapping("/{userId}/follow/{counsellorId}")
	public ResponseEntity<String> followCounsellor(@PathVariable String userId, @PathVariable String counsellorId) {
	    try {
	    	boolean isAlreadyFollwing = userService.hasFollowedCounsellor(userId, counsellorId);
	    	
	    	if(!isAlreadyFollwing) {
		        boolean result = userService.followCounsellor(userId, counsellorId);
		        if (result) {
		            return ResponseEntity.ok("Successfully followed the counsellor.");
		        }
		        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		                             .body("Cannot follow. Either the user or counsellor does not exist.");
	    	}
	    	else {
	    		return ResponseEntity.ok("Already following the counsellor.");
	    	}
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                             .body("An error occurred while following: " + e.getMessage());
	    }
	}

	@GetMapping("/{userId}/followed-counsellors")
	public ResponseEntity<?> getFollowedCounsellors(@PathVariable String userId) {
	    try {
	        List<Counsellor> followedCounsellors = userService.getFollowedCounsellors(userId);
	        if (followedCounsellors == null || followedCounsellors.isEmpty()) {
	            return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                                 .body("No counsellors followed by user with ID: " + userId);
	        }
	        return ResponseEntity.ok(followedCounsellors);
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                             .body("An error occurred while retrieving followed counsellors: " + e.getMessage());
	    }
	}
	
	@GetMapping("/course-types")
	public ResponseEntity<List<String>> getCourseTypes() throws Exception {
	    Firestore db = FirestoreClient.getFirestore();
	    ApiFuture<QuerySnapshot> future = db.collection("courseTypes").get();
	    List<String> courses = new ArrayList<>();

	    for (DocumentSnapshot doc : future.get().getDocuments()) {
	        courses.add(doc.getString("name"));
	    }

	    return ResponseEntity.ok(courses);
	}

	@GetMapping("/states")
	public ResponseEntity<List<String>> getStates() throws Exception {
	    Firestore db = FirestoreClient.getFirestore();
	    ApiFuture<QuerySnapshot> future = db.collection("states").get();
	    List<String> states = new ArrayList<>();

	    for (DocumentSnapshot doc : future.get().getDocuments()) {
	        states.add(doc.getString("name"));
	    }

	    return ResponseEntity.ok(states);
	}
	
	@PostMapping("/{userId1}/add-friend/{userId2}")
	public ResponseEntity<String> addFriend(@PathVariable String userId1, @PathVariable String userId2) {
	    try {
	    	boolean hasAlreadySubscribed = userService.isFriendToUser(userId1, userId2);
	    	if(!hasAlreadySubscribed) {
		        boolean result = userService.addFriend(userId1, userId2);
		        if (result) {
		            return ResponseEntity.ok("Successfully added as friend.");
		        }
		        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		                             .body("Adding as friend failed. Either user1 or user2 does not exist.");
	    	}
	    	else {
	    		return ResponseEntity.ok("Already friends.");
	    	}
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                             .body("An error occurred while adding as friend: " + e.getMessage());
	    }
	}

	@GetMapping("/{userId}/friends")
	public ResponseEntity<?> getFriends(@PathVariable String userId) {
	    try {
	        List<User> friends = userService.getFriends(userId);
	        if (friends == null || friends.isEmpty()) {
	            return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                                 .body("No counsellors subscribed by user with ID: " + userId);
	        }
	        return ResponseEntity.ok(friends);
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                             .body("An error occurred while retrieving subscribed counsellors: " + e.getMessage());
	    }
	}
	
	@GetMapping("/{userId}/is-subscribed/{counsellorId}")
	public ResponseEntity<Boolean> isSubscribedToCounsellor(@PathVariable String userId, @PathVariable String counsellorId) {
	    boolean isSubscribed = userService.isSubscribedToCounsellor(userId, counsellorId);
	    return ResponseEntity.ok(isSubscribed);
	}
	
	@GetMapping("/{userId}/has-followed/{counsellorId}")
	public ResponseEntity<Boolean> hasFollowedCounsellor(@PathVariable String userId, @PathVariable String counsellorId) {
	    boolean hasFollowed = userService.hasFollowedCounsellor(userId, counsellorId);
	    if (hasFollowed) {
	        return ResponseEntity.ok(hasFollowed);
	    }
	    return ResponseEntity.ok(hasFollowed);
	}
	
	@GetMapping("/{userId1}/is-friend/{userId2}")
	public ResponseEntity<Boolean> isFriendToUser(@PathVariable String userId1, @PathVariable String userId2) {
	    boolean isFriend = userService.isFriendToUser(userId1, userId2);
	    return ResponseEntity.ok(isFriend);
	}
	
	@DeleteMapping("/{userId}/unsubscribe/{counsellorId}")
	public ResponseEntity<String> unsubscribeCounsellor(
	        @PathVariable String userId, 
	        @PathVariable String counsellorId) {
	    try {
	        // Check if the user is subscribed to the counsellor
	        boolean isSubscribed = userService.isSubscribedToCounsellor(userId, counsellorId);

	        if (!isSubscribed) {
	            return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                    .body("User is not subscribed to this counsellor.");
	        }

	        // Proceed to unsubscribe
	        boolean result = userService.unsubscribeCounsellor(userId, counsellorId);
	        if (result) {
	            return ResponseEntity.ok("Successfully unsubscribed from the counsellor.");
	        } else {
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                    .body("An error occurred while trying to unsubscribe.");
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body("An unexpected error occurred.");
	    }
	}

	
	@DeleteMapping("/{userId}/unfollow/{counsellorId}")
	public ResponseEntity<String> unfollowCounsellor(
	        @PathVariable String userId, 
	        @PathVariable String counsellorId) {
	    try {
	        // Check if the user has followed to the counsellor
	        boolean hasFollowed = userService.hasFollowedCounsellor(userId, counsellorId);

	        if (!hasFollowed) {
	            return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                    .body("User has not followed this counsellor.");
	        }

	        // Proceed to undollow
	        boolean result = userService.unfollowCounsellor(userId, counsellorId);
	        if (result) {
	            return ResponseEntity.ok("Successfully unfollowed the counsellor.");
	        } else {
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                    .body("An error occurred while trying to unfollow.");
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body("An unexpected error occurred.");
	    }
	}
	
	@DeleteMapping("/{userId1}/unfriend/{userId2}")
	public ResponseEntity<String> unfriend(
	        @PathVariable String userId1, 
	        @PathVariable String userId2) {
	    try {
	        // Check if the user1 is friend to the user2
	        boolean isFriend = userService.isFriendToUser(userId1, userId2);

	        if (!isFriend) {
	            return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                    .body("User1 is not friend to this user2.");
	        }

	        // Proceed to unfriend
	        boolean result = userService.unfriend(userId1, userId2);
	        if (result) {
	            return ResponseEntity.ok("Successfully unfriended the user.");
	        } else {
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                    .body("An error occurred while trying to unfriend.");
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body("An unexpected error occurred.");
	    }
	}
	
	@PostMapping("/{userId}/photo")
    public String updateUserPhoto(@PathVariable String userId, @RequestParam("photo") MultipartFile file) {
        try {
            String fileType = file.getContentType().split("/")[1];

            // Upload the photo and get the photo URL
            String photoUrl = photoService.uploadPhoto(userId, file.getBytes(), fileType, "user");

            // Update the user's photo URL in Firestore
            userService.updateUserPhotoUrl(userId, photoUrl);

            return "Photo updated successfully: " + photoUrl;
        } catch (IOException e) {
            return "Error uploading photo: " + e.getMessage();
        }
    }
	
	 	 /**
	     * Update user state API using PathVariable.
	     *
	     * @param userName the username of the user
	     * @param state    the presence state to be updated
	     * @return ResponseEntity indicating success or failure
	     */
	    @PostMapping("/{userName}/{state}")
	    public ResponseEntity<String> updateUserState(
	            @PathVariable String userName,
	            @PathVariable String state) {

	        boolean isUpdated = userService.updateUserState(userName, state);
	        if (isUpdated) {
	            return ResponseEntity.ok("User state updated successfully.");
	        } else {
	            return ResponseEntity.status(500).body("Failed to update user state.");
	        }
	    }
	    
	    /**
	     * Check if the user is online by their username.
	     *
	     * @param userName the username of the user
	     * @return true if the user is online, false otherwise
	     */
	    @GetMapping("/{userName}/isOnline")
	    public boolean isOnline(@PathVariable String userName) {
	        try {
	            return userService.isUserOnline(userName);
	        } catch (Exception e) {
	            // Log the error and return false
	            System.err.println("Error checking user online status: " + e.getMessage());
	            return false;
	        }
	    }
	    
//	    
//	    /**
//	     *To fetch the list of counsellors according to user's interested course (KARNATAKA)
//	     */
//	    @GetMapping("/{userName}/counsellorsAccordingToInterestedCourse/karnataka")
//	    public ResponseEntity<List<Counsellor>> getCounsellorsInKarnataka(@PathVariable String userName) {
//	        try {
//	            User user = userService.getUserById(userName);
//	            Course interestedCourse = user.getInterestedCourse();
//	            List<Counsellor> counsellors = userService
//	                    .getCounsellorsByCourseAndState(interestedCourse, AllowedStates.KARNATAKA);
//
//	            return new ResponseEntity<>(counsellors, HttpStatus.OK);
//	        } catch (NotFoundException e) {
//	            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//	        } catch (ExecutionException | InterruptedException e) {
//	            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
//	        }
//	    }
//
//	    /**
//	     *To fetch the list of counsellors according to user's interested course (MAHARASHTRA)
//	     */
//	    @GetMapping("/{userName}/counsellorsAccordingToInterestedCourse/maharashtra")
//	    public ResponseEntity<List<Counsellor>> getCounsellorsInMaharashtra(@PathVariable String userName) {
//	        try {
//	            User user = userService.getUserById(userName);
//	            Courses interestedCourse = user.getInterestedCourse();
//	            List<Counsellor> counsellors = userService
//	                    .getCounsellorsByCourseAndState(interestedCourse, AllowedStates.MAHARASHTRA);
//
//	            return new ResponseEntity<>(counsellors, HttpStatus.OK);
//	        } catch (NotFoundException e) {
//	            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//	        } catch (ExecutionException | InterruptedException e) {
//	            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
//	        }
//	    }
//
//	    /**
//	     * To fetch the list of counsellors according to user's interested course (TAMILNADU)
//	     */
//	    @GetMapping("/{userName}/counsellorsAccordingToInterestedCourse/tamilnadu")
//	    public ResponseEntity<List<Counsellor>> getCounsellorsInTamilNadu(@PathVariable String userName) {
//	        try {
//	            User user = userService.getUserById(userName);
//	            Courses interestedCourse = user.getInterestedCourse();
//	            List<Counsellor> counsellors = userService
//	                    .getCounsellorsByCourseAndState(interestedCourse, AllowedStates.TAMILNADU);
//
//	            return new ResponseEntity<>(counsellors, HttpStatus.OK);
//	        } catch (NotFoundException e) {
//	            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//	        } catch (ExecutionException | InterruptedException e) {
//	            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
//	        }
//	    }
	    
	    @GetMapping("/getUserNameFromEmail")
	    public String getUserNameFromEmail(@RequestParam String email) throws ExecutionException, InterruptedException {
	        try {
	            String userName = userService.getUserNameFromEmail(email);
	            return userName;
	        } catch (UserNotFoundException e) {
	            return "Cannot find User ID";
	        }
	    }

	    @GetMapping("/getUserNameFromPhoneNumber")
	    public String getUserNameFromPhoneNumber(@RequestParam String phoneNumber) throws ExecutionException, InterruptedException {
	        try {
	            String userName = userService.getUserNameFromPhoneNumber(phoneNumber);
	            return userName;
	        } catch (UserNotFoundException e) {
	        	return "Cannot find User ID";
	        }
	    }
}
