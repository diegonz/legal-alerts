package es.smartidea.android.legalalerts.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;

import es.smartidea.android.legalalerts.alarms.AlarmWorker;
import es.smartidea.android.legalalerts.receivers.AlarmReceiver;
import es.smartidea.android.legalalerts.okHttp.OkHttpGetURL;
import es.smartidea.android.legalalerts.utils.FileLogger;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * helper methods.
 */
public class ServiceStarter extends IntentService {

    private static final String LOG_TAG = "ServiceLauncher";

    // ServiceLauncherReceiver related String Broadcast actions & extras
    public final static String START_ALERTS_SERVICE =
            "es.smartidea.legalalerts.START_ALERTS_SERVICE";
    public final static String START_MANUAL_SYNC_SERVICE =
            "es.smartidea.legalalerts.START_MANUAL_SYNC_SERVICE";
    private final static String ALARM_SNOOZE = AlarmReceiver.ALARM_SNOOZE;


    public ServiceStarter() {
        super("ServiceStarter");
    }

    /**
     * Starts this service to perform action startServiceManual. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startServiceManual(final Context context) {
        Intent intent = new Intent(context, ServiceStarter.class);
        intent.setAction(START_MANUAL_SYNC_SERVICE);
        context.startService(intent);
    }

    // Added @NonNull annotation to avoid inner if-else checking
    @Override
    protected void onHandleIntent(@NonNull  Intent intent) {
        // Acquire WakeLock at first, and assign to its
        // specific static reference holder class
        AlertsWakeLock.setWakeLock(this);

        String intentAction = intent.getAction();
        // Log to file for debugging TODO: REMOVE STRING intentAction AFTER DEBUG
        FileLogger.logToExternalFile(LOG_TAG + " - @onHandleIntent() action: " + intentAction);

        switch (intentAction) {
            case START_ALERTS_SERVICE:
                handleStartServiceDefault();
                break;
            case START_MANUAL_SYNC_SERVICE:
                handleStartServiceManual();
                break;
            case ALARM_SNOOZE:
                handleStartServiceDefault();
                break;
            default:
                // If there is no matching action release the WakeLock
                Log.d(LOG_TAG, "No matching action!");

                // Log to file for debugging
                FileLogger.logToExternalFile(LOG_TAG + " - @onHandleIntent() No matching action!: " + intentAction);

                AlertsWakeLock.doRelease();
                break;
        }
    }

    /**
     * Handle action handleStartServiceDefault in the provided background thread.
     */
    private void handleStartServiceDefault() {
        // Launch service if wan is available and user preference requirements are ok
        if (isWanAvailable() && isPreferenceCheckOK(this)) {
            Log.d(LOG_TAG, "Preferences requirements OK, launching service.");

            // Log to file for debugging
            FileLogger.logToExternalFile(LOG_TAG + " - Preferences requirements OK, launching service.");

            // User preferences OK, star service
            startService(new Intent(this, AlertsService.class));
        } else {
            Log.d(LOG_TAG, "Don't meet requirements. Snoozing alarm one hour...");

            // Log to file for debugging
            FileLogger.logToExternalFile(LOG_TAG + " - Don't meet requirements. Snoozing alarm one hour...");

            // Snooze alarm for 1 hour
            AlarmWorker.snoozeAlarm(this);

            // If service was not launched release the WakeLock
            AlertsWakeLock.doRelease();
        }
    }

    /**
     * Handle action handleStartServiceManual in the provided background thread.
     */
    private void handleStartServiceManual() {
        if (isWanAvailable()) {
            Log.d(LOG_TAG, "Manual sync started, launching service.");

            // Log to file for debugging
            FileLogger.logToExternalFile(LOG_TAG + " - Manual sync started, launching service.");

            // Manual sync requested TODO: Add confirm dialog according to user preferences
            startService(new Intent(this, AlertsService.class));
        } else {
            Log.d(LOG_TAG, "Manual sync failed, unavailable WAN.");
            // TODO: Inform user about network unavailable

            // Log to file for debugging
            FileLogger.logToExternalFile(LOG_TAG + " - Manual sync failed, due to unavailable WAN.");


            // If service was not launched release the WakeLock
            AlertsWakeLock.doRelease();
        }
    }

    /* Methods for checking about user preferences requirements */

    /**
     * Checks for current charging state creating a new IntentFilter
     * passing null as BroadcastReceiver to get actual info
     * without registering a receiver.
     * +info about implementation and NPE warning: http://goo.gl/DVPW9i
     *
     * @param context   Context of Application to get BatteryManager
     * @return  TRUE if the device is plugged to AC or USB
     */
    public static boolean isDeviceCharging(final Context context) {
        @SuppressWarnings("ConstantConditions")
        int plugged = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                .getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

        return plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                plugged == BatteryManager.BATTERY_PLUGGED_USB;
    }

    /**
     * Checks for available working internet connection
     *
     * @return TRUE if WAN access to internet is currently available
     */
    public static boolean isWanAvailable() {
        try {
            return new OkHttpGetURL().isWanAvailable();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Checks for availability of unmetered networks
     *
     * @param context   Context of Application to get connectivity service
     * @return  TRUE if unmetered network is currently available
     */
    public static boolean isUnmeteredNetworkAvailable(final Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return (networkInfo != null && (networkInfo.getType() == ConnectivityManager.TYPE_WIFI));
    }

    /**
     * Check user preferences, returning TRUE or FALSE
     * depending if all requirements are accomplished.
     *
     * @param context   Context of Application to get user preferences
     * @return  TRUE if user preferences are successfully checked
     */
    public static boolean isPreferenceCheckOK(final Context context) {

        // Get shared preferences
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        // Check charging status
        if (preferences.getBoolean("sync_only_when_charging", true) && !isDeviceCharging(context)) {
            Log.d(LOG_TAG, "Device charging required!");
            return false;
        }
        // Check unmetered network
        if (preferences.getBoolean("sync_only_over_unmetered_network", true) &&
                !isUnmeteredNetworkAvailable(context)) {
            Log.d(LOG_TAG, "WiFi network required!");
            return false;
        }
        return true;
    }
}
