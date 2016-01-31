package es.smartidea.android.legalalerts.database.dbCursorAdapter;

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
import es.smartidea.android.legalalerts.database.dbContentProvider.DBContentProvider;
import es.smartidea.android.legalalerts.database.dbHelper.DBContract;

/**
 * A custom ResourceCursorAdapter {@link ResourceCursorAdapter} subclass.
 * Binds alerts info to corresponding list view item.
 * Also sets on click event listeners for delete buttons
 */

public class DBAlertsCursorAdapter extends ResourceCursorAdapter {
    // URI of DB
    private static final Uri ALERTS_URI = DBContentProvider.ALERTS_URI;
    private LayoutInflater inflater;

    // ViewHolder static class to store associated Views
    static class ViewHolder {
        @Bind(R.id.textViewAlertListItem) TextView textViewAlertListItem;
        @Bind(R.id.imageViewAlertListItemLiteral) ImageView imageViewAlertListItemLiteral;
        @Bind(R.id.buttonDeleteAlertListItem) ImageButton imageButtonAlertListItem;

        // Public constructor which receives a View to bind its contents to the holder
        public ViewHolder(View view) {
            // Bind the ViewHolder with ButterKnife
            ButterKnife.bind(this, view);
        }
    }

    public DBAlertsCursorAdapter(Context context, int layout, Cursor c, int flags) {
        super(context, layout, c, flags);
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public View newView(final Context context, Cursor cursor, ViewGroup parent) {
        View view = inflater.inflate(R.layout.list_item_alert, parent, false);
        // Bind the ViewHolder with ButterKnife passing the View to ViewHolder constructor
        ViewHolder holder = new ViewHolder(view);
        view.setTag(holder);
        return view;
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        // Get associated ViewHolder
        ViewHolder holder = (ViewHolder) view.getTag();

        // Get alertName from DBCursor
        final String alertName =
                cursor.getString(cursor.getColumnIndexOrThrow(DBContract.Alerts.COL_ALERT_NAME));

        // Populate the ViewHolder fields
        holder.textViewAlertListItem.setText(alertName);
        // Change the resource image to an open/closed padlock
        // according if its set to not literal search
        switch (cursor.getInt(
                cursor.getColumnIndexOrThrow(DBContract.Alerts.COL_ALERT_SEARCH_NOT_LITERAL))){
            case 0:
                holder.imageViewAlertListItemLiteral
                        .setImageResource(android.R.drawable.ic_secure);
                break;
            case 1:
                holder.imageViewAlertListItemLiteral
                        .setImageResource(android.R.drawable.ic_partial_secure);
                break;
        }
        // Set onClick() methods fot buttons TODO: Check deletion implementation
        holder.imageButtonAlertListItem.setOnClickListener(
                new OnDeleteAlertClickListener(context, alertName)
        );
    }

    // Custom static click listener to attach on delete buttons
    private static class OnDeleteAlertClickListener implements View.OnClickListener{
        Context context;
        String alertName;
        public OnDeleteAlertClickListener(Context context, String alertName) {
            this.context = context;
            this.alertName = alertName;
        }

        @Override
        public void onClick(View v) {
            final String SELECTION = DBContract.Alerts.COL_ALERT_NAME + "='" + alertName + '\'';
            // Perform deletion according to SELECTION
            int hits = context.getContentResolver().delete(ALERTS_URI, SELECTION, null);
            Snackbar.make(v,
                    hits + " Alerts named: " + alertName + " deleted from DB",
                    Snackbar.LENGTH_SHORT
            ).setAction("Action", null).show();
        }
    }
}
