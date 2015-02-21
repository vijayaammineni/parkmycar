package com.parkmycar;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

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



//github.com/vijayaammineni/parkmycar.git
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.parkmycar.json.JSONKeys;

public class MainActivity extends ActionBarActivity {

	GoogleMap googleMap;
	Location currentLocation;

	private PendingIntent pendingIntent;

	public static boolean isAddress = false;

	public static String CURRENT_LOCATION = "My Location";
	
	public static HashMap<Marker, Integer> gMapMarkers = new HashMap<Marker, Integer>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		String address = null;

		setContentView(R.layout.activity_main);

		googleMap = ((MapFragment) getFragmentManager().findFragmentById(
				R.id.map)).getMap();

		if (googleMap == null) {
			Toast.makeText(this, "Error in loading Google Maps.", Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		LocationUtils lu = new LocationUtils(this, getApplicationContext());
        
		currentLocation = lu.getMyLocation(false);
		if (currentLocation != null) {
			googleMap.setMyLocationEnabled(true);
			LatLng currentCoordinates = new LatLng(
					currentLocation.getLatitude(),
					currentLocation.getLongitude());
			googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
					currentCoordinates, LocationUtils.DEFAULT_ZOOM_LEVEL));
		} else {
			googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
					new LatLng(LocationUtils.DEFAULT_LATITUDE, LocationUtils.DEFAULT_LONGITUDE), 
					LocationUtils.DEFAULT_ZOOM_LEVEL));
		}

//		Button button = new Button(this);
//		button.setText("Click me");
//		addContentView(button, new LayoutParams(LayoutParams.WRAP_CONTENT,
//				LayoutParams.WRAP_CONTENT));
//		// attach a clik listener to click me button
//		button.setOnClickListener(new View.OnClickListener() {
//
//			@Override
//			public void onClick(View v) {
//				// TODO Auto-generated method stub
//				Intent detailsIntent = new Intent(MainActivity.this,
//						DisplayDetailsActivity.class);
//				detailsIntent.putExtra("id", 3);
//				startActivity(detailsIntent);
//
//			}
//		});

		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			address = intent.getStringExtra(SearchManager.QUERY);
			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
					this, SearchSuggestionProvider.AUTHORITY,
					SearchSuggestionProvider.MODE);
			suggestions.saveRecentQuery(address, null);
			GetParkingLocations getPL = new GetParkingLocations(this);
			if (address != null 
					&& !address.isEmpty()) {
				if (CURRENT_LOCATION.equalsIgnoreCase(address)) {
					currentLocation = lu.getMyLocation(true);
					if (currentLocation != null) {
						getPL.execute(currentLocation.getLatitude(),
								currentLocation.getLongitude());
					}					
				} else {
					getPL.execute(address);
					isAddress = true;						
				}
			}
		}
		
		//add marker info window click event for all the markers
		googleMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {						
			@Override
			public void onInfoWindowClick(Marker marker) {
				Intent myIntent = new Intent(MainActivity.this,
						DisplayDetailsActivity.class);
				Integer parkingLocationId = gMapMarkers.get(marker);
				myIntent.putExtra(com.parkmycar.Constants.PARKING_LOCATION_ID, parkingLocationId); 
				startActivity(myIntent);
			}
		});
		
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.startReminder:
			startNotificationAlarm();
			return true;
		case R.id.cancelReminder:
			stopNotificationAlarm();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	// @author: Bhavya
	public void startNotificationAlarm() {
		AlertDialog.Builder builderSingle = new AlertDialog.Builder(
				MainActivity.this);
		builderSingle.setIcon(R.drawable.ic_launcher);
		builderSingle.setTitle("Remind me in:");
		final String[] names = { "1 minute", "10 minutes", "20 minutes",
				"25 minutes", "40 minutes", "55 minutes", "1 hour",
				"1 hour 15 minutes", "1 hour 25 minutes", "1 hour 40 minutes",
				"1 hour 55 minutes", "2 hours 10 minutes",
				"2 hours 25 minutes", "2 hours 40 minutes",
				"2 hours 55 minutes" };
		final int[] timings = { 1, 10, 20, 25, 40, 55, 60, 76, 85, 100, 115,
				130, 145, 160, 185 };
		final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
				MainActivity.this, android.R.layout.select_dialog_item, names);

		builderSingle.setAdapter(arrayAdapter,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						// Intent to start the background service when alarm is
						// set
						Intent notificationIntent = new Intent(
								MainActivity.this, NotificationService.class);
						pendingIntent = PendingIntent.getService(
								MainActivity.this, 0, notificationIntent, 0);
						AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
						Calendar calendar = Calendar.getInstance();
						calendar.setTimeInMillis(System.currentTimeMillis());
						calendar.add(Calendar.MINUTE, timings[which]);
						alarmManager.set(AlarmManager.RTC_WAKEUP,
								calendar.getTimeInMillis(), pendingIntent);

						Toast.makeText(MainActivity.this,
								"Reminder Set Successful!", Toast.LENGTH_SHORT)
								.show();
						dialog.dismiss();

						// alert the user with another notification, that says
						// remider has set

						// Set the icon, scrolling text and timestamp
						Uri soundUri = RingtoneManager
								.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

						// The PendingIntent to launch our activity if the user
						// selects this notification
						PendingIntent contentIntent = PendingIntent
								.getActivity(MainActivity.this, 0, new Intent(
										MainActivity.this, MainActivity.class),
										0);

						Notification notification = new Notification.Builder(
								MainActivity.this)
								.setContentTitle("ParkMyCar")
								.setContentText(
										"Reminder is set to: "
												+ calendar.get(Calendar.HOUR)
												+ ":"
												+ calendar.get(Calendar.MINUTE)
												+ " "
												+ calendar.getDisplayName(
														Calendar.AM_PM,
														Calendar.SHORT,
														Locale.US))
								.setWhen(System.currentTimeMillis())
								.setSound(soundUri)
								.setSmallIcon(R.drawable.ic_launcher)
								.setContentIntent(contentIntent)
								.setVibrate(
										new long[] { 1000, 1000, 1000, 1000,
												1000 })
								.setLights(Color.RED, 3000, 3000).build();

						// clear notification after pressing:
						notification.flags |= Notification.FLAG_AUTO_CANCEL;

						// Send the notification.

						NotificationManager mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
						if (mNM != null) {
							mNM.cancel(123456);
							mNM.cancel(123457);
							mNM.notify(123457, notification);
						}
					}
				});
		builderSingle.show();

	}

	// @author: Bhavya
	public void stopNotificationAlarm() {
		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		// cancel all the alarms associated with pendingIntent
		alarmManager.cancel(pendingIntent);
		NotificationManager mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if (mNM != null) {
			mNM.cancel(123456);
			mNM.cancel(123457);
		}
		Toast.makeText(MainActivity.this, "Reminder deleted",
				Toast.LENGTH_SHORT).show();

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
		searchView.setQuery(CURRENT_LOCATION, false);
		return true;
	}

	/**
	 * Async task to fetch parking locations from DB
	 * 
	 */
	private class GetParkingLocations extends
			AsyncTask<Object, Void, FetchParkingLocationsResult<byte[]>> {
		private Context context;
		HttpClient httpClient = new DefaultHttpClient();

		public GetParkingLocations(Context context) {
			this.context = context;

		}

		// check Internet conenction.
		private void checkInternetConenction() {
			ConnectivityManager check = (ConnectivityManager) this.context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (check != null) {
				NetworkInfo[] info = check.getAllNetworkInfo();
				if (info != null)
					for (int i = 0; i < info.length; i++)
						if (info[i].getState() == NetworkInfo.State.CONNECTED) {
							Toast.makeText(context, "Internet is connected",
									Toast.LENGTH_SHORT).show();
						}

			} else {
				Toast.makeText(context, "not conencted to internet",
						Toast.LENGTH_SHORT).show();
			}
		}

		protected void onPreExecute() {
			checkInternetConenction();
		}

		@Override
		protected FetchParkingLocationsResult<byte[]> doInBackground(Object... params) {
			Double latitude = null;
			Double longitude = null;
			HttpPost httpPost = new HttpPost(
					ServerUtils.getFullUrl(ServerUtils.PARKING_LOCATIONS_CPATH));
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			byte[] bytes = null;
			try {

				if (isAddress) {
					byte[] addressBytes = null;
					StringBuilder url = new StringBuilder(
							"http://maps.googleapis.com/maps/api/geocode/json?address=");
					String addressObj = (String) params[0];
					url.append(URLEncoder.encode(addressObj, "UTF-8")
							+ "&sensor=false");
					HttpResponse pageResp = httpClient.execute(new HttpGet(url
							.toString()));
					if (pageResp.getStatusLine().getStatusCode()
							!= HttpStatus.SC_OK) {
						return new FetchParkingLocationsResult<byte[]>(
								new InvalidAddressException(
										"Failed to resolve this address. Google Maps API error."));
					}
					System.out.print("Address: " + addressObj);
					InputStream inAddress = pageResp.getEntity().getContent();
					addressBytes = IOUtils.toByteArray(inAddress);
					String jsonAddressStr = new String(addressBytes);
					JSONObject addresses = new JSONObject(jsonAddressStr);
					JSONArray results = addresses
							.getJSONArray(JSONKeys.RESULTS);
					String status = addresses.getString(JSONKeys.STATUS);
					if (!status.equalsIgnoreCase("OK") || results.length() > 1) {
						return new FetchParkingLocationsResult<byte[]>(
								new InvalidAddressException(
										"Please specify a valid address."));
					}
					if (results != null && results.length() > 0){
						for (int i = 0; i < results.length(); i++) {
							JSONObject result = results.getJSONObject(i);
							JSONObject geometry = result
									.getJSONObject(JSONKeys.GEOMETRY);
							if (geometry != null) {
								JSONObject location = geometry
										.getJSONObject(JSONKeys.LOCATION);
								if (location != null) {
									longitude = location
											.getDouble(JSONKeys.LNG_GEOCODE);
									latitude = location
											.getDouble(JSONKeys.LAT_GEOCODE);
								}

							}
						}
					} else {
						return new FetchParkingLocationsResult<byte[]>(
								new InvalidAddressException(
										"Could not find this address. Please modify the address and try again."));
					}

				} else {
					latitude = (Double) params[0];
					longitude = (Double) params[1];
					System.out.print("Latitude: " + latitude + ", Longitude: "
							+ longitude);
				}

				nameValuePairs.add(new BasicNameValuePair(JSONKeys.LATITUDE,
						latitude.toString()));
				nameValuePairs.add(new BasicNameValuePair(JSONKeys.LONGITUDE,
						longitude.toString()));

				// add data

				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				// Execute HTTP Post Request
				HttpResponse response = httpClient.execute(httpPost);
				if (HttpStatus.SC_OK == response.getStatusLine()
						.getStatusCode()) {
					InputStream in = response.getEntity().getContent();
					bytes = IOUtils.toByteArray(in);
					FetchParkingLocationsResult<byte[]> fplr = new FetchParkingLocationsResult<byte[]>(bytes);
					fplr.setLatitude(latitude);
					fplr.setLongitude(longitude);
					return fplr;
				} else {
					return new FetchParkingLocationsResult<byte[]>(
							"Server error while fetching the parking locations!");
				}
			}

			catch (Exception e) {
				System.out.print(e.getMessage());
				return new FetchParkingLocationsResult<byte[]>(
						"Failed to fetch data from server!");
			}

		}

		@Override
		protected void onPostExecute(FetchParkingLocationsResult<byte[]> result) {
			try {
				if (result.getMessage() != null || result.getError() != null)

				{
					AlertDialog.Builder builder = new AlertDialog.Builder(
							this.context);
					builder.setIcon(android.R.drawable.ic_dialog_alert);
					builder.setTitle("Failure");
					builder.setMessage(result.getMessage());
					builder.setPositiveButton("OK", null);
					builder.show();

					/*
					 * // Must call show() prior to fetching text view TextView
					 * messageView = (TextView) dialog
					 * .findViewById(android.R.id.message);
					 * messageView.setGravity(Gravity.CENTER);
					 */
					return;
				}
				if (result != null) {
					//clear the previous results
					googleMap.clear();
					gMapMarkers.clear();
					//add the new results to the map
					LocationUtils.addParkingLocations((Activity) context,
							googleMap, new String(result.getResult()), gMapMarkers);
					//move the google maps camera to the search address location
					googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
							new LatLng(result.getLatitude(), result.getLongitude()), 
							LocationUtils.DEFAULT_ZOOM_LEVEL));					
				}

			} catch (JSONException e) {
				Toast.makeText(getParent(), "Failed to parse server response.",
						Toast.LENGTH_SHORT).show();

			}
		}
	}
}
