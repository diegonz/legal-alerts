package es.smartidea.android.legalalerts.alerts;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import java.util.Calendar;

import es.smartidea.android.legalalerts.alerts.alertsServices.AlertsServiceStarter;

/*
* Public pseudo-builder class AlertsAlarmBuilder
* Sets an alarm according to given hour and minute
* if any parameter is missing it defaults to 00:00
* */
public class AlertsAlarmBuilder {

    // Private empty constructor to avoid instantiation
    private AlertsAlarmBuilder() {}

    // Static Builder class
    public static class Builder {
        private Context context;
        private AlarmManager alarmMgr;
        private PendingIntent alarmIntent;
        private Calendar calendar;
        private int hour = 9, minute = 30;

        /**
         * Public Builder constructor sets the Context and ALARM_TYPE according to given parameters
         *
         * @param mContext  Context of Application to get Alarm Service
         * @param ALARM_TYPE    String containing type of alarm that must be set.
         */
        public Builder(final Context mContext, final String ALARM_TYPE ) {
            this.context = mContext;
            this.alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            // Setup the alarm.
            alarmIntent = PendingIntent.getService(
                    context,
                    0,
                    new Intent(context, AlertsServiceStarter.class).setAction(ALARM_TYPE),
                    0
            );
        }

        /**
         * Set the alarm to start at desired hour (24h)
         *
         * @param hour  int representing hour of day to set the alarm (defaults to 09 h)
         * @return  AlertsAlarmBuilder Builder
         */
        public Builder setHour(final int hour) {
            this.hour = hour;
            return this;
        }

        /**
         * Set the alarm to start at desired minute
         *
         * @param minute    int representing minute to set the alarm (defaults to 30 min)
         * @return  AlertsAlarmBuilder Builder
         */
        public Builder setMinute(final int minute) {
            this.minute = minute;
            return this;
        }

        /**
         * Set daily repeating alarm with setInexactRepeating()
         * according to already set up (or default) parameters
         * Also calls releases all references calling to releaseReferences
         */
        public void setDailyAlarm() {
            // Set up time of alarm
            this.calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, this.hour);
            calendar.set(Calendar.MINUTE, this.minute);
            // check if this time has already passed, if TRUE add one day to the date of the alarm
            if (calendar.getTimeInMillis() < System.currentTimeMillis()) calendar.add(Calendar.DATE, 1);
            /*
            * With setInexactRepeating(), you have to use one of the AlarmManager interval
            * constants, in this case, AlarmManager.INTERVAL_DAY.
            * */
            alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, alarmIntent);

            // Release references after alarm setup
            releaseReferences();
        }

        /**
         * Set new one-shot alarm which fires within about one hour
         * Also calls releases all references calling to releaseReferences
         */
        public void setRetryAlarm() {
            // Postpone checking one hour
            alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 3600000L,
                    alarmIntent);

            // Release references after alarm setup
            releaseReferences();
        }

        /**
         * Sets to null all inner references
         */
        private void releaseReferences(){
            if (this.context != null) this.context = null;
            if (this.calendar != null) this.calendar = null;
            if (this.alarmMgr != null) this.alarmMgr = null;
            if (this.alarmIntent != null) this.alarmIntent = null;
        }
    }
}
