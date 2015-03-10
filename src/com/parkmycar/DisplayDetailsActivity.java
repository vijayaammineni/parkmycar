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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.parkmycar.json.JSONKeys;

public class DisplayDetailsActivity extends Activity implements OnClickListener {

	TableLayout pricingTable;
	TextView plName;
	TextView addressView;
	TextView availability_text;
	TextView publicParkingAvailableLabel;
	TextView noPricingInfoAvailableLabel;
	TextView upVote;
	TextView downVote;
	Button getDirectionsBtn;

	private String pname;
	private Double latitude;
	private Double longitude;
	
	private SharedPreferences sharedPref;
	private boolean isNavigatingToParkingLocation = false;
	private AsyncTaskUtils atUtils;

	// Broadcast receiver for receiving the location change events
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
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
					Integer destPLId = sharedPref
							.getInt(getString(R.string.Destination_Parking_Location_Id),
									0);
					String destPLName = sharedPref
							.getString(
									getString(R.string.Destination_Parking_Location_Name),
									"Unknown");

					Double distance = LocationUtils.distance(destLocationLat,
							destLocationLng, latitude, longitude, 'M');
					if (distance
							.compareTo(LocationUtils.DEFAULT_PARKING_LOCATION_RADIUS) <= 0) {
						LocationUtils.stopLocationChangeService(context);
						atUtils.createParkedFeedbackPopup(destPLId, destPLName);
						isNavigatingToParkingLocation = false;
					}

				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		final int plId = intent.getIntExtra(
				com.parkmycar.Constants.PARKING_LOCATION_ID, 0);
		setContentView(R.layout.activity_details);
		plName = (TextView) findViewById(R.id.name);
		addressView = (TextView) findViewById(R.id.address);
		publicParkingAvailableLabel = (TextView) findViewById(R.id.public_parking_label);
		noPricingInfoAvailableLabel = (TextView) findViewById(R.id.no_pricing_available_label);
		upVote = (TextView) findViewById(R.id.icon_up_vote);
		downVote = (TextView) findViewById(R.id.icon_down_vote);
		availability_text = (TextView) findViewById(R.id.availabilityText);
		pricingTable = (TableLayout) findViewById(R.id.priceTableLayout);
		getDirectionsBtn = (Button) findViewById(R.id.button);
		final LocationUtils lu = new LocationUtils(this,
				getApplicationContext());
		final Activity activity = this;
		atUtils = new AsyncTaskUtils(this);
		sharedPref = getPreferences(MODE_PRIVATE);
		isNavigatingToParkingLocation = sharedPref.getBoolean(
				getString(R.string.isNavigatingToParkingLocation), false);
 
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
				editor.putInt(
						getString(R.string.Destination_Parking_Location_Id),
						plId);
				editor.putString(
						getString(R.string.Destination_Parking_Location_Name),
						pname);
				editor.commit();
				LocationUtils.startLocationChangeService(activity);
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

				String totalAddress = address + "\n" + city + "," + state
						+ "\n" + zipcode;
				plName.setText(pname);
				addressView.setText(totalAddress);

				upVote.setText(upVotes + "");
				upVote.setVisibility(View.VISIBLE);

				downVote.setText(downVotes + "");
				downVote.setVisibility(View.VISIBLE);

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

}
