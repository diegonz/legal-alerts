package es.smartidea.android.legalalerts;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import es.smartidea.android.legalalerts.boehandler.BoeXMLHandler;

public class AlertsIntentService extends IntentService implements BoeXMLHandler.BoeXMLHandlerEvents {
    public static String ACTION_DONE = "es.smartidea.android.legalalerts.service.intent.DONE";
    public static String ACTION_RESULT = "es.smartidea.android.legalalerts.service.intent.RESULT";
    public static String ACTION_NO_RESULT = "es.smartidea.android.legalalerts.service.intent.NO_RESULT";

    private Boolean fetchCompleted = false;

    public AlertsIntentService() {
        super("AlertsIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("Service", "Service started!");
        // TODO: Check if data has been downloaded (store on DB for re-querying? ex:new alert added)
        // Create new BoeXMLHandler object
        BoeXMLHandler boeXMLHandler = new BoeXMLHandler();
        // Fetch all documents
        Log.d("Service", "Starting to fetch all documents");
        boeXMLHandler.fetchXML();
        List<String> foundAlertsList = new ArrayList<>();
        String[] alertsToSearch = intent.getStringArrayExtra("alertsToSearch");
        // Check received alerts to search for
        for (String eachAlertToSearch: alertsToSearch ) {
            Log.d("Service", "Alert to search: " + eachAlertToSearch);
        }
        // Wait to complete fetching documents
        Log.d("Service", "Waiting to finish fetching documents");
//        while (!fetchCompleted);
        // Fetching completed
        Log.d("Service", "Fetching documents complete!");
        for (String eachAlert : alertsToSearch){
            // TODO: Check searching method
            foundAlertsList.addAll(boeXMLHandler.boeRawDataQuery(eachAlert));
            Log.d("Service", "List size:" + foundAlertsList.size());
        }
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
        if (foundAlertsList.size() > 0){
            resultMessageIntent.setAction(ACTION_RESULT);
        } else {
            resultMessageIntent.setAction(ACTION_NO_RESULT);
        }
        sendBroadcast(resultMessageIntent);
        Log.d("Service", "Service result sent!");
    }

    @Override
    public void onBoeFetchCompleted() {
        this.fetchCompleted = true;
    }

    @Override
    public void onSearchQueryCompleted(int searchQueryResults, String searchTerm) {

    }

    @Override
    public void onFoundXMLErrorTag(String description) {

    }
}
