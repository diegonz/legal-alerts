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
import android.widget.Toast;

import es.smartidea.android.legalalerts.R;
import es.smartidea.android.legalalerts.alarms.AlarmDelayer;
import es.smartidea.android.legalalerts.receivers.AlarmReceiver;
import es.smartidea.android.legalalerts.network.NetWorker;
import es.smartidea.android.legalalerts.utils.FileLogger;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * helper methods.
 */
public class ServiceStarter extends IntentService {

    private final static String LOG_TAG = "ServiceLauncher";

    public final static String ALARM_SYNC = "es.smartidea.legalalerts.ALARM_SYNC";
    public final static String MANUAL_SYNC = "es.smartidea.legalalerts.MANUAL_SYNC";
    public final static String MANUAL_SYNC_STRING = "MANUAL_SYNC";

    public ServiceStarter() {
        super("ServiceStarter");
    }

    /**
     * Starts this service to perform action startServiceManual. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startServiceManual(Context context) {
        Intent intent = new Intent(context, ServiceStarter.class);
        intent.setAction(MANUAL_SYNC);
        context.startService(intent);
    }

    // Added @NonNull annotation to avoid inner if-else checking
    @Override
    protected void onHandleIntent(@NonNull Intent intent) {
        // Acquire WakeLock at first, and assign to its
        // specific static reference holder class
        AlertsWakeLock.setWakeLock(this);

        switch (intent.getAction()) {
            case ALARM_SYNC:
                handleStartServiceDefault();
                break;
            case MANUAL_SYNC:
                handleStartServiceManual();
                break;
            case AlarmReceiver.ALARM_SNOOZE:
                handleStartServiceDefault();
                break;
            default:
                // Log to file for debugging
                FileLogger.logToExternalFile(
                        LOG_TAG + " - @onHandleIntent() No matching action!: " + intent.getAction());
                // If there is no matching action release the WakeLock
                AlertsWakeLock.doRelease();
                break;
        }
    }

    /**
     * Handle action handleStartServiceDefault in the provided background thread.
     */
    private void handleStartServiceDefault() {
        // Launch service if wan is available and user preference requirements are ok
        if (isWanAvailable() && isUserPrefMet(this)) {
            // Log to file for debugging
            FileLogger.logToExternalFile(LOG_TAG + " - Preferences requirements OK, launching service.");
            startService(new Intent(this, AlertsService.class));
        } else {
            // Log to file for debugging
            FileLogger.logToExternalFile(LOG_TAG + " - Don't meet requirements. Snoozing alarm one hour...");
            // Snooze alarm for 1 hour
            AlarmDelayer.snoozeAlarm(this);
            AlertsWakeLock.doRelease();
        }
    }

    /**
     * Handle action handleStartServiceManual in the provided background thread.
     */
    private void handleStartServiceManual() {
        if (isWanAvailable()) {
            // Log to file for debugging
            FileLogger.logToExternalFile(LOG_TAG + " - Manual sync started, launching service.");
            // Manual sync requested TODO: Add confirm dialog according to user preferences
            startService(new Intent(this, AlertsService.class)
                    .putExtra(MANUAL_SYNC_STRING, MANUAL_SYNC));
        } else {
            Toast.makeText(this, getText(R.string.text_toast_no_wan), Toast.LENGTH_SHORT).show();
            // Log to file for debugging
            FileLogger.logToExternalFile(LOG_TAG + " - Manual sync failed, due to unavailable WAN.");
            // If service was not launched release the WakeLock
            AlertsWakeLock.doRelease();
        }
    }

    /**
     * Check user preferences, returning TRUE or FALSE
     * depending if all requirements are accomplished.
     *
     * @param context Context of Application to get user preferences
     * @return TRUE if user preferences are successfully checked
     */
    private static boolean isUserPrefMet(Context context) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean chargingRequired = preferences.getBoolean("sync_only_when_charging", true);
        boolean unmeteredRequired = preferences.getBoolean("sync_only_over_unmetered_network", true);

        if (chargingRequired && !isDeviceCharging(context)) {
            // Log to file for debugging
            FileLogger.logToExternalFile(LOG_TAG + " - ERROR: Device charging required!");
            return false;
        }
        if (unmeteredRequired && !isUnmeteredNetwork(context)) {
            // Log to file for debugging
            FileLogger.logToExternalFile(LOG_TAG + " - ERROR: WiFi network required!");
            return false;
        }
        return true;
    }

    /**
     * Checks for current charging state creating a new IntentFilter
     * passing null as BroadcastReceiver to get actual info
     * without registering a receiver.
     * +info about implementation and NPE warning: http://goo.gl/DVPW9i
     *
     * @param context   Context of Application to get BatteryManager
     * @return  TRUE if the device is plugged to AC or USB
     */
    private static boolean isDeviceCharging(Context context) {
        @SuppressWarnings("ConstantConditions")
        int pluggedState =
                context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                        .getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

        return pluggedState == BatteryManager.BATTERY_PLUGGED_AC ||
                pluggedState == BatteryManager.BATTERY_PLUGGED_USB;
    }

    /**
     * Checks for available working internet connection
     *
     * @return TRUE if WAN access to internet is currently available
     */
    private static boolean isWanAvailable() {
        return new NetWorker().isWanAvailable();
    }

    /**
     * Checks for availability of unmetered networks
     *
     * @param context   Context of Application to get connectivity service
     * @return  TRUE if unmetered network is currently available
     */
    private static boolean isUnmeteredNetwork(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return (networkInfo != null && (networkInfo.getType() == ConnectivityManager.TYPE_WIFI));
    }
}
