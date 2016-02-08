package es.smartidea.android.legalalerts;


import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import es.smartidea.android.legalalerts.alerts.alertsServices.boeHandler.BoeXMLHandler;
import es.smartidea.android.legalalerts.database.dbContentProvider.DBContentProvider;
import es.smartidea.android.legalalerts.database.dbCursorAdapter.DBHistoryCursorAdapter;
import es.smartidea.android.legalalerts.database.DBContract;

/**
 * An simple listView container {@link Fragment} subclass.
 */
public class HistoryFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    // URI of DB
    private static final Uri HISTORY_URI = DBContentProvider.HISTORY_URI;
    // Static String arguments for querying
    private static final String[] PROJECTION = DBContract.HISTORY_PROJECTION;
    private static final String ORDER_DESC_BY_ID = DBContract.History._ID + " DESC";
    // Unique Loader ID to correct management
    private static final int HISTORY_LOADER_ID = 2;
    // Declare DBAdapter
    private DBHistoryCursorAdapter historyAdapter;
    // ButterKnife bindings
    @Bind(R.id.listViewHistory) ListView listViewHistory;

    // Required empty public constructor
    public HistoryFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        // Bind ButterKnife to view
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Attach alertsAdapter to ListViewAlerts
        initHistoryLoader();
        registerForContextMenu(listViewHistory);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(getString(R.string.fragment_history_context_menu_view));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Unbind ButterKnife
        ButterKnife.unbind(this);
        getActivity().getSupportLoaderManager().destroyLoader(HISTORY_LOADER_ID);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
//        return super.onContextItemSelected(item);
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        TextView textView = ButterKnife.findById(
                listViewHistory.getChildAt(info.position), R.id.textViewHistoryListItemDocumentName);
        //noinspection StringConcatenationMissingWhitespace
        startActivity(new Intent(Intent.ACTION_VIEW).setData(
                        Uri.parse(BoeXMLHandler.BOE_BASE_URL + textView.getTag()))
        );
        return true;
    }

    // Returns a new loader after the initAlertsLoader() call
    @Override
    public CursorLoader onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), HISTORY_URI, PROJECTION, null, null, ORDER_DESC_BY_ID);
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

    // Attach alertsAdapter to ListViewAlerts
    private void initHistoryLoader() {
        historyAdapter =
                new DBHistoryCursorAdapter(getActivity(), R.layout.list_item_history, null, 0);
        listViewHistory.setAdapter(historyAdapter);
        // Prepare the loader.  Either re-connect with an existing one or start a new one.
        getActivity().getSupportLoaderManager().initLoader(HISTORY_LOADER_ID, null, this);
    }
}
