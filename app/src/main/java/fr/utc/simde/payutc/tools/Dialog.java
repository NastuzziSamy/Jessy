package fr.utc.simde.payutc.tools;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import fr.utc.simde.payutc.MainActivity;
import fr.utc.simde.payutc.R;

/**
 * Created by Samy on 25/10/2017.
 */

public class Dialog {
    private static final String LOG_TAG = "_Dialog";
    private static Activity activity;
    private static AlertDialog alertDialog;
    private static AlertDialog.Builder alertDialogBuilder;

    public Dialog(final Activity activity) {
        this.activity = activity;
    }

    public void dismiss() {
        if (this.alertDialog != null)
            this.alertDialog.dismiss();
    }

    public Boolean isShowing() { return this.alertDialog != null && this.alertDialog.isShowing(); }

    public void createDialog() { createDialog((EditText) null); }
    public void createDialog(AlertDialog.Builder alertDialogBuilder) { createDialog(alertDialogBuilder, null); }
    public void createDialog(AlertDialog.Builder alertDialogBuilder, final EditText input) { this.alertDialogBuilder = alertDialogBuilder; createDialog(input); }
    public void createDialog(final EditText input) {
        dismiss();

        this.alertDialog = alertDialogBuilder.create();
        this.alertDialog.show();

        // Auto open keyboard
        if (input != null) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            input.requestFocus();
                            input.setFocusableInTouchMode(true);

                            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
                        }
                    });
                }
            }.start();
        }
    }

    public void errorDialog(final String title, final String message) {
        this.alertDialogBuilder = new AlertDialog.Builder(this.activity);
        this.alertDialogBuilder
                .setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .setNegativeButton(R.string.ok, null);

        createDialog();
    }

}
