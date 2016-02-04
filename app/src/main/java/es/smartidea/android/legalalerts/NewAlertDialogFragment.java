package es.smartidea.android.legalalerts;

import android.support.annotation.NonNull;
import android.content.ContentValues;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.nineoldandroids.animation.Animator;

import butterknife.ButterKnife;
import butterknife.Bind;
import butterknife.OnCheckedChanged;
import butterknife.OnTextChanged;
import es.smartidea.android.legalalerts.database.dbContentProvider.DBContentProvider;
import es.smartidea.android.legalalerts.database.dbHelper.DBContract;

public class NewAlertDialogFragment extends DialogFragment {
    // URI of DB
    private static final Uri ALERTS_URI = DBContentProvider.ALERTS_URI;
    private ViewGroup container;
    // ButterKnife bindings
    @Bind(R.id.textInputLayoutDialogAlert) TextInputLayout textInputLayout;
    @Bind(R.id.textViewLiteralInfo) TextView textViewLiteralInfo;
    @Bind(R.id.switchLiteralSearch) SwitchCompat switchLiteralSearch;
    @OnCheckedChanged(R.id.switchLiteralSearch)
    public void onIsLiteralChanged(boolean isChecked){
        if (isChecked) {
            YoYo.with(Techniques.SlideOutRight).duration(150L).withListener(
                    new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    textViewLiteralInfo.setText(R.string.info_literal);
                    YoYo.with(Techniques.SlideInLeft).duration(150L).playOn(textViewLiteralInfo);
                }

                @Override
                public void onAnimationCancel(Animator animation) {}

                @Override
                public void onAnimationRepeat(Animator animation) {}
            }).playOn(textViewLiteralInfo);
        }
        else {
            YoYo.with(Techniques.SlideOutLeft).duration(150L).withListener(
                    new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {}

                @Override
                public void onAnimationEnd(Animator animation) {
                    textViewLiteralInfo.setText(R.string.info_not_literal);
                    YoYo.with(Techniques.SlideInRight).duration(150L).playOn(textViewLiteralInfo);
                }

                @Override
                public void onAnimationCancel(Animator animation) {}

                @Override
                public void onAnimationRepeat(Animator animation) {}
            }).playOn(textViewLiteralInfo);
        }
    }

    @Bind(R.id.editTextDialogAlert) EditText editTextDialogAlert;
    @OnTextChanged(R.id.editTextDialogAlert)
    public void alertNameTextChanged(){
        if (isAlertNameValid(editTextDialogAlert.length())) textInputLayout.setErrorEnabled(false);
        else {
            // Set error to true on TextInputLayout after setting error message
            textInputLayout.setError(getString(R.string.text_dialog_alert_name_invalid));
            textInputLayout.setErrorEnabled(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState){

        getArguments();
        // Get reference to the rootView (container)
        this.container = container;
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    @NonNull    // Added annotations to avoid warnings and assure not launched with null Bundle
    public android.support.v7.app.AlertDialog onCreateDialog(Bundle savedInstanceState) {

        // Use the Builder class for convenient dialog construction
        final android.support.v7.app.AlertDialog.Builder builder =
                new android.support.v7.app.AlertDialog.Builder(this.getContext());
        final View view = LayoutInflater.from(this.getContext())
                .inflate(R.layout.dialog_legal_alerts, container);
        // ButterKnife bind
        ButterKnife.bind(this, view);
        // Set positive button listener to null, will be overridden in setOnShowListener
        builder.setView(view)
                .setPositiveButton(R.string.button_dialog_alert_save, null)
                .setNegativeButton(R.string.button_dialog_alert_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                NewAlertDialogFragment.this.getDialog().cancel();
                            }
                        }
                ).setTitle(R.string.text_new_alert);

        // Create the NewAlertDialogFragment object and override positive button´s click listener
        final android.support.v7.app.AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().getAttributes().windowAnimations = R.style.dialog_animation;
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isAlertNameValid(editTextDialogAlert.length())){
                            ContentValues values = new ContentValues();
                            values.put(
                                    DBContract.Alerts.COL_ALERT_NAME,
                                    editTextDialogAlert.getText().toString()
                            );
                            // Get REVERSE toggle button state by casting a boolean with ternary
                            // operator expression. If checked returns 0 and if not returns 1
                            int literalSearchIntValue = (!switchLiteralSearch.isChecked()) ? 1 : 0;
                            values.put(
                                    DBContract.Alerts.COL_ALERT_SEARCH_NOT_LITERAL,
                                    literalSearchIntValue
                            );
                            Uri resultID = getActivity().getContentResolver()
                                    .insert(ALERTS_URI, values);
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
                            NewAlertDialogFragment.this.getDialog().dismiss();
                        } else {
                            YoYo.with(Techniques.Tada).duration(300L).withListener(
                                    new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animation) {}

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    // Set error to TextInputLayout after setting error message
                                    textInputLayout.setError(getString(R.string.text_dialog_alert_name_invalid));
                                    textInputLayout.setErrorEnabled(true);
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {}

                                @Override
                                public void onAnimationRepeat(Animator animation) {}
                            }).playOn(editTextDialogAlert);

                            Toast.makeText(
                                    getContext(),
                                    getString(R.string.text_dialog_alert_name_invalid),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        // Return the alertDialog, only pending to show().
        return alertDialog;
    }

    // Static method to check alert name length
    private static boolean isAlertNameValid(int textLength){
        // If alert name has 3 or more char return true
        return textLength > 2;
    }
}