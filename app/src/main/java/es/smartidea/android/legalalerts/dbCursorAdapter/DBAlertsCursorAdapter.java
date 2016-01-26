package es.smartidea.android.legalalerts.dbCursorAdapter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import es.smartidea.android.legalalerts.R;
import es.smartidea.android.legalalerts.dbContentProvider.DBContentProvider;
import es.smartidea.android.legalalerts.dbHelper.DBContract;

public class DBAlertsCursorAdapter extends ResourceCursorAdapter {
    // URI of DB
    private static final Uri ALERTS_URI = DBContentProvider.ALERTS_URI;
    @Bind(R.id.imageViewAlertListItemLiteral) ImageView imageViewAlertListItemLiteral;
    @Bind(R.id.buttonDeleteAlertListItem) ImageButton buttonAlertListItem;
    @Bind(R.id.textViewAlertListItem) TextView textViewAlertListItem;

    public DBAlertsCursorAdapter(Context context, int layout, Cursor c, int flags) {
        super(context, layout, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item_alert, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        // Get data from DBCursor
        final String alertName = cursor.getString(cursor.getColumnIndexOrThrow(DBContract.Alerts.COL_ALERT_NAME));
        // Bind ButterKnife
        ButterKnife.bind(this, view);
        // Populate the fields
        textViewAlertListItem.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBContract.Alerts.COL_ALERT_NAME)));
        // Change the resource image to an open/closed padlock according if its set to not literal search
        switch (cursor.getInt(cursor.getColumnIndexOrThrow(DBContract.Alerts.COL_ALERT_SEARCH_NOT_LITERAL))){
            case 0:
                imageViewAlertListItemLiteral.setImageResource(android.R.drawable.ic_secure);
                break;
            case 1:
                imageViewAlertListItemLiteral.setImageResource(android.R.drawable.ic_partial_secure);
                break;
        }

        // Set onClick() methods fot buttons TODO: Check deletion implementation
        buttonAlertListItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String SELECTION = DBContract.Alerts.COL_ALERT_NAME + "='" + alertName + '\'';

                // Perform deletion according to SELECTION
                int hits = context.getContentResolver().delete(ALERTS_URI, SELECTION, null);

                Snackbar.make(v, hits + " Alerts named: " + alertName + " deleted from DB", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            }
        });
    }
}
