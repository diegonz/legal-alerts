package es.smartidea.android.legalalerts.services;

import android.support.annotation.NonNull;
import android.annotation.SuppressLint;
import android.content.Context;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import es.smartidea.android.legalalerts.R;
import es.smartidea.android.legalalerts.alarms.AlarmDelayer;
import es.smartidea.android.legalalerts.services.builders.NotificationBuilder;
import es.smartidea.android.legalalerts.services.handlers.boe.BoeHandler;
import es.smartidea.android.legalalerts.database.DBContentProvider;
import es.smartidea.android.legalalerts.database.DBContract;
import es.smartidea.android.legalalerts.services.wakelock.AlertsWakeLock;
import es.smartidea.android.legalalerts.utils.FileLogger;

public class AlertsService extends Service {

    private static final String LOG_TAG = "Service";

    private static final String ALERTS_SELECTION_NOTNULL = "((" +
            DBContract.Alerts.COL_ALERT_NAME + " NOTNULL) AND (" +
            DBContract.Alerts.COL_ALERT_NAME + " != '' ))";
    private static boolean serviceRunning = false;
    private String lastSyncDateString;
    private BoeHandler boeHandler;

    @Override
    public void onCreate() {
        // do nothing
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                serviceRunning = true;
                setupBoeHandler(intent.hasExtra(ServiceStarter.MANUAL_SYNC_STRING));
                boeHandler.start();
            }
        }).start();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        //noinspection ReturnOfNull
        return null;
    }

    @Override
    public void onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putString(AlarmDelayer.SNOOZE_DATE_NAME, AlarmDelayer.SNOOZE_DATE_DEFAULT).apply();
        PreferenceManager.getDefaultSharedPreferences(AlertsService.this).edit()
                .putString(AlarmDelayer.LAST_SUCCESSFUL_SYNC, lastSyncDateString).apply();
        FileLogger.logToExternalFile(
                LOG_TAG + " - Stopping service, last successful sync: " + lastSyncDateString
        );
        boeHandler.unsetListener();
        boeHandler = null;
        serviceRunning = false;
        AlertsWakeLock.doRelease();
    }

    /**
     * Public static method returning service running state
     *
     * @return TRUE if AlertsService is already running
     */
    public static boolean isRunning() { return serviceRunning; }

    /**
     * Setup new BoeHandler with corresponding search terms (Alerts) and Dates
     * also creates and sets associated listener methods
     */
    private void setupBoeHandler(boolean manualSync) {
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        Date syncDate;
        Date todayDate = new Date();
        lastSyncDateString = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(AlarmDelayer.LAST_SUCCESSFUL_SYNC, dateFormat.format(todayDate));
        //lastSyncDateString = "20160307";
        try {
            syncDate = manualSync ? dateFormat.parse(lastSyncDateString) :
                    incrementOneDay(dateFormat.parse(lastSyncDateString));
        } catch (ParseException e){
            FileLogger.logToExternalFile(LOG_TAG + " - " + " Exception while parsing Date!\n" + e.toString());
            syncDate = todayDate;
        }
        ArrayList<String> daysToCheck = new ArrayList<>(1);
        while (todayDate.after(syncDate)) {
            daysToCheck.add(dateFormat.format(syncDate));
            syncDate = incrementOneDay(syncDate);
        }
        boeHandler = new BoeHandler();
        boeHandler.setAlertsAndDates(getAlertsFromDB(this), daysToCheck.toArray(new String[daysToCheck.size()]));
        boeHandler.setListener(new BoeHandler.BoeListener() {
            @Override
            public void onWorkCompleted(Map<String, String> searchResults, Map<String, String> xmlPdfUrls) {
                if (!searchResults.isEmpty()) {
                    int newResults = storeResultsOnDB(searchResults, xmlPdfUrls);
                    showAlertNotification(
                            newResults + " " + getString(R.string.notification_ok_results_title),
                            getString(R.string.notification_ok_results_description)
                    );
                    FileLogger.logToExternalFile(LOG_TAG + " - " + newResults + " New coincidences.");
                } else {
                    FileLogger.logToExternalFile(LOG_TAG + " - Any coincidence found.");
                }
                stopSelf();
            }

            @Override
            public void onSummaryFetchSuccess(String summaryDateInString) {
                lastSyncDateString = summaryDateInString;
            }
        });
    }

    /**
     * Increments received Date object by number of received days,
     * returning the resulting incremented Date
     *
     * @param date received Date to be incremented
     * @param days int number of days to be added to given date
     * @return received Date, incremented according to given days parameter
     */
    private static Date incrementDays(@NonNull Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days);
        return cal.getTime();
    }

    /**
     * Increments received Date object by ONE day and returns the resulting incremented Date
     *
     * @param date received Date to be incremented
     * @return received Date, incremented according to given days parameter
     */
    private static Date incrementOneDay(@NonNull Date date) {
        return incrementDays(date, 1);
    }

    /**
     * Get current alerts stored on DB, returns it/them in a Map containing alert text
     * and associated int value COL_ALERT_SEARCH_NOT_LITERAL converted to "reverse" boolean
     * by evaluating COL_ALERT_SEARCH_NOT_LITERAL == 0
     *
     * @return Map containing all alerts stored onto the application DB.
     */
    private static Map<String, Boolean> getAlertsFromDB(@NonNull final Context context) {

        Cursor alertsCursor = context.getContentResolver().query(
                DBContentProvider.ALERTS_URI,
                DBContract.ALERTS_PROJECTION,
                ALERTS_SELECTION_NOTNULL,
                null,
                DBContract.Alerts.COL_ALERT_NAME + " ASC"
        );
        if (alertsCursor != null) {
            Map<String, Boolean> alertsList = new HashMap<>(alertsCursor.getCount());
            while (alertsCursor.moveToNext()) {
                // reverseBooleanValue = eval( COL_ALERT_SEARCH_NOT_LITERAL == 0 )
                alertsList.put(
                        alertsCursor.getString(alertsCursor.getColumnIndexOrThrow(
                                DBContract.Alerts.COL_ALERT_NAME)),
                        alertsCursor.getInt(alertsCursor.getColumnIndexOrThrow(
                                DBContract.Alerts.COL_ALERT_SEARCH_NOT_LITERAL)) == 0
                );
            }
            alertsCursor.close();
            return alertsList;
        }
        return new HashMap<>(0);
    }

    /**
     * Stores search results on DB launching, combining alert data from
     * received resultUrlsAndAlerts Map and related PDF Url from received urls Map,
     * saving generated combined result data into DB and returning number of successful inserts.
     *
     * @param resultUrlsAndAlerts   Map of String-String, corresponding to founded search results
     * @param xmlPdfUrls    Map of String-String, corresponding to raw today attachment´s urls
     * @return  int Number of successful inserts
     */
    private int storeResultsOnDB(@NonNull final Map<String, String> resultUrlsAndAlerts,
                                 @NonNull final Map<String, String> xmlPdfUrls) {

        final Uri historyUri = DBContentProvider.HISTORY_URI;
        ContentValues values = new ContentValues();
        Uri resultID;
        int insertCount = 0;
        for (Map.Entry<String, String> eachResult : resultUrlsAndAlerts.entrySet()) {
            values.put(
                    DBContract.History.COL_HISTORY_DOCUMENT_NAME,
                    eachResult.getKey().substring(eachResult.getKey().indexOf('=') + 1));
            values.put(
                    DBContract.History.COL_HISTORY_RELATED_ALERT_NAME,
                    eachResult.getValue()
            );
            //noinspection StringConcatenationMissingWhitespace
            values.put(
                    DBContract.History.COL_HISTORY_DOCUMENT_URL,
                    BoeHandler.BOE_BASE_URL + xmlPdfUrls.get(eachResult.getKey())
            );
            resultID = this.getContentResolver().insert(historyUri, values);
            if (resultID != null && Integer.parseInt(resultID.getLastPathSegment()) != -1){
                insertCount++;
            }
        }
        return insertCount;
    }

    /**
     * Build and Shows a notification in a new Runnable object
     * according to given title and message parameters.
     *
     * @param title   String corresponding to Notification´s title
     * @param message String corresponding to Notification´s message
     **/
    public void showAlertNotification(@NonNull final String title, @NonNull final String message) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                final Context context = AlertsService.this;
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                // Notify only if user preference flags if notifyON
                if (sharedPreferences.getBoolean("notifications_new_message", true)) {
                    new NotificationBuilder.Builder(context)
                        .setTitle(title)
                        .setMessage(message)
                        .setVibrate(sharedPreferences.getBoolean(
                                "notifications_new_message_vibrate", true))
                        .setSound(sharedPreferences.getString(
                                "notifications_new_message_ringtone",
                                "content://settings/system/notification_sound"))
                        .send();
                }
            }
        }).start();
    }
}