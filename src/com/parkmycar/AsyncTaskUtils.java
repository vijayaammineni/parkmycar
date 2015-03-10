package com.parkmycar;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

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
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.parkmycar.json.JSONKeys;
import com.parkmycar.json.UserFeedbackType;

public class AsyncTaskUtils {

	private Context context = null;

	private String androidId;
	
	private AlertDialog alertDialog;
	
	private static final int USER_FEEDBACK_POPUP = 0;
	private static final long USER_FEEDBACK_POPUP_TIMEOUT = 5000;
	private static final String PARKING_AVAILABLE_FEEDBACK_STR = "Are there any more parking spots available?";
	private static final String PARKING_LOT_CHECKOUT_FEEDBACK_STR = "Did you check out from this parking lot?";
	private static final String PARKING_LOT_CAR_PARKED_FEEDBACK_STR = "Did you park your car in '%s' parking lot?";
	private static final String THANK_YOU_MSG = "Thank you for your feedback :)";

	public AsyncTaskUtils(Context context) {

		this.context = context;
		if (this.context == null) {
			return;
		}
		final TelephonyManager tm = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);

		final String tmDevice, tmSerial, androidIdStr;
		tmDevice = "" + tm.getDeviceId();
		tmSerial = "" + tm.getSimSerialNumber();
		androidIdStr = ""
				+ android.provider.Settings.Secure.getString(
						context.getContentResolver(),
						android.provider.Settings.Secure.ANDROID_ID);

		UUID deviceUuid = new UUID(androidIdStr.hashCode(),
				((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
		androidId = deviceUuid.toString();

	}

	private void showFeedbackPopup(final Integer  plId, final String text, final UserFeedbackType ufType1, 
			UserFeedbackType ufType2) {		
		if (plId == null) {
			return;
		}		
		final int parkinglID = plId;
		final UserFeedbackType yesUfType = ufType1;
		final UserFeedbackType noUfType = ufType2;
		AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
		builder1.setMessage(text);
		builder1.setCancelable(true);
		builder1.setPositiveButton("Yes",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						if (yesUfType != null) {
							update(yesUfType, parkinglID);
						} else {
							Toast.makeText(context, THANK_YOU_MSG,
								Toast.LENGTH_SHORT).show();
						}
						dialog.cancel();
					}
				});
		builder1.setNegativeButton("No", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				if (noUfType != null) {
					update(noUfType, parkinglID);
				} else {
					Toast.makeText(context, THANK_YOU_MSG,
						Toast.LENGTH_SHORT).show();
				}
				dialog.cancel();
			}
		});
		alertDialog = builder1.create();
		alertDialog.show();
        mHandler.sendEmptyMessageDelayed(USER_FEEDBACK_POPUP, USER_FEEDBACK_POPUP_TIMEOUT);
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
		showFeedbackPopup(plId, PARKING_AVAILABLE_FEEDBACK_STR, UserFeedbackType.AVAILABLE, UserFeedbackType.NOTAVAILABLE);
	}
	
	public void createCheckoutFeedbackPopup(final Integer plID) {
        showFeedbackPopup (plID, PARKING_LOT_CHECKOUT_FEEDBACK_STR, UserFeedbackType.CHECKOUT, null);
	}
	
	public void createParkedFeedbackPopup(final Integer plID, final String parkingLocationName) {
        showFeedbackPopup (plID, 
        		String.format(PARKING_LOT_CAR_PARKED_FEEDBACK_STR, parkingLocationName), UserFeedbackType.PARKED, null);
	}

	public void update(UserFeedbackType ufType, int plID) {
		UpdateUserFeedbackInDB updateTask = new UpdateUserFeedbackInDB();
		updateTask.execute(ufType, plID);
	}

	private class UpdateUserFeedbackInDB extends
			AsyncTask<Object, Void, Integer> {

		HttpClient httpClient = new DefaultHttpClient();

		@Override
		protected Integer doInBackground(Object... params) {

			HttpPost httpPost = new HttpPost(
					ServerUtils.getFullUrl(ServerUtils.USER_FEEDBACK_PATH));
			try {

				JSONObject jObj = new JSONObject();
				jObj.put(JSONKeys.ANDROID_ID, androidId);
				jObj.put(JSONKeys.PARKINGLOCATION_ID, params[1]);
				jObj.put(JSONKeys.TIME_STAMP, new Date().getTime());
				jObj.put(JSONKeys.FEEDBACK_TYPE, params[0]);

				// add data
				httpPost.setEntity(new StringEntity(jObj.toString()));
				// Execute HTTP Post Request
				HttpResponse response = httpClient.execute(httpPost);

				return response.getStatusLine().getStatusCode();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return 0;
		}

		@Override
		protected void onPostExecute(Integer statusCode) {
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
