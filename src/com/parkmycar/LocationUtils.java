package com.parkmycar;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.parkmycar.json.JSONKeys;

public class LocationUtils {

	public static final String LOCATION_CHANGE_BROADCAST_ACTION = "Location_Change";

	public static Double DEFAULT_LATITUDE = 32.7150;
	public static Double DEFAULT_LONGITUDE = -117.1625;
	public static int DEFAULT_ZOOM_LEVEL = 14;
	public static Double DEFAULT_PARKING_LOCATION_RADIUS = 0.075;
	private Context context;
	private Activity activity;
	private LocationManager lm;

	private final LocationListener locationListener = new LocationListener() {
		public void onLocationChanged(Location location) {

		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub

		}
	};

	public LocationUtils(Activity activity, Context context) {
		this.activity = activity;
		this.context = context;
		lm = (LocationManager) this.activity
				.getSystemService(Context.LOCATION_SERVICE);
	}

	public Location getMyLocation(boolean promptForGpsSettings) {
		// Get location from GPS if it's available
		Location myLocation = lm
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (!isServicesEnabled(lm, LocationManager.GPS_PROVIDER)
				&& promptForGpsSettings) {
			// Provider not enabled, prompt user to enable it
			Toast.makeText(context, R.string.please_turn_on_gps,
					Toast.LENGTH_LONG).show();
			Intent myIntent = new Intent(
					Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			this.activity.startActivity(myIntent);
		}
		// Location wasn't found, check the next most accurate place for the
		// current location
		if (myLocation == null) {
			if (isServicesEnabled(lm, LocationManager.GPS_PROVIDER)) {
				lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
						locationListener);
			}
			if (isServicesEnabled(lm, LocationManager.NETWORK_PROVIDER)) {
				lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0,
						0, locationListener);
			}

			Criteria criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_FINE);

			// Finds a provider that matches the criteria
			String provider = lm.getBestProvider(criteria, false);

			// Use the provider to get the last known location
			myLocation = lm.getLastKnownLocation(provider);
		}
		lm.removeUpdates(locationListener);
		return myLocation;
	}

	/**
	 * Adds a marker to the map
	 */
	public static void addDefaultMarker(GoogleMap googleMap, Location cLocation) {

		/** Make sure that the map has been initialised **/
		if (null != googleMap) {
			googleMap.addMarker(new MarkerOptions()
					.position(
							new LatLng(cLocation.getLatitude(), cLocation
									.getLongitude())).title("CurrentLocation")
					.draggable(true)

			);
		}
	}

	public Marker addCarMarker(GoogleMap googleMap, LatLng latlng) {
		Marker marker = null;
		if (null != googleMap) {
			marker = googleMap.addMarker(new MarkerOptions()
					.position(latlng)
					.title("You Parked Here")
					.draggable(false)
					.icon(BitmapDescriptorFactory
							.fromResource(R.drawable.ic_parked_car)));
			
		}
		return marker;
	}

	public static void addParkingLocations(final Activity activity,
			GoogleMap googleMap, String json, HashMap<Marker, Integer> markers)
			throws JSONException {

		JSONObject locations = new JSONObject(json);
		JSONArray parkingLocations = locations
				.getJSONArray(JSONKeys.PARKING_LOCATIONS);
		if (parkingLocations != null) {
			for (int i = 0; i < parkingLocations.length(); i++) {
				JSONObject parkingLocation = parkingLocations.getJSONObject(i);
				String id = parkingLocation.getString(JSONKeys.ID);
				String name = parkingLocation.getString(JSONKeys.NAME);
				Double latitude = parkingLocation.getDouble(JSONKeys.LATITUDE);
				Double longitude = parkingLocation
						.getDouble(JSONKeys.LONGITUDE);
				String address = parkingLocation.getString(JSONKeys.ADDRESS);
				String category = parkingLocation.getString(JSONKeys.CATEGORY);
				String distance = parkingLocation.getString(JSONKeys.DISTANCE);
				// add marker to the map
				if (null != googleMap) {
					MarkerOptions markerOptions = new MarkerOptions();
					markerOptions.position(new LatLng(latitude, longitude))
							.title(name)
							.snippet(address + "\n" + distance + " miles")
							.draggable(false);
					if (category != null && category.equals("PUBLIC")) {
						markerOptions
								.icon(BitmapDescriptorFactory
										.fromResource(R.drawable.ic_public_parking_marker));
					} else if (category != null && category.equals("PAID")) {
						markerOptions
								.icon(BitmapDescriptorFactory
										.fromResource(R.drawable.ic_paid_parking_marker));
					} else {
						markerOptions
								.icon(BitmapDescriptorFactory
										.fromResource(R.drawable.ic_unknown_parking_marker));
					}
					Marker m = googleMap.addMarker(markerOptions);
					markers.put(m, Integer.parseInt(id));
				}
			}
		} else {
			Toast.makeText(activity, "Failed to fetch data from server!",
					Toast.LENGTH_SHORT).show();
		}

		LayoutInflater inflater = (LayoutInflater) activity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		googleMap.setInfoWindowAdapter(new MarkerPopupCustom(inflater));

	}

	public static boolean isServicesEnabled(LocationManager locManager,
			String provider) {
		if (locManager.isProviderEnabled(provider)) {
			// GPS enabled
			return true;
		} else {
			// GPS disabled
			return false;
		}
	}

	public static double distance(double lat1, double lon1, double lat2,
			double lon2, char unit) {
		double theta = lon1 - lon2;
		double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2))
				+ Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2))
				* Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515;
		if (unit == 'K') {
			dist = dist * 1.609344;
		} else if (unit == 'N') {
			dist = dist * 0.8684;
		}
		return (round(dist, 2));
	}

	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	/* :: This function converts decimal degrees to radians : */
	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	private static double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
	}

	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	/* :: This function converts radians to decimal degrees : */
	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	private static double rad2deg(double rad) {
		return (rad * 180.0 / Math.PI);
	}

	public static double round(double value, int places) {
		if (places < 0)
			throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	public void navigateTo(Activity activity, Double latitude,
			Double longitude, boolean isDriving) {
		Location location = getMyLocation(true);
		if (location == null) {
			Toast.makeText(
					activity,
					"Failed to estimate current location.\nPlease try again in few secs.",
					Toast.LENGTH_SHORT).show();
		} else {
			StringBuffer sb = new StringBuffer();
			sb.append("http://maps.google.com/maps?saddr=")
					.append(location.getLatitude()).append(",")
					.append(location.getLongitude()).append("&daddr=")
					.append(latitude).append(",").append(longitude);
			if (!isDriving) {
				sb.append("&mode=walking");
			}
			Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
					Uri.parse(sb.toString()));
			activity.startActivity(intent);
		}
	}
		
	// check Internet conenction.
	public boolean checkInternetConenction(Context context) {
		ConnectivityManager check = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (check != null) {
			NetworkInfo[] info = check.getAllNetworkInfo();
			if (info != null)
				for (int i = 0; i < info.length; i++)
					if (info[i].getState() == NetworkInfo.State.CONNECTED) {
						Toast.makeText(context, "Internet is connected",
								Toast.LENGTH_SHORT).show();
						return true;
					}

		} else {
			Toast.makeText(context, "not conencted to internet",
					Toast.LENGTH_SHORT).show();
		}
		return false;
	}
	public static void startLocationChangeService(Context context) {
		boolean isMyServiceRunning = isMyServiceRunning(LocationChangeService.class,context);
		if (!isMyServiceRunning) {			
			context.startService(new Intent(context, LocationChangeService.class));
		}
	}
	
	public static void stopLocationChangeService(Context context) {
		boolean isMyServiceRunning = isMyServiceRunning(LocationChangeService.class,context);
		if (isMyServiceRunning) {			
			context.stopService(new Intent(context, LocationChangeService.class));
		}
	}
	public static boolean isMyServiceRunning(Class<?> serviceClass,Context context) {
	    ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (serviceClass.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
	
	public static Float getRadius (GoogleMap map) {
		
		VisibleRegion vr = map.getProjection().getVisibleRegion();
		double left = vr.latLngBounds.southwest.longitude;
		double top = vr.latLngBounds.northeast.latitude;
		double right = vr.latLngBounds.northeast.longitude;
		
		Location topMiddleCorner = new Location("topMiddleCorner");
		topMiddleCorner.setLatitude(top);
		topMiddleCorner.setLongitude((left + right)/2.0);
		Location center = new Location("center");
		center.setLatitude( vr.latLngBounds.getCenter().latitude);
		center.setLongitude( vr.latLngBounds.getCenter().longitude);
		
		return center.distanceTo(topMiddleCorner);

	}

}
