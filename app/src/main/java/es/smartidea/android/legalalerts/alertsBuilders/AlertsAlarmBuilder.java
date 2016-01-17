package es.smartidea.android.legalalerts.alertsBuilders;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

import es.smartidea.android.legalalerts.broadcastReceivers.AlertServiceLauncherReceiver;
/*
* Public pseudo-builder class AlertsAlarmBuilder
* Sets an alarm according to given hour and minute
* if any parameter is missing it defaults to 00:00
* */
public class AlertsAlarmBuilder {

    // Private empty constructor
    private AlertsAlarmBuilder() {
    }

    // Static Builder class
    public static class Builder {

        public final static String START_ALARMS_SERVICE = "es.smartidea.legalalerts.START_ALARMS_SERVICE";
        private AlarmManager alarmMgr;
        private PendingIntent alarmIntent;
        private Calendar calendar;
        private Context context;
        private int hour, minute = 0;

        public Builder(Context mContext) {
            this.context = mContext;
            this.alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            // Setup the alarm.
            this.alarmIntent = PendingIntent.getBroadcast(context, 0,
                    new Intent(context, AlertServiceLauncherReceiver.class).setAction(START_ALARMS_SERVICE), 0);
            this.calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
        }

        // Set the alarm to start at desired hour (24h)
        public Builder setHour(int hour) {
            this.hour = hour;
            return this;
        }

        // Set the alarm to start at desired minute
        public Builder setMinute(int minute) {
            this.minute = minute;
            return this;
        }

        public Builder build(){
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            return this;
        }

        public void setAlarm(){
            // With setInexactRepeating(), you have to use one of the AlarmManager interval
            // constants, in this case, AlarmManager.INTERVAL_DAY.
            alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, alarmIntent);
        }
    }
}
