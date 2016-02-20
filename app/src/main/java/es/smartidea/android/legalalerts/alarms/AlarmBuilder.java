package es.smartidea.android.legalalerts.alarms;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import java.util.Calendar;

import es.smartidea.android.legalalerts.receivers.AlarmReceiver;
import es.smartidea.android.legalalerts.services.ServiceStarter;
import es.smartidea.android.legalalerts.utils.FileLogger;

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
         * Public Builder constructor sets the Context and alarmType according to given parameters
         *
         * @param mContext  Context of Application to get Alarm Service
         * @param alarmType    String containing type of alarm that must be set.
         * @param alarmID   int representing ID of alarm by type
         *                  0 as daily alarm, 1 as snooze alarm
         */
        public Builder(final Context mContext, final String alarmType, final int alarmID) {
            this.context = mContext;
            this.alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            // Setup the flags according to alarm type.
            final int flags = alarmID == AlarmReceiver.DAILY_ALARM_ID ?
                    PendingIntent.FLAG_UPDATE_CURRENT :
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT;
            // Setup the alarm.
            alarmIntent = PendingIntent.getService(
                    context,
                    alarmID,
                    new Intent(context, ServiceStarter.class).setAction(alarmType),
                    flags
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
                    AlarmDelayer.isSyncUpToDate(this.context)) {
                this.calendar.add(Calendar.DATE, 1);
            }
            alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, this.calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, alarmIntent);

            // Log to file for debugging
            FileLogger.logToExternalFile(LOG_TAG + " DAILY Alarm set!");

            // Release references after alarm setup
            releaseReferences();
        }

        /**
         * Set new one-shot alarm which fires within about one hour (3600000L)
         * Also calls releases all references calling to releaseReferences
         */
        public void setRetryAlarm() {
            // Postpone checking one hour
            alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 1800000L,
                    alarmIntent);
            // Log to file for debugging
            FileLogger.logToExternalFile(LOG_TAG + " RETRY Alarm set!");

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
