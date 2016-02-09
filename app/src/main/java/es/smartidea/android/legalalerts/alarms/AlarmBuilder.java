package es.smartidea.android.legalalerts.alarms;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import java.util.Calendar;

import es.smartidea.android.legalalerts.services.ServiceStarter;

/*
* Public pseudo-builder class AlarmBuilder
* Sets an alarm according to given hour and minute
* if any parameter is missing it defaults to 00:00
* */
public class AlarmBuilder {

    private static final String LOG_TAG = "AlarmBuilder";

    // Private empty constructor to avoid instantiation
    private AlarmBuilder() {}

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
        public Builder(final Context mContext, final String ALARM_TYPE) {
            this.context = mContext;
            this.alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            // Setup the alarm.
            alarmIntent = PendingIntent.getService(
                    context,
                    0,
                    new Intent(context, ServiceStarter.class).setAction(ALARM_TYPE),
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
        }

        /**
         * Set the alarm to start at desired hour (24h)
         *
         * @param hour  int representing hour of day to set the alarm (defaults to 09 h)
         * @return  AlarmBuilder Builder
         */
        public Builder setHour(final int hour) {
            this.hour = hour;
            return this;
        }

        /**
         * Set the alarm to start at desired minute
         *
         * @param minute    int representing minute to set the alarm (defaults to 30 min)
         * @return  AlarmBuilder Builder
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
            this.calendar.setTimeInMillis(System.currentTimeMillis());
            this.calendar.set(Calendar.HOUR_OF_DAY, this.hour);
            this.calendar.set(Calendar.MINUTE, this.minute);
            // check if this time has already passed, if TRUE add one day to the date of the alarm
            if (this.calendar.getTimeInMillis() < System.currentTimeMillis() &&
                    AlarmWorker.isSyncUpToDate(this.context)) {
                this.calendar.add(Calendar.DATE, 1);
            }
            alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, this.calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, alarmIntent);
            Log.d(LOG_TAG, "DAILY Alarm set!");
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
            Log.d(LOG_TAG, "RETRY Alarm set!");

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
