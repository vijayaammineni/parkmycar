package com.parkmycar;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.SearchRecentSuggestions;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.parkmycar.json.JSONKeys;

public class MainActivity extends ActionBarActivity {

	static final String TAG = MainActivity.class.getSimpleName();
	
	static final String DISTANCE_TEXT = "%.2f miles";
	
	GoogleMap googleMap;
	Marker carMarker;
	Location currentLocation;

	private Timer t;
	private long TimeCounter = 0;
	final Handler timeHandler = new Handler();
	TextView timerText;
	TextView distanceText;
	
	private static float currentZoom = LocationUtils.DEFAULT_ZOOM_LEVEL;
	private static Float searchRadius = 3.0f;


	private PendingIntent pendingIntent;

	public static boolean isAddress = false;

	public static String CURRENT_LOCATION = "My Location";
	private static Integer defaultMaxNumSearchResults = 1000;


	public static HashMap<Marker, Integer> gMapMarkers = new HashMap<Marker, Integer>();
	SharedPreferences sharedPref;
	LinearLayout layout;
	Button saveButton;
	boolean isSaveRequest = false;
	int pId;
	String pName;

	private LocationUtils lu;
	
	private AsyncTaskUtils atUtils;
	
	boolean isLocationStored = false;
	
	boolean isNavigatingToParkingLocation = false;

	//Broadcast receiver for receiving the location change events
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();
			if (LocationUtils.LOCATION_CHANGE_BROADCAST_ACTION.equals(intent.getAction())
					&& bundle != null) {
				Double latitude = bundle.getDouble(JSONKeys.LATITUDE);
				Double longitude = bundle.getDouble(JSONKeys.LONGITUDE);
				//if the user has saved the parked location then we need to update the distance 
				//from of the parked location from his current location
				if (isLocationStored) {
					Double carLocationLat = Double.longBitsToDouble(sharedPref.getLong(
							getString(R.string.Stored_Location_Latitude), 0));
					Double carLocationLng = Double.longBitsToDouble(sharedPref.getLong(
							getString(R.string.Stored_Location_Longitude), 0));	
					double dist = LocationUtils.distance(carLocationLat, 
							carLocationLng, latitude, longitude, 'M');
					distanceText.setText(String.format(DISTANCE_TEXT, dist));
				}	
				// if the user is navigating to a parking location then check 
				// to see the user reached the parking location, if so ask for feedback
				else if (isNavigatingToParkingLocation) {					
					Double destLocationLat = Double.longBitsToDouble(sharedPref.getLong(
							getString(R.string.Destination_Location_Latitude), 0));
					Double destLocationLng = Double.longBitsToDouble(sharedPref.getLong(
							getString(R.string.Destination_Location_Longitude), 0));	
					Integer destPLId = sharedPref.getInt(
							getString(R.string.Destination_Parking_Location_Id), 0);	
					String destPLName = sharedPref.getString(
							getString(R.string.Destination_Parking_Location_Name), "Unknown");	
					
					Double distance = LocationUtils.distance(destLocationLat, 
							destLocationLng, latitude, longitude, 'M');
					if(distance.compareTo(LocationUtils.DEFAULT_PARKING_LOCATION_RADIUS) <= 0)
					{
						LocationUtils.stopLocationChangeService(context);
						atUtils.createParkedFeedbackPopup(destPLId, destPLName);
					}					
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		String address = null;
		sharedPref = getPreferences(MODE_PRIVATE);

		setContentView(R.layout.activity_main);

		lu = new LocationUtils(this, getApplicationContext());
		atUtils = new AsyncTaskUtils (this);
		
		// initializing GUI elements for displaying saved Location details
		layout = (LinearLayout) findViewById(R.id.afterSaveOptions);
		timerText = (TextView) findViewById(R.id.time_elapsed);
		distanceText = (TextView) findViewById(R.id.distance);
		saveButton = (Button) findViewById(R.id.my_button_save);

		googleMap = ((MapFragment) getFragmentManager().findFragmentById(
				R.id.map)).getMap();

		if (googleMap == null) {
			Toast.makeText(this, "Error in loading Google Maps.",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		googleMap.setMyLocationEnabled(true);
		currentLocation = lu.getMyLocation(false);

		// If last location is stored, need to show timer after application is
		// launched.
		isLocationStored = sharedPref.getBoolean(
				getString(R.string.Is_Location_Stored), false);
		
		isNavigatingToParkingLocation = sharedPref.getBoolean(
				getString(R.string.isNavigatingToParkingLocation), false);

		Double carLocationLat = null;
		Double carLocationLng = null;
		if (isLocationStored) {
			saveButton.setEnabled(false);
			layout.setVisibility(View.VISIBLE);
			long lastTimeStored = sharedPref.getLong(
					getString(R.string.Stored_Location_TimeInMs), 0);
			long timediff = System.currentTimeMillis() - lastTimeStored;
			TimeCounter = timediff / 1000;
			setTimer();
			carLocationLat = Double.longBitsToDouble(sharedPref.getLong(
					getString(R.string.Stored_Location_Latitude), 0));
			carLocationLng = Double.longBitsToDouble(sharedPref.getLong(
					getString(R.string.Stored_Location_Longitude), 0));
			LocationUtils.startLocationChangeService(this);
		}

		if (currentLocation != null) {
			googleMap.setMyLocationEnabled(true);
			LatLng currentCoordinates = new LatLng(
					currentLocation.getLatitude(),
					currentLocation.getLongitude());
			googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
					currentCoordinates, LocationUtils.DEFAULT_ZOOM_LEVEL));
			if (isLocationStored) {
				if (carMarker != null) {
					carMarker.remove();
				}
				carMarker = lu.addCarMarker(googleMap, new LatLng(
						carLocationLat, carLocationLng));
			}
		} else if (isLocationStored) {
			googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
					new LatLng(carLocationLat, carLocationLng),
					currentZoom));
			if (carMarker != null) {
				carMarker.remove();
			}
			carMarker = lu.addCarMarker(googleMap, new LatLng(carLocationLat,
					carLocationLng));
		} else {
			//googleMap.setMyLocationEnabled(true);
			googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
					new LatLng(LocationUtils.DEFAULT_LATITUDE,
							LocationUtils.DEFAULT_LONGITUDE),
							currentZoom));
		}

		
	
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			address = intent.getStringExtra(SearchManager.QUERY);
			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
					this, SearchSuggestionProvider.AUTHORITY,
					SearchSuggestionProvider.MODE);
			suggestions.saveRecentQuery(address, null);
			GetParkingLocations getPL = new GetParkingLocations(this);
			if (address != null && !address.isEmpty()) {
				if (CURRENT_LOCATION.equalsIgnoreCase(address)) {
					currentLocation = lu.getMyLocation(true);
					if (currentLocation != null) {
						getPL.execute(currentLocation.getLatitude(),
								currentLocation.getLongitude());
					}
					isAddress = false;
				} else {
					getPL.execute(address);
					isAddress = true;
				}
			}
		} else {
			GetParkingLocations getPL = new GetParkingLocations(this);
			if (currentLocation != null) {
				getPL.execute(currentLocation.getLatitude(),
						currentLocation.getLongitude());
			} else {
				getPL.execute(LocationUtils.DEFAULT_LATITUDE,
						LocationUtils.DEFAULT_LONGITUDE);
			}
		}

		// add marker info window click event for all the markers
		googleMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {
			@Override
			public void onInfoWindowClick(Marker marker) {
				Intent myIntent = new Intent(MainActivity.this,
						DisplayDetailsActivity.class);
				Integer parkingLocationId = gMapMarkers.get(marker);
				if (parkingLocationId != null) {
					myIntent.putExtra(com.parkmycar.Constants.PARKING_LOCATION_ID,
							parkingLocationId);
					startActivity(myIntent);
				}
			}
		});
		
		googleMap.setOnCameraChangeListener(new OnCameraChangeListener() {
		    @Override
		    public void onCameraChange(CameraPosition pos) {
		        if (pos.zoom != currentZoom){
		            currentZoom = pos.zoom;
		            searchRadius = LocationUtils.getRadius(googleMap);
		        }
		    }
		});
	}
	
	

	@Override
    protected void onResume() {
        IntentFilter filter 
        	= new IntentFilter(LocationUtils.LOCATION_CHANGE_BROADCAST_ACTION);
        registerReceiver(broadcastReceiver, filter);
        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(broadcastReceiver);
        super.onPause();
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

	private void saveCurrentParkedLocationAsLastParked(
			SharedPreferences.Editor editor) {
		editor.putLong(getString(R.string.Last_Stored_Location_Longitude),
				Double.doubleToRawLongBits(sharedPref.getLong(
						getString(R.string.Stored_Location_Latitude), 0)));
		editor.putLong(getString(R.string.Last_Stored_Location_Longitude),
				Double.doubleToRawLongBits(sharedPref.getLong(
						getString(R.string.Stored_Location_Longitude), 0)));
		editor.putLong(getString(R.string.Last_Stored_Location_TimeInMs),
				sharedPref.getLong(
						getString(R.string.Stored_Location_TimeInMs), 0));
	}

	public void saveParkedLocation(View view) {

		if (currentLocation == null) {
			currentLocation = lu.getMyLocation(true);
		} else {
			carMarker = lu.addCarMarker(
					googleMap,
					new LatLng(currentLocation.getLatitude(), currentLocation
							.getLongitude()));
			TimeCounter = 0;
			layout.setVisibility(View.VISIBLE);

			SharedPreferences.Editor editor = sharedPref.edit();

			saveCurrentParkedLocationAsLastParked(editor);

			editor.putBoolean(getString(R.string.Is_Location_Stored), true);

			editor.putLong(getString(R.string.Stored_Location_Longitude),
					Double.doubleToRawLongBits(currentLocation.getLongitude()));
			editor.putLong(getString(R.string.Stored_Location_Latitude),
					Double.doubleToRawLongBits(currentLocation.getLatitude()));
			editor.putLong(getString(R.string.Stored_Location_TimeInMs),
					System.currentTimeMillis());
			editor.commit();
			setTimer();
			saveButton.setEnabled(false);
			//start the location change listener
			LocationUtils.startLocationChangeService(this);
			
			isAddress = false;
			isSaveRequest = true;
			GetParkingLocations getPL = new GetParkingLocations(this);
			getPL.execute(currentLocation.getLatitude(),
						currentLocation.getLongitude());
			
			//move the google map focus to the parked location
			googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
					new LatLng(currentLocation.getLatitude(), currentLocation
							.getLongitude()),
					LocationUtils.DEFAULT_ZOOM_LEVEL));			
		}

	}

	public void navigateTo(View view) {
		lu.navigateTo(this, Double.longBitsToDouble(sharedPref.getLong(
				getString(R.string.Stored_Location_Latitude), 0)), Double
				.longBitsToDouble(sharedPref.getLong(
						getString(R.string.Stored_Location_Longitude), 0)),
				false);
	}

	private void setTimer() {
		t = new Timer();
		t.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				runOnUiThread(new Runnable() {
					public void run() {
						updateGUI();
					}
				});

			}
		}, 1000, 1000);
	}

	private void updateGUI() {
		TimeCounter++;
		// tv.setText(String.valueOf(i));
		timeHandler.post(myRunnable);
	}

	final Runnable myRunnable = new Runnable() {
		public void run() {
			long millis = 1000 * TimeCounter;
			TimeZone tz = TimeZone.getTimeZone("PST");
			SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
			df.setTimeZone(tz);
			String time = df.format(new Date(millis));
			Log.d(TAG, "time in hh:mm:ss " + time);
			timerText.setText(String.valueOf(time));
		}
	};

	public void clearParkedLocationDetails(View view) throws IOException {
		carMarker.remove();
		carMarker = null;
		TimeCounter = 0;
		timerText.setText(R.string.default_elapsed_time);
		distanceText.setText(String.format(DISTANCE_TEXT, 0.00));
		SharedPreferences.Editor editor = sharedPref.edit();
		saveCurrentParkedLocationAsLastParked(editor);
		editor.putBoolean(getString(R.string.Is_Location_Stored), false);
		int pId = sharedPref.getInt(getString(R.string.Last_Stored_Parking_Location_Id), 0);
		editor.putInt(getString(R.string.Last_Stored_Parking_Location_Id),0);
		editor.commit();
		t.cancel();
		saveButton.setEnabled(true);
		layout.setVisibility(View.INVISIBLE);
		LocationUtils.stopLocationChangeService(this);
		AsyncTaskUtils atUtils = new AsyncTaskUtils(this);
		
		if(pId != 0)
		{
			atUtils.createCheckoutFeedbackPopup(pId);
		}
		stopNotificationAlarm();

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
								"Reminder is Set!", Toast.LENGTH_SHORT)
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
//			Toast.makeText(MainActivity.this, "Reminder deleted",
//					Toast.LENGTH_SHORT).show();
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

	
		protected void onPreExecute() {
			//boolean internetStatus = lu.checkInternetConenction(this.context);
			/*while(!internetStatus)
			{
				internetStatus = lu.checkInternetConenction(this.context);
			}*/
		}

		@Override
		protected FetchParkingLocationsResult<byte[]> doInBackground(
				Object... params) {
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
					if (pageResp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
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
					if (results != null && results.length() > 0) {
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
				String radius = searchRadius.toString();
				String maxNumResults = defaultMaxNumSearchResults.toString();
				if (isSaveRequest)
				{
					radius = "0.05";	
					maxNumResults = "5";
				}
				
				nameValuePairs.add(new BasicNameValuePair(JSONKeys.RADIUS,
						radius));
				nameValuePairs.add(new BasicNameValuePair(JSONKeys.MAX_NUM_RESULTS,
						maxNumResults));
				

				// add data

				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				// Execute HTTP Post Request
				HttpResponse response = httpClient.execute(httpPost);
				if (HttpStatus.SC_OK == response.getStatusLine()
						.getStatusCode()) {
					InputStream in = response.getEntity().getContent();
					bytes = IOUtils.toByteArray(in);
					FetchParkingLocationsResult<byte[]> fplr = new FetchParkingLocationsResult<byte[]>(
							bytes);
					fplr.setLatitude(latitude);
					fplr.setLongitude(longitude);
					return fplr;
				} else {
					return new FetchParkingLocationsResult<byte[]>(
							"Server error while fetching the parking locations!");
				}
			}

			catch (Exception e) {
				e.printStackTrace();
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
				else if(result != null 
						&& isSaveRequest)
				{
					setCurrentParkingLocationID(new String(result.getResult()));
					isSaveRequest= false;
					//get the parking lot id from shared preferences below
					if(pId != 0 
							&& pName != null)
					{
						atUtils.createParkedFeedbackPopup(pId, pName);
						atUtils.createAvailabilityFeedbackPopup(pId);						
						SharedPreferences.Editor editor = sharedPref.edit();
						editor.putInt(getString(R.string.Last_Stored_Parking_Location_Id),pId);
						editor.commit();
					}
					return;
				}
				if (result != null) {
					// clear the previous results
					googleMap.clear();
					gMapMarkers.clear();
					// add the new results to the map
					LocationUtils.addParkingLocations((Activity) context,
							googleMap, new String(result.getResult()),
							gMapMarkers);
					// move the google maps camera to the search address
					// location
					googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
							new LatLng(result.getLatitude(), result
									.getLongitude()), currentZoom));
				}

			} catch (JSONException e) {
				Toast.makeText(getParent(), "Failed to parse server response.",
						Toast.LENGTH_SHORT).show();

			}
		}
	}
	
	public void setCurrentParkingLocationID(String resultJson)throws JSONException
	{
		JSONObject locations = new JSONObject(resultJson);
		JSONArray parkingLocations = locations
				.getJSONArray(JSONKeys.PARKING_LOCATIONS);
		if (parkingLocations != null)
		{
			if (parkingLocations.length() >= 1)
			{
				JSONObject parkingLocation = parkingLocations.getJSONObject(0);
				pId = Integer.parseInt(parkingLocation.getString(JSONKeys.ID));
				pName = parkingLocation.getString(JSONKeys.NAME);
				
			}
		}
		
	}
	
}
