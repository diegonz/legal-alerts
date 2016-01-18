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

import es.smartidea.android.legalalerts.R;
import es.smartidea.android.legalalerts.dbContentProvider.DBContentProvider;
import es.smartidea.android.legalalerts.dbHelper.DBContract;

public class DBAlertsCursorAdapter extends ResourceCursorAdapter {
    // URI of DB
    private static final Uri ALERTS_URI = DBContentProvider.ALERTS_URI;

    public DBAlertsCursorAdapter(Context context, int layout, Cursor c, int flags) {
        super(context,layout, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return LayoutInflater.from(context).inflate(R.layout.list_item_alert, viewGroup, false);
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        // Find a reference fields to inflate template
        TextView textViewAlertListItem = (TextView)view.findViewById(R.id.textViewAlertListItem);
        ImageView imageViewAlertListItemLiteral = (ImageView)view.findViewById(R.id.imageViewAlertListItemLiteral);
        ImageButton buttonAlertListItem = (ImageButton)view.findViewById(R.id.buttonDeleteAlertListItem);
        // Get data from DBCursor
        final String alertName = cursor.getString(cursor.getColumnIndexOrThrow(DBContract.Alerts.COL_ALERT_NAME));
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
            public void onClick(View view) {
                final String SELECTION = DBContract.Alerts.COL_ALERT_NAME + "='" + alertName + "'";

                // Perform deletion according to SELECTION
                int hits = context.getContentResolver().delete(ALERTS_URI, SELECTION, null);

                Snackbar.make(view, hits + " Alerts named: " + alertName + " deleted from DB", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            }
        });
    }
}
