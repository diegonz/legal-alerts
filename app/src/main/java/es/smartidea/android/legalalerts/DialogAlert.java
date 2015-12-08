package es.smartidea.android.legalalerts;

import android.support.annotation.NonNull;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import es.smartidea.android.legalalerts.dbcontentprovider.DBContentProvider;
import es.smartidea.android.legalalerts.dbhelper.DBContract;

public class DialogAlert extends DialogFragment {
    // URI of DB
    private static final Uri ALERTS_URI = DBContentProvider.ALERTS_URI;

    @Override
    @NonNull    // Added annotation to avoid warnings and assure not launched with null Bundle
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.alert_dialog, null); // TODO: Check add passing rootView
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
                            int literalIntValue = (!switchLiteralSearch.isChecked()) ? 1 : 0;
                            values.put(DBContract.Alerts.COL_ALERT_SEARCH_NOT_LITERAL, literalIntValue);
                            getActivity().getContentResolver().insert(ALERTS_URI, values);
                            Snackbar.make(getActivity().findViewById(R.id.fragmentMainPlaceholder),
                                    "Alert inserted into DB", Snackbar.LENGTH_SHORT)
                                    .setAction("Action", null).show();
                            // TODO: Check for alternatives to close dialog after job done.
                            DialogAlert.this.getDialog().dismiss();
                        } else {
                            Snackbar.make(view, "Insert at least one character!!!", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                        }
                    }
                })
                .setNegativeButton(R.string.button_dialog_alert_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Selected negative (Cancel) from dialog buttons
                        DialogAlert.this.getDialog().cancel();
                    }
                });
        builder.setTitle(R.string.text_new_alert);
        // Create the AlertDialog object and return it
        return builder.create();
    }
}