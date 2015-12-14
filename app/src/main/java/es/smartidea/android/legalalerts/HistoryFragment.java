package es.smartidea.android.legalalerts;


import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import es.smartidea.android.legalalerts.dbcontentprovider.DBContentProvider;
import es.smartidea.android.legalalerts.dbcursoradapter.DBHistoryCursorAdapter;
import es.smartidea.android.legalalerts.dbhelper.DBContract;


/**
 * An simple listView container {@link Fragment} subclass.
 */
public class HistoryFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {


    public HistoryFragment() {
        // Required empty public constructor
    }

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

    private static final Uri HISTORY_URI = DBContentProvider.HISTORY_URI;
    // Static String arguments for querying
    private static final String[] PROJECTION = new String[]{
            DBContract.History._ID,
            DBContract.History.COL_HISTORY_RELATED_ALERT_NAME,
            DBContract.History.COL_HISTORY_DOCUMENT_NAME,
            DBContract.History.COL_HISTORY_DOCUMENT_URL
    };
//    private static final String SELECTION_NOTNULL = "((" +
//            DBContract.Alerts.COL_ALERT_NAME + " NOTNULL) AND (" +
//            DBContract.Alerts.COL_ALERT_NAME + " != '' ))";

    private static final String ORDER_ASC_BY_NAME = DBContract.History.COL_HISTORY_DOCUMENT_NAME + " ASC";
    // Declare ListView and DBCursor for adapter
    private ListView listViewHistory;
    private Cursor historyCursor;
    // Declare DBAdapter
    private DBHistoryCursorAdapter historyAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        if (view != null){
            // Get FAB reference with getActivity() to access MainActivity's FAB in CoordinatorLayout
            FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Starting manual download and search of XML data trough IntentService
                    Cursor alertsCursor = getActivity().getContentResolver().query(ALERTS_URI,
                            ALERTS_PROJECTION, ALERTS_SELECTION_NOTNULL, null, ALERTS_ORDER_ASC_BY_NAME);
                    String[] alertsArray;
                    if (alertsCursor != null){
                        Log.d("History", "Alerts found on DB, inflating alerts array");
                        List<String> alertsList = new ArrayList<>();
                        while (alertsCursor.moveToNext()){
                            alertsList.add(alertsCursor.getString(alertsCursor.getColumnIndexOrThrow(DBContract.Alerts.COL_ALERT_NAME)));
                        }
                        alertsCursor.close();
                        alertsArray = new String[alertsList.size()];
                        alertsList.toArray(alertsArray);
                        for (String eachAlert: alertsArray ) {
                            Log.d("History", "Alert to search: " + eachAlert);
                        }
                    } else {
                        alertsArray = new String[]{"impuesto", "estado", "ayuda"};
                        Log.d("History", "No alerts from cursor, inflating dummy alerts array");
                        for (String eachAlert : alertsArray) {
                            Log.d("History", "Dummy alert to search: " + eachAlert);
                        }
                    }
                    Intent searchAlertsIntent = new Intent(getActivity(), AlertsIntentService.class);
                    searchAlertsIntent.putExtra("alertsToSearch", alertsArray);
                    Log.d("History", "Launching Alerts Service...");
                    getActivity().startService(searchAlertsIntent);
                }
            });
            // Get ListView reference
            listViewHistory = (ListView)view.findViewById(R.id.listViewHistory);
        }
        return view;
    }

    // Attach alertsAdapter to ListViewAlerts
    private void initHistoryLoader() {
        historyCursor = getActivity().getContentResolver().query(HISTORY_URI, PROJECTION, null, null, ORDER_ASC_BY_NAME);
        // TODO: Check CONTEXT: ((AppCompatActivity) getActivity())
        historyAdapter = new DBHistoryCursorAdapter(getActivity(), R.layout.history_list_item, historyCursor, 0);
        listViewHistory.setAdapter(historyAdapter);
        // Prepare the loader.  Either re-connect with an existing one or start a new one.
        getActivity().getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Attach alertsAdapter to ListViewAlerts
        initHistoryLoader();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Launch LoaderManager when onAttach() Fragment;
        initHistoryLoader();

    }

    @Override
    public void onPause() {
        super.onPause();
        if (historyCursor != null) {
            // Close Cursor and destroy LoaderManager when onDetach()
            historyCursor.close();
            getActivity().getSupportLoaderManager().destroyLoader(0);
        }
    }

    // Returns a new loader after the initAlertsLoader() call
    @Override
    public CursorLoader onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), HISTORY_URI, PROJECTION, null, null, ORDER_ASC_BY_NAME);
    }

    @Override
    public void onLoadFinished(Loader loader, Cursor data) {
        data.moveToFirst();
        historyAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        // data is not available anymore, delete reference
        historyAdapter.swapCursor(null);
    }

}
