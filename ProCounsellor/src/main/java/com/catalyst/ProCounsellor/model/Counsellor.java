package com.catalyst.ProCounsellor.model;

import java.util.List;
import java.util.Map;

import com.google.cloud.firestore.annotation.DocumentId;

import lombok.Data;

@Data
public class Counsellor {
	@DocumentId
    private String userName;
	
	private String role;
    private String firstName;   
    private String lastName;   
    private String phoneNumber; 
    private String description;// to be written in third party
    private String email;
    private Long walletAmount;
    private List<Transaction> transactions;
    private BankDetails bankDetails;
    private String photoUrl;
    private String photoUrlSmall;
    private String password;
    private String organisationName;
    private String experience;
    private List<ActivityLog> activityLog;
    private List<CallHistory> callHistory;
    private List<String> stateOfCounsellor;
    private List<Map<String,String>> chatIdsCreatedForCounsellor;
    private Double ratePerYear;
    private Double ratePerMinute;
    private List<String> expertise;
    private Integer noOfClients;
    private Integer noOfFollowers;
    private List<String> clientIds;
    private List<String> followerIds;
    private Integer rating;
    private List<String> languagesKnow;
    private List<String> reviewIds;
    private String minuteSpendOnCall;
    private String minuteSpendOnVideoCall;
    //to be verified by admin;
    private boolean isVerified;
    private StateType state;
    private String fcmToken;
    private String voipToken;
    private String platform;
    private String currectCallUUID; //handle call cancel
    private List<String> workingDays;
    private String officeStartTime;
    private String officeEndTime;
    private Address fullOfficeAddress;
    private boolean phoneOtpVerified;
    private boolean emailOtpVerified;
    private List<String> appointmentIds;
}
