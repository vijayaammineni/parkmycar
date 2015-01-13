package com.parkmycar;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;



public class MainActivity extends ActionBarActivity {

	 @Override
	 protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GoogleMap mMap;
		mMap = ((MapFragment) getFragmentManager().
        		findFragmentById(R.id.map)).getMap();
        //invoke of map fragment by id from main xml file
        if (mMap == null) {
            Toast.makeText(this,"Error in Creation", Toast.LENGTH_LONG)
                    .show();
            finish();
            return;
        }
        mMap.setMyLocationEnabled(true);
        Location currentLocation = getMyLocation();
        if(currentLocation!=null){
           LatLng currentCoordinates = new LatLng(
                                currentLocation.getLatitude(),
                                currentLocation.getLongitude());
           mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentCoordinates, 10));
        }
	 }
	 private Location getMyLocation() {
		    // Get location from GPS if it's available
		    LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		    Location myLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

		    // Location wasn't found, check the next most accurate place for the current location
		    if (myLocation == null) {
		        Criteria criteria = new Criteria();
		        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
		        // Finds a provider that matches the criteria
		        String provider = lm.getBestProvider(criteria, true);
		        // Use the provider to get the last known location
		        myLocation = lm.getLastKnownLocation(provider);
		    }

		    return myLocation;
		}
}
