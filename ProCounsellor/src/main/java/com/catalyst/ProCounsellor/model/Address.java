package com.catalyst.ProCounsellor.model;

import lombok.Data;

@Data
public class Address {
	private String role;
	private String officeNameFloorBuildingAndArea;
	private String city;
	private String state;
	private String pinCode;
	private String latCoordinate;
	private String longCoordinate;
	private String mapLink;
}
