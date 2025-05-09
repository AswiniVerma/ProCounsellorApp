package com.catalyst.ProCounsellor.service;

import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import com.catalyst.ProCounsellor.model.ActivityLog;
import com.catalyst.ProCounsellor.model.Counsellor;
import com.catalyst.ProCounsellor.model.User;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;

@Service
public class SharedService {
	
	Firestore firestore = FirestoreClient.getFirestore();

	    public void updateCounsellor(Counsellor counsellor) throws ExecutionException, InterruptedException {
	        firestore.collection("counsellors").document(counsellor.getUserName()).set(counsellor).get();
	    }
	    
	    public void updateUser(User user) throws ExecutionException, InterruptedException {
	        firestore.collection("users").document(user.getUserName()).set(user).get();
	    }
	    
	    public User getUserById(String userId) throws ExecutionException, InterruptedException {
	        DocumentSnapshot snapshot = firestore.collection("users").document(userId).get().get();
	        return snapshot.exists() ? snapshot.toObject(User.class) : null;
	    }
	    
	    public Counsellor getCounsellorById(String counsellorId) throws ExecutionException, InterruptedException {
	        DocumentSnapshot snapshot = firestore.collection("counsellors").document(counsellorId).get().get();
	        return snapshot.exists() ? snapshot.toObject(Counsellor.class) : null;
	    }
	    
	    public String getFullNameFromUserName(String userName) {
	        try {
	            DocumentSnapshot userDoc = firestore.collection("users")
	                    .document(userName)
	                    .get()
	                    .get();

	            if (!userDoc.exists()) {
	                userDoc = firestore.collection("counsellors")
	                        .document(userName)
	                        .get()
	                        .get();
	            }

	            if (userDoc.exists()) {
	                String firstName = userDoc.getString("firstName");
	                String lastName = userDoc.getString("lastName");

	                if (firstName != null && lastName != null) {
	                    return firstName + " " + lastName;
	                }
	            }

	            return userName;

	        } catch (Exception e) {
	            e.printStackTrace();
	            return "Error retrieving user details";
	        }
	    }
	    
	    public ActivityLog createActivityObject(String activityString) {
			ActivityLog activity = new ActivityLog();
			activity.setActivity(activityString);
			activity.setTimestamp(Timestamp.now());
			return activity;
		}
}
