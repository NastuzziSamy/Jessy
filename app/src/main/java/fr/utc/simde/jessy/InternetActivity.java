package fr.utc.simde.jessy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

/**
 * Created by Samy on 16/11/2017.
 */

public abstract class InternetActivity extends NFCActivity {
    private AlertDialog.Builder internetAlertDialog;

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (!checkInternet(context) && internetAlertDialog == null)
                enableInternetDialog(context);
        }
    };

    public boolean checkInternet(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnected();
    }

    protected void enableInternetDialog(final Context context) {
        Toast.makeText(context, R.string.internet_not_available, Toast.LENGTH_SHORT).show();

        this.internetAlertDialog = new AlertDialog.Builder(context);
        this.internetAlertDialog
                .setTitle(R.string.connection)
                .setMessage(R.string.internet_accessibility)
                .setCancelable(true)
                .setPositiveButton(R.string.pass, null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(final DialogInterface dialog) {
                        internetAlertDialog = null;
                        if (!checkInternet(context))
                            enableInternetDialog(context);
                    }
                });

        AlertDialog alertDialog = internetAlertDialog.create();
        alertDialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();

        registerReceiver(broadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        if (!checkInternet(InternetActivity.this) && internetAlertDialog == null)
            enableInternetDialog(InternetActivity.this);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onIdentification(String badgeId) {}
}
