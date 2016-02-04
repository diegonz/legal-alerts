package es.smartidea.android.legalalerts.alertsServices;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import es.smartidea.android.legalalerts.broadcastReceivers.AlertsAlarmReceiver;
import es.smartidea.android.legalalerts.okHttp.OkHttpGetURL;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * helper methods.
 */
public class AlertsServiceStarter extends IntentService {
    private static final String LOG_TAG = "ServiceLauncher";
    // ServiceLauncherReceiver related String Broadcast actions & extras
    public final static String START_ALERTS_SERVICE =
            "es.smartidea.legalalerts.START_ALERTS_SERVICE";
    public final static String START_MANUAL_SYNC_SERVICE =
            "es.smartidea.legalalerts.START_MANUAL_SYNC_SERVICE";

    // AlarmReceiver related String Broadcast actions & extras
    private final static String ALARM_SNOOZE = AlertsAlarmReceiver.ALARM_SNOOZE;

    public AlertsServiceStarter() {
        super("AlertsServiceStarter");
    }

    /**
     * Starts this service to check wan availability and check user preferences too. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
//    public static void startServiceDefault(Context context) {
//        Intent intent = new Intent(context, AlertsServiceStarter.class);
//        intent.setAction(START_ALERTS_SERVICE);
//        context.startService(intent);
//    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startServiceManual(Context context) {
        Intent intent = new Intent(context, AlertsServiceStarter.class);
        intent.setAction(START_MANUAL_SYNC_SERVICE);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (START_ALERTS_SERVICE.equals(action)) {
                handleStartServiceDefault();
            } else if (START_MANUAL_SYNC_SERVICE.equals(action)) {
                handleStartServiceManual();
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleStartServiceDefault() {
        // Launch service if wan is available and user preference requirements are ok
        if (isWanAvailable() && isPreferenceCheckOK(this)) {
            Log.d(LOG_TAG, "Preferences requirements OK, launching service.");
            // User preferences OK, star service
            startService(new Intent(this, AlertsService.class));
        } else {
            Log.d(LOG_TAG, "Don't meet requirements. Snoozing alarm one hour...");
            // Snooze alarm for 1 hour
            snoozeAlarm(this);
        }
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleStartServiceManual() {
        if (isWanAvailable()) {
            Log.d(LOG_TAG, "Manual sync started, launching service.");
            // Manual sync requested TODO: Add confirm dialog according to user preferences
            startService(new Intent(this, AlertsService.class));
        } else {
            Log.d(LOG_TAG, "Manual sync failed, unavailable WAN.");
            // TODO: Inform user about network unavailable
        }
    }

    // Send broadcast to create a new retry alarm from now to about one hour
    private static void snoozeAlarm(Context context) {
        @SuppressLint("SimpleDateFormat")
        String todayDateString = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String snoozeDateString = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("snooze_alarm_date", "done");
        // Check snoozeDateString, if its "done" AND
        if (!snoozeDateString.equals("done") && snoozeDateString.equals(todayDateString)) {
            context.sendBroadcast(
                    new Intent(context, AlertsAlarmReceiver.class)
                            .setAction(ALARM_SNOOZE)
            );
            Log.d(LOG_TAG, "Snoozing alarm one hour...");
        } else if (!snoozeDateString.equals(todayDateString)) {
            // If day has changed, dismiss snoozed alarm (next check, daily alarm)
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putString("snooze_alarm_date", "done")
                    .commit(); // Call commit() to make changes
        }
    }

    /* Methods for checking about user preferences requirements */

    // Check charging state, for warning info about NPE  check: http://goo.gl/DVPW9i
    public static boolean isDeviceCharging(Context context) {
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
    public static boolean isWanAvailable() {
        try {
            return new OkHttpGetURL().isWanAvailable();
        } catch (IOException e) {
            Log.d(LOG_TAG, "Unreachable WAN: \n" + e);
            return false;
        }
    }

    // Check user preferences, returns true/false depending if all requirements are accomplished
    public static boolean isPreferenceCheckOK(Context context) {

        // Get shared preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        // Check charging status
        if (preferences.getBoolean("sync_only_when_charging", true)
                && !isDeviceCharging(context)) {
            Log.d(LOG_TAG, "Device charging required!");
            return false;
        }
        // Check unmetered network
        if (preferences.getBoolean("sync_only_over_unmetered_network", true)
                && !isUnmeteredNetworkAvailable(context)) {
            Log.d(LOG_TAG, "WiFi network required!");
            return false;
        }
        return true;
    }
}
