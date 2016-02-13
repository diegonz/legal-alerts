package es.smartidea.android.legalalerts;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
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

public class LegalAlertDialog extends AppCompatDialogFragment {

    /*
    * ButterKnife bindings - START
    * */

    @Bind(R.id.textInputLayoutDialogAlert) TextInputLayout textInputLayout;
    @Bind(R.id.textViewLiteralInfo) TextView textViewLiteralInfo;
    @Bind(R.id.switchLiteralSearch) SwitchCompat switchLiteralSearch;
    @Bind(R.id.editTextDialogAlert) EditText editTextDialogAlert;

    @OnTextChanged(R.id.editTextDialogAlert)
    public void alertNameTextChanged() {
        if (isAlertNameValid(editTextDialogAlert.length())) textInputLayout.setErrorEnabled(false);
        else {
            textInputLayout.setError(getString(R.string.text_dialog_alert_name_invalid));
            textInputLayout.setErrorEnabled(true);
        }
    }

    @OnCheckedChanged(R.id.switchLiteralSearch)
    public void onIsLiteralChanged(boolean isChecked) {
        if (isChecked) {
            YoYo.with(Techniques.SlideOutRight).duration(150L).withListener(
                    new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            textViewLiteralInfo.setText(R.string.text_dialog_info_literal_yes);
                            YoYo.with(Techniques.SlideInLeft).duration(150L)
                                    .playOn(textViewLiteralInfo);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {
                        }
                    }).playOn(textViewLiteralInfo);
        } else {
            YoYo.with(Techniques.SlideOutLeft).duration(150L).withListener(
                    new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            textViewLiteralInfo.setText(R.string.text_dialog_info_not_literal);
                            YoYo.with(Techniques.SlideInRight).duration(150L)
                                    .playOn(textViewLiteralInfo);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {
                        }
                    }).playOn(textViewLiteralInfo);
        }
    }

    /*
    * ButterKnife bindings - END
    * */

    /*
    * LOCAL_METHODS - START
    * */

    /**
     * Static factory method receiving alertName and returning new instance
     * with given parameters set to bundle
     *
     * @param alertName String representing alert name
     * @return new instance of LegalAlertDialog with given parameter as bundle
     */
    @NonNull
    public static LegalAlertDialog newInstance(String alertName, boolean isLiteralSearch) {
        LegalAlertDialog fragment = new LegalAlertDialog();

        // Supply input
        Bundle bundle = new Bundle();
        bundle.putString("alert_name", alertName);
        bundle.putBoolean("is_literal_search", isLiteralSearch);
        fragment.setArguments(bundle);

        return fragment;
    }

    /**
     * Validates length of alert name
     *
     * @param textLength int representing alert name string length
     * @return true if given textLength is valid according to specifications
     */
    private static boolean isAlertNameValid(int textLength) {
        // Return true if alert name has 3 or more char
        return textLength > 2;
    }

    /*
    * LOCAL_METHODS - END
    * */

    /*
    * Lifecycle - START
    * */

    @NonNull
    @Override
    public android.support.v7.app.AlertDialog onCreateDialog(Bundle savedInstanceState) {

        // Use the Builder class for convenient dialog construction
        final android.support.v7.app.AlertDialog.Builder builder =
                new android.support.v7.app.AlertDialog.Builder(this.getContext());
        @SuppressLint("InflateParams")
        final View view =
                LayoutInflater.from(this.getContext()).inflate(R.layout.dialog_legal_alerts, null);

        // ButterKnife bind
        ButterKnife.bind(this, view);

        int dialogTitleInt = R.string.text_dialog_new_alert;
        // Get alert name if are present and set dialog title
        final Bundle bundle = getArguments();
        if (bundle != null) {
            editTextDialogAlert.setText(bundle.getString("alert_name"));
            switchLiteralSearch.setChecked(bundle.getBoolean("is_literal_search"));
            dialogTitleInt = R.string.text_dialog_edit_alert;
        }

        // Set builder View and positive button listener to null
        // which will be overridden in setOnShowListener
        builder.setView(view)
                .setPositiveButton(R.string.button_dialog_alert_save, null)
                .setNegativeButton(R.string.button_dialog_alert_cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            LegalAlertDialog.this.getDialog().cancel();
                        }
                    }
                ).setTitle(dialogTitleInt);

        /*
        * Create the LegalAlertDialog object to override
        * positive button´s click listener using setOnShowListener
        * on the already created AlertDialog
        * */
        final android.support.v7.app.AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().getAttributes().windowAnimations = R.style.dialog_animation;
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (isAlertNameValid(editTextDialogAlert.length())) {
                                // Get int result and assign by ternary operator expression,
                                // invoking correct method checking bundle nullity
                                int result = bundle != null
                                        ? MainActivity.updateAlert(getContext(),
                                                bundle.getString("alert_name"),
                                                editTextDialogAlert.getText().toString(),
                                                switchLiteralSearch.isChecked())
                                        : MainActivity.insertNewAlert(getContext(),
                                                editTextDialogAlert.getText().toString(),
                                                switchLiteralSearch.isChecked());
                                switch (result){
                                    case -1:
                                        // Show action was unsuccessful and DON´T dismiss the dialog
                                        Toast.makeText(getContext(),
                                                getString(R.string.text_dialog_error_inserting_or_updating),
                                                Toast.LENGTH_SHORT
                                        ).show();
                                        break;
                                    default:
                                        Snackbar.make(
                                                getActivity().findViewById(R.id.fragmentMainPlaceholder),
                                                getString(R.string.text_dialog_inserted_or_updated_ok),
                                                Snackbar.LENGTH_SHORT
                                        ).setAction("Action", null).show();
                                        // Dismiss the dialog after successful insert or update
                                        LegalAlertDialog.this.getDialog().dismiss();
                                        break;
                                }
                            } else {
                                YoYo.with(Techniques.Tada).duration(300L).withListener(
                                        new Animator.AnimatorListener() {
                                            @Override
                                            public void onAnimationStart(Animator animation) {
                                            }

                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                // Set error to TextInputLayout
                                                // after setting error message
                                                textInputLayout.setError(
                                                        getString(R.string.text_dialog_alert_name_invalid)
                                                );
                                                textInputLayout.setErrorEnabled(true);
                                            }

                                            @Override
                                            public void onAnimationCancel(Animator animation) {
                                            }

                                            @Override
                                            public void onAnimationRepeat(Animator animation) {
                                            }
                                        }
                                ).playOn(editTextDialogAlert);

                                Toast.makeText(getContext(),
                                    getString(R.string.text_dialog_alert_name_invalid),
                                    Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                    }
                );
            }
        });
        // Return the alertDialog, only pending to show().
        return alertDialog;
    }

    /*
    * Lifecycle - END
    * */
}