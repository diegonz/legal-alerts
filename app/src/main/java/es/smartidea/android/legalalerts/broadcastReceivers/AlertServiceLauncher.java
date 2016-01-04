package es.smartidea.android.legalalerts.broadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.util.Log;

import es.smartidea.android.legalalerts.AlertsService;

/*
* Simple BroadcastReceiver subclass
* Manages direct order to star th Alarms Service
* */
public class AlertServiceLauncher extends BroadcastReceiver {
    public AlertServiceLauncher() {
    }

    public final static String START_ALARMS_SERVICE = "es.smartidea.legalalerts.START_ALARMS_SERVICE";
    public final static String START_MANUAL_SYNC_SERVICE = "es.smartidea.legalalerts.START_MANUAL_SYNC_SERVICE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(START_ALARMS_SERVICE)){

            // Get shared preferences
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

            // User preferences requirements boolean flag
            // TODO: Refactor checking, external/inner class?
            boolean userPrefOK = true;

            // Check connectivity type and status
            if (!wanAvailable(context)) {
                Log.d("ServiceLauncher", "Unavailable WAN!");
                userPrefOK = false;
            } else {

                // Check charging status
                if (sharedPreferences.getBoolean("sync_only_when_charging", true) && !deviceIsCharging(context)) {
                    Log.d("ServiceLauncher", "Device charging required!");
                    userPrefOK = false;
                }

                if (sharedPreferences.getBoolean("sync_only_over_unmetered_network", true) && !unmeteredNetworkAvailable(context)) {
                    Log.d("ServiceLauncher", "WiFi network required!");
                    userPrefOK = false;
                }
            }

            // Launch service if meet the requirements
            if (userPrefOK){

                Log.d("ServiceLauncher", "Preferences requirements OK, launching service.");

                // User preferences OK, star service
                context.startService(new Intent(context, AlertsService.class));
            } else {
                Log.d("ServiceLauncher", "Doesn't meet user preferences requirements.");
            }

        } else if (intent.getAction().equals(START_MANUAL_SYNC_SERVICE)){

            if (wanAvailable(context)) {

                Log.d("ServiceLauncher", "Manual sync started, launching service.");

                // Manual sync requested TODO: Implement dialog according to user preferences
                context.startService(new Intent(context, AlertsService.class));
            } else {
                Log.d("ServiceLauncher", "Manual sync failed, unavailable WAN.");
            }
        }
    }

    /* Methods for checking about user preferences requirements */

    // Check charging state
    public boolean deviceIsCharging(Context context) {
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        // TODO: Check warning about battery
        int plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
    }

    // Check for unmetered network availability
    private boolean unmeteredNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return (networkInfo != null && (networkInfo.getType() == ConnectivityManager.TYPE_WIFI));
    }

    // Check for available internet connection
    public boolean wanAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

}
