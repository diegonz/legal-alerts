package es.smartidea.android.legalalerts.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import es.smartidea.android.legalalerts.R;
import es.smartidea.android.legalalerts.database.DBContract;
import es.smartidea.android.legalalerts.services.boeHandler.BoeHandler;

/**
 * A custom ResourceCursorAdapter {@link ResourceCursorAdapter} subclass.
 * Binds history records info to corresponding list view item.
 * Also binds data as tag object using a viewHolder class
 * to reuse int id´s and avoid findViewById calls
 */

public class HistoryAdapter extends ResourceCursorAdapter {
    private LayoutInflater inflater;

    /**
     * ViewHolder static class to store associated Views
     */
    static class ViewHolder{
        @Bind(R.id.historyItemRelatedAlert) TextView textViewHistoryItemRelatedAlert;
        @Bind(R.id.historyItemDocID) TextView textViewHistoryItemDocName;
        @Bind(R.id.historyItemDocDate) TextView textViewHistoryItemDocDate;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    public HistoryAdapter(Context context, int layout, Cursor c, int flags) {
        super(context, layout, c, flags);
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = inflater.inflate(R.layout.list_item_history, parent, false);
        // Bind the ViewHolder with ButterKnife passing the View to ViewHolder constructor
        ViewHolder holder = new ViewHolder(view);
        view.setTag(holder);
        return view;
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        // Get associated ViewHolder
        ViewHolder holder = (ViewHolder) view.getTag();

        // Get data from DBCursor
        final String relatedAlertName =
                cursor.getString(cursor.getColumnIndexOrThrow(
                        DBContract.History.COL_HISTORY_RELATED_ALERT_NAME)
                );
        final String relatedDocumentName =
                cursor.getString(cursor.getColumnIndexOrThrow(
                        DBContract.History.COL_HISTORY_DOCUMENT_NAME)
                );
        final String relatedPdfDocumentURL =
                cursor.getString(cursor.getColumnIndexOrThrow(
                        DBContract.History.COL_HISTORY_DOCUMENT_URL)
                );
        // Populate the ViewHolder fields
        holder.textViewHistoryItemRelatedAlert.setText(relatedAlertName);
        holder.textViewHistoryItemDocName.setText(relatedDocumentName);
        holder.textViewHistoryItemDocDate.setText(relatedPdfDocumentURL.substring(10,20));

        // Set PDF url as holder´s TextView tag
        holder.textViewHistoryItemDocName.setTag(BoeHandler.BOE_BASE_URL + relatedPdfDocumentURL);
    }
}
