package com.parkmycar;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.parkmycar.json.JSONKeys;

public class MainActivity extends ActionBarActivity {

	GoogleMap googleMap;
	Location currentLocation;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		String address = null;
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			address = intent.getStringExtra(SearchManager.QUERY);
		}		
		setContentView(R.layout.activity_main);

		googleMap = ((MapFragment) getFragmentManager().findFragmentById(
				R.id.map)).getMap();

		if (googleMap == null) {
			Toast.makeText(this, "Error in Creation", Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		// Get location from GPS if it's available
		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		if (!LocationUtils.isServicesEnabled(lm, LocationManager.GPS_PROVIDER)) {
			// Provider not enabled, prompt user to enable it
			Toast.makeText(this, R.string.please_turn_on_gps, Toast.LENGTH_LONG)
					.show();
			Intent myIntent = new Intent(
					Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			startActivity(myIntent);
		}

		else {
			googleMap.setMyLocationEnabled(true);
			currentLocation = LocationUtils.getMyLocation(lm, locationListener);
			if (currentLocation != null) {
				LatLng currentCoordinates = new LatLng(
						currentLocation.getLatitude(),
						currentLocation.getLongitude());
				googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
						currentCoordinates, 20));
			}			
			GetParkingLocations getPL = new GetParkingLocations();
			if (address != null && !address.isEmpty()) {
				getPL.execute(address);
			}
			else if (currentLocation != null) {
				getPL.execute(currentLocation.getLatitude(), 
						currentLocation.getLongitude());
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);

		// Associate searchable configuration with the SearchView
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		SearchView searchView = (SearchView) menu.findItem(R.id.search)
				.getActionView();

		searchView.setSearchableInfo(searchManager
				.getSearchableInfo(getComponentName()));
		return true;
	}

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

	/**
	 * Async task to fetch parking locations from DB
	 * 
	 */
	private class GetParkingLocations extends AsyncTask<Object, Void, Void> {

		@Override
		protected Void doInBackground(Object ... params) {
			
			HttpClient httpClient = new DefaultHttpClient();			
			HttpPost httpPost = new HttpPost(ServerUtils.getFullUrl(ServerUtils.PARKING_LOCATIONS_CPATH));			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			String address = (String) params[0];
			if (address != null 
					&& !address.isEmpty())
			{
				nameValuePairs.add(new BasicNameValuePair(JSONKeys.ADDRESS ,address));
				System.out.print("Address: " + address);
			} 
			else 
			{
				Double latitude = (Double) params[1];
				Double longitude = (Double) params[2];
				nameValuePairs.add(new BasicNameValuePair(JSONKeys.LATITUDE, latitude.toString()));
				nameValuePairs.add(new BasicNameValuePair(JSONKeys.LONGITUDE, longitude.toString()));
				System.out.print("Latitude: " + latitude + ", Longitude: " + longitude);
			}	
			
			try
			{
				//add data
				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				// Execute HTTP Post Request
				HttpResponse response = httpClient.execute(httpPost);	
				
				if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
					InputStream in = response.getEntity().getContent();					
					byte [] bytes = IOUtils.toByteArray(in);				
					
//					String sampleJson = "{\"parkingLocations\": [{\"latitude\": 32.9214519, \"longitude\": -117.1681123, \"name\": \"Calle Cristobel\", \"address\":\"Park Road, CalleCristobel, 92126\"}, "
//							+ "									 {\"latitude\": 32.9223165, \"longitude\": -117.1441226, \"name\": \"Edwards, Mira Mesa\", \"address\":\"Mira Mesa Road, Mira Mesa, 92126\"}]}";

					LocationUtils.addParkingLocations(getParent(), googleMap, new String(bytes));
//					LocationUtils.addParkingLocations(googleMap, sampleJson);
				} else {
					Toast.makeText(getParent(), "Failed to fetch data from server!",
							Toast.LENGTH_SHORT).show();
				}
					        
			}
			catch (ClientProtocolException e) {
				Toast.makeText(getParent(), "Failed to fetch data from server!",
						Toast.LENGTH_SHORT).show();
		    } catch (JSONException e) {
				Toast.makeText(getParent(), "Failed to parse server response.",
						Toast.LENGTH_SHORT).show();
			} catch (IOException e) {
				Toast.makeText(getParent(), "Failed to fetch data from server!",
						Toast.LENGTH_SHORT).show();
		    }
		    
			return null;
		}
	}
}
