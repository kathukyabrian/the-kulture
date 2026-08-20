package tech.kitucode.kulture;

import android.Manifest;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;

@CapacitorPlugin(
	name = "LocationBroadcast",
	permissions = @Permission(alias = "location", strings = { Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION })
)
public class LocationBroadcastPlugin extends Plugin {
	@PluginMethod
	public void start(PluginCall call) {
		if (getPermissionState("location") != com.getcapacitor.PermissionState.GRANTED) {
			requestPermissionForAlias("location", call, "startAfterPermission");
			return;
		}
		startService(call);
	}

	@com.getcapacitor.annotation.PermissionCallback
	private void startAfterPermission(PluginCall call) {
		if (getPermissionState("location") == com.getcapacitor.PermissionState.GRANTED) startService(call);
		else call.reject("Location permission was denied");
	}

	private void startService(PluginCall call) {
		String vehicleId = call.getString("vehicleId");
		String apiBaseUrl = call.getString("apiBaseUrl");
		String vehicleName = call.getString("vehicleName", "your nganya");
		if (vehicleId == null || apiBaseUrl == null) { call.reject("vehicleId and apiBaseUrl are required"); return; }
		Intent intent = new Intent(getContext(), LocationBroadcastService.class)
			.setAction(LocationBroadcastService.ACTION_START)
			.putExtra(LocationBroadcastService.EXTRA_VEHICLE_ID, vehicleId)
			.putExtra(LocationBroadcastService.EXTRA_API_BASE_URL, apiBaseUrl)
			.putExtra(LocationBroadcastService.EXTRA_VEHICLE_NAME, vehicleName);
		ContextCompat.startForegroundService(getContext(), intent);
		call.resolve(new JSObject().put("active", true));
	}

	@PluginMethod
	public void stop(PluginCall call) {
		Intent intent = new Intent(getContext(), LocationBroadcastService.class).setAction(LocationBroadcastService.ACTION_STOP);
		getContext().startService(intent);
		call.resolve(new JSObject().put("active", false));
	}

	@PluginMethod
	public void getStatus(PluginCall call) {
		var preferences = getContext().getSharedPreferences(LocationBroadcastService.PREFERENCES, android.content.Context.MODE_PRIVATE);
		JSObject status = new JSObject().put("active", preferences.getBoolean(LocationBroadcastService.PREFERENCE_ACTIVE, false));
		String vehicleId = preferences.getString(LocationBroadcastService.PREFERENCE_VEHICLE_ID, null);
		if (vehicleId != null) status.put("vehicleId", vehicleId);
		call.resolve(status);
	}
}
