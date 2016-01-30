package es.smartidea.android.legalalerts.dbCursorAdapter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import es.smartidea.android.legalalerts.R;
import es.smartidea.android.legalalerts.boeHandler.BoeXMLHandler;
import es.smartidea.android.legalalerts.dbHelper.DBContract;

public class DBHistoryCursorAdapter extends ResourceCursorAdapter {

    @Bind(R.id.textViewHistoryListItemRelatedAlert) TextView textViewHistoryListItemRelatedAlert;
    @Bind(R.id.textViewHistoryListItemDocumentName) TextView textViewHistoryListItemDocumentName;
    @Bind(R.id.buttonViewHistoryListItem) ImageButton buttonViewHistoryListItem;

    public DBHistoryCursorAdapter(Context context, int layout, Cursor c, int flags) {
        super(context, layout, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item_history, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
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
        // Bind ButterKnife
        ButterKnife.bind(this, view);
        // Populate the fields
        textViewHistoryListItemRelatedAlert.setText(relatedAlertName);
        textViewHistoryListItemDocumentName.setText(relatedDocumentName);
        // Set onClick() methods fot buttons
        buttonViewHistoryListItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context.startActivity(
                        new Intent(Intent.ACTION_VIEW).setData(
                                Uri.parse(BoeXMLHandler.BOE_BASE_URL + relatedPdfDocumentURL))
                );
            }
        });
    }

}
