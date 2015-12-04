package es.smartidea.android.legalalerts.dbcursoradapter;

import android.content.Context;
import android.database.Cursor;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.ResourceCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import es.smartidea.android.legalalerts.R;
import es.smartidea.android.legalalerts.dbhelper.DBContract;

public class DBCursorAdapter extends ResourceCursorAdapter {
    public DBCursorAdapter(AppCompatActivity context,int layout, Cursor c, int flags) {
        super(context,layout, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return LayoutInflater.from(context).inflate(R.layout.alert_list_item, viewGroup, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Find a reference fields to inflate template
        TextView textViewAlertListItem = (TextView) view.findViewById(R.id.textViewAlertListItem);
        ImageButton buttonAlertListItem = (ImageButton) view.findViewById(R.id.buttonDeleteAlertListItem);
        // Get data from DBCursor
        final String alertName = cursor.getString(cursor.getColumnIndexOrThrow(DBContract.Alerts.COL_ALERT_NAME));
        // Populate the fields
        textViewAlertListItem.setText(alertName);
        // Set onClick() methods fot buttons
        buttonAlertListItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Clicked on: " + alertName + " !!!", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
            }
        });
    }
}
