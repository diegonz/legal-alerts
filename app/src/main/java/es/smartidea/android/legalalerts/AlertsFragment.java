package es.smartidea.android.legalalerts;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import es.smartidea.android.legalalerts.dbcontentprovider.DBContentProvider;
import es.smartidea.android.legalalerts.dbcursoradapter.DBAlertsCursorAdapter;
import es.smartidea.android.legalalerts.dbhelper.DBContract;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AlertsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
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
//    private Cursor alertsCursor;
    // Declare DBAdapter
    private DBAlertsCursorAdapter alertsAdapter;

    private OnFragmentInteractionListener fragmentInteractionListener;

    // Required empty public constructor
    public AlertsFragment() {}

    /**
     * Start fragment lifecycle
     * */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Get inflated view of this layout´s fragment
        final View view = inflater.inflate(R.layout.fragment_alerts, container, false);

        if (view != null){
            final EditText editTextAlert = (EditText) view.findViewById(R.id.editTextAlert);

            Button buttonAdd = (Button) view.findViewById(R.id.buttonAdd);
            buttonAdd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (editTextAlert.getText().length() > 0) {
                        // Send notification event
                        fragmentInteractionListener.onClickedAddButton(editTextAlert.getText().toString(),
                                getResources().getString(R.string.info_literal));
                    } else {
                        Snackbar.make(view, "Insert at least one character!!!", Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                    }
                }
            });

            // Get FAB reference with getActivity() to access MainActivity's FAB in CoordinatorLayout
            FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DialogAlert dialogAlert = new DialogAlert();
                    dialogAlert.show(getFragmentManager(), "dialog_alert");
                }
            });
            // Assign listViewAlerts, setup of adapter and onClick methods are attached on initAlertsLoader()
            listViewAlerts = (ListView) view.findViewById(R.id.listViewAlerts);
        }
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            fragmentInteractionListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
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
//        if (alertsCursor != null) {
//            // TODO: Check close Cursor when onPause() if needed (LoaderManager)
//            alertsCursor.close();
//            // Destroy LoaderManager when onPause()
//        }
        getActivity().getSupportLoaderManager().destroyLoader(ALERTS_LOADER_ID);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        fragmentInteractionListener = null;
    }

    /**
     * End fragment lifecycle
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
//        alertsCursor = getActivity().getContentResolver().query(ALERTS_URI, PROJECTION, SELECTION_NOTNULL, null, ORDER_ASC_BY_NAME);
//        // TODO: Check CONTEXT
//        alertsAdapter = new DBAlertsCursorAdapter(((AppCompatActivity) getActivity()), R.layout.alert_list_item, alertsCursor, 0);
        alertsAdapter = new DBAlertsCursorAdapter(((AppCompatActivity) getActivity()), R.layout.alert_list_item, null, 0);
        listViewAlerts.setAdapter(alertsAdapter);
        // Prepare the loader.  Either re-connect with an existing one or start a new one.
        getActivity().getSupportLoaderManager().initLoader(ALERTS_LOADER_ID, null, this);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void onClickedAddButton(String title, String message);
    }
}