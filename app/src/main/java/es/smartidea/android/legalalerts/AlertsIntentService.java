package es.smartidea.android.legalalerts;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import es.smartidea.android.legalalerts.boehandler.BoeXMLHandler;

public class AlertsIntentService extends IntentService {
    public static String ACTION_DONE = "es.smartidea.android.legalalerts.service.intent.DONE";
    public static String ACTION_RESULT = "es.smartidea.android.legalalerts.service.intent.RESULT";
    public static String ACTION_NO_RESULT = "es.smartidea.android.legalalerts.service.intent.NO_RESULT";

    public AlertsIntentService() {
        super("AlertsIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // TODO: Check if data has been downloaded (store on DB for re-querying? ex:new alert added)
        BoeXMLHandler boeXMLHandler = new BoeXMLHandler();
        boeXMLHandler.fetchXML();
        List<String> foundAlertsList = new ArrayList<>();
        String[] alertsToSearch = intent.getStringArrayExtra("alertsToSearch");
        for (String eachAlert : alertsToSearch){
            // TODO: Check searching method
            foundAlertsList.addAll(boeXMLHandler.boeRawDataQuery(eachAlert));
            Log.d("Service", "List size:" + foundAlertsList.size());
        }
        // Send broadcast message confirming work done
        Intent broadcastMessage = new Intent();
        broadcastMessage.setAction(ACTION_DONE);
        sendBroadcast(broadcastMessage);

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
    }
}
