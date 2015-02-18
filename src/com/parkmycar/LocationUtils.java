package com.parkmycar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parkmycar.json.JSONKeys;

public class LocationUtils {
	
	public static Double DEFAULT_LATITUDE = 32.7150;
	public static Double DEFAULT_LONGITUDE = -117.1625;
	public static int DEFAULT_ZOOM_LEVEL = 14;
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

	public static void addCarMarker(GoogleMap googleMap, Location cLocation) {

		/** Make sure that the map has been initialised **/
		if (null != googleMap) {
			googleMap.addMarker(new MarkerOptions()
					.position(
							new LatLng(cLocation.getLatitude(), cLocation
									.getLongitude()))
					.title("Car Location")
					.draggable(false)
					.icon(BitmapDescriptorFactory
							.fromResource(R.drawable.ic_car))

			);
		}
	}

	public static void addParkingLocations(final Activity activity,
			GoogleMap googleMap, String json) throws JSONException {
		JSONObject locations = new JSONObject(json);
		JSONArray parkingLocations = locations
				.getJSONArray(JSONKeys.PARKING_LOCATIONS);

		if (parkingLocations != null) {
			for (int i = 0; i < parkingLocations.length(); i++) {
				JSONObject parkingLocation = parkingLocations.getJSONObject(i);
				String name = parkingLocation.getString(JSONKeys.NAME);
				Double latitude = parkingLocation.getDouble(JSONKeys.LATITUDE);
				Double longitude = parkingLocation
						.getDouble(JSONKeys.LONGITUDE);
				String address = parkingLocation.getString(JSONKeys.ADDRESS);
				String category = parkingLocation.getString(JSONKeys.CATEGORY);
				
				// add marker to the map
				if (null != googleMap) {
					googleMap.addMarker(new MarkerOptions()
							.position(new LatLng(latitude, longitude))
							.title(name).snippet(address).draggable(true)

					);

				}
			}

		} else {
			Toast.makeText(activity, "Failed to fetch data from server!",
					Toast.LENGTH_SHORT).show();
		}

		LayoutInflater inflater = (LayoutInflater) activity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		googleMap.setInfoWindowAdapter(new MarkerPopupCustom(inflater));

		ViewGroup infoWindow = (ViewGroup) inflater.inflate(R.layout.popup,
				null);

		Button infoBtn = (Button) infoWindow.findViewById(R.id.button1);

		infoBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent myIntent = new Intent(activity,
						DisplayDetailsActivity.class);
				myIntent.putExtra("key", 1); // Optional parameters
				activity.startActivity(myIntent);

			}
		});

	}

	public static boolean isServicesEnabled(LocationManager locManager,
			String provider) {
		if (locManager.isProviderEnabled(provider)) {
			// GPS enabled
			return true;
		}
		else {
			// GPS disabled
			return false;
		}
	}

}
