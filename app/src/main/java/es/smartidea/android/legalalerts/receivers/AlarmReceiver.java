package es.smartidea.android.legalalerts.receivers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import es.smartidea.android.legalalerts.alarms.AlarmBuilder;
import es.smartidea.android.legalalerts.services.ServiceStarter;
import es.smartidea.android.legalalerts.utils.FileLogger;

public class AlarmReceiver extends BroadcastReceiver {

    private final static String LOG_TAG = "AlarmReceiver";

    // ServiceLauncherReceiver related String Broadcast actions & extras
    public final static String ALARM_SNOOZE = "es.smartidea.legalalerts.ALARM_SNOOZE";
    public final static int DAILY_ALARM_ID = 0;
    public final static int SNOOZE_ALARM_ID = 1;
    private final static String START_ALERTS_SERVICE = ServiceStarter.START_ALERTS_SERVICE;

    public AlarmReceiver() {}

    @Override
    public void onReceive(Context context, Intent intent) {
        /*
        * This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        *
        * Check if another PendingIntent (Alarm) exists
        * via creating new one with flag PendingIntent.FLAG_NO_CREATE
        * which returns null if exists.
        * +info check Android documentation: http://goo.gl/V3WnGz
        */

        // Get intent action or set to default for setup alarm (also BOOT_COMPLETED)
        final String ALARM_TYPE = intent.getAction() != null
                ? intent.getAction()
                : START_ALERTS_SERVICE;

        if (ALARM_TYPE.equals(ALARM_SNOOZE)){
            // Create a snooze alarm passing correct alarm ID
            new AlarmBuilder.Builder(context, ALARM_TYPE, SNOOZE_ALARM_ID).setRetryAlarm();

            // Log to file for debugging
            FileLogger.logToExternalFile(LOG_TAG + " - CREATING " + ALARM_TYPE);
        } else {
            PendingIntent alarmIntent =
                    PendingIntent.getService(
                            context,
                            DAILY_ALARM_ID,
                            new Intent(context, ServiceStarter.class).setAction(START_ALERTS_SERVICE),
                            PendingIntent.FLAG_NO_CREATE
                    );
            // Check if an alarm of same type exists
            //noinspection VariableNotUsedInsideIf
            if (alarmIntent == null) {
                // Alarm not exist, create it
                new AlarmBuilder.Builder(context, START_ALERTS_SERVICE, DAILY_ALARM_ID)
                        .setHour(9).setMinute(30).setDailyAlarm();
                // Log to file for debugging
                FileLogger.logToExternalFile(LOG_TAG + " - CREATING " + ALARM_TYPE);
            } else {
                // Log to file for debugging
                FileLogger.logToExternalFile(LOG_TAG + " -" + ALARM_TYPE + " already set SKIPPING.");
            }
        }
    }
}