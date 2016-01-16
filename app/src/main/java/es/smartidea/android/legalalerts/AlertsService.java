package es.smartidea.android.legalalerts;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import es.smartidea.android.legalalerts.boeHandler.BoeXMLHandler;
import es.smartidea.android.legalalerts.dbContentProvider.DBContentProvider;
import es.smartidea.android.legalalerts.dbHelper.DBContract;

public class AlertsService extends Service {

    // Public boolean flag indicating if service is running.
    private static boolean serviceRunning = false;

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

    // Shared preferences
    private SharedPreferences sharedPreferences;
    // boolean preferences
    private boolean notifyON = false;
    private boolean vibrateON = false;

    /*
    * Start of Service´s lifecycle
    * */

    @Override
    public void onCreate() {

        Log.d("Service", "Starting AlertsService...");

        // Set TRUE to serviceRunning flag
        serviceRunning = true;
        // Get shared preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // Set user preference flags
        notifyON = sharedPreferences.getBoolean("notifications_new_message", true);
        vibrateON = sharedPreferences.getBoolean("notifications_new_message_vibrate", true);

        //setup BoeHandler and BoeHandler.Listeners
        setupBoeHandler();

        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.

        // Start fetching summary
        boeFetchSummaryThread.start();
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
    * End of Service´s lifecycle
    * */

    private void setupBoeHandler(){
        // Init new boeXMLHandler
        boeXMLHandler = new BoeXMLHandler();

        // Set BoeXMLHandler event listener
        boeXMLHandler.setBoeXMLHandlerEvents(new BoeXMLHandler.BoeXMLHandlerEvents() {
            @Override
            public void onBoeSummaryFetchCompleted(boolean xmlError) {
                if (!xmlError){
                    Log.d("Service", "BOE´s summary fetching completed");
                    boeFetchAttachmentThread.start();
                }
            }

            @Override
            public void onBoeAttachmentsFetchCompleted() {
                Log.d("Service", "Fetching " + boeXMLHandler.getURLsCount() + " XMLs completed");
                // Get alerts from DB to start search thread
                getAlertsFromDB();
                boeSearchThread.start();
            }

            @Override
            public void onSearchQueryCompleted(int searchQueryResults, String searchTerm, boolean isLiteralSearch) {
                Log.d("Service", searchQueryResults + " results for: " + searchTerm + " - Literal " + isLiteralSearch);
            }

            @Override
            public void onFoundXMLErrorTag(String description) {
                Log.d("Service", "ERROR TAG found on XML summary.");
                showAlertNotification("ERROR TAG FOUND", description);
                Log.d("Service", "Requesting service stop...");
                stopSelf();
            }
        });
    }

    private Thread boeFetchSummaryThread = new Thread(new Runnable() {
        @Override
        public void run() {
            boeXMLHandler.fetchXMLSummary();
        }
    });

    private Thread boeFetchAttachmentThread = new Thread(new Runnable() {
        @Override
        public void run() {
            boeXMLHandler.fetchXMLAttachments();
        }
    });

    private Thread boeSearchThread = new Thread(new Runnable() {
        @Override
        public void run() {
            Map<String, String> resultUrlsAndAlerts = new HashMap<>();
            for (Map.Entry<String, Boolean> eachAlert : alertsListFullData.entrySet()){
                resultUrlsAndAlerts.putAll(boeXMLHandler.boeRawDataQuery(eachAlert.getKey(), eachAlert.getValue()));
            }
            Log.d("Service", "List size:" + resultUrlsAndAlerts.size());
            if (!resultUrlsAndAlerts.isEmpty()) {
                // Store found alerts on DB
                storeResultsOnDB(resultUrlsAndAlerts);
                showAlertNotification(
                        resultUrlsAndAlerts.size() + " " +
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
    });

    private void getAlertsFromDB() {
        // Get current Alerts to look for from DB
        Cursor alertsCursor = getApplicationContext().getContentResolver().query(ALERTS_URI,
                ALERTS_PROJECTION, ALERTS_SELECTION_NOTNULL, null, ALERTS_ORDER_ASC_BY_NAME);
        if (alertsCursor != null) {
            try {
                alertsListFullData = new HashMap<>();
                while (alertsCursor.moveToNext()) {
                    boolean isLiteralSearch = (alertsCursor.getInt(
                            alertsCursor.getColumnIndexOrThrow(
                                    DBContract.Alerts.COL_ALERT_SEARCH_NOT_LITERAL)) == 0);
                    alertsListFullData.put(
                            alertsCursor.getString(alertsCursor.getColumnIndexOrThrow(
                            DBContract.Alerts.COL_ALERT_NAME)), isLiteralSearch);
                }
                for (Map.Entry<String, Boolean> eachAlert : alertsListFullData.entrySet()) {
                    Log.d("Service", "Alert to search: " + eachAlert.getKey() + " literal search: " + eachAlert.getValue());
                }
                } catch (Exception e) {
                e.printStackTrace();
            } finally {
                alertsCursor.close();
            }
        }
    }

    private void storeResultsOnDB(Map<String, String> resultUrlsAndAlerts) {
        // URI of DB
        final Uri HISTORY_URI = DBContentProvider.HISTORY_URI;
        for (Map.Entry<String, String> eachResult : resultUrlsAndAlerts.entrySet()) {
            ContentValues values = new ContentValues();
            values.put(DBContract.History.COL_HISTORY_DOCUMENT_NAME,
                    eachResult.getKey().substring(eachResult.getKey().indexOf("=") + 1));
            values.put(DBContract.History.COL_HISTORY_RELATED_ALERT_NAME, eachResult.getValue());
            values.put(DBContract.History.COL_HISTORY_DOCUMENT_URL, boeXMLHandler.urls.get(eachResult.getKey()));

            getApplicationContext().getContentResolver().insert(HISTORY_URI, values);
        }
    }

    /**
     * Void method showAlertNotification(String title, String message)
     *
     * Build and Shows a notification, according to given parameters
     *
     * @param title   String corresponding to Notification´s title
     * @param message String corresponding to Notification´s message
     **/
    public void showAlertNotification(String title, String message) {

        // If notification not enabled on user preferences return
        if (!notifyON){ return; }

        // Notification ID
        final int ALERT_NOTIFICATION_ID = 0;

        // Define notification´s associated intent action
        Intent intent = new Intent(getBaseContext(), MainActivity.class);

        // Put Fragment (int) identifier on "start_on_fragment" (where to start if app is not running)
        intent.putExtra("start_on_fragment", MainActivity.FRAGMENT_HISTORY);
        intent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Resources resources = getResources();
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this);
        notification.setTicker(resources.getString(R.string.app_name) + " - " + title)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle(title)
                    .setContentText(message);
        notification.setSound(
                Uri.parse(
                        sharedPreferences.getString(
                                "notifications_new_message_ringtone",
                                "content://settings/system/notification_sound"))
        );
        // Check vibrate from preferences
        if (vibrateON){
            notification.setVibrate(new long[]{0, 500, 250, 500});
        }
        notification.setContentIntent(pendingIntent)
                    .setAutoCancel(true);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(ALERT_NOTIFICATION_ID, notification.build());
    }

    public static boolean isRunning(){
        return serviceRunning;
    }
}