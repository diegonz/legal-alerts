package es.smartidea.android.legalalerts;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    // Broadcast Message Actions
    public static String ACTION_DONE = "es.smartidea.android.legalalerts.service.intent.DONE";
    public static String ACTION_RESULT = "es.smartidea.android.legalalerts.service.intent.RESULT";
    public static String ACTION_NO_RESULT = "es.smartidea.android.legalalerts.service.intent.NO_RESULT";

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

        // Set TRUE to serviceRunning flag
        serviceRunning = true;

        Log.d("Service", "Service created!");
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
        Log.d("Service", "Service work DONE! Stopping...");
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
                } else {
                    Log.d("Service", "Error tag found on BOE´s summary");
                    Log.d("Service", "Requesting service stop...");

                    // Error tag found on summary, stop service.
                    stopSelf();
                }
            }

            @Override
            public void onBoeAttachmentsFetchCompleted() {
                Log.d("Service", "Fetching " + boeXMLHandler.getURLXMLsCount() + " XMLs completed");
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
            }
        });
    }

    private Thread boeFetchSummaryThread = new Thread(new Runnable() {
        @Override
        public void run() {
            Log.d("Service", "Starting to fetch BOE´s summary");
            boeXMLHandler.fetchXMLSummary();
        }
    });

    private Thread boeFetchAttachmentThread = new Thread(new Runnable() {
        @Override
        public void run() {
            Log.d("Service", "Starting to fetch BOE´s attachments");
            boeXMLHandler.fetchXMLAttachments();
        }
    });

    private Thread boeSearchThread = new Thread(new Runnable() {
        @Override
        public void run() {

            List<String> foundAlertsList = new ArrayList<>();
            for (Map.Entry<String, Boolean> eachAlert : alertsListFullData.entrySet()){
                foundAlertsList.addAll(boeXMLHandler.boeRawDataQuery(eachAlert.getKey(), eachAlert.getValue()));
            }
            Log.d("Service", "List size:" + foundAlertsList.size());

            // Send broadcast message confirming work done
            Intent broadcastMessage = new Intent();
            broadcastMessage.setAction(ACTION_DONE);
            sendBroadcast(broadcastMessage);
            Log.d("Service", "Service work done!");

            // Get from List<String> to String[]
            String[] resultAlertsArray = new String[foundAlertsList.size()];
            foundAlertsList.toArray(resultAlertsArray);

            // Send result through broadcast message
            Intent resultMessageIntent = new Intent();
            resultMessageIntent.putExtra("resultAlerts", resultAlertsArray);
            if (foundAlertsList.size() > 0) {
                resultMessageIntent.setAction(ACTION_RESULT);
                showAlertNotification(foundAlertsList.size() + " RESULTS FOUND", "Coincidences are found");
            } else {
                resultMessageIntent.setAction(ACTION_NO_RESULT);
                // If notification enabled in shared preferences
                if (notifyON) {
                    showAlertNotification("NO RESULTS FOUND", "Any coincidences where found");
                }
            }
            sendBroadcast(resultMessageIntent);
            Log.d("Service", "Service result sent!");

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

    /**
     * Void method showAlertNotification(String title, String message)
     *
     * Build and Shows a notification, according to given parameters
     *
     * @param title   String corresponding to Notification´s title
     * @param message String corresponding to Notification´s message
     **/
    public void showAlertNotification(String title, String message) {

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