package es.smartidea.android.legalalerts.alertsFactories;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import java.util.Calendar;

import es.smartidea.android.legalalerts.broadcastReceivers.AlertServiceLauncherReceiver;
/*
* Public pseudo-builder class AlertsAlarmFactory
* Sets an alarm according to given hour and minute
* if any parameter is missing it defaults to 00:00
* */
public class AlertsAlarmFactory {

    // Private empty constructor
    private AlertsAlarmFactory() {
    }

    // Static Builder class
    public static class Builder {

        private final static String START_ALERTS_SERVICE = AlertServiceLauncherReceiver.START_ALERTS_SERVICE;
        private final Context context;
        private final AlarmManager alarmMgr;
        private final PendingIntent alarmIntent;
        private Calendar calendar;
        private int hour, minute = 0;

        public Builder(Context mContext) {
            this.context = mContext;
            this.alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            // Setup the alarm.
            this.alarmIntent = PendingIntent.getBroadcast(
                    context, 0,
                    new Intent(context, AlertServiceLauncherReceiver.class)
                            .setAction(START_ALERTS_SERVICE),
                    0
            );
        }

        // Set the alarm to start at desired hour (24h)
        public Builder setHour(int hour) {
            this.hour = hour;
            this.calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            // Add 1 day, set alarm for tomorrow
            calendar.add(Calendar.DATE, 1);
            return this;
        }

        // Set the alarm to start at desired minute
        public Builder setMinute(int minute) {
            this.minute = minute;
            calendar.set(Calendar.HOUR_OF_DAY, this.hour);
            calendar.set(Calendar.MINUTE, this.minute);
            return this;
        }

        // Set daily repeating alarm with setInexactRepeating()
        public void setDailyAlarm() {
            // With setInexactRepeating(), you have to use one of the AlarmManager interval
            // constants, in this case, AlarmManager.INTERVAL_DAY.
            alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, alarmIntent);
        }

        // Set new one-shot alarm in one hour from now
        public void setRetryAlarm() {
            // Postpone checking one hour
            alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime(),
                    AlarmManager.INTERVAL_HOUR, alarmIntent);
        }
    }
}
