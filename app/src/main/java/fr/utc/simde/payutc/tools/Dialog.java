package fr.utc.simde.payutc.tools;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

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
    private static ProgressDialog loading;

    public Dialog(final Activity activity) {
        this.activity = activity;
    }

    public void dismiss() {
        if (this.alertDialog != null)
            this.alertDialog.dismiss();

        if (this.loading != null)
            this.loading.dismiss();

        this.alertDialog = null;
        this.loading = null;
    }

    public Boolean isShowing() { return (this.alertDialog != null && this.alertDialog.isShowing()) || (this.loading != null && this.loading.isShowing()); }

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

    public void infoDialog(final Activity activity, final String title, final String message) { errorDialog(activity, title, message, null); }
    public void infoDialog(final Activity activity, final String title, final String message, final DialogInterface.OnClickListener onClickListener) {
        this.alertDialogBuilder = new AlertDialog.Builder(activity);
        this.alertDialogBuilder
            .setTitle(title)
            .setMessage(message)
            .setCancelable(true)
            .setNegativeButton(R.string.ok, onClickListener);

        createDialog();
    }

    public void errorDialog(final Activity activity, final String title, final String message) { errorDialog(activity, title, message, null); }
    public void errorDialog(final Activity activity, final String title, final String message, final DialogInterface.OnClickListener onClickListener) {
        infoDialog(activity, title, message, onClickListener);
    }

    public void fatalDialog(final Activity activity, final String title, final String message) { errorDialog(activity, title, message, null); }
    public void fatalDialog(final Activity activity, final String title, final String message, final DialogInterface.OnClickListener onClickListener) {
        infoDialog(activity, title, message, onClickListener);
    }

    public void startLoading(Activity activity, final String title, final String message) {
        dismiss();
        this.loading = ProgressDialog.show(activity, title, message, true, false);
    }

    public void changeLoading(final String message) {
        this.loading.setMessage(message);
    }

    public void stopLoading() {
        if (this.loading != null)
          this.loading.dismiss();

        this.loading = null;
    }
}
