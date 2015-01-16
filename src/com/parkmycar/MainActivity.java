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
import com.google.android.gms.maps.model.MarkerOptions;



public class MainActivity extends ActionBarActivity {

	GoogleMap googleMap;
	 @Override
	 protected void onCreate(Bundle savedInstanceState)
	 {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        
        googleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
		
        if (googleMap == null) 
        {
            Toast.makeText(this,"Error in Creation", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        googleMap.setMyLocationEnabled(true);
        
        Location currentLocation = LocationUtils.getMyLocation(this);
        
        if(currentLocation!=null)
        {
           LatLng currentCoordinates = new LatLng( currentLocation.getLatitude(),currentLocation.getLongitude());
           googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentCoordinates, 10));
        }
        
        LocationUtils.addCarMarker(googleMap,currentLocation);
	 }
	
	
}
