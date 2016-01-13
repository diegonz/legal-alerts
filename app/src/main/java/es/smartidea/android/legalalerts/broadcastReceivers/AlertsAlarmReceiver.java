package es.smartidea.android.legalalerts.broadcastReceivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

public class AlertsAlarmReceiver extends BroadcastReceiver {

    // Setup broadcast messages
    public final static String SET_ALARM_FROM_ACTIVITY = "es.smartidea.android.legalalerts.SET_ALARM_FROM_ACTIVITY";
    public final static String START_ALARMS_SERVICE = "es.smartidea.legalalerts.START_ALARMS_SERVICE";


    // AlertsAlarmReceiver public empty constructor
    public AlertsAlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        Log.d("AlertsAlarmReceiver", "Receiver started! ");

        // Log intent action
        String intentAction = intent.getAction();
        Log.d("AlertsAlarmReceiver","IntentAction: " + intentAction);

        AlarmManager alarmMgr;
        PendingIntent alarmIntent;
        // Check received broadcast message
        if (intent.getAction().equals(SET_ALARM_FROM_ACTIVITY)) {

            // Received SET_ALARM_FROM_ACTIVITY broadcast message

            /*
            * TODO: Check if another PendingIntent exists
            * via creating new one with flag PendingIntent.FLAG_NO_CREATE
            * which returns null if exists, +info check Android documentation:
            * http://developer.android.com/intl/es/reference/android/app/PendingIntent.html#FLAG_NO_CREATE
            */
            alarmIntent = PendingIntent.getBroadcast(context, 0,
                    new Intent(context, AlertServiceLauncherReceiver.class).setAction(START_ALARMS_SERVICE),
                    PendingIntent.FLAG_NO_CREATE);

            if (alarmIntent == null) {

                Log.d("AlertsAlarmReceiver", "No Alarm found, creating new one.");

                // Setup the alarm.
                alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                alarmIntent = PendingIntent.getBroadcast(context, 0,
                        new Intent(context, AlertServiceLauncherReceiver.class).setAction(START_ALARMS_SERVICE), 0);

                // Set the alarm to start at 9:30 a.m.
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.set(Calendar.HOUR_OF_DAY, 9);
                calendar.set(Calendar.MINUTE, 30);

                // With setInexactRepeating(), you have to use one of the AlarmManager interval
                // constants, in this case, AlarmManager.INTERVAL_DAY.
                alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY, alarmIntent);
                Log.d("AlertsAlarmReceiver", "Alarm set from activity.");
            } else {
                Log.d("AlertsAlarmReceiver", "Alarm already set.");
            }

        } else {

            // Received BOOT_COMPLETED broadcast message, set new alarm
            Log.d("AlertsAlarmReceiver", "Boot completed, setting Alerts Service Alarm...");

            // Setup the alarm.
            alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent searchAlertsIntent = new Intent(context, AlertServiceLauncherReceiver.class)
                    .setAction(START_ALARMS_SERVICE);

            alarmIntent = PendingIntent.getBroadcast(context, 0, searchAlertsIntent, 0);

            // Set the alarm to start at 9:30 a.m.
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, 9);
            calendar.set(Calendar.MINUTE, 30);

            // With setInexactRepeating(), you have to use one of the AlarmManager interval
            // constants, in this case, AlarmManager.INTERVAL_DAY.
            alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, alarmIntent);

        }
    }
}