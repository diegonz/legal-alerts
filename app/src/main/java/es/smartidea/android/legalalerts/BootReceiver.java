package es.smartidea.android.legalalerts;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

public class BootReceiver extends BroadcastReceiver {
    public BootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        AlarmManager alarmMgr;
        PendingIntent alarmIntent;
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {

            Log.d("BootReceiver", "Setting Alerts Service Alarm...");

            // Set the alarm here.
            alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent searchAlertsIntent = new Intent(context, AlertsService.class);
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
