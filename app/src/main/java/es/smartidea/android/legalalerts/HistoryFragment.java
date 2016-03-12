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

    private final static Uri HISTORY_URI = DBContentProvider.HISTORY_URI;
    private final static String[] PROJECTION = DBContract.HISTORY_PROJECTION;
    private final static String ORDER_DESC_BY_ID = DBContract.History._ID + " DESC";
    private final static int HISTORY_LOADER_ID = 2;
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
        getActivity().getSupportLoaderManager().initLoader(HISTORY_LOADER_ID, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initHistoryLoader();
        registerForContextMenu(listViewHistory);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
        getActivity().getSupportLoaderManager().destroyLoader(HISTORY_LOADER_ID);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(R.string.history_context_menu_title);
        getActivity().getMenuInflater().inflate(R.menu.history_context, menu);
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
        historyAdapter.swapCursor(null);
    }
}
