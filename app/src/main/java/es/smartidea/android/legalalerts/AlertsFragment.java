package es.smartidea.android.legalalerts;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
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
    // Declare DBAdapter
    private DBAlertsCursorAdapter alertsAdapter;
    // ButterKnife bindings
    @Bind(R.id.listViewAlerts) ListView listViewAlerts;

    // Required empty public constructor
    public AlertsFragment() {}

    /**
     * Start of fragment lifecycle
     * */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // It is explicitly stated that the fragment has menu options to contribute
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Add fragment´s contributed options BEFORE calling super method
        inflater.inflate(R.menu.fragment_alerts_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alerts, container, false);
        // Bind ButterKnife to view
        ButterKnife.bind(this, view);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Unbind ButterKnife
        ButterKnife.unbind(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            // Start manual sync if service is not already running
            if (AlertsService.isRunning()){
                Toast.makeText(getActivity(), "service already running...", Toast.LENGTH_SHORT).show();
            } else {
                // Starting manual download and search of XML data trough IntentService
                getActivity().startService(new Intent(getActivity(), AlertsService.class));
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * End of fragment lifecycle
     * */

    // Returns a new loader after the initAlertsLoader() call
    @Override
    public CursorLoader onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                getActivity(), ALERTS_URI, PROJECTION, SELECTION_NOTNULL, null, ORDER_ASC_BY_NAME
        );
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
        alertsAdapter = new DBAlertsCursorAdapter(getActivity(), R.layout.list_item_alert, null, 0);
        listViewAlerts.setAdapter(alertsAdapter);
        // Prepare the loader.  Either re-connect with an existing one or start a new one.
        getActivity().getSupportLoaderManager().initLoader(ALERTS_LOADER_ID, null, this);
    }
}