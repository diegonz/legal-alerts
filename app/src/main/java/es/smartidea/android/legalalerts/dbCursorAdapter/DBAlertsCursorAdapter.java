package es.smartidea.android.legalalerts.dbCursorAdapter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.ResourceCursorAdapter;
import android.support.v7.app.AppCompatActivity;
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

    public DBAlertsCursorAdapter(AppCompatActivity context, int layout, Cursor c, int flags) {
        super(context,layout, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return LayoutInflater.from(context).inflate(R.layout.list_item_alert, viewGroup, false);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        // Find a reference fields to inflate template
        TextView textViewAlertListItem = (TextView)view.findViewById(R.id.textViewAlertListItem);
        ImageView imageViewAlertListItemLiteral = (ImageView)view.findViewById(R.id.imageViewAlertListItemLiteral);
        ImageButton buttonAlertListItem = (ImageButton)view.findViewById(R.id.buttonDeleteAlertListItem);
        // Get data from DBCursor
        final String alertName = cursor.getString(cursor.getColumnIndexOrThrow(DBContract.Alerts.COL_ALERT_NAME));
        final int alertIsLiteral = cursor.getInt(cursor.getColumnIndexOrThrow(DBContract.Alerts.COL_ALERT_SEARCH_NOT_LITERAL));
        // Populate the fields
        textViewAlertListItem.setText(alertName);
        if (alertIsLiteral > 0){
            // Change the resource image to an open padlock (not locked) if is set to not literal search
            imageViewAlertListItemLiteral.setImageResource(android.R.drawable.ic_partial_secure);
        }
        // Set onClick() methods fot buttons
        buttonAlertListItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String SELECTION = DBContract.Alerts.COL_ALERT_NAME + "='" + alertName + "'";
                int hits = context.getContentResolver().delete(ALERTS_URI, SELECTION, null);
                Snackbar.make(view, hits + " Alerts named: " + alertName + " deleted from DB", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            }
        });
    }
}
