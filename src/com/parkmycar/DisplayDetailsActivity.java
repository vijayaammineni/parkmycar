package com.parkmycar;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.parkmycar.json.JSONKeys;
import com.parkmycar.json.UserFeedbackType;

public class DisplayDetailsActivity extends Activity implements OnClickListener {
	
	private final String TAG = DisplayDetailsActivity.class.getSimpleName();

	TableLayout pricingTable;
	TextView plName;
	TextView addressView;
	TextView availability_text;
	TextView publicParkingAvailableLabel;
	TextView noPricingInfoAvailableLabel;
	Button upVote;
	Button downVote;
	TextView upVoteCount;
	TextView downVoteCount;
	Button getDirectionsBtn;
	String androidId;
	String voteType = null;

	private String pname;
	private Double latitude;
	private Double longitude;
	private Integer parkingLocationId;
	
	private SharedPreferences sharedPref;
	private boolean isNavigatingToParkingLocation = false;
	private UserFeedbackUtils ufUtils;
	
	private TimerTask askUserFeedbackTask;
	private Timer timer = new Timer();


	// Broadcast receiver for receiving the location change events
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(final Context context, Intent intent) {
			Bundle bundle = intent.getExtras();
			if (LocationUtils.LOCATION_CHANGE_BROADCAST_ACTION.equals(intent
					.getAction()) && bundle != null) {
				Double latitude = bundle.getDouble(JSONKeys.LATITUDE);
				Double longitude = bundle.getDouble(JSONKeys.LONGITUDE);
				// if the user is navigating to a parking location then check
				// to see the user reached the parking location, if so ask for
				// feedback
				if (isNavigatingToParkingLocation) {

					Double destLocationLat = Double
							.longBitsToDouble(sharedPref
									.getLong(
											getString(R.string.Destination_Location_Latitude),
											0));
					Double destLocationLng = Double
							.longBitsToDouble(sharedPref
									.getLong(
											getString(R.string.Destination_Location_Longitude),
											0));
					final Integer destPLId = sharedPref
							.getInt(getString(R.string.Destination_Parking_Location_Id),
									0);
					final String destPLName = sharedPref
							.getString(
									getString(R.string.Destination_Parking_Location_Name),
									"Unknown");

					Double distance = LocationUtils.distance(destLocationLat,
							destLocationLng, latitude, longitude, 'M');
					if (distance
							.compareTo(LocationUtils.DEFAULT_PARKING_LOCATION_RADIUS) <= 0) {
						//mark that the user has reached the location
						SharedPreferences.Editor editor = sharedPref.edit();
						editor.putBoolean(
								getString(R.string.reachedDestination), true);
						editor.commit();
						if (askUserFeedbackTask == null) {
							askUserFeedbackTask = new TimerTask() {							
								@Override
								public void run() {
									LocationUtils.stopLocationChangeService(context);						
									ufUtils.showFeedbackPopup (destPLId, 
						        		String.format(UserFeedbackUtils.PARKING_LOT_CAR_PARKED_FEEDBACK_STR, destPLName), 
						        		UserFeedbackType.PARKED, null,
						        		new DialogInterface.OnClickListener() {	
											@Override
											public void onClick(DialogInterface dialog, int which) {
												ufUtils.update(UserFeedbackType.PARKED, destPLId, false);
												dialog.cancel();
												//we are asking for availability feedback here because if user hasn't responded to this then dont 
												//bother user by asking about availability
												ufUtils.createAvailabilityFeedbackPopup(destPLId);
											}
										}, null);
								}
							};
							timer.schedule(askUserFeedbackTask, new Date(System.currentTimeMillis() + (5 * 60 * 1000)));
						}
					}

				}
			}
			else if (MotionSensorUpdatesService.WALKING.equals(intent.getAction())) {
				//TBD: see if the user is inside a parking lot, if so record that the user has parked the car here
				Log.d(TAG,  "User is walking!");
				final Integer destPLId = sharedPref.getInt(
						getString(R.string.Destination_Parking_Location_Id), 0);	
				final String destPLName = sharedPref.getString(
						getString(R.string.Destination_Parking_Location_Name), "Unknown");	
				
				if (isNavigatingToParkingLocation
						&& destPLId != null
						&& destPLName != null && !destPLName.equals("Unknown")) {
					String lastMotionType = sharedPref.getString(
							getString(R.string.lastMotionType), null);
					Boolean reachedLocation = sharedPref.getBoolean(
							getString(R.string.reachedDestination), false);
					if (reachedLocation.booleanValue() 
							&& lastMotionType != null
								&& lastMotionType.equals(MotionSensorUpdatesService.DRIVING)) {
						//we have found that user was last driving and now walking after parking the vehicle
						timer.cancel();
						ufUtils.update(UserFeedbackType.PARKED, destPLId, true);
						clearAllNavigationParams();
						MotionSensorUpdatesService.stopMotionSensorUpdatesService(context);
					}
				}
				//Toast.makeText(context,  "User is walking!", Toast.LENGTH_LONG).show();
			}
			else if (MotionSensorUpdatesService.DRIVING.equals(intent.getAction())) {
				Log.d(TAG,  "User is driving!");
				if (isNavigatingToParkingLocation) {
					String lastMotionType = sharedPref.getString(
							getString(R.string.lastMotionType), null);
					Boolean reachedLocation = sharedPref.getBoolean(
							getString(R.string.reachedDestination), false);
					if (lastMotionType == null
							&& !reachedLocation.booleanValue()) {
						//we now store that the user started driving
						SharedPreferences.Editor editor = sharedPref.edit();
						editor.putString(
								getString(R.string.lastMotionType), MotionSensorUpdatesService.DRIVING);
						editor.commit();
					}
				}
				//Toast.makeText(context, "User is driving!", Toast.LENGTH_LONG).show();
			}
		}
	};
	
	private void clearAllNavigationParams() {
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putLong(
				getString(R.string.Destination_Location_Latitude), 0);
		editor.putLong(
				getString(R.string.Destination_Location_Longitude), 0);
		editor.putBoolean(
				getString(R.string.isNavigatingToParkingLocation), false);
		editor.putLong("Time_Navigation_Started", 0);
		editor.putInt(
				getString(R.string.Destination_Parking_Location_Id), 0);
		editor.putString(
				getString(R.string.Destination_Parking_Location_Name), null);
		editor.commit();		
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		final int plId = intent.getIntExtra(
				com.parkmycar.Constants.PARKING_LOCATION_ID, 0);
		parkingLocationId = plId;
		setContentView(R.layout.activity_details);
		plName = (TextView) findViewById(R.id.name);
		addressView = (TextView) findViewById(R.id.address);
		publicParkingAvailableLabel = (TextView) findViewById(R.id.public_parking_label);
		noPricingInfoAvailableLabel = (TextView) findViewById(R.id.no_pricing_available_label);
		upVote = (Button) findViewById(R.id.icon_up_vote);
		downVote = (Button) findViewById(R.id.icon_down_vote);
		upVoteCount = (TextView) findViewById(R.id.up_votes);
		downVoteCount = (TextView) findViewById(R.id.down_votes);
		availability_text = (TextView) findViewById(R.id.availabilityText);
		pricingTable = (TableLayout) findViewById(R.id.priceTableLayout);
		getDirectionsBtn = (Button) findViewById(R.id.button);
		final LocationUtils lu = new LocationUtils(this,
				getApplicationContext());
		final Activity activity = this;
		ufUtils = new UserFeedbackUtils(this);
		sharedPref = getPreferences(MODE_PRIVATE);
		isNavigatingToParkingLocation = sharedPref.getBoolean(
				getString(R.string.isNavigatingToParkingLocation), false);
		Long timeNavigationStarted = sharedPref.getLong(
				"Time_Navigation_Started", 0);
		Long currentTime = System.currentTimeMillis();
		if (currentTime == 0 || (currentTime - timeNavigationStarted) > (30 * 60 * 1000)) {
			isNavigatingToParkingLocation = false;
			SharedPreferences.Editor editor = sharedPref.edit();
			editor.putBoolean(
					getString(R.string.isNavigatingToParkingLocation), false);
			editor.putString(
					getString(R.string.lastMotionType), null);
			editor.putLong("Time_Navigation_Started", 0L);
			MotionSensorUpdatesService.startMotionSensorUpdatesService(activity);
			editor.commit();
			
		}
		androidId = android.provider.Settings.Secure.getString(this.getContentResolver(),
				android.provider.Settings.Secure.ANDROID_ID);
		
 
		getDirectionsBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				SharedPreferences.Editor editor = sharedPref.edit();
				editor.putLong(
						getString(R.string.Destination_Location_Latitude),
						Double.doubleToRawLongBits(latitude));
				editor.putLong(
						getString(R.string.Destination_Location_Longitude),
						Double.doubleToRawLongBits(longitude));
				editor.putBoolean(
						getString(R.string.isNavigatingToParkingLocation), true);
				editor.putLong("Time_Navigation_Started", System.currentTimeMillis());
				editor.putInt(
						getString(R.string.Destination_Parking_Location_Id),
						plId);
				editor.putString(
						getString(R.string.Destination_Parking_Location_Name),
						pname);
				editor.commit();
				LocationUtils.startLocationChangeService(activity);
				//start the motion detection sensor updates service
				MotionSensorUpdatesService.startMotionSensorUpdatesService(activity);
				lu.navigateTo(activity, latitude, longitude, true);
			}
		});
		GetParkingLocationDetails detailsAsyncTask = new GetParkingLocationDetails(
				this);
		detailsAsyncTask.execute(plId);
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

	public void setFields(Context context, String result) throws JSONException {
		JSONObject locations = new JSONObject(result);
		JSONArray parkingLocations = locations
				.getJSONArray(JSONKeys.PARKING_LOCATIONS);
		if (parkingLocations != null) {
			for (int i = 0; i < parkingLocations.length(); i++) {
				JSONObject parkingLocation = parkingLocations.getJSONObject(i);
				pname = parkingLocation.getString(JSONKeys.NAME);
				String address = parkingLocation.getString(JSONKeys.ADDRESS);
				String city = parkingLocation.getString(JSONKeys.CITY);
				Integer zipcode = parkingLocation.getInt(JSONKeys.ZIPCODE);
				String state = parkingLocation.getString(JSONKeys.STATE);
				String category = parkingLocation.getString(JSONKeys.CATEGORY);
				latitude = parkingLocation.getDouble(JSONKeys.LATITUDE);
				longitude = parkingLocation.getDouble(JSONKeys.LONGITUDE);
				Integer upVotes = parkingLocation.getInt(JSONKeys.UPVOTES);
				Integer downVotes = parkingLocation.getInt(JSONKeys.DOWNVOTES);
				String voteType = null;
				if(parkingLocation.has(JSONKeys.VOTE_TYPE))
				{
				   voteType = parkingLocation.getString(JSONKeys.VOTE_TYPE);
				}

				String totalAddress = address + "\n" + city + "," + state
						+ "\n" + zipcode;
				plName.setText(pname);
				addressView.setText(totalAddress);

				
				upVote.setVisibility(View.VISIBLE);
				downVote.setVisibility(View.VISIBLE);
			
				// TODO: Disable up vote image when the mcdId has already up voted
				if(voteType != null 
						&& voteType.equals("UPVOTE"))
				{
					upVote.setClickable(false);
					upVote.setEnabled(false);
					
				}
				else if(voteType != null
						&& voteType.equals("DOWNVOTE"))
				{
					downVote.setClickable(false);
					downVote.setEnabled(false);
				}
				upVoteCount.setVisibility(View.VISIBLE);
				upVoteCount.setText(upVotes + "");
				downVoteCount.setVisibility(View.VISIBLE);
				downVoteCount.setText(downVotes + "");
				JSONArray pricingList = parkingLocation
						.getJSONArray(JSONKeys.PRICING_DETAILS_LIST);

				//
				if (category.equals("PUBLIC")) {
					publicParkingAvailableLabel.setVisibility(View.VISIBLE);
				} else if (category.equals("PAID")) {
					// give an info message if pricing list is null
					if (pricingList == null || pricingList.length() == 0) {
						noPricingInfoAvailableLabel.setVisibility(View.VISIBLE);
					} else {
						pricingTable.setVisibility(View.VISIBLE);
						buildTable(pricingList);
					}
				}

				getDirectionsBtn.setVisibility(View.VISIBLE);

				JSONObject feedbackList = parkingLocation
						.getJSONObject(JSONKeys.FEDDBACK_LIST);

				// give an info message if feedback list is null
				if (feedbackList != null) {
					StringBuilder sb = new StringBuilder(
							"In last 30 Min.. \n\n");
					int availableVotes = feedbackList
							.getInt(JSONKeys.AVAILABLE_VOTES);
					int unavailableVotes = feedbackList
							.getInt(JSONKeys.NOTAVAILABLE_VOTES);
					int parkedNum = feedbackList.getInt(JSONKeys.PARKED_NUM);
					int checkedNum = feedbackList.getInt(JSONKeys.CHECKOUT_NUM);
					if (availableVotes == 0 && unavailableVotes == 0
							&& checkedNum == 0 && parkedNum == 0) {
						sb.append("No parking activity was reported.\n");
					} else {
						if (availableVotes > 0) {
							sb.append(availableVotes
									+ " said parking available here \n");
						}
						if (unavailableVotes > 0) {
							sb.append(availableVotes
									+ " said No parking available here\n");
						}
						if (parkedNum > 0) {
							sb.append(parkedNum + " parked here\n");
						}
						if (checkedNum > 0) {
							sb.append(parkedNum + " checked out from here\n");
						}

					}
					availability_text.setText(sb.toString());
				}
			}
		}

		else {
			Toast.makeText(context, "Failed to fetch data from server!",
					Toast.LENGTH_SHORT).show();
		}

	}

	private void buildTable(JSONArray pricingArray) throws JSONException {

		TableRow header = new TableRow(this);
		header.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT));
		TextView h1 = new TextView(this);
		h1.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT));
		h1.setBackgroundResource(R.drawable.header_cell);
		h1.setPadding(5, 5, 5, 5);
		h1.setText("DAY");
		h1.setGravity(Gravity.CENTER_HORIZONTAL);
		h1.setTypeface(null, Typeface.BOLD);
		TextView h2 = new TextView(this);
		h2.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT));
		h2.setBackgroundResource(R.drawable.header_cell);
		h2.setPadding(5, 5, 5, 5);
		h2.setText("Per Hour");
		h2.setGravity(Gravity.CENTER_HORIZONTAL);
		h2.setTypeface(null, Typeface.BOLD);
		TextView h3 = new TextView(this);
		h3.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT));
		h3.setBackgroundResource(R.drawable.header_cell);
		h3.setPadding(5, 5, 5, 5);
		h3.setText("Per Day");
		h3.setGravity(Gravity.CENTER_HORIZONTAL);
		h3.setTypeface(null, Typeface.BOLD);

		header.addView(h1);
		header.addView(h2);
		header.addView(h3);
		
		pricingTable.addView(header);
		
		// outer for loop
		for (int i = 0; i < pricingArray.length(); i++) {

			TableRow row = new TableRow(this);
			row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.WRAP_CONTENT));
			JSONObject pricingFortheDay = pricingArray.getJSONObject(i);
			// inner for loop
			for (int j = 1; j <= 3; j++) {

				TextView tv = new TextView(this);
				tv.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
						LayoutParams.WRAP_CONTENT));
				tv.setBackgroundResource(R.drawable.cell_shape);
				tv.setPadding(5, 5, 5, 5);
				if (j == 1)
					tv.setText(getDayOfWeek(pricingFortheDay
							.getInt(JSONKeys.DAY_OF_WEEK)));
				else if (j == 2)
					tv.setText(String.valueOf(pricingFortheDay
							.getDouble(JSONKeys.HOURLY_PRICE)));
				else
					tv.setText(String.valueOf(pricingFortheDay
							.getDouble(JSONKeys.DAY_PRICE)));

				row.addView(tv);

			}
			pricingTable.addView(row);

		}
	}

	private String getDayOfWeek(int day) {
		String dayOfWeek;
		switch (day) {
		case 0:
			dayOfWeek = "SUN";
			break;
		case 1:
			dayOfWeek = "MON";
			break;
		case 2:
			dayOfWeek = "TUE";
			break;

		case 3:
			dayOfWeek = "WED";
			break;
		case 4:
			dayOfWeek = "THU";
			break;
		case 5:
			dayOfWeek = "FRI";
			break;
		default:
			dayOfWeek = "SAT";
			break;
		}
		return dayOfWeek;
	}
	
	public void setFieldsOnVoteType(String result,String voteType) 
	{
		if(voteType != null&& voteType.equals("UPVOTE"))
		{
			upVote.setClickable(false);
			upVote.setEnabled(false);
			upVoteCount.setText(result);
		}
		else if(voteType != null&& voteType.equals("DOWNVOTE"))
		{
			downVote.setClickable(false);
			downVote.setEnabled(false);
			downVoteCount.setText(result);
		}
	}
	
	public void onVoteBtnClicked(View v){
		String voteType = null;
	    if(v.getId() == R.id.icon_up_vote){
	       voteType = "UPVOTE";
	    }
	    else if(v.getId() == R.id.icon_down_vote)
	    {
	    	voteType = "DOWNVOTE";
	    }
	    
	    SaveVotingDetails saveVoteAsyncTask = new SaveVotingDetails(
				this);
	    saveVoteAsyncTask.execute(parkingLocationId,voteType);
	}

	/**
	 * Async task to fetch parking locations from DB
	 * 
	 */
	private class GetParkingLocationDetails extends
			AsyncTask<Integer, Void, byte[]> {

		private Context context;

		public GetParkingLocationDetails(Context context) {
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
		protected byte[] doInBackground(Integer... params) {

			HttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(
					ServerUtils
							.getFullUrl(ServerUtils.PARKING_LOCATION_DETAILS_PATH));
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

			Integer idObj = params[0];
			Integer id = (idObj != null) ? idObj : 0;
			nameValuePairs.add(new BasicNameValuePair(JSONKeys.LOCATION_ID, id
					.toString()));
			System.out.print("ID: " + id);

			nameValuePairs.add(new BasicNameValuePair(JSONKeys.MCD_ID,androidId));
			
			
			byte[] bytes = null;

			try {
				// add data

				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				// Execute HTTP Post Request
				HttpResponse response = httpClient.execute(httpPost);

				if (HttpStatus.SC_OK == response.getStatusLine()
						.getStatusCode()) {
					InputStream in = response.getEntity().getContent();
					bytes = IOUtils.toByteArray(in);

				}
			} catch (ClientProtocolException e) {
				Toast.makeText(context, "Failed to fetch data from server!",
						Toast.LENGTH_SHORT).show();
			} catch (IOException e) {
				Toast.makeText(context, "Failed to fetch data from server!",
						Toast.LENGTH_SHORT).show();
			}

			return bytes;
		}

		@Override
		protected void onPostExecute(byte[] result) {
			try {
				if (result != null) {
					setFields(context, new String(result));
				} else {
					Toast.makeText(getParent(),
							"Failed to fetch data from server!",
							Toast.LENGTH_SHORT).show();
				}
			} catch (JSONException e) {
				Toast.makeText(getParent(), "Failed to parse server response.",
						Toast.LENGTH_SHORT).show();

			}
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		// TODO Auto-generated method stub

	}

	/**
	 * Async task to fetch parking locations from DB
	 * 
	 */
	private class SaveVotingDetails extends
			AsyncTask<Object, Void, byte[]> {

		private Context context;

		public SaveVotingDetails(Context context) {
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
		protected byte[] doInBackground(Object... params) {

			HttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(
					ServerUtils
							.getFullUrl(ServerUtils.SAVE_VOTE_DETAILS_PATH));
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

			Object idObj = (params[0] != null) ? params[0] : 0;
			Integer id = (Integer) idObj;
			
			nameValuePairs.add(new BasicNameValuePair(JSONKeys.LOCATION_ID, id
					.toString()));
			System.out.print("ID: " + id);
			voteType = params[1].toString();
			nameValuePairs.add(new BasicNameValuePair(JSONKeys.MCD_ID,androidId));
			nameValuePairs.add(new BasicNameValuePair(JSONKeys.VOTE_TYPE,voteType));
						
			byte[] bytes = null;

			try {
				// add data

				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				// Execute HTTP Post Request
				HttpResponse response = httpClient.execute(httpPost);

				if (HttpStatus.SC_OK == response.getStatusLine()
						.getStatusCode()) {
					InputStream in = response.getEntity().getContent();
					bytes = IOUtils.toByteArray(in);

				}
			} catch (ClientProtocolException e) {
				Toast.makeText(context, "Failed to fetch data from server!",
						Toast.LENGTH_SHORT).show();
			} catch (IOException e) {
				Toast.makeText(context, "Failed to fetch data from server!",
						Toast.LENGTH_SHORT).show();
			}

			return bytes;
		}

		@Override
		protected void onPostExecute(byte[] result2) {
				if (result2 != null) {
					setFieldsOnVoteType(new String(result2),voteType);
					voteType = null;
				}
				else {
					Log.d(TAG, "Error while saving vote information on server.");
				}
		}
	}
			
			

}
