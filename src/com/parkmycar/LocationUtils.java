package com.parkmycar;

import java.util.BitSet;

import com.google.android.gms.internal.gg;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.app.Activity;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;

public class LocationUtils 
{

	 public static Location getMyLocation(Activity activity) 
	 {
		    // Get location from GPS if it's available
		    LocationManager lm = (LocationManager)activity.getSystemService(Context.LOCATION_SERVICE);
		    Location myLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

		    // Location wasn't found, check the next most accurate place for the current location
		    if (myLocation == null) 
		    {
		        Criteria criteria = new Criteria();
		        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
		        // Finds a provider that matches the criteria
		        String provider = lm.getBestProvider(criteria, true);
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
}
