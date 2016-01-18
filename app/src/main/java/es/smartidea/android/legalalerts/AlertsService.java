package es.smartidea.android.legalalerts;

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

import java.util.HashMap;
import java.util.Map;

import es.smartidea.android.legalalerts.alertsBuilders.AlertsNotificationBuilder;
import es.smartidea.android.legalalerts.boeHandler.BoeXMLHandler;
import es.smartidea.android.legalalerts.dbContentProvider.DBContentProvider;
import es.smartidea.android.legalalerts.dbHelper.DBContract;

public class AlertsService extends Service {

    // URI of DB
    private static final Uri ALERTS_URI = DBContentProvider.ALERTS_URI;
    // Static String arguments for querying
    private static final String[] ALERTS_PROJECTION = new String[]{
            DBContract.Alerts._ID,
            DBContract.Alerts.COL_ALERT_NAME,
            DBContract.Alerts.COL_ALERT_SEARCH_NOT_LITERAL
    };
    private static final String ALERTS_SELECTION_NOTNULL = "((" +
            DBContract.Alerts.COL_ALERT_NAME + " NOTNULL) AND (" +
            DBContract.Alerts.COL_ALERT_NAME + " != '' ))";

    private static final String ALERTS_ORDER_ASC_BY_NAME = DBContract.Alerts.COL_ALERT_NAME + " ASC";

    private Map<String, Boolean> alertsListFullData;
    private BoeXMLHandler boeXMLHandler;

    // Public boolean flag indicating if service is running.
    private static boolean serviceRunning = false;

    /*
    * Service Methods START
    * */

    // Public static method returning service running state
    public static boolean isRunning() { return serviceRunning; }

    // SetUp new BoeXMLHandler and associated listener methods
    private void setupBoeHandler() {
        // Init new boeXMLHandler passing null as String Object
        // fake valid String date = "20160114"
        boeXMLHandler = new BoeXMLHandler(null);
        // Set BoeXMLHandler event listener
        boeXMLHandler.setBoeXMLHandlerEvents(new BoeXMLHandler.BoeXMLHandlerEvents() {
            @Override
            public void onBoeSummaryFetchCompleted(boolean xmlError) {
                if (xmlError) {
                    stopSelf();
                } else {
                    boeXMLHandler.fetchXMLAttachments();
                }
            }

            @Override
            public void onBoeAttachmentsFetchCompleted() {
                Log.d("Service", "Fetching " + boeXMLHandler.getURLsCount() + " XMLs completed");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // Get alerts from DB
                        getAlertsFromDB();

                        Map<String, String> resultUrlsAndAlerts = new HashMap<>();
                        for (Map.Entry<String, Boolean> eachAlert : alertsListFullData.entrySet()) {
                            resultUrlsAndAlerts.putAll(
                                    boeXMLHandler.boeRawDataQuery(eachAlert.getKey(), eachAlert.getValue())
                            );
                        }
                        Log.d("Service", "List size: " + resultUrlsAndAlerts.size());
                        if (!resultUrlsAndAlerts.isEmpty()) {
                            int contador = 0;
                            for (Map.Entry<String, String> eachResult : resultUrlsAndAlerts.entrySet()) {
                                Log.d("TEST", contador++ + " - " + eachResult.getKey() + " : " + eachResult.getValue());
                            }
                            // Store found alerts on DB
                            storeResultsOnDB(resultUrlsAndAlerts, boeXMLHandler.urls);
                            showAlertNotification(
                                    getString(R.string.notification_ok_results_title),
                                    getString(R.string.notification_ok_results_description)
                            );
                        } else {
                            showAlertNotification(
                                    getString(R.string.notification_no_results_title),
                                    getString(R.string.notification_no_results_description)
                            );
                        }
                        // Stop Service after search completed and Notification sent.
                        stopSelf();
                    }
                }).start();
            }

            @Override
            public void onSearchQueryCompleted(int searchQueryResults, String searchTerm, boolean isLiteralSearch) {
                Log.d("Service", searchQueryResults + " results for: " + searchTerm + " - Literal " + isLiteralSearch);
            }

            @Override
            public void onFoundXMLErrorTag(String description) {
                Log.d("Service", "ERROR TAG found on XML summary.");
                showAlertNotification("ERROR TAG FOUND", description);
            }
        });
    }

    // Get Alerts list from DB
    private void getAlertsFromDB() {
        // Get current Alerts to look for from DB
        Cursor alertsCursor = getApplicationContext().getContentResolver().query(ALERTS_URI,
                ALERTS_PROJECTION, ALERTS_SELECTION_NOTNULL, null, ALERTS_ORDER_ASC_BY_NAME);
        if (alertsCursor != null) {
            try {
                alertsListFullData = new HashMap<>();
                while (alertsCursor.moveToNext()) {
                    // Reversing value from int to boolean
                    // reverseBooleanValue = eval( COL_ALERT_SEARCH_NOT_LITERAL == 0 )
                    alertsListFullData.put(
                            alertsCursor.getString(alertsCursor.getColumnIndexOrThrow(
                                    DBContract.Alerts.COL_ALERT_NAME)),
                            alertsCursor.getInt(alertsCursor.getColumnIndexOrThrow(
                                    DBContract.Alerts.COL_ALERT_SEARCH_NOT_LITERAL)) == 0
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                alertsCursor.close();
            }
        }
    }

    /**
     * Void method storeResultsOnDB(final Map<String, String> resultUrlsAndAlerts)
     *
     * Stores search results on DB running a new Runnable() Object
     *
     * @param resultUrlsAndAlerts HashMap<String,String> corresponding to search results
     **/
    private void storeResultsOnDB(final Map<String, String> resultUrlsAndAlerts, final Map<String, String> urls) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // URI of DB
                final Uri HISTORY_URI = DBContentProvider.HISTORY_URI;
                ContentValues values = new ContentValues();
                for (Map.Entry<String, String> eachResult : resultUrlsAndAlerts.entrySet()) {
                    values.put(DBContract.History.COL_HISTORY_DOCUMENT_NAME,
                            eachResult.getKey().substring(eachResult.getKey().indexOf("=") + 1));
                    values.put(DBContract.History.COL_HISTORY_RELATED_ALERT_NAME, eachResult.getValue());
                    values.put(DBContract.History.COL_HISTORY_DOCUMENT_URL, urls.get(eachResult.getKey()));
                    // Do the C(RUD).
                    getApplicationContext().getContentResolver().insert(HISTORY_URI, values);
                }

            }
        }).start();
    }

    /**
     * Void method showAlertNotification(String title, String message)
     *
     * Build and Shows a notification in a new Runnable() Object
     * according to given parameters.
     *
     * @param title   String corresponding to Notification´s title
     * @param message String corresponding to Notification´s message
     **/
    public void showAlertNotification(final String title, final String message) {

        // If notification not enabled on user preferences return
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Set TRUE to serviceRunning flag
                serviceRunning = true;
                // Get shared preferences
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                // Set user preference flags if notifyON
                if (sharedPreferences.getBoolean("notifications_new_message", true)) {
                    new AlertsNotificationBuilder
                            .Builder(getApplicationContext())
                            .setTitle(title)
                            .setMessage(message)
                            .setVibrate(sharedPreferences.getBoolean("notifications_new_message_vibrate", true))
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
                //setup BoeHandler and BoeHandler.Listeners
                setupBoeHandler();
                // Start fetching summary
                boeXMLHandler.fetchXMLSummary();
            }
        }).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        Log.d("Service", "Service started!");
        // If we get killed, after returning from here, AVOID restarting
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
        Log.d("Service", "Stopping AlertsService...");
        // Release as many resources as possible
        if (boeXMLHandler != null){
            boeXMLHandler.unsetBoeXMLHandlerEvents();
            boeXMLHandler = null;
        }

        // Set serviceRunning flag to FALSE
        serviceRunning = false;
    }

    /*
    * Lifecycle events END
    * */
}