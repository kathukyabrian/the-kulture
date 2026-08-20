package tech.kitucode.kulture;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;
import android.webkit.CookieManager;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

public class LocationBroadcastService extends Service {
	public static final String ACTION_START = "tech.kitucode.kulture.START_LOCATION_BROADCAST";
	public static final String ACTION_STOP = "tech.kitucode.kulture.STOP_LOCATION_BROADCAST";
	public static final String EXTRA_VEHICLE_ID = "vehicleId";
	public static final String EXTRA_API_BASE_URL = "apiBaseUrl";
	public static final String EXTRA_VEHICLE_NAME = "vehicleName";
	public static final String PREFERENCES = "location_broadcast_state";
	public static final String PREFERENCE_ACTIVE = "active";
	public static final String PREFERENCE_VEHICLE_ID = "vehicleId";
	private static final String CHANNEL_ID = "location_broadcast";
	private static final int NOTIFICATION_ID = 4101;
	private static final String TAG = "KultureLocation";
	private static final long MAX_UPLOAD_INTERVAL_MS = 30000;
	private static final float MIN_UPLOAD_DISTANCE_METERS = 25;
	private final String sessionId = UUID.randomUUID().toString();
	private final ExecutorService uploads = Executors.newSingleThreadExecutor();
	private FusedLocationProviderClient locations;
	private String vehicleId;
	private String apiBaseUrl;
	private String vehicleName;
	private Location lastSubmittedLocation;

	private final LocationCallback callback = new LocationCallback() {
		@Override public void onLocationResult(LocationResult result) {
			Location location = result.getLastLocation();
			if (location == null) { Log.w(TAG, "Location callback contained no location"); updateNotification("Waiting for a GPS fix"); return; }
			Log.i(TAG, "Location fix lat=" + location.getLatitude() + " lon=" + location.getLongitude() + " accuracy=" + location.getAccuracy() + "m");
			if (!location.hasAccuracy() || location.getAccuracy() > 50) { Log.w(TAG, "Ignoring low-accuracy location: " + location.getAccuracy() + "m"); updateNotification("Waiting for better GPS accuracy (" + Math.round(location.getAccuracy()) + "m)"); return; }
			if (lastSubmittedLocation != null) {
				long elapsedMs = location.getTime() - lastSubmittedLocation.getTime();
				float distanceMeters = lastSubmittedLocation.distanceTo(location);
				if (elapsedMs < MAX_UPLOAD_INTERVAL_MS && distanceMeters < MIN_UPLOAD_DISTANCE_METERS) {
					Log.d(TAG, "Skipping location; elapsed=" + elapsedMs + "ms distance=" + Math.round(distanceMeters) + "m");
					return;
				}
			}
			lastSubmittedLocation = new Location(location);
			updateNotification("GPS fix acquired; sending location");
			uploads.execute(() -> upload(location));
		}
	};

	@Override public void onCreate() {
		super.onCreate();
		locations = LocationServices.getFusedLocationProviderClient(this);
		NotificationManager manager = getSystemService(NotificationManager.class);
		manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "Live nganya location", NotificationManager.IMPORTANCE_LOW));
	}

	@Override public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null && ACTION_STOP.equals(intent.getAction())) { stopBroadcasting(); return START_NOT_STICKY; }
		if (intent == null || !ACTION_START.equals(intent.getAction())) return START_NOT_STICKY;
		vehicleId = intent.getStringExtra(EXTRA_VEHICLE_ID);
		apiBaseUrl = intent.getStringExtra(EXTRA_API_BASE_URL);
		vehicleName = intent.getStringExtra(EXTRA_VEHICLE_NAME);
		Log.i(TAG, "Starting broadcast vehicleId=" + vehicleId + " apiBaseUrl=" + apiBaseUrl);
		getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit().putBoolean(PREFERENCE_ACTIVE, true).putString(PREFERENCE_VEHICLE_ID, vehicleId).apply();
		Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
		Intent stop = new Intent(this, LocationBroadcastService.class).setAction(ACTION_STOP);
		PendingIntent stopIntent = PendingIntent.getService(this, 1, stop, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
		startForeground(NOTIFICATION_ID, new NotificationCompat.Builder(this, CHANNEL_ID)
			.setSmallIcon(R.mipmap.ic_launcher)
			.setContentTitle("The Kulture is sharing location")
			.setContentText("Broadcasting " + vehicleName)
			.setContentIntent(contentIntent).setOngoing(true)
			.addAction(0, "Stop", stopIntent).build());
		startUpdates();
		return START_REDELIVER_INTENT;
	}

	private void startUpdates() {
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) { Log.e(TAG, "Location permission is missing"); updateNotification("Location permission is missing"); stopBroadcasting(); return; }
		LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).setMinUpdateIntervalMillis(5000).build();
		locations.requestLocationUpdates(request, callback, getMainLooper()).addOnSuccessListener(unused -> Log.i(TAG, "Fused location updates registered")).addOnFailureListener(error -> { Log.e(TAG, "Could not register location updates", error); updateNotification("Could not start GPS: " + error.getMessage()); });
	}

	private void upload(Location location) {
		HttpURLConnection connection = null;
		try {
			String endpoint = apiBaseUrl + "/crew/vehicles/" + vehicleId + "/locations";
			Log.i(TAG, "Uploading coordinates to " + endpoint);
			JSONObject sample = new JSONObject().put("sampleId", UUID.randomUUID().toString()).put("latitude", location.getLatitude()).put("longitude", location.getLongitude()).put("accuracyMeters", location.getAccuracy()).put("speedKph", Math.max(0, Math.min(160, Math.round(location.getSpeed() * 3.6f)))).put("recordedAt", Instant.ofEpochMilli(location.getTime()).toString());
			if (location.hasBearing()) sample.put("headingDegrees", location.getBearing());
			JSONObject body = new JSONObject().put("sessionId", sessionId).put("samples", new JSONArray().put(sample));
			connection = (HttpURLConnection) new URL(endpoint).openConnection();
			connection.setRequestMethod("POST"); connection.setConnectTimeout(15000); connection.setReadTimeout(15000); connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "application/json");
			String cookies = CookieManager.getInstance().getCookie(apiBaseUrl);
			Log.i(TAG, cookies == null || cookies.isBlank() ? "No API session cookie is available" : "API session cookie found");
			if (cookies != null) connection.setRequestProperty("Cookie", cookies);
			try (OutputStream output = connection.getOutputStream()) { output.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
			int status = connection.getResponseCode();
			Log.i(TAG, "Location upload completed status=" + status);
			if (status >= 200 && status < 300) updateNotification("Location sent successfully");
			else updateNotification("Location upload failed (HTTP " + status + ")");
			if (status == 401 || status == 403 || status == 409) { Log.e(TAG, "Stopping broadcast after HTTP " + status); stopBroadcasting(); }
		} catch (Exception error) {
			Log.e(TAG, "Location upload failed", error);
			updateNotification("Upload failed: " + error.getClass().getSimpleName());
		} finally { if (connection != null) connection.disconnect(); }
	}

	private void updateNotification(String message) {
		NotificationManager manager = getSystemService(NotificationManager.class);
		Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
		Intent stop = new Intent(this, LocationBroadcastService.class).setAction(ACTION_STOP);
		PendingIntent stopIntent = PendingIntent.getService(this, 1, stop, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
		manager.notify(NOTIFICATION_ID, new NotificationCompat.Builder(this, CHANNEL_ID)
			.setSmallIcon(R.mipmap.ic_launcher).setContentTitle("The Kulture location: " + vehicleName)
			.setContentText(message).setContentIntent(contentIntent).setOngoing(true).addAction(0, "Stop", stopIntent).build());
	}

	private void stopBroadcasting() { getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit().putBoolean(PREFERENCE_ACTIVE, false).remove(PREFERENCE_VEHICLE_ID).apply(); if (locations != null) locations.removeLocationUpdates(callback); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); }
	@Override public void onDestroy() { getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit().putBoolean(PREFERENCE_ACTIVE, false).remove(PREFERENCE_VEHICLE_ID).apply(); if (locations != null) locations.removeLocationUpdates(callback); uploads.shutdownNow(); super.onDestroy(); }
	@Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
