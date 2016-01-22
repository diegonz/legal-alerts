package es.smartidea.android.legalalerts.broadcastReceivers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import es.smartidea.android.legalalerts.alertsBuilders.AlertsAlarmBuilder;

public class AlertsAlarmReceiver extends BroadcastReceiver {

    // Setup intent action/s message
    public final static String ALARM_SNOOZE = "es.smartidea.legalalerts.ALARM_SNOOZE";
    public final static String SET_ALARM_FROM_ACTIVITY = "es.smartidea.legalalerts.SET_ALARM_FROM_ACTIVITY";

    // ServiceLauncherReceiver related String Broadcast actions & extras
    private final static String START_ALERTS_SERVICE = AlertServiceLauncherReceiver.START_ALERTS_SERVICE;


    // AlertsAlarmReceiver public empty constructor
    public AlertsAlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        /*
        * This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        *
        * Check if another PendingIntent (Alarm) exists
        * via creating new one with flag PendingIntent.FLAG_NO_CREATE
        * which returns null if exists, +info check Android documentation:
        * http://developer.android.com/intl/es/reference/android/app/PendingIntent.html#FLAG_NO_CREATE
        */
        final String ALARM_TYPE;

        // Get intent action
        switch (intent.getAction()){
            case ALARM_SNOOZE:
                ALARM_TYPE = ALARM_SNOOZE;
                break;
            default:
                ALARM_TYPE = START_ALERTS_SERVICE;
                break;
        }

        // Check if an alarm of same type exists
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0,
                new Intent(context, AlertServiceLauncherReceiver.class).setAction(ALARM_TYPE),
                PendingIntent.FLAG_NO_CREATE);
        // If alarm not exist, create it
        if (alarmIntent == null) {
            switch (ALARM_TYPE){
                case ALARM_SNOOZE:
                    new AlertsAlarmBuilder.Builder(context).setRetryAlarm();
                    break;
                default:
                    new AlertsAlarmBuilder.Builder(context)
                            .setHour(9)
                            .setMinute(30)
                            .setDailyAlarm();
                    break;
            }
            Log.d("AlertsAlarmReceiver", "CREATING new " + ALARM_TYPE + " alarm.");
        } else {
            Log.d("AlertsAlarmReceiver", "SKIPPING " + ALARM_TYPE + " already set.");
        }
    }
}