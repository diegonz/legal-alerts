package es.smartidea.android.legalalerts.receivers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import es.smartidea.android.legalalerts.alarms.AlarmBuilder;
import es.smartidea.android.legalalerts.services.ServiceStarter;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "AlarmReceiver";

    // ServiceLauncherReceiver related String Broadcast actions & extras
    public final static String ALARM_SNOOZE = "es.smartidea.legalalerts.ALARM_SNOOZE";
    private final static String START_ALERTS_SERVICE = ServiceStarter.START_ALERTS_SERVICE;


    // AlarmReceiver public empty constructor
    public AlarmReceiver() {
    }

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

        // Get intent action or set to default for setup alarm
        final String ALARM_TYPE = intent.getAction() != null
                ? intent.getAction()
                : START_ALERTS_SERVICE;
        // Check if an alarm of same type exists
        PendingIntent alarmIntent =
                PendingIntent.getService(
                        context,
                        0,
                        new Intent(context, ServiceStarter.class).setAction(ALARM_TYPE),
                        PendingIntent.FLAG_NO_CREATE
                );
        //noinspection VariableNotUsedInsideIf
        if (alarmIntent == null) {
            // Alarm not exist, create it
            switch (ALARM_TYPE){
                case ALARM_SNOOZE:
                    new AlarmBuilder.Builder(context, ALARM_TYPE).setRetryAlarm();
                    break;
                default:
                    new AlarmBuilder.Builder(context, ALARM_TYPE).setHour(9).setMinute(30).setDailyAlarm();
                    break;
            }
            Log.d(LOG_TAG, "CREATING new " + ALARM_TYPE + " alarm.");
        } else {
            Log.d(LOG_TAG, "SKIPPING " + ALARM_TYPE + " already set.");
        }
    }
}