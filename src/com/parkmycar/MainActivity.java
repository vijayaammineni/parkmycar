package com.parkmycar;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
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
	public static boolean isAddress = false;
	
	public static String CURRENT_LOCATION = "My Location";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		String address = null;
				
		setContentView(R.layout.activity_main);

		googleMap = ((MapFragment) getFragmentManager().findFragmentById(
				R.id.map)).getMap();

		if (googleMap == null) {
			Toast.makeText(this, "Error in Creation", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		
		LocationUtils lu = new LocationUtils(this, getApplicationContext());
		
		googleMap.setMyLocationEnabled(true);
		currentLocation = lu.getMyLocation();
		if (currentLocation != null) {
			LatLng currentCoordinates = new LatLng(
					currentLocation.getLatitude(),
					currentLocation.getLongitude());
			googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
					currentCoordinates, 12));
		}

		Button button = new Button(this);
		button.setText("Click me");
		addContentView(button, new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT));

		button.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent detailsIntent = new Intent(MainActivity.this,
						DisplayDetailsActivity.class);
				detailsIntent.putExtra("id", 3);
				startActivity(detailsIntent);

			}
		});
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			address = intent.getStringExtra(SearchManager.QUERY);
			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
					this, SearchSuggestionProvider.AUTHORITY,
					SearchSuggestionProvider.MODE);
			suggestions.saveRecentQuery(address, null);
			GetParkingLocations getPL = new GetParkingLocations(this);
			if (address != null && !address.isEmpty()
							&& !CURRENT_LOCATION.equals(address)) {
					getPL.execute(address);
					isAddress = true;
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
		searchView.setQuery(CURRENT_LOCATION,false);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
	 return super.onOptionsItemSelected(item);
	}
	/**
	 * Async task to fetch parking locations from DB
	 * 
	 */
	private class GetParkingLocations extends AsyncTask<Object, Void, AsyncTaskResult<byte[]>> 
	{
		private Context context;
		HttpClient httpClient = new DefaultHttpClient();
		public GetParkingLocations(Context context)
		{
			this.context = context;
			
		}
		//check Internet conenction.
		   private void checkInternetConenction()
		   {
		      ConnectivityManager check = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
		      if (check != null) 
		      {
		         NetworkInfo[] info = check.getAllNetworkInfo();
		         if (info != null) 
		            for (int i = 0; i <info.length; i++) 
		            if (info[i].getState() == NetworkInfo.State.CONNECTED)
		            {
		               Toast.makeText(context, "Internet is connected",
		               Toast.LENGTH_SHORT).show();
		            }

		      }
		      else{
		         Toast.makeText(context, "not conencted to internet",
		         Toast.LENGTH_SHORT).show();
		          }
		   }
		   protected void onPreExecute()
		   {
		      checkInternetConenction();
		   }

		@Override
		protected AsyncTaskResult<byte[]> doInBackground(Object ... params)
		{
			Double latitude = 90.0;
			Double longitude = 180.0;
			
			HttpPost httpPost = new HttpPost(ServerUtils.getFullUrl(ServerUtils.PARKING_LOCATIONS_CPATH));			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			byte [] bytes = null;
			try{
				
			if (isAddress)
			{
				byte[] addressBytes = null;
				StringBuilder url = new StringBuilder("http://maps.googleapis.com/maps/api/geocode/json?address=");
				String addressObj = (String)params[0];
				url.append(URLEncoder.encode(addressObj,"UTF-8")+"+&sensor=true_or_false");
				HttpResponse pageResp = httpClient.execute(new HttpGet(url.toString()));
				System.out.print("Address: " + addressObj);
				InputStream inAddress = pageResp.getEntity().getContent();					
				addressBytes = IOUtils.toByteArray(inAddress);	
				String jsonAddressStr = new String(addressBytes);
				JSONObject addresses = new JSONObject(jsonAddressStr);
				JSONArray results = addresses.getJSONArray(JSONKeys.RESULTS);
				String status = addresses.getString(JSONKeys.STATUS);
				if(!status.equalsIgnoreCase("OK") || results.length() > 1)
				{
					return new AsyncTaskResult<byte[]>(new InvalidAddressException("Please specify valid address."));	
				}
				if(results != null)
				{
					for(int i=0;i<results.length();i++)
					{
						JSONObject geometry = results.getJSONObject(i);
						JSONObject geometryAray = geometry.getJSONObject(JSONKeys.GEOMETRY);  
						if(geometryAray != null)
						{
							
								JSONObject location = geometryAray.getJSONObject(JSONKeys.LOCATION);
								longitude = location.getDouble(JSONKeys.LNG_GEOCODE);
								latitude = location.getDouble(JSONKeys.LAT_GEOCODE);
							
						}
					}
				}
				
			
			}
			else 
			{
				latitude = (Double) params[0];
				longitude = (Double) params[1];
				System.out.print("Latitude: " + latitude + ", Longitude: " + longitude);
			}	
			
		
				nameValuePairs.add(new BasicNameValuePair(JSONKeys.LATITUDE, latitude.toString()));
				nameValuePairs.add(new BasicNameValuePair(JSONKeys.LONGITUDE, longitude.toString()));
			
				//add data
		
				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				// Execute HTTP Post Request
				HttpResponse response = httpClient.execute(httpPost);	
								
				if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) 
				{
					InputStream in = response.getEntity().getContent();					
					bytes = IOUtils.toByteArray(in);				
					
				}
				return new AsyncTaskResult<byte[]>(bytes);
			}
			      
			catch (Exception e)
			{
				System.out.print(e.getMessage());
				return new AsyncTaskResult<byte[]>("Failed to fetch data from server!");
				}
		    
			
		}
		
		@Override
		protected void onPostExecute(AsyncTaskResult<byte[]> result)
		{
			try
			{
				 if ( result.getMessage()!= null || result.getError()!= null)

				 {
					AlertDialog.Builder builder = new AlertDialog.Builder(
							this.context);
					builder.setTitle("Info");
					builder.setMessage(result.getMessage());
					builder.setPositiveButton("OK", null);
					AlertDialog dialog = builder.show();

					/*// Must call show() prior to fetching text view
					TextView messageView = (TextView) dialog
							.findViewById(android.R.id.message);
					messageView.setGravity(Gravity.CENTER);
					*/ 
					return;
		            }
				if(result != null)
				{
					LocationUtils.addParkingLocations((Activity)context, googleMap, new String(result.getResult()));
				}
				
			}
			catch (JSONException e) {
				Toast.makeText(getParent(), "Failed to parse server response.",
						Toast.LENGTH_SHORT).show();
			
		}
	 }
  }
}
