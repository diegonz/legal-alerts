package es.smartidea.android.legalalerts.dbcursoradapter;

import android.content.Context;
import android.database.Cursor;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import es.smartidea.android.legalalerts.R;
import es.smartidea.android.legalalerts.dbhelper.DBContract;

public class DBHistoryCursorAdapter extends ResourceCursorAdapter {

    public DBHistoryCursorAdapter(Context context, int layout, Cursor c, int flags) {
        super(context, layout, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return LayoutInflater.from(context).inflate(R.layout.history_list_item, viewGroup, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Find a reference fields to inflate template
        TextView textViewHistoryListItemRelatedAlert = (TextView) view.findViewById(R.id.textViewHistoryListItemRelatedAlert);
        TextView textViewHistoryListItemDocumentName = (TextView)view.findViewById(R.id.textViewHistoryListItemDocumentName);
        ImageButton buttonDeleteHistoryListItem = (ImageButton) view.findViewById(R.id.buttonDeleteHistoryListItem);
        // Get data from DBCursor
        final String relatedAlertName = cursor.getString(cursor.getColumnIndexOrThrow(DBContract.History.COL_HISTORY_RELATED_ALERT_NAME));
        final String relatedDocumentName = cursor.getString(cursor.getColumnIndexOrThrow(DBContract.History.COL_HISTORY_DOCUMENT_NAME));
        final String relatedPdfDocumentURL = cursor.getString(cursor.getColumnIndexOrThrow(DBContract.History.COL_HISTORY_DOCUMENT_URL));
        // Populate the fields
        textViewHistoryListItemRelatedAlert.setText(relatedAlertName);
        textViewHistoryListItemDocumentName.setText(relatedDocumentName);
        // Set onClick() methods fot buttons
        buttonDeleteHistoryListItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Should (web) intent to: " + relatedPdfDocumentURL, Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            }
        });
    }

}
