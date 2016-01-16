package es.smartidea.android.legalalerts;


import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import es.smartidea.android.legalalerts.dbContentProvider.DBContentProvider;
import es.smartidea.android.legalalerts.dbCursorAdapter.DBHistoryCursorAdapter;
import es.smartidea.android.legalalerts.dbHelper.DBContract;


/**
 * An simple listView container {@link Fragment} subclass.
 */
public class HistoryFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {


    // Required empty public constructor
    public HistoryFragment() {}

    // URI of DB
    private static final Uri HISTORY_URI = DBContentProvider.HISTORY_URI;
    // Static String arguments for querying
    private static final String[] PROJECTION = new String[]{
            DBContract.History._ID,
            DBContract.History.COL_HISTORY_RELATED_ALERT_NAME,
            DBContract.History.COL_HISTORY_DOCUMENT_NAME,
            DBContract.History.COL_HISTORY_DOCUMENT_URL
    };

    private static final String ORDER_ASC_BY_NAME = DBContract.History.COL_HISTORY_DOCUMENT_NAME + " ASC";
    // Unique Loader ID to correct management
    private static final int HISTORY_LOADER_ID = 2;

    // Declare ListView
    private ListView listViewHistory;
    // Declare DBAdapter
    private DBHistoryCursorAdapter historyAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        listViewHistory = (ListView) view.findViewById(R.id.listViewHistory);
        return view;
    }

    // Attach alertsAdapter to ListViewAlerts
    private void initHistoryLoader() {
        historyAdapter = new DBHistoryCursorAdapter(getActivity(), R.layout.list_item_history, null, 0);
        listViewHistory.setAdapter(historyAdapter);
        // Prepare the loader.  Either re-connect with an existing one or start a new one.
        getActivity().getSupportLoaderManager().initLoader(HISTORY_LOADER_ID, null, this);
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
        getActivity().getSupportLoaderManager().destroyLoader(HISTORY_LOADER_ID);
    }

    @Override
    public void onDestroyView() {
        // Show fab before destroy view
        super.onDestroyView();
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
