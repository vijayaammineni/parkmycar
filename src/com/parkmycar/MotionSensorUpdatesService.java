package com.parkmycar;

import java.util.Arrays;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.os.IBinder;
import android.util.Log;

public class MotionSensorUpdatesService extends Service {
	
	
	public static final String TAG = MotionSensorUpdatesService.class.getSimpleName();
	
	SignificantMotionDetector smd;
	
	Intent intent;
	
	public static final String WALKING = "walking";
	public static final String DRIVING = "driving";

	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		smd = new SignificantMotionDetector();	
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.v("STOP_MOTION_SENSOR_UPDATES_SERVICE", "DONE");
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}	
		
	public class SignificantMotionDetector {
		
		private SensorManager mSensorManager;
		public Sensor signMotionDetector;
		public Sensor accelerometerSensor;
		public TriggerEventListener mTriggerEventListener;

		public SignificantMotionDetector () {
			
			mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
			signMotionDetector = mSensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);
			accelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			mTriggerEventListener = new TriggerEventListener() {
			    @Override
			    public void onTrigger(TriggerEvent event) {
			    	
			    	StringBuffer sb = new StringBuffer();
					sb.append(System.currentTimeMillis()/1000).append(": ");
					sb.append(Arrays.toString(event.values));
					sb.append("\n");
					Log.d(TAG + "_SIGNIFICANT_MOTION", sb.toString());
					//TBD: determine if the user is walking or driving based on latest accelerometer props or 
					//gravity props
					intent = new Intent(WALKING);
					sendBroadcast(intent);
					mSensorManager.requestTriggerSensor(mTriggerEventListener, signMotionDetector);
			    }
			};	
			if(signMotionDetector != null) {
				mSensorManager.requestTriggerSensor(mTriggerEventListener, signMotionDetector);		
			}
		}
	}
	
	public static void startMotionSensorUpdatesService(Context context) {
		boolean isMyServiceRunning = isMyServiceRunning(MotionSensorUpdatesService.class,context);
		if (!isMyServiceRunning) {			
			context.startService(new Intent(context, MotionSensorUpdatesService.class));
		}
	}
	
	public static void stopMotionSensorUpdatesService(Context context) {
		boolean isMyServiceRunning = isMyServiceRunning(MotionSensorUpdatesService.class,context);
		if (isMyServiceRunning) {			
			context.stopService(new Intent(context, MotionSensorUpdatesService.class));
		}
	}
	public static boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
	    ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (serviceClass.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
	
	
}
