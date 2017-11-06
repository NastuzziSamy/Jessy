package fr.utc.simde.payutc.tools;

/**
 * Created by Samy on 24/10/2017.
 */

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import fr.utc.simde.payutc.R;

public class InternetBroadcast extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (!checkInternet(context))
            enableInternetDialog(context);
    }

    public boolean checkInternet(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnected();
    }

    protected void enableInternetDialog(final Context context) {
        Toast.makeText(context, R.string.internet_not_available, Toast.LENGTH_SHORT).show();

        AlertDialog.Builder internetAlertDialog = new AlertDialog.Builder(context);
        internetAlertDialog
                .setTitle(R.string.connection)
                .setMessage(R.string.internet_accessibility)
                .setCancelable(true)
                .setPositiveButton(R.string.pass, null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(final DialogInterface dialog) {
                        if (!checkInternet(context))
                            enableInternetDialog(context);
                    }
                });

        AlertDialog alertDialog = internetAlertDialog.create();
        alertDialog.show();
    }
}