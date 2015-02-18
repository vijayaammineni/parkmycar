package com.parkmycar;

public class FetchParkingLocationsResult<T> {
	private T result;
	private Exception error;
	private String message;

	private Double latitude;
	private Double longitude;

	public T getResult() {
		return result;
	}

	public String getError() {
		if (error != null) {
			return error.getMessage();
		} else {
			return null;
		}
	}

	public String getMessage() {
		return message;
	}

	public FetchParkingLocationsResult(T result) {
		super();
		this.result = result;
	}

	public FetchParkingLocationsResult(Exception error) {
		super();
		this.error = error;
	}

	public FetchParkingLocationsResult(String message) {
		super();
		this.message = message;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}
	
}