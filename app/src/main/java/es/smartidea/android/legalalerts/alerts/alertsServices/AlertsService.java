package es.smartidea.android.legalalerts.alerts.alertsServices;

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
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import es.smartidea.android.legalalerts.R;
import es.smartidea.android.legalalerts.alerts.AlertsNotificationBuilder;
import es.smartidea.android.legalalerts.alerts.AlertsWakeLock;
import es.smartidea.android.legalalerts.alerts.alertsServices.boeHandler.BoeXMLHandler;
import es.smartidea.android.legalalerts.database.dbContentProvider.DBContentProvider;
import es.smartidea.android.legalalerts.database.DBContract;

public class AlertsService extends Service {

    // URI of DB
    private static final Uri ALERTS_URI = DBContentProvider.ALERTS_URI;
    // Static String arguments for querying
    private static final String[] ALERTS_PROJECTION = DBContract.ALERTS_PROJECTION;
    private static final String ALERTS_SELECTION_NOTNULL = "((" +
            DBContract.Alerts.COL_ALERT_NAME + " NOTNULL) AND (" +
            DBContract.Alerts.COL_ALERT_NAME + " != '' ))";

    private static final String ALERTS_ORDER_ASC_BY_NAME =
            DBContract.Alerts.COL_ALERT_NAME + " ASC";
    private static final String LOG_TAG = "Service";
    private static final String LAST_SUCCESSFUL_SYNC = "last_successful_sync";
    private String todayDateString;
    private BoeXMLHandler boeXMLHandler;
    // boolean flag indicating if service is running.
    private static boolean serviceRunning = false;
    // Flag indicating today´s summary sync status
    private static boolean todaySyncedOK = false;

    /*
    * Service Methods START
    * */

    // Public static method returning service running state
    public static boolean isRunning() { return serviceRunning; }

    // SetUp new BoeXMLHandler and associated listener methods
    private void setupBoeHandler() {
        @SuppressLint("SimpleDateFormat")
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        final Date todayDate = new Date();
        Date lastSuccessfulSyncDate;
        ArrayList<String> daysToCheck = new ArrayList<>(1);

        this.todayDateString = dateFormat.format(todayDate);
        final String lastSuccessfulSyncString = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext())
                .getString(LAST_SUCCESSFUL_SYNC, todayDateString);

        try {
            lastSuccessfulSyncDate = dateFormat.parse(lastSuccessfulSyncString);
            // Add pending day to list, adding at least today´s date
            while (todayDate.after(lastSuccessfulSyncDate)) {
                daysToCheck.add(dateFormat.format(lastSuccessfulSyncDate));
                lastSuccessfulSyncDate = incrementDays(lastSuccessfulSyncDate, 1);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        // Initialize BoeXMLHandler passing pending days to check as string array
        boeXMLHandler = new BoeXMLHandler(daysToCheck.toArray(new String[daysToCheck.size()]));
        // Set/send BoeXMLHandler event listener
        boeXMLHandler.setBoeXMLHandlerEvents(new BoeXMLHandler.BoeXMLHandlerEvents() {
            @Override
            public void onWorkCompleted(Map<String, String> searchResults, Map<String, String> xmlPdfUrls) {
                if (!searchResults.isEmpty()){
                    storeResultsOnDB(getApplicationContext(), searchResults, xmlPdfUrls);
                    showAlertNotification(
                            getApplicationContext(),
                            searchResults.size() + " " +
                            getString(R.string.notification_ok_results_title),
                            getString(R.string.notification_ok_results_description)
                    );
                    Log.d(LOG_TAG, searchResults.size() + " Coincidences found.");
                    // Stop Service after search completed and Notification sent.
                    stopSelf();
                } else {
                    showAlertNotification(
                            getApplicationContext(),
                            getString(R.string.notification_no_results_title),
                            getString(R.string.notification_no_results_description)
                    );
                    Log.d(LOG_TAG, "Any coincidence were found.");
                    // Stop Service after search completed and Notification sent.
                    stopSelf();
                }
            }

            @Override
            public void todaySummaryResultOK(boolean todaySyncResultOK) {
                todaySyncedOK = todaySyncResultOK;
                Log.d(LOG_TAG, "Today´s summary fetched OK: " + todaySyncResultOK);
            }
        });
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
                    values.put(
                            DBContract.History.COL_HISTORY_DOCUMENT_URL,
                            xmlPdfUrls.get(eachResult.getKey())
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
    public void showAlertNotification(@NonNull final Context context,
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
                    new AlertsNotificationBuilder.Builder(getApplicationContext())
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

    /*
    * Lifecycle events START
    * */

    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Set TRUE to serviceRunning flag
                serviceRunning = true;
                //setup BoeHandler and BoeHandler.Listeners
                setupBoeHandler();
                // Start fetching summary
                boeXMLHandler.startFetchAndSearch(getAlertsFromDB(getApplicationContext()));
            }
        }).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        Log.d(LOG_TAG, "Service started!");
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
        if (todaySyncedOK){
            // Set LAST_SUCCESSFUL_SYNC to todayDateString
            PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                    .edit()
                    .putString(LAST_SUCCESSFUL_SYNC, todayDateString)
                    .apply(); // Call apply() to make changes in background (commit() is immediate)
        }
        // Set snooze_alarm_date to "done"
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .edit()
                .putString("snooze_alarm_date", "done")
                .apply(); // Call apply() to make changes in background (commit() is immediate)

        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
        Log.d(LOG_TAG, "Stopping AlertsService...");

        // Release as many resources as possible
        if (boeXMLHandler != null){
            boeXMLHandler.unsetBoeXMLHandlerEvents();
            boeXMLHandler = null;
        }

        // Set serviceRunning flag to FALSE
        serviceRunning = false;

        // Then release the WakeLock from its static reference
        AlertsWakeLock.doRelease();
    }

    /*
    * Lifecycle events END
    * */
}