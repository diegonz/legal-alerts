package es.smartidea.android.legalalerts;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import es.smartidea.android.legalalerts.dbContentProvider.DBContentProvider;
import es.smartidea.android.legalalerts.dbHelper.DBContract;

public class AlertDialog extends DialogFragment {
    // URI of DB
    private static final Uri ALERTS_URI = DBContentProvider.ALERTS_URI;

    @Override
    @NonNull    // Added annotations to avoid warnings and assure not launched with null Bundle
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Use the Builder class for convenient dialog construction
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getActivity());
        // Annotation to disable warning on inflating dialog
        @SuppressLint("InflateParams")
        final View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_alert, null, false);
        final EditText editTextDialogAlert = (EditText)view.findViewById(R.id.editTextDialogAlert);
        final TextView textViewLiteralInfo = (TextView)view.findViewById(R.id.textViewLiteralInfo);
        final SwitchCompat switchLiteralSearch = (SwitchCompat)view.findViewById(R.id.switchLiteralSearch);
        switchLiteralSearch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    textViewLiteralInfo.setText(R.string.info_literal);
                } else {
                    textViewLiteralInfo.setText(R.string.info_not_literal);
                }
            }
        });
        builder.setView(view)
                .setPositiveButton(R.string.button_dialog_alert_save, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (editTextDialogAlert.getText().length() > 0) {
                            ContentValues values = new ContentValues();
                            values.put(DBContract.Alerts.COL_ALERT_NAME, editTextDialogAlert.getText().toString());
                            // Get REVERSE toggle button state by casting a boolean with ternary operator expression.
                            // If its checked returns 0 and if not checked returns 1
                            int literalSearchIntValue = (!switchLiteralSearch.isChecked()) ? 1 : 0;
                            values.put(DBContract.Alerts.COL_ALERT_SEARCH_NOT_LITERAL, literalSearchIntValue);
                            Uri resultID = getActivity().getContentResolver().insert(ALERTS_URI, values);
                            if (resultID != null) {
                                String resultMessageString;
                                if (resultID.getLastPathSegment().equals("-1")){
                                    resultMessageString = "Alert already existed into DB";
                                } else {
                                    resultMessageString = "Alert inserted into DB";
                                }
                                Snackbar.make(getActivity().findViewById(R.id.fragmentMainPlaceholder),
                                        resultMessageString, Snackbar.LENGTH_SHORT)
                                        .setAction("Action", null).show();
                            }
                            AlertDialog.this.getDialog().dismiss();
                        } else {
                            Snackbar.make(view, "Insert at least one character!!!", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                        }
                    }
                })
                .setNegativeButton(R.string.button_dialog_alert_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Selected negative (Cancel) from dialog buttons
                        AlertDialog.this.getDialog().cancel();
                    }
                });
        builder.setTitle(R.string.text_new_alert);
        // Create the AlertDialog object and return it
        return builder.create();
    }
}