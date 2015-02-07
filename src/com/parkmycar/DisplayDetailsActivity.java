package com.parkmycar;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
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

public class DisplayDetailsActivity extends Activity implements OnClickListener
{
	TableLayout table_layout;
	TextView text_view;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		int value = intent.getIntExtra("id",3);
		setContentView(R.layout.activity_details);
		text_view = (TextView)findViewById(R.id.address);
		table_layout = (TableLayout)findViewById(R.id.priceTableLayout);
		Button btn = (Button) findViewById(R.id.button);
		btn.setVisibility(View.VISIBLE);
		btn.setOnClickListener(new View.OnClickListener() {

		    public void onClick(View v) {
		    	Intent intent = new Intent(android.content.Intent.ACTION_VIEW, 
		    			Uri.parse("http://maps.google.com/maps?saddr=20.344,34.34&daddr=20.5666,45.345"));
		    	startActivity(intent);
		    }
		});
		GetParkingLocationDetails detailsAsyncTask = new GetParkingLocationDetails(this);
		detailsAsyncTask.execute(value);
		
	}

	
	public  void setFields(Context context,String result) throws JSONException
	{
		 JSONObject locations = new JSONObject(result);
		 JSONArray parkingLocations = locations.getJSONArray(JSONKeys.PARKING_LOCATIONS);
		 if (parkingLocations != null) 
		 {
			 for (int i = 0; i < parkingLocations.length(); i++) 
			 {
				 JSONObject parkingLocation = parkingLocations.getJSONObject(i);
				 String name = parkingLocation.getString(JSONKeys.NAME);
				 String address = parkingLocation.getString(JSONKeys.ADDRESS);
				 String city = parkingLocation.getString(JSONKeys.CITY);
				 Integer zipcode = parkingLocation.getInt(JSONKeys.ZIPCODE);
				 String state = parkingLocation.getString(JSONKeys.STATE);
				 String category = parkingLocation.getString(JSONKeys.CATEGORY);
				 Integer upVotes = parkingLocation.getInt(JSONKeys.UPVOTES);
				 Integer downVotes = parkingLocation.getInt(JSONKeys.DOWNVOTES);
				 
				 String totalAddress = address +"\n"+city+","+state+"\n"+zipcode;
				 
				 text_view.setText(totalAddress);
				 
				 TextView ic = (TextView)findViewById(R.id.icon_up_vote);
				 ic.setText(""+upVotes);
				 
				 TextView ic_down = (TextView)findViewById(R.id.icon_down_vote);
				 ic_down.setText(""+downVotes);
				
				 
				JSONArray pricingList = parkingLocation.getJSONArray(JSONKeys.PRICING_DETAILS_LIST);
				
				//give an info message if pricing list is null
				if(pricingList != null)
				{
					buildTable(pricingList);
					/*for(int j=0; j < pricingList.length() ; j++)
					{
						JSONObject pricingFortheDay = pricingList.getJSONObject(j);
						
						String dayOfWeek = pricingFortheDay.getString(JSONKeys.DAY_OF_WEEK);
						String dayPrice = pricingFortheDay.getString(JSONKeys.DAY_PRICE);
						String hourlyPrice = pricingFortheDay.getString(JSONKeys.HOURLY_PRICE);
						
						
					}*/
				}
				    			     
				 }
			 }
			 
		 
	else {
			 Toast.makeText(context, "Failed to fetch data from server!",
						Toast.LENGTH_SHORT).show();
		 }
		 
	}
	
	
	
	 private void buildTable(JSONArray pricingArray) throws JSONException{

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
		   if(j==1)
			   tv.setText(getDayOfWeek(pricingFortheDay.getInt(JSONKeys.DAY_OF_WEEK)));
		   else if(j==2)
			   tv.setText(String.valueOf(pricingFortheDay.getDouble(JSONKeys.HOURLY_PRICE)));
		   else
			   tv.setText(String.valueOf(pricingFortheDay.getDouble(JSONKeys.DAY_PRICE)));

		    row.addView(tv);

		   }

		   table_layout.addView(row);

		  }
		 }
	
	 
	
	private String getDayOfWeek(int day)
	{
		String dayOfWeek;
		switch (day)
		{
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
	private class GetParkingLocationDetails extends AsyncTask<Integer, Void, byte[]> 
	{
		private Context context;
		public GetParkingLocationDetails(Context context)
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
		protected byte[] doInBackground(Integer ... params)
		{
			
			HttpClient httpClient = new DefaultHttpClient();			
			HttpPost httpPost = new HttpPost(ServerUtils.getFullUrl(ServerUtils.PARKING_LOCATION_DETAILS_PATH));			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			
			Integer idObj = params[0];
			Integer id = (idObj != null) ? idObj:0;
			nameValuePairs.add(new BasicNameValuePair(JSONKeys.LOCATION_ID ,id.toString()));
			System.out.print("ID: " + id);
			
			byte [] bytes = null;
			
			try
			{
				//add data
		
				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				// Execute HTTP Post Request
				HttpResponse response = httpClient.execute(httpPost);	
								
				if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) 
				{
					InputStream in = response.getEntity().getContent();					
					bytes = IOUtils.toByteArray(in);									
					
				}
			}
			catch (ClientProtocolException e)
			{
				Toast.makeText(context, "Failed to fetch data from server!",
						Toast.LENGTH_SHORT).show();
		    } 
			 catch (IOException e)
			 {
				Toast.makeText(context, "Failed to fetch data from server!",
						Toast.LENGTH_SHORT).show();
		    }
		    
			 return bytes;
		}
		
		@Override
		protected void onPostExecute(byte[] result)
		{
			try
			{
			if(result != null)
			{
				setFields(context,new String(result));
			}
			else
			{
				Toast.makeText(getParent(), "Failed to fetch data from server!",
						Toast.LENGTH_SHORT).show();
			}
			}
			catch (JSONException e) {
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
