//@author: Bhavya
//this services will be started after stipulated time
//when it's started a notification will be sent to android os using NotificationManager
package com.parkmycar;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;

public class NotificationService extends Service {

	String notificationText = "Your Parking Time Is About to Complete.";
	String notificationTitle = "ParkMyCar";
	String notificationTextBody = "Your Parking Time Is About to Complete in 5 minutes.";
	@Override
	public IBinder onBind(Intent intent) {
	// TODO Auto-generated method stub
	return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		
	}

	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		showNotification();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		showNotification();    
		// We want this service to continue running until it is explicitly    
		// stopped, so return sticky.    
		return START_STICKY;
		}
	
	private NotificationManager mNM;
	
	private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        
        // Set the icon, scrolling text and timestamp
		Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		
		// The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);
        
		Notification notification = new Notification.Builder(this)
							.setContentTitle(notificationTitle)
							.setContentText(notificationText)
							.setWhen(System.currentTimeMillis())
							.setSound(soundUri)
							.setSmallIcon(R.drawable.ic_launcher)
							.setContentIntent(contentIntent)
							.setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 })
							.setLights(Color.RED, 3000, 3000)
							.build();
						
        // clear notification after pressing:
		notification.defaults |= Notification.DEFAULT_SOUND;
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        // Send the notification.
        if(mNM == null)
        	mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		if(mNM !=null){
			mNM.cancel(123456);
			mNM.cancel(123457);
        	mNM.notify(123456, notification);
		}
        stopSelf();
    }

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}
}