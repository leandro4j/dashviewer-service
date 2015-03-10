package br.com.sankhya.dashviewer;

import org.json.JSONException;
import org.json.JSONObject;

import android.R;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import br.com.sankhya.dashviewer.NotificationService.NotificationInfo;

import com.red_folder.phonegap.plugin.backgroundservice.BackgroundService;

public class DashViewerService extends BackgroundService {

	private long				lastExecution;
	private String				mgeSession;
	private String				serverUrl;
	private String				kID;

	@Override
	protected JSONObject doWork() {
		JSONObject result = new JSONObject();

		if(lastExecution == 0){
			lastExecution = System.currentTimeMillis();
		}
		
		try {
			int notificationCounter = 0;
			
			if (serverUrl != null) {
				NotificationService notificationService = new NotificationService(getApplicationContext());
				notificationService.setkID(kID);
				notificationService.setLastExecution(lastExecution);
				notificationService.setMgeSession(mgeSession);
				notificationService.setServerUrl(serverUrl);

				NotificationInfo notificationInfo = notificationService.getNotification();

				if(notificationInfo != null && notificationInfo.lastMessage != null){
					Notification notification = getActivityNotification(notificationInfo);
	
					NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					mNotificationManager.notify(1, notification);
					
					notificationCounter = notificationInfo.amount;
				}
			}

			result.put("NotificationCounter", notificationCounter);
		} catch (JSONException e) {
		} finally{
			lastExecution = System.currentTimeMillis();
		}

		return result;
	}

	@Override
	protected void setConfig(JSONObject config) {
		try {
			if (config.has("mgeSession")) {
				this.mgeSession = config.getString("mgeSession");
			}

			if (config.has("serverUrl")) {
				this.serverUrl = config.getString("serverUrl");
			}

			if (config.has("kID")) {
				this.kID = config.getString("kID");
			}
			
			storeConfig();
		} catch (JSONException e) {
		}
	}
	
	private void storeConfig(){
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		
		Editor prefEdit = pref.edit();
		prefEdit.putString("mgeSession", mgeSession);
		prefEdit.putString("serverUrl", serverUrl);
		prefEdit.putString("kID", kID);
		
		prefEdit.commit();
	}
	
	private void loadConfig(){
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		
		mgeSession = pref.getString("mgeSession", null);
		serverUrl = pref.getString("serverUrl", null);
		kID = pref.getString("kID", null);
	}

	private Notification getActivityNotification(NotificationInfo notificationInfo) {
		Intent main = getApplicationContext().getPackageManager().getLaunchIntentForPackage(getApplicationContext().getPackageName());
		
		if(notificationInfo.amount > 1){
			main.putExtra("notification", "-1");
		}else{
			main.putExtra("notification", notificationInfo.lastMessageID);
		}
		
		main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, main, PendingIntent.FLAG_UPDATE_CURRENT);

		int icon = R.drawable.star_big_on;
		int normalIcon = getResources().getIdentifier("icon", "drawable", getPackageName());

		if (normalIcon != 0) {
			icon = normalIcon;
		}

		Notification.Builder builder = new Notification.Builder(this);
		
		if(notificationInfo.amount > 1){
			builder.setContentTitle(notificationInfo.amount + " notificações");
			builder.setContentText("Você possui algumas notificações no DashViewer");
		}else{
			builder.setContentTitle("Notificação");
			builder.setContentText(notificationInfo.lastMessage);
		}
		
		builder.setSmallIcon(icon);
		builder.setContentIntent(pendingIntent);
		builder.setAutoCancel(true);
		builder.setLights(Color.GREEN, 300, 1000);

		Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		builder.setSound(alarmSound);

		Notification notification;

		if (android.os.Build.VERSION.SDK_INT >= 16) {
			notification = buildForegroundNotification(builder);
		} else {
			notification = buildForegroundNotificationCompat(builder);
		}

		notification.defaults = 0;
		notification.flags |= Notification.FLAG_SHOW_LIGHTS | Notification.FLAG_AUTO_CANCEL | Notification.DEFAULT_VIBRATE;
		notification.ledARGB = 0xff00ff00;
		notification.ledOnMS = 300;
		notification.ledOffMS = 1000;

		return notification;
	}

	@TargetApi(16)
	private Notification buildForegroundNotification(Notification.Builder builder) {
		return builder.build();
	}

	@SuppressWarnings("deprecation")
	@TargetApi(15)
	private Notification buildForegroundNotificationCompat(Notification.Builder builder) {
		return builder.getNotification();
	}

	@Override
	protected JSONObject initialiseLatestResult() {
		loadConfig();
		return null;
	}

	@Override
	protected void onTimerEnabled() {

	}

	@Override
	protected void onTimerDisabled() {

	}

	@Override
	protected JSONObject getConfig() {
		return null;
	}
}
