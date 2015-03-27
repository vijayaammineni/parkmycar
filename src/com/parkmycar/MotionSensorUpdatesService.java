package com.parkmycar;

import java.util.Arrays;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.os.IBinder;
import android.util.Log;

public class MotionSensorUpdatesService extends Service {

	public static final String TAG = MotionSensorUpdatesService.class
			.getSimpleName();

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
		smd = new SignificantMotionDetector(intent);
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

	public class SignificantMotionDetector implements SensorEventListener {

		private SensorManager mSensorManager;
		public Sensor signMotionDetector;
		public Sensor accelerometerSensor;
		public TriggerEventListener mTriggerEventListener;

		private Integer maxSpeed = 0;
		
		private Integer speedX = 0;
		private Integer speedY = 0;
		private Integer speedZ = 0;

		
		private Float lastX = 0.0f;
		private Float lastY = 0.0f;
		private Float lastZ = 0.0f;
		private Long lastEventTime = 0L;
		
		public SignificantMotionDetector(Intent intent) {

			mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
			accelerometerSensor = mSensorManager
					.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			mSensorManager.registerListener(this, accelerometerSensor, 
					SensorManager.SENSOR_DELAY_GAME);
			
//			signMotionDetector = mSensorManager
//					.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);			
//			mTriggerEventListener = new TriggerEventListener() {
//				@Override
//				public void onTrigger(TriggerEvent event) {
//
//					Intent intent = new Intent(WALKING);
//					sendBroadcast(intent);
//					mSensorManager.requestTriggerSensor(mTriggerEventListener,
//							signMotionDetector);
//				}
//			};
//			if (signMotionDetector != null) {
//				mSensorManager.requestTriggerSensor(mTriggerEventListener,
//						signMotionDetector);
//			}
			
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, 
				int accuracy) {
			// Nothing to do
		}

		@Override
		public void onSensorChanged (SensorEvent event) {
			
			
			Float currX = event.values[0];
			Float currY = event.values[0];
			Float currZ = event.values[0];
			
			//reset speeds if the device comes to rest
			if (currX.compareTo(0.03f) < 0 && currX.compareTo(0.03f) < 0
					&& currX.compareTo(0.03f) < 0) {
				speedX = speedY = speedZ = 0;
				lastEventTime = System.currentTimeMillis();
				return;
			}
			long currTime = System.currentTimeMillis();
			long dt = currTime - lastEventTime;
			if (lastEventTime != 0
					&& dt > 5) {
				speedX = (int) (speedX + ((currX-lastX) * dt));
				speedY = (int) (speedY + ((currY-lastY) * dt));
				speedZ = (int) (speedZ + ((currZ-lastZ) * dt));
				maxSpeed = Math.max(Math.max(speedX, speedY), speedZ);
				if (maxSpeed <= 10
						&& (speedX != 0 && speedY != 0 && speedZ != 0)) {
					Intent intent = new Intent(WALKING);
					intent.putExtra("Speed", maxSpeed);
					sendBroadcast(intent);				
				} else if(maxSpeed > 10){
					Intent intent = new Intent(DRIVING);
					intent.putExtra("Speed", maxSpeed);
					sendBroadcast(intent);
				}
			}
			lastEventTime = System.currentTimeMillis();
			lastX = currX;
			lastY = currY;
			lastZ = currZ;
			
		}
	}

	public static void startMotionSensorUpdatesService(Context context) {
		boolean isMyServiceRunning = isMyServiceRunning(
				MotionSensorUpdatesService.class, context);
		if (!isMyServiceRunning) {
			context.startService(new Intent(context,
					MotionSensorUpdatesService.class));
		}
	}

	public static void stopMotionSensorUpdatesService(Context context) {
		boolean isMyServiceRunning = isMyServiceRunning(
				MotionSensorUpdatesService.class, context);
		if (isMyServiceRunning) {
			context.stopService(new Intent(context,
					MotionSensorUpdatesService.class));
		}
	}

	public static boolean isMyServiceRunning(Class<?> serviceClass,
			Context context) {
		ActivityManager manager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

}
