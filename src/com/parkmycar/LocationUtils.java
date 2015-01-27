package com.parkmycar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parkmycar.json.JSONKeys;

public class LocationUtils 
{

	 public static Location getMyLocation(LocationManager lm,LocationListener listner) 
	 {
		 	Location myLocation = null;
		   /* // Get location from GPS if it's available
		    Location myLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
*/
		    // Location wasn't found, check the next most accurate place for the current location
		    if (myLocation == null) 
		    {
		    	if (isServicesEnabled(lm, LocationManager.GPS_PROVIDER)) { 
		            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,listner);
		        } 
		        if (isServicesEnabled(lm, LocationManager.NETWORK_PROVIDER)) { 
		        	lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listner);
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
	 public static void addDefaultMarker(GoogleMap googleMap, Location cLocation)
	 {

	     /** Make sure that the map has been initialised **/
	     if(null != googleMap){
	         googleMap.addMarker(new MarkerOptions()
	                             .position(new LatLng(cLocation.getLatitude(),cLocation.getLongitude()))
	                             .title("CurrentLocation")
	                             .draggable(true)
	                             
	         );
	     }
	 }
	 
	 public static void addCarMarker(GoogleMap googleMap, Location cLocation)
	 {

	     /** Make sure that the map has been initialised **/
	     if(null != googleMap){
	         googleMap.addMarker(new MarkerOptions()
	                             .position(new LatLng(cLocation.getLatitude(),cLocation.getLongitude()))
	                             .title("Car Location")
	                             .draggable(false)
	                             .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car))
	                             
	         );
	     }
	 }
	 
	 public static void addParkingLocations(Activity activity, GoogleMap googleMap, String json) throws JSONException
	 {
		 JSONObject locations = new JSONObject(json);
		 JSONArray parkingLocations = locations.getJSONArray(JSONKeys.PARKING_LOCATIONS);
		 
		 if (parkingLocations != null) {
			 for (int i = 0; i < parkingLocations.length(); i++) {
				 JSONObject parkingLocation = parkingLocations.getJSONObject(i);
				 String name = parkingLocation.getString(JSONKeys.NAME);
				 Double latitude = parkingLocation.getDouble(JSONKeys.LATITUDE);
				 Double longitude = parkingLocation.getDouble(JSONKeys.LONGITUDE);
				 String address = parkingLocation.getString(JSONKeys.ADDRESS);
				 //add marker to the map
				 if(null != googleMap){
			         googleMap.addMarker(new MarkerOptions()
			                             .position(new LatLng(latitude, longitude))
			                             .title(name + ", " + "Address: " + address)
			                             .draggable(true)
			                             
			         );
			     }
			 }
			 
		 } else {
			 Toast.makeText(activity, "Failed to fetch data from server!",
						Toast.LENGTH_SHORT).show();
		 }
		 
		 
	 }
	 
	 public static boolean isServicesEnabled(LocationManager locManager,String provider)
	 {  

		         if (locManager.isProviderEnabled(provider))
		         {
		            //GPS enabled
		            return true;
		         }
		            
		         else
		         {
		            //GPS disabled
		            return false;
		         }
		 }       
	
}
