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

import butterknife.Bind;
import butterknife.ButterKnife;
import es.smartidea.android.legalalerts.database.DBContentProvider;
import es.smartidea.android.legalalerts.adapters.HistoryAdapter;
import es.smartidea.android.legalalerts.database.DBContract;

/**
 * An simple listView container {@link Fragment} subclass.
 */
public class HistoryFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final Uri HISTORY_URI = DBContentProvider.HISTORY_URI;
    private static final String[] PROJECTION = DBContract.HISTORY_PROJECTION;
    private static final String ORDER_DESC_BY_ID = DBContract.History._ID + " DESC";
    private static final int HISTORY_LOADER_ID = 2;
    private HistoryAdapter historyAdapter;
    @Bind(R.id.listViewHistory) ListView listViewHistory;

    public HistoryFragment() {}

    /**
     * Set historyAdapter to ListViewHistory and initialize Loader via LoaderManager
     */
    private void initHistoryLoader() {
        historyAdapter =
                new HistoryAdapter(getActivity(), R.layout.list_item_history, null, 0);
        listViewHistory.setAdapter(historyAdapter);
        // Prepare the loader.  Either re-connect with an existing one or start a new one.
        getActivity().getSupportLoaderManager().initLoader(HISTORY_LOADER_ID, null, this);
    }

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
    public void onDestroyView() {
        super.onDestroyView();
        // Unbind ButterKnife
        ButterKnife.unbind(this);
        getActivity().getSupportLoaderManager().destroyLoader(HISTORY_LOADER_ID);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
        super.onCreateContextMenu(menu, v, menuInfo);
        // Inflate context menu
        getActivity().getMenuInflater().inflate(R.menu.list_view_history_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Cursor cursor = (Cursor) listViewHistory.getAdapter().getItem(info.position);
        switch (item.getItemId()){
            case R.id.contextListHistoryView:
                //noinspection StringConcatenationMissingWhitespace
                final String relatedPdfDocumentURL =
                        cursor.getString(cursor.getColumnIndexOrThrow(
                                DBContract.History.COL_HISTORY_DOCUMENT_URL)
                        );
                startActivity(
                        new Intent(Intent.ACTION_VIEW).setData(Uri.parse(relatedPdfDocumentURL))
                );
                return true;
            case R.id.contextListHistoryDelete:
                final String relatedAlertName =
                        cursor.getString(cursor.getColumnIndexOrThrow(
                                DBContract.History.COL_HISTORY_RELATED_ALERT_NAME)
                        );
                final String relatedDocumentName =
                        cursor.getString(cursor.getColumnIndexOrThrow(
                                DBContract.History.COL_HISTORY_DOCUMENT_NAME)
                        );
                final int deletedItems = MainActivity.deleteHistory(
                        getContext(), relatedDocumentName, relatedAlertName);
                MainActivity.showSnackBar(listViewHistory, "Deleted " + deletedItems + " item(s).");
                return true;
            default:
                return super.onContextItemSelected(item);

        }
    }

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
}
