package com.parkmycar;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.parkmycar.json.JSONKeys;

public class LocationChangeService extends Service {
	
	private static final int ONE_MINUTE = 1000 * 60 * 1;
	public LocationManager locationManager;
	public MyLocationListener listener;
	public Location previousBestLocation = null;

	Intent intent;
	int counter = 0;

	@Override
	public void onCreate() {
		super.onCreate();
		intent = new Intent(LocationUtils.LOCATION_CHANGE_BROADCAST_ACTION);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		listener = new MyLocationListener();
		locationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, 4000, 0, listener);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				4000, 0, listener);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	protected boolean isBetterLocation(Location location,
			Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > ONE_MINUTE;
		boolean isSignificantlyOlder = timeDelta < -ONE_MINUTE;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
				.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.v("STOP_SERVICE", "DONE");
		locationManager.removeUpdates(listener);
	}

	public static Thread performOnBackgroundThread(final Runnable runnable) {
		final Thread t = new Thread() {
			@Override
			public void run() {
				try {
					runnable.run();
				} finally {

				}
			}
		};
		t.start();
		return t;
	}

	public class MyLocationListener implements LocationListener {

		public void onLocationChanged(final Location loc) {
			if (isBetterLocation(loc, previousBestLocation)) {
				loc.getLatitude();
				loc.getLongitude();
				intent.putExtra(JSONKeys.LATITUDE, loc.getLatitude());
				intent.putExtra(JSONKeys.LONGITUDE, loc.getLongitude());
				sendBroadcast(intent);
			}
		}

		public void onProviderDisabled(String provider) {
			//do nothing
		}

		public void onProviderEnabled(String provider) {
			//fo nothing
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {

		}
	}
}
