package com.parkmycar;

import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Handler;
import android.widget.Toast;

import com.parkmycar.json.JSONKeys;
import com.parkmycar.json.UserFeedbackType;

public class UserFeedbackUtils {

	private Context context = null;

	private String androidId;
	
	private AlertDialog alertDialog;
	
	private static final int USER_FEEDBACK_POPUP = 0;
	private static final long USER_FEEDBACK_POPUP_TIMEOUT = 5000;
	public static final String PARKING_AVAILABLE_FEEDBACK_STR = "Are there any more parking spots available?";
	public static final String PARKING_LOT_CHECKOUT_FEEDBACK_STR = "Did you check out from this parking lot?";
	public static final String PARKING_LOT_CAR_PARKED_FEEDBACK_STR = "Did you park your car in '%s' parking lot?";
	public static final String THANK_YOU_MSG = "Thank you for your feedback :)";
	
	private Timer timer;

	public UserFeedbackUtils(Context context) {

		this.context = context;
		if (this.context == null) {
			return;
		}
		
		androidId = android.provider.Settings.Secure.getString(
				context.getContentResolver(),
				android.provider.Settings.Secure.ANDROID_ID);
		
		timer = new Timer();

	}
	
	public void showFeedbackPopup(final Integer  plId, final String text, final UserFeedbackType ufType1, 
			UserFeedbackType ufType2, DialogInterface.OnClickListener yesListener, DialogInterface.OnClickListener noListener) {		
		if (plId == null) {
			return;
		}		
		final int parkinglID = plId;
		final UserFeedbackType yesUfType = ufType1;
		final UserFeedbackType noUfType = ufType2;
		
		AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
		builder1.setMessage(text);
		builder1.setCancelable(true);
		if (yesListener != null) {
			builder1.setPositiveButton("Yes", yesListener);
		} else {
			builder1.setPositiveButton("Yes",
				new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					if (yesUfType != null) {
						update(yesUfType, parkinglID, false);
					} else {
						Toast.makeText(context, THANK_YOU_MSG,
							Toast.LENGTH_SHORT).show();
					}
					dialog.cancel();
				}
			});	
		}
		
		if (noListener != null) {
			builder1.setNegativeButton("No", noListener);
		} else {
			builder1.setNegativeButton("No", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					if (noUfType != null) {
						update(noUfType, parkinglID, false);
					} else {
						Toast.makeText(context, THANK_YOU_MSG,
							Toast.LENGTH_SHORT).show();
					}
					dialog.cancel();
				}
			});
		}
		builder1.setIcon(R.drawable.ic_launcher);
		builder1.setTitle("Feedback");
		alertDialog = builder1.create();
		alertDialog.show();
		try {
		timer.schedule(new TimerTask() {			
			@Override
			public void run() {
				//alertDialog.show();
				mHandler.sendEmptyMessageDelayed(USER_FEEDBACK_POPUP, USER_FEEDBACK_POPUP_TIMEOUT);
			}
		}, new Date (System.currentTimeMillis() + (2 * 1000)));     
		} catch (Exception e) {
			//ignore any exceptions while showing the pop up window
		}
	}
	
	private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
            case USER_FEEDBACK_POPUP:
                if (alertDialog != null && alertDialog.isShowing()) {
                	alertDialog.dismiss();
                }
                break;

            default:
                break;
            }
        }
    };
    
    public void createAvailabilityFeedbackPopup(final Integer plId) {
		showFeedbackPopup(plId, PARKING_AVAILABLE_FEEDBACK_STR, null, null,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {				
						update(UserFeedbackType.AVAILABLE, plId, false);
						dialog.cancel();
					}
				}, 
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {				
						update(UserFeedbackType.NOTAVAILABLE, plId, false);
						dialog.cancel();
					}
		});
	}
	
	public void createCheckoutFeedbackPopup(final Integer plID, 
			DialogInterface.OnClickListener yesListener) {
        showFeedbackPopup (plID, PARKING_LOT_CHECKOUT_FEEDBACK_STR, UserFeedbackType.CHECKOUT, null, 
        		yesListener, null);
	}

	public void update(UserFeedbackType ufType, int plID, boolean silent) {
		UpdateUserFeedbackInDB updateTask = new UpdateUserFeedbackInDB();
		updateTask.execute(ufType, plID, silent);
	}

	private class UpdateUserFeedbackInDB extends
			AsyncTask<Object, Void, Integer> {

		HttpClient httpClient = new DefaultHttpClient();

		@Override
		protected Integer doInBackground(Object... params) {

			HttpPost httpPost = new HttpPost(
					ServerUtils.getFullUrl(ServerUtils.USER_FEEDBACK_PATH));
			Boolean silent = false;
			try {
				if (params.length > 2
						&& params [2] != null) {
					silent = (Boolean) params [2];
				}
				JSONObject jObj = new JSONObject();
				jObj.put(JSONKeys.ANDROID_ID, androidId);
				jObj.put(JSONKeys.PARKINGLOCATION_ID, params[1]);
				jObj.put(JSONKeys.TIME_STAMP, new Date().getTime());
				jObj.put(JSONKeys.FEEDBACK_TYPE, params[0]);

				// add data
				httpPost.setEntity(new StringEntity(jObj.toString()));
				// Execute HTTP Post Request
				HttpResponse response = httpClient.execute(httpPost);
				if (silent) {
					return -1;
				} else {
					return response.getStatusLine().getStatusCode();
				}				
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (silent) {
				return -1;
			} else {
				return 0;
			}
		}

		@Override
		protected void onPostExecute(Integer statusCode) {
			if (statusCode == -1) {
				return;
			}
			try {
				if (statusCode != null 
						&& statusCode == HttpStatus.SC_OK) {
					Toast.makeText(context, "Thank you for your feedback :)",
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(
							context,
							"Our bad, some error occured while sending your feedback.\nThank you!",
							Toast.LENGTH_SHORT).show();
				}
			} catch (Exception e) {

			}
		}
	}
}
