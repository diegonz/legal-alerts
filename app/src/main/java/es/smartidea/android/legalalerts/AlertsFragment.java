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
import android.widget.ListView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import es.smartidea.android.legalalerts.adapters.AlertsAdapter;
import es.smartidea.android.legalalerts.database.DBContentProvider;
import es.smartidea.android.legalalerts.database.DBContract;
import es.smartidea.android.legalalerts.services.AlertsService;
import es.smartidea.android.legalalerts.services.ServiceStarter;

/**
 * A simple {@link Fragment} subclass.
 * Displays ListView showing alerts stored on DB
 * Shows context menu to edit or delete selected alert
 */
public class AlertsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final Uri ALERTS_URI = DBContentProvider.ALERTS_URI;
    private static final String[] PROJECTION = DBContract.ALERTS_PROJECTION;
    private static final String SELECTION_NOTNULL = "((" +
            DBContract.Alerts.COL_ALERT_NAME + " NOTNULL) AND (" +
            DBContract.Alerts.COL_ALERT_NAME + " != '' ))";
    private static final String ORDER_ASC_BY_NAME = DBContract.Alerts.COL_ALERT_NAME + " ASC";
    private static final String DIALOG_TAG = "dialog_legal_alerts";
    private static final int ALERTS_LOADER_ID = 1;
    private AlertsAdapter alertsAdapter;
    @BindView(R.id.listViewAlerts) ListView listViewAlerts;
    private Unbinder unbinder;

    public AlertsFragment() {}

    /**
     * Set alertsAdapter to ListViewAlerts and initialize Loader via LoaderManager
     */
    private void initAlertsLoader() {
        alertsAdapter = new AlertsAdapter(getActivity(), R.layout.list_item_alert, null, 0);
        listViewAlerts.setAdapter(alertsAdapter);
        getActivity().getSupportLoaderManager().initLoader(ALERTS_LOADER_ID, null, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_alerts_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_alerts, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initAlertsLoader();
        registerForContextMenu(listViewAlerts);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
        getActivity().getSupportLoaderManager().destroyLoader(ALERTS_LOADER_ID);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(R.string.alerts_context_menu_title);
        getActivity().getMenuInflater().inflate(R.menu.alerts_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Cursor cursor = (Cursor) listViewAlerts.getAdapter().getItem(info.position);
        final String alertName =
                cursor.getString(cursor.getColumnIndexOrThrow(DBContract.Alerts.COL_ALERT_NAME));
        final boolean isLiteralSearchFlag =
                cursor.getInt(cursor.getColumnIndexOrThrow(
                        DBContract.Alerts.COL_ALERT_SEARCH_NOT_LITERAL)) == 0;
        switch (item.getItemId()) {
            case R.id.contextListAlertsEdit:
                LegalAlertDialog.newInstance(alertName, isLiteralSearchFlag)
                        .show(getActivity().getSupportFragmentManager(), DIALOG_TAG);
                return true;
            case R.id.contextListAlertsDelete:
                final int hits = MainActivity.deleteAlert(getContext(), alertName);
                Snackbar.make(
                        listViewAlerts,
                        hits + " Alerts named: " + alertName + " deleted from DB",
                        Snackbar.LENGTH_SHORT).setAction("Action", null)
                        .show();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            if (AlertsService.isRunning()) {
                Toast.makeText(getContext(),
                        getString(R.string.text_toast_service_already_running),
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                ServiceStarter.startServiceManual(getContext());
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

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
        alertsAdapter.swapCursor(null);
    }
}