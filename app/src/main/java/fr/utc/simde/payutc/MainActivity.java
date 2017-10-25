package fr.utc.simde.payutc;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import fr.utc.simde.payutc.tools.HTTPRequest;
import fr.utc.simde.payutc.tools.NFCActivity;
import fr.utc.simde.payutc.tools.CASConnexion;

public class MainActivity extends NFCActivity {
    private static final String LOG_TAG = "MainActivity";
    private static Boolean registered = false;

    private static AlertDialog alertDialog;
    private static CASConnexion casConnexion;

    private static TextView AppConfigText;
    private static TextView AppRegisteredText;
    private static Button usernameButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        casConnexion = new CASConnexion();

        AppConfigText = findViewById(R.id.text_app_config);
        AppRegisteredText = findViewById(R.id.text_app_registered);
        usernameButton = findViewById(R.id.button_username);

        usernameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (registered) {
                    Log.d(LOG_TAG, "Enregistré");
                    connectDialog();
                } else {
                    Log.d(LOG_TAG, "Non enregistré");

                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                    alertDialogBuilder
                        .setTitle("Application non enregistrée")
                        .setMessage("Application non enregistrée")
                        .setCancelable(true)
                        .setPositiveButton("Continuer", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                setRegistered(true);
                                dialog.cancel();
                            }
                        })
                        .setNegativeButton("Quitter",new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                                MainActivity.this.finish();
                            }
                        });

                    createDialog(alertDialogBuilder);
                }
            }
        });
    }

    @Override
    protected void onIdentification(String idBadge) {
        Log.d(LOG_TAG, idBadge);
        badgeDialog(idBadge);
    }

    protected void setRegistered(boolean p_registered) {
        registered = p_registered;
        AppRegisteredText.setText(registered ? R.string.app_registred : R.string.app_not_registred);
    }

    protected void connectWithCAS(final String username, final String password) throws InterruptedException {
        Log.d(LOG_TAG, "Login: " + username);
        Log.d(LOG_TAG, "Mdp: " + password);
        Log.d(LOG_TAG, "Url: " + casConnexion.getUrl());

        final ProgressDialog loading = ProgressDialog.show(MainActivity.this, getResources().getString(R.string.cas_connection), getResources().getString(R.string.cas_in_connection), true);
        loading.setCancelable(false);
        new Thread() {
            @Override
            public void run() {
                try {
                    casConnexion.connect(username, password);
                    Thread.sleep(100);
                } catch (Exception e) {
                    Log.e(LOG_TAG, e.getMessage());
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (casConnexion.isConnected())
                            loading.setMessage(getResources().getString(R.string.cas_in_service_adding));
                        else {
                            loading.dismiss();
                            errorDialog(getResources().getString(R.string.cas_connection), getResources().getString(R.string.cas_error_connection));
                        }
                    }
                });

                if (casConnexion.isConnected()) {
                    try {
                        casConnexion.addService();
                        Thread.sleep(100);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, e.getMessage());
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loading.dismiss();

                            if (!casConnexion.isServiceAdded()) {
                                errorDialog(getResources().getString(R.string.cas_connection), getResources().getString(R.string.cas_error_service_adding));
                            }
                            else
                                Toast.makeText(MainActivity.this, "Connexion à réaliser avec Nemopay", Toast.LENGTH_SHORT).show(); // https://api.nemopay.net/services/POSS3/loginCas2?system_id=payutc
                        }
                    });
                }
            }
        }.start();
    }

    protected void connectWithBadge(final String idBadge, final String pin) {
        Log.d(LOG_TAG, "ID: " + idBadge);
        Log.d(LOG_TAG, "PIN: " + pin);

        final ProgressDialog ringProgressDialog = ProgressDialog.show(MainActivity.this, "Connexion ...", "Chargement ...", true);
        ringProgressDialog.setCancelable(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10000);
                } catch (Exception e) {
                }
                ringProgressDialog.dismiss();
            }
        }).start();
    }

    protected void createDialog(AlertDialog.Builder alertDialogBuilder) {
        if (alertDialog != null)
            alertDialog.dismiss();

        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    protected void errorDialog(final String title, final String message) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder
                .setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .setNegativeButton(R.string.ok, null);

        createDialog(alertDialogBuilder);
    }

    protected void badgeDialog(final String idBadge) {
        final View pinView = getLayoutInflater().inflate(R.layout.dialog_badge, null);
        final EditText pinInput = pinView.findViewById(R.id.input_pin);

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder
            .setTitle(R.string.badge_dialog)
            .setView(pinView)
            .setCancelable(true)
            .setPositiveButton(R.string.connexion, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    if (pinInput.getText().toString().equals("")) {
                        Toast.makeText(MainActivity.this, R.string.pin_required, Toast.LENGTH_SHORT).show();
                        dialog.cancel();
                        badgeDialog(idBadge);
                    }
                    else {
                        connectWithBadge(idBadge, pinInput.getText().toString());
                        dialog.cancel();
                    }
                }
            })
            .setNeutralButton(R.string.no_pin, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    connectWithBadge(idBadge, "0000");
                    dialog.cancel();
                }
            });

        createDialog(alertDialogBuilder);
    }

    protected void connectDialog() {
        final View usernameView = getLayoutInflater().inflate(R.layout.dialog_username, null);
        final EditText usernameInput = usernameView.findViewById(R.id.input_username);
        final EditText passwordInput = usernameView.findViewById(R.id.input_password);

        usernameInput.setText(casConnexion.getUsername());

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder
            .setTitle(R.string.username_dialog)
            .setView(usernameView)
            .setCancelable(false)
            .setPositiveButton(R.string.connexion, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    if (usernameInput.getText().toString().equals("") || passwordInput.getText().toString().equals("")) {
                        Toast.makeText(MainActivity.this, R.string.username_and_password_required, Toast.LENGTH_SHORT).show();
                        dialog.cancel();
                        connectDialog();
                    }
                    else {
                        try {
                            connectWithCAS(usernameInput.getText().toString(), passwordInput.getText().toString());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        dialog.cancel();
                    }
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });

        createDialog(alertDialogBuilder);
    }
}
