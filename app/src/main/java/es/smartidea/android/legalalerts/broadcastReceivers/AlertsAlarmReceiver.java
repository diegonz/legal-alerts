package es.smartidea.android.legalalerts.broadcastReceivers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import es.smartidea.android.legalalerts.alertsBuilders.AlertsAlarmBuilder;

public class AlertsAlarmReceiver extends BroadcastReceiver {

    // Setup intent action/s message
    public final static String START_ALARMS_SERVICE = "es.smartidea.legalalerts.START_ALARMS_SERVICE";


    // AlertsAlarmReceiver public empty constructor
    public AlertsAlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        Log.d("AlertsAlarmReceiver", "Receiver started! ");
        /*
        * Check if another PendingIntent (Alarm) exists
        * via creating new one with flag PendingIntent.FLAG_NO_CREATE
        * which returns null if exists, +info check Android documentation:
        * http://developer.android.com/intl/es/reference/android/app/PendingIntent.html#FLAG_NO_CREATE
        */
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0,
                new Intent(context, AlertServiceLauncherReceiver.class).setAction(START_ALARMS_SERVICE),
                PendingIntent.FLAG_NO_CREATE);
        if (alarmIntent == null) {
            new AlertsAlarmBuilder.Builder(context).setHour(9).setMinute(30).build().setAlarm();
            Log.d("AlertsAlarmReceiver", "No Alarm found, created new one.");
        } else {
            Log.d("AlertsAlarmReceiver", "Alarm already set, skipping.");
        }
    }
}