package es.smartidea.android.legalalerts;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import es.smartidea.android.legalalerts.services.AlertsService;
import es.smartidea.android.legalalerts.services.ServiceStarter;
import es.smartidea.android.legalalerts.database.dbContentProvider.DBContentProvider;
import es.smartidea.android.legalalerts.database.dbCursorAdapter.DBAlertsCursorAdapter;
import es.smartidea.android.legalalerts.database.DBContract;

/**
 * A simple {@link Fragment} subclass.
 * Displays ListView showing alerts stored on DB
 * Shows context menu to edit or delete selected alert
 */
public class AlertsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    // URI of DB
    private static final Uri ALERTS_URI = DBContentProvider.ALERTS_URI;
    // Static String arguments for querying
    private static final String[] PROJECTION = DBContract.ALERTS_PROJECTION;
    private static final String SELECTION_NOTNULL = "((" +
            DBContract.Alerts.COL_ALERT_NAME + " NOTNULL) AND (" +
            DBContract.Alerts.COL_ALERT_NAME + " != '' ))";

    private static final String ORDER_ASC_BY_NAME = DBContract.Alerts.COL_ALERT_NAME + " ASC";
    private static final String DIALOG_TAG = "dialog_legal_alerts";
    // Unique Loader ID to correct management
    private static final int ALERTS_LOADER_ID = 1;
    // Declare DBAdapter
    private DBAlertsCursorAdapter alertsAdapter;
    // ButterKnife bindings
    @Bind(R.id.listViewAlerts) ListView listViewAlerts;

    // Required empty public constructor
    public AlertsFragment() {}

    /**
     * Set alertsAdapter to ListViewAlerts and initialize Loader via LoaderManager
     */
    private void initAlertsLoader() {
        alertsAdapter = new DBAlertsCursorAdapter(getActivity(), R.layout.list_item_alert, null, 0);
        listViewAlerts.setAdapter(alertsAdapter);
        // Prepare the loader.  Either re-connect with an existing one or start a new one.
        getActivity().getSupportLoaderManager().initLoader(ALERTS_LOADER_ID, null, this);
    }

    /*
    * LIFECYCLE START
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
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
        registerForContextMenu(listViewAlerts);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Unbind ButterKnife
        ButterKnife.unbind(this);
        // Destroy LoaderManager when onPause()
        getActivity().getSupportLoaderManager().destroyLoader(ALERTS_LOADER_ID);
    }

    /*
    * LIFECYCLE END
    * */

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        // Inflate context menu
        getActivity().getMenuInflater().inflate(R.menu.list_view_alerts_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        TextView textView = ButterKnife.findById(
                listViewAlerts.getChildAt(info.position), R.id.textViewAlertListItem);
        ImageView imageView = ButterKnife.findById(
                listViewAlerts.getChildAt(info.position), R.id.imageAlertListItemLiteralSearch);
        switch (item.getItemId()) {
            case R.id.contextListAlertsEdit:
                // Show dialog passing to the factory method its alertName
                // and TRUE or FALSE if marked as literal search
                // on imageView tag´s added on DBAlertsCursorAdapter
                CustomAlertDialogFragment.newInstance(
                        textView.getText().toString(),
                        imageView.getTag().equals(true)
                ).show(getActivity().getSupportFragmentManager(), DIALOG_TAG);
                return true;
            case R.id.contextListAlertsDelete:
                // Delete alert sending alertName to MainActivity deleteAlert() method.
                final int hits =
                        MainActivity.deleteAlert(getContext(), textView.getText().toString());
                // Set textView (or ListView) as view for the SnackBar as since CoordinatorLayout
                // manages animations an viewGroup relationship
                // check url for details: https://goo.gl/XwjDM4
                Snackbar.make(textView,
                        hits + " Alerts named: " + textView.getText() + " deleted from DB",
                        Snackbar.LENGTH_SHORT
                ).setAction("Action", null).show();

                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (item.getItemId() == R.id.action_refresh) {
            // Start manual sync if service is not already running
            if (AlertsService.isRunning()) {
                Toast.makeText(getContext(),
                        getString(R.string.text_toast_service_already_running),
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                // Starting manual download and search of XML data trough IntentService
                ServiceStarter.startServiceManual(getContext());
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

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
}