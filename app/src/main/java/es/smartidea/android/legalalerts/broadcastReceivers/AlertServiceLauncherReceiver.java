package es.smartidea.android.legalalerts.broadcastReceivers;

import android.annotation.SuppressLint;
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
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

import es.smartidea.android.legalalerts.AlertsService;
import es.smartidea.android.legalalerts.R;

/**
* Simple {@link BroadcastReceiver} subclass
* Manages direct order to start the alerts service
* */
public class AlertServiceLauncherReceiver extends BroadcastReceiver {

    public AlertServiceLauncherReceiver() {
    }

    // ServiceLauncherReceiver related String Broadcast actions & extras
    public final static String START_ALERTS_SERVICE = "es.smartidea.legalalerts.START_ALERTS_SERVICE";
    public final static String START_MANUAL_SYNC_SERVICE = "es.smartidea.legalalerts.START_MANUAL_SYNC_SERVICE";

    // AlarmReceiver related String Broadcast actions & extras
    private final static String ALARM_SNOOZE = AlertsAlarmReceiver.ALARM_SNOOZE;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Check intent action
        switch (intent.getAction()){
            case START_ALERTS_SERVICE:
                // Launch service if wan is available and user preference requirements are ok
                if (!isWanAvailable(context) || isPreferenceCheckOK(context)) {
                    Log.d("ServiceLauncher", "Preferences requirements OK, launching service.");
                    // User preferences OK, star service
                    context.startService(new Intent(context, AlertsService.class));
                } else {
                    Log.d("ServiceLauncher", "Don't meet requirements. Snoozing alarm one hour...");
                    snoozeAlarm(context);
                }
                break;
            case START_MANUAL_SYNC_SERVICE:
                if (isWanAvailable(context)) {
                    Log.d("ServiceLauncher", "Manual sync started, launching service.");
                    // Manual sync requested TODO: Add confirm dialog according to user preferences
                    context.startService(new Intent(context, AlertsService.class));
                } else {
                    Log.d("ServiceLauncher", "Manual sync failed, unavailable WAN.");
                    // Notify no wan available via toast message
                    Toast.makeText(context, context.getString(R.string.toast_no_wan), Toast.LENGTH_SHORT)
                            .show();
                }
                break;
        }
    }

    /* Methods for checking about user preferences requirements */

    // Check charging state
    public static boolean isDeviceCharging(Context context) {
        // http://developer.android.com/intl/es/training/monitoring-device-state/battery-monitoring.html#DetermineChargeState
        @SuppressWarnings("ConstantConditions")
        int plugged = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                .getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

        return plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                plugged == BatteryManager.BATTERY_PLUGGED_USB;
    }

    // Check for unmetered network availability
    public static boolean isUnmeteredNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return (networkInfo != null && (networkInfo.getType() == ConnectivityManager.TYPE_WIFI));
    }

    // Check for available internet connection
    public static boolean isWanAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    // Check user preferences, returns true/false depending if all requirements are accomplished
    public static boolean isPreferenceCheckOK(Context context){

        // Get shared preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        // Check charging status
        if (preferences.getBoolean("sync_only_when_charging", true)
                && !isDeviceCharging(context)) {
                    Log.d("ServiceLauncher", "Device charging required!");
                    return false;
        }
        // Check unmetered network
        if (preferences.getBoolean("sync_only_over_unmetered_network", true)
                && !isUnmeteredNetworkAvailable(context)) {
                    Log.d("ServiceLauncher", "WiFi network required!");
                    return false;
        }
        return true;
    }

    // Send broadcast to create a new retry alarm from now to about one hour
    private static void snoozeAlarm(Context context){
        @SuppressLint("SimpleDateFormat")
        String todayDateString = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String snoozeDateString = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("snooze_alarm_date", "done");
        // Check snoozeDateString, if its "done" AND
        if (!snoozeDateString.equals("done") && snoozeDateString.equals(todayDateString)){
            context.sendBroadcast(
                    new Intent(context, AlertsAlarmReceiver.class)
                            .setAction(ALARM_SNOOZE)
            );
            Toast.makeText(context, "Snoozing alarm one hour...", Toast.LENGTH_SHORT).show();
        } else if (!snoozeDateString.equals(todayDateString)){
            // If day has changed, dismiss snoozed alarm (next check, daily alarm)
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putString("snooze_alarm_date", "done")
                    .commit(); // Call commit() to make changes
        }
    }
}
