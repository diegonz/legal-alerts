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

    @Override
    public void onReceive(Context context, Intent intent) {
        final String START_ALARMS_SERVICE = "es.smartidea.legalalerts.START_ALARMS_SERVICE";
        final String START_MANUAL_SYNC_SERVICE = "es.smartidea.legalalerts.START_MANUAL_SYNC_SERVICE";
        if (intent.getAction().equals(START_ALARMS_SERVICE)){

            // Get shared preferences
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

            boolean userPrefOK = true;
            // Check charging status
            if (sharedPreferences.getBoolean("sync_only_when_charging", true) && !deviceIsCharging(context)) {
                Log.d("Service", "Charging required!");
                userPrefOK = false;
            }

            // Check connectivity type and status
            if (!wanAvailable(context)) {
                Log.d("Service", "Unavailable WAN!");
                userPrefOK = false;
            } else {
                if (sharedPreferences.getBoolean("sync_only_over_unmetered_network", true) && !unmeteredNetworkAvailable(context)) {
                    Log.d("Service", "WiFi required!");
                    userPrefOK = false;
                }
            }

            // Launch service if meet the requirements
            if (userPrefOK){
                context.startService(new Intent(context, AlertsService.class));
            }

        } else if (intent.getAction().equals(START_MANUAL_SYNC_SERVICE)){
            context.startService(new Intent(context, AlertsService.class));
        }
    }

    /* Methods checking for user preferences */

    // Check charging state
    public boolean deviceIsCharging(Context context) {
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        // TODO: Check warning about battery
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
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
