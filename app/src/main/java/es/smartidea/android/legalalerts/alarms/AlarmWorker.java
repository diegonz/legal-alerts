package es.smartidea.android.legalalerts.alarms;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import es.smartidea.android.legalalerts.receivers.AlarmReceiver;

/**
 * Static class that handles up to date syncing check and snoozing alarm logic,
 * also sends a broadcast message to create a new retry alarm
 * from now to "about" one hour (InexactRepeating) if its
 * the same day as snoozing started.
 */
public class AlarmWorker {

    private static final String LOG_TAG = "AlarmWorker";

    public static final String LAST_SUCCESSFUL_SYNC = "last_successful_sync";
    // AlarmReceiver related String Broadcast actions & extras
    private final static String ALARM_SNOOZE = AlarmReceiver.ALARM_SNOOZE;
    // SNOOZE dateString default value
    private static final String SNOOZE_DATE_DEFAULT = "default_value";


    // Private empty constructor to avoid instantiation
    private AlarmWorker() {
    }

    /**
     * Checks last successful sync date against today´s date
     *
     * @param context   Context of application to get SharedPreferences
     * @return  TRUE if today has synced OK
     */
    public static boolean isSyncUpToDate(final Context context){
        @SuppressLint("SimpleDateFormat")
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

        final String lastSuccessfulSyncString = PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(LAST_SUCCESSFUL_SYNC, dateFormat.format(new Date()));
        try {
            return dateFormat.parse(lastSuccessfulSyncString)
                    .equals(dateFormat.parse(dateFormat.format(new Date())));
        } catch (ParseException e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Checks for snoozed alarm state and sends a broadcast message
     * to create a new retry alarm from now to "about" one hour (InexactRepeating)
     * if its the same day as snoozing started.
     *
     * @param context Context of Application to send Broadcast
     *                and get user preferences (for checking snooze alarm status)
     */
    public static void snoozeAlarm(final Context context) {
        @SuppressLint("SimpleDateFormat")
        final String todayDateString = new SimpleDateFormat("yyyyMMdd").format(new Date());
        final String snoozeDateString = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("snooze_alarm_date", SNOOZE_DATE_DEFAULT);
        switch (snoozeDateString) {

            // Empty or new snoozing request
            case SNOOZE_DATE_DEFAULT:
                setupSnooze(context, todayDateString);
                Log.d(LOG_TAG, "Snoozing alarm one hour...");
                break;

            // Default when has any date String to compare
            default:
                if (snoozeDateString.equals(todayDateString)) {
                    setupSnooze(context, todayDateString);
                    Log.d(LOG_TAG, "Snoozing alarm one hour...");
                } else {
                    setupSnooze(context, SNOOZE_DATE_DEFAULT);
                    Log.d(LOG_TAG, "Day has changed, DO NOT SET any alarm");
                }
                break;
        }
    }

    /**
     * Sends a broadcast message to create a new retry alarm
     * from now to "about" one hour (InexactRepeating) if received
     * date in predefined String format not equals "default_value"
     *
     * @param context    Context of Application to send Broadcast
     *                   and get user preferences (for writing snooze alarm date)
     * @param dateString date in predefined String format to write to preferences
     *                   and to check if day has changed (not firing new alarms)
     */
    private static void setupSnooze(final Context context, final String dateString) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString("snooze_alarm_date", dateString)
                .apply(); // Call apply() to make changes in background (commit() is immediate)

        // If date has changed, DO NOT SET any snoozed alarm (next check, daily alarm)
        if (!dateString.equals(SNOOZE_DATE_DEFAULT)) {
            context.sendBroadcast(
                    new Intent(context, AlarmReceiver.class).setAction(ALARM_SNOOZE)
            );
        }
    }

}
