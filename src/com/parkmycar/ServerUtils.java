package com.parkmycar;

import java.net.URI;
import java.net.URISyntaxException;

public class ServerUtils {
	
	private static String BASE_SERVER_URL = "parkmycar.elasticbeanstalk.com";
	private static int SERVER_PORT = 80;
	private static String PROTOCOL = "http";
	public static String PARKING_LOCATIONS_CPATH = "/ParkingLocations";
	public static String SAVE_VOTE_DETAILS_PATH = "/SaveVote";
	public static String PARKING_LOCATION_DETAILS_PATH = "/ParkingLocationDetails";
	public static String USER_FEEDBACK_PATH = "/UserFeedback";
	
	public static URI getFullUrl (String contextPath) {
		try {
			URI uri = new URI (PROTOCOL, null, BASE_SERVER_URL, SERVER_PORT, contextPath, null, null);
			return uri;
		} catch (URISyntaxException e) {
			//
		}
		return null;
	}
	
}
