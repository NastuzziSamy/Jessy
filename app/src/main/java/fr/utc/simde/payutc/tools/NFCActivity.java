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
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import fr.utc.simde.payutc.R;

public abstract class NFCActivity extends Activity {
    private static final String	LOG_TAG = "NFCActivity";
    private NfcAdapter NFCAdapter;

    private static AlertDialog.Builder alertDialogBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NFCAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
        alertDialogBuilder = new AlertDialog.Builder(this);

        IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
        this.registerReceiver(NFCReceiver, filter);

        if (NFCAdapter == null) {
            Toast.makeText(this, R.string.nfc_not_available, Toast.LENGTH_LONG).show();
            alertDialogBuilder
                .setTitle(R.string.nfc_not_available)
                .setMessage(R.string.nfc_availability)
                .setCancelable(false)
                .setNegativeButton(R.string.quit, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        finish();
                    }
                });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
        else if (!NFCAdapter.isEnabled())
            enableNFCDialog();
    }

    protected abstract void onIdentification(String idBadge);

    protected Boolean identifierIsAvailable() { return NFCAdapter != null; }

    private String ByteArrayToHexString(byte[] inarray) {
        String[] hex = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};
        String out = "";

        for (byte b : inarray) {
            int in = b & MotionEventCompat.ACTION_MASK;
            out = (out + hex[(in >> 4) & 15]) + hex[in & 15];
        }

        return out;
    }

    protected void onNewIntent(Intent intent) {
        if (intent.getAction().equals(NFCAdapter.ACTION_TAG_DISCOVERED)) {
            Log.d(LOG_TAG, "Lecture d'un ID NFC");
            String idBadge = ByteArrayToHexString(((Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)).getId());
            Log.d(LOG_TAG, "ID: " + idBadge);
            onIdentification(idBadge);
        }
    }

    protected void onResume() {
        super.onResume();

        if (identifierIsAvailable()) {
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
            IntentFilter[] filters = {new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)};
            String[][] techs = {{Ndef.class.getName()}};
            NFCAdapter.enableForegroundDispatch(this, pendingIntent, filters, techs);

            if (!NFCAdapter.isEnabled())
                enableNFCDialog();
        }
    }

    protected void onPause() {
        super.onPause();

        if (identifierIsAvailable())
            NFCAdapter.disableForegroundDispatch(this);
    }

    public void onDestroy() {
        super.onDestroy();

        this.unregisterReceiver(NFCReceiver);
    }

    private final BroadcastReceiver NFCReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)) {
                final int state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF);

                //if (state == NfcAdapter.STATE_OFF || state == NfcAdapter.STATE_TURNING_OFF)
                if (state == NfcAdapter.STATE_OFF)
                    enableNFCDialog();
            }
        }
    };

    protected void enableNFCDialog() {
        Toast.makeText(this, R.string.nfc_not_enabled, Toast.LENGTH_SHORT).show();

        alertDialogBuilder
                .setTitle(R.string.nfc_not_enabled)
                .setMessage(R.string.nfc_accessibility)
                .setCancelable(true)
                .setPositiveButton(R.string.pass, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .setNeutralButton(R.string.activate, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                        dialog.cancel();
                    }
                })
                .setNegativeButton(R.string.quit, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        finish();
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(final DialogInterface dialog) {
                        if (!NFCAdapter.isEnabled())
                            enableNFCDialog();
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}