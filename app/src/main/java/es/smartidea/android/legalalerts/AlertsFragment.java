package es.smartidea.android.legalalerts;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import es.smartidea.android.legalalerts.dbContentProvider.DBContentProvider;
import es.smartidea.android.legalalerts.dbCursorAdapter.DBAlertsCursorAdapter;
import es.smartidea.android.legalalerts.dbHelper.DBContract;

/**
 * A simple {@link Fragment} subclass.
 * Displays ListView showing alerts stored on DB
 * Shows delete button
 * TODO: Add edit-on-click behaviour
 */
public class AlertsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    // URI of DB
    private static final Uri ALERTS_URI = DBContentProvider.ALERTS_URI;
    // Static String arguments for querying
    private static final String[] PROJECTION = new String[]{
            DBContract.Alerts._ID,
            DBContract.Alerts.COL_ALERT_NAME,
            DBContract.Alerts.COL_ALERT_SEARCH_NOT_LITERAL
    };
    private static final String SELECTION_NOTNULL = "((" +
            DBContract.Alerts.COL_ALERT_NAME + " NOTNULL) AND (" +
            DBContract.Alerts.COL_ALERT_NAME + " != '' ))";

    private static final String ORDER_ASC_BY_NAME = DBContract.Alerts.COL_ALERT_NAME + " ASC";
    // Unique Loader ID to correct management
    private static final int ALERTS_LOADER_ID = 1;

    // Declare ListView and DBCursor for adapter
    private ListView listViewAlerts;
    // Declare DBAdapter
    private DBAlertsCursorAdapter alertsAdapter;

    // Required empty public constructor
    public AlertsFragment() {}

    /**
     * Start of fragment lifecycle
     * */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Get inflated view of this layout´s fragment
        final View view = inflater.inflate(R.layout.fragment_alerts, container, false);

        if (view != null){
            // Get FAB reference with getActivity() to access MainActivity's FAB in CoordinatorLayout
            FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlertDialog alertDialog = new AlertDialog();
                    alertDialog.show(getFragmentManager(), "dialog_alert");
                }
            });
            // Assign listViewAlerts, setup of adapter and onClick methods are attached on initAlertsLoader()
            listViewAlerts = (ListView) view.findViewById(R.id.listViewAlerts);
        }
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Set alertsAdapter to ListViewAlerts
        initAlertsLoader();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Launch LoaderManager when onResume()
        initAlertsLoader();

    }

    @Override
    public void onPause() {
        super.onPause();
        // Destroy LoaderManager when onPause()
        getActivity().getSupportLoaderManager().destroyLoader(ALERTS_LOADER_ID);
    }
    /**
     * End of fragment lifecycle
     * */

    // Returns a new loader after the initAlertsLoader() call
    @Override
    public CursorLoader onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), ALERTS_URI, PROJECTION, SELECTION_NOTNULL, null, ORDER_ASC_BY_NAME);
    }

    @Override
    public void onLoadFinished(Loader loader, Cursor data) {
        data.moveToFirst();
        alertsAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        // data is not available anymore, delete reference
        alertsAdapter.swapCursor(null);
    }

    // Set alertsAdapter to ListViewAlerts
    private void initAlertsLoader() {
        alertsAdapter = new DBAlertsCursorAdapter(((AppCompatActivity) getActivity()), R.layout.list_item_alert, null, 0);
        listViewAlerts.setAdapter(alertsAdapter);
        // Prepare the loader.  Either re-connect with an existing one or start a new one.
        getActivity().getSupportLoaderManager().initLoader(ALERTS_LOADER_ID, null, this);
    }
}