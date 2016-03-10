package es.smartidea.android.legalalerts.services;

import android.content.Context;
import android.support.annotation.NonNull;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import es.smartidea.android.legalalerts.R;
import es.smartidea.android.legalalerts.alarms.AlarmDelayer;
import es.smartidea.android.legalalerts.services.boeHandler.BoeHandler;
import es.smartidea.android.legalalerts.database.DBContentProvider;
import es.smartidea.android.legalalerts.database.DBContract;
import es.smartidea.android.legalalerts.utils.FileLogger;

public class AlertsService extends Service {

    private final static String LOG_TAG = "Service";

    private final static Uri ALERTS_URI = DBContentProvider.ALERTS_URI;
    private final static String[] ALERTS_PROJECTION = DBContract.ALERTS_PROJECTION;
    private final static String ALERTS_SELECTION_NOTNULL = "((" +
            DBContract.Alerts.COL_ALERT_NAME + " NOTNULL) AND (" +
            DBContract.Alerts.COL_ALERT_NAME + " != '' ))";
    private final static String ALERTS_ORDER_ASC_BY_NAME = DBContract.Alerts.COL_ALERT_NAME + " ASC";
    private final static String LAST_SUCCESSFUL_SYNC = AlarmDelayer.LAST_SUCCESSFUL_SYNC;
    private final static String SNOOZE_DATE_DEFAULT = AlarmDelayer.SNOOZE_DATE_DEFAULT;
    private final static String SNOOZE_DATE_NAME = AlarmDelayer.SNOOZE_DATE_NAME;
    private static boolean serviceRunning = false;
    private String lastOkSyncDateInString;
    private BoeHandler boeHandler;

    /*
    * Lifecycle events START
    * */

    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.

        // Get a reference to the Service (Context)
        new Thread(new Runnable() {
            @Override
            public void run() {
                serviceRunning = true;
                setupBoeHandler(AlertsService.this);
                boeHandler.start();
            }
        }).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        // Log to file for debugging
        FileLogger.logToExternalFile(LOG_TAG + " - Service started!");
        // If we get killed, after returning from here, AVOID restarting
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
        // Set snooze_alarm_date to SNOOZE_DATE_DEFAULT
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(SNOOZE_DATE_NAME, SNOOZE_DATE_DEFAULT)
                .apply(); // Call apply() to make changes in background (commit() is immediate)

        //Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();

        // Log to file for debugging
        FileLogger.logToExternalFile(
                LOG_TAG + " - Stopping service, last successful sync: " + lastOkSyncDateInString
        );

        // Release as many resources as possible
        if (boeHandler != null) {
            boeHandler.unsetListener();
            boeHandler = null;
        }

        // Set serviceRunning flag to FALSE
        serviceRunning = false;

        // Then release the WakeLock from its static reference
        AlertsWakeLock.doRelease();
    }

    /*
    * Lifecycle events END
    * */

    /*
    * Service Methods START
    * */

    // Public static method returning service running state
    public static boolean isRunning() { return serviceRunning; }

    /**
     * SetUp new BoeHandler and associated listener methods
     *
     * @param context Context of service to set up BoeHandler
     */
    private void setupBoeHandler(final Context context) {
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        final Date todayDate = new Date();
        lastOkSyncDateInString =
                PreferenceManager.getDefaultSharedPreferences(context)
                        .getString(LAST_SUCCESSFUL_SYNC, dateFormat.format(todayDate));

        ArrayList<String> daysToCheck = new ArrayList<>(1);
        Date syncDate;
        try {
            syncDate = dateFormat.parse(lastOkSyncDateInString);
        } catch (Exception e){
            // TODO correct error handling
            e.printStackTrace();
            syncDate = todayDate;
        }
        while (todayDate.after(syncDate)) {
            daysToCheck.add(dateFormat.format(syncDate));
            syncDate = incrementDays(syncDate, 1);
        }
        boeHandler = new BoeHandler();
        boeHandler.setListener(new BoeHandler.BoeListener() {
            @Override
            public void onWorkCompleted(final Map<String, String> searchResults, final Map<String, String> xmlPdfUrls) {
                if (!searchResults.isEmpty()) {
                    storeResultsOnDB(context, searchResults, xmlPdfUrls);
                    showAlertNotification(
                            context,
                            searchResults.size() + " " +
                                    getString(R.string.notification_ok_results_title),
                            getString(R.string.notification_ok_results_description)
                    );

                    // Log to file for debugging
                    FileLogger.logToExternalFile(LOG_TAG + " - " + searchResults.size() + " Coincidences found.");

                    // Stop Service after search completed and Notification sent.
                    stopSelf();
                } else {
                    showAlertNotification(
                            context,
                            getString(R.string.notification_no_results_title),
                            getString(R.string.notification_no_results_description)
                    );

                    // Log to file for debugging
                    FileLogger.logToExternalFile(LOG_TAG + " - Any coincidences were found.");

                    // Stop Service after search completed and Notification sent.
                    stopSelf();
                }
            }

            @Override
            public void onSummaryFetchSuccess(String summaryDateInString) {
                // Save each summary successfully processed as last successful sync date
                lastOkSyncDateInString = summaryDateInString;
                PreferenceManager.getDefaultSharedPreferences(context)
                        .edit()
                        .putString(LAST_SUCCESSFUL_SYNC, summaryDateInString)
                        .apply(); // Call apply() to make changes in background (commit() is immediate)
                // Log to file for debugging
                FileLogger.logToExternalFile(
                        String.format(Locale.getDefault(),
                                "%s - BOE´s summary fetched ok, updating last sync date to: %s",
                                LOG_TAG, summaryDateInString
                        )
                );

            }
        });
        boeHandler.setAlertsAndDates(
                getAlertsFromDB(context), daysToCheck.toArray(new String[daysToCheck.size()])
        );
    }

    /**
     * Increments received Date object by one day and returns the resulting incremented Date
     *
     * @param date received date to be incremented
     * @param days number of days to be added to given date
     * @return received Date, incremented according to given days parameter
     */
    private static Date incrementDays(@NonNull Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days); //minus number would decrement the days
        return cal.getTime();
    }

    /**
     * Get current alerts stored on DB, returns it/them in a Map containing alert text
     * and associated int value COL_ALERT_SEARCH_NOT_LITERAL converted to "reverse" boolean
     * by evaluating COL_ALERT_SEARCH_NOT_LITERAL == 0
     *
     * @return Map containing all alerts stored onto the application DB.
     */
    private static Map<String, Boolean> getAlertsFromDB(@NonNull final Context context) {
        // Get current Alerts to look for from DB
        Cursor alertsCursor = context.getContentResolver().query(ALERTS_URI,
                ALERTS_PROJECTION, ALERTS_SELECTION_NOTNULL, null, ALERTS_ORDER_ASC_BY_NAME);
        Map<String, Boolean> alertsList;
        if (alertsCursor != null) {
            alertsList = new HashMap<>(alertsCursor.getCount());
            try {
                while (alertsCursor.moveToNext()) {
                    // Reversing value from int to boolean
                    // reverseBooleanValue = eval( COL_ALERT_SEARCH_NOT_LITERAL == 0 )
                    alertsList.put(
                            alertsCursor.getString(alertsCursor.getColumnIndexOrThrow(
                                    DBContract.Alerts.COL_ALERT_NAME)),
                            alertsCursor.getInt(alertsCursor.getColumnIndexOrThrow(
                                    DBContract.Alerts.COL_ALERT_SEARCH_NOT_LITERAL)) == 0
                    );
                }
                return alertsList;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                alertsCursor.close();
            }
        }
        return new HashMap<>(0);
    }

    /**
     * Stores search results on DB launching a new Runnable object,
     * combining alert data from received resultUrlsAndAlerts Map
     * and related PDF Url from received urls Map,
     * saving generated combined result data into DB.
     *
     * @param context   Context of Application to get ContentResolver
     * @param resultUrlsAndAlerts   Map<String,String> corresponding to founded search results
     * @param xmlPdfUrls    Map<String,String> corresponding to raw today attachment´s urls
     */
    private static void storeResultsOnDB(@NonNull final Context context,
                                         @NonNull final Map<String, String> resultUrlsAndAlerts,
                                         @NonNull final Map<String, String> xmlPdfUrls) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // URI of DB
                final Uri HISTORY_URI = DBContentProvider.HISTORY_URI;
                ContentValues values = new ContentValues();
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
                    // Do the C(RUD).
                    context.getContentResolver().insert(HISTORY_URI, values);
                }
            }
        }).start();
    }

    /**
     * Build and Shows a notification in a new Runnable object
     * according to given title and message parameters.
     *
     * @param context   Context of Application to get SharedPreferences
     * @param title   String corresponding to Notification´s title
     * @param message String corresponding to Notification´s message
     **/
    public static void showAlertNotification(@NonNull final Context context,
                                             @NonNull final String title,
                                             @NonNull final String message) {

        // If notification not enabled on user preferences return
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Get shared preferences
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                // Notify if user preference flags if notifyON
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

    /*
    * Service Methods END
    * */
}