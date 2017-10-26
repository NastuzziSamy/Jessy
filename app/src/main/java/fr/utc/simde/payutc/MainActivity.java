package fr.utc.simde.payutc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import fr.utc.simde.payutc.tools.HTTPRequest;
import fr.utc.simde.payutc.tools.NFCActivity;
import fr.utc.simde.payutc.tools.CASConnexion;
import fr.utc.simde.payutc.tools.Dialog;
import fr.utc.simde.payutc.tools.NemopaySession;

public class MainActivity extends NFCActivity {
    private static final String LOG_TAG = "_MainActivity";
    private static final String service = "https://assos.utc.fr";
    private static Dialog dialog;

    private static NemopaySession nemopaySession;
    private static CASConnexion casConnexion;
    private static SharedPreferences sharedPreferences;

    private static TextView AppConfigText;
    private static TextView AppRegisteredText;
    private static Button usernameButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dialog = new Dialog(MainActivity.this);
        nemopaySession = new NemopaySession();
        casConnexion = new CASConnexion(nemopaySession);
        sharedPreferences = getSharedPreferences("payutc", Activity.MODE_PRIVATE);

        final String key = sharedPreferences.getString("key", "");
        if (!key.equals(""))
            setKey(key);

        AppConfigText = findViewById(R.id.text_app_config);
        AppRegisteredText = findViewById(R.id.text_app_registered);
        usernameButton = findViewById(R.id.button_username);

        AppRegisteredText.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!nemopaySession.isRegistered())
                    addKeyDialog();
                else // A supprimer = embêtant si les clés sont réinitialisées
                    delKey();

                return false;
            }
        });

        usernameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectDialog();
            }
        });
    }

    @Override
    protected void onIdentification(final String idBadge) {
        badgeDialog(idBadge);
    }

    protected void delKey() {
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.remove("key");
        edit.apply();
    }

    protected void setKey(final String key) {
        if (nemopaySession.isRegistered()) {
            dialog.errorDialog(getResources().getString(R.string.nemopay_connection), getResources().getString(R.string.nemopay_already_registered));
            return;
        }

        final ProgressDialog loading = ProgressDialog.show(MainActivity.this, getResources().getString(R.string.nemopay_connection), getResources().getString(R.string.nemopay_authentification), true);
        loading.setCancelable(false);

        new Thread() {
            @Override
            public void run() {
                try {
                    nemopaySession.loginApp(key);
                    Thread.sleep(100);
                } catch (Exception e) {
                    Log.e(LOG_TAG, e.getMessage());
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loading.dismiss();

                        if (nemopaySession.isRegistered()) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("key", key);
                            editor.apply();

                            ((TextView) findViewById(R.id.text_app_registered)).setText(nemopaySession.getName().substring(0, nemopaySession.getName().length() - (nemopaySession.getName().matches("^.* - ([0-9]{4})([/-])([0-9]{2})\\2([0-9]{2})$") ? 13 : 0)));
                        }
                        else
                            dialog.errorDialog(getResources().getString(R.string.nemopay_connection), getResources().getString(R.string.nemopay_error_registering));
                    }
                });
            }
        }.start();
    }

    protected void connectWithCAS(final String username, final String password) throws InterruptedException {
        dialog.dismiss();

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
                            dialog.errorDialog(getResources().getString(R.string.cas_connection), getResources().getString(R.string.cas_error_connection));
                        }
                    }
                });

                if (casConnexion.isConnected()) {
                    try {
                        casConnexion.addService(service);
                        Thread.sleep(100);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, e.getMessage());
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (casConnexion.isServiceAdded())
                                loading.setMessage(getResources().getString(R.string.nemopay_connection));
                            else {
                                loading.dismiss();
                                dialog.errorDialog(getResources().getString(R.string.cas_connection), getResources().getString(R.string.cas_error_service_adding));
                            }
                        }
                    });

                    if (casConnexion.isServiceAdded()) {
                        try {
                            nemopaySession.loginCas(casConnexion.getTicket(), service);
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            Log.e(LOG_TAG, e.getMessage());
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loading.dismiss();

                                if (!nemopaySession.isConnected())
                                    dialog.errorDialog(getResources().getString(R.string.cas_connection), getResources().getString(R.string.cas_error_service_linking));
                                else if (!nemopaySession.isRegistered())
                                    keyDialog();
                                else
                                    Toast.makeText(MainActivity.this, "Tout est bon !", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }.start();
    }

    protected void connectWithBadge(final String idBadge, final String pin) {
        dialog.dismiss();

        if (!nemopaySession.isRegistered() || nemopaySession.isConnected())
            return;

        final ProgressDialog loading = ProgressDialog.show(MainActivity.this, getResources().getString(R.string.badge_dialog), getResources().getString(R.string.badge_recognization), true);
        loading.setCancelable(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    nemopaySession.loginBadge(idBadge, pin);
                    Thread.sleep(100);
                } catch (Exception e) {
                    Log.e(LOG_TAG, e.getMessage());
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loading.dismiss();

                        try {
                            if (nemopaySession.isConnected())
                                Toast.makeText(MainActivity.this, "Tout est bon !", Toast.LENGTH_SHORT).show();
                            else if (nemopaySession.getRequest().getResponseCode() == 400)
                                dialog.errorDialog(getResources().getString(R.string.badge_dialog), getResources().getString(R.string.badge_pin_error_not_recognized));
                            else
                                dialog.errorDialog(getResources().getString(R.string.badge_dialog), getResources().getString(R.string.badge_error_no_rights));
                        } catch (Exception e) {
                            Log.e(LOG_TAG, e.getMessage());
                        }
                    }
                });
            }
        }).start();
    }

    protected void badgeDialog(final String idBadge) {
        if (!nemopaySession.isRegistered()) {
            dialog.errorDialog(getResources().getString(R.string.badge_connection), getResources().getString(R.string.badge_app_not_registered));
            return;
        }

        if (nemopaySession.isConnected()) {
            dialog.errorDialog(getResources().getString(R.string.badge_connection), getResources().getString(R.string.already_connected));
            return;
        }

        final View pinView = getLayoutInflater().inflate(R.layout.dialog_badge, null);
        final EditText pinInput = pinView.findViewById(R.id.input_pin);

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder
            .setTitle(R.string.badge_dialog)
            .setView(pinView)
            .setCancelable(true)
            .setPositiveButton(R.string.connexion, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int id) {
                    if (pinInput.getText().toString().equals("")) {
                        Toast.makeText(MainActivity.this, R.string.pin_required, Toast.LENGTH_SHORT).show();
                        dialogInterface.cancel();
                        badgeDialog(idBadge);
                    }
                    else {
                        connectWithBadge(idBadge, pinInput.getText().toString());
                        dialogInterface.cancel();
                    }
                }
            })
            .setNeutralButton(R.string.no_pin, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int id) {
                    connectWithBadge(idBadge, "0000");
                    dialogInterface.cancel();
                }
            });

        dialog.createDialog(alertDialogBuilder, pinInput);
    }

    protected void connectDialog() {
        if (nemopaySession.isConnected()) {
            dialog.errorDialog(getResources().getString(R.string.cas_connection), getResources().getString(R.string.already_connected));
            return;
        }

        final View usernameView = getLayoutInflater().inflate(R.layout.dialog_login, null);
        final EditText usernameInput = usernameView.findViewById(R.id.input_username);
        final EditText passwordInput = usernameView.findViewById(R.id.input_password);

        usernameInput.setText(casConnexion.getUsername());

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder
            .setTitle(R.string.username_dialog)
            .setView(usernameView)
            .setCancelable(false)
            .setPositiveButton(R.string.connexion, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int id) {
                    if (usernameInput.getText().toString().equals("") || passwordInput.getText().toString().equals("")) {
                        if (!usernameInput.getText().toString().equals(""))
                            casConnexion.setUsername(usernameInput.getText().toString());

                        Toast.makeText(MainActivity.this, R.string.username_and_password_required, Toast.LENGTH_SHORT).show();
                        dialogInterface.cancel();
                        connectDialog();
                    }
                    else {
                        try {
                            connectWithCAS(usernameInput.getText().toString(), passwordInput.getText().toString());
                        } catch (Exception e) {
                            Log.e(LOG_TAG, e.getMessage());
                        }
                        dialogInterface.cancel();
                    }
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int id) {
                    dialogInterface.cancel();
                }
            });

        dialog.createDialog(alertDialogBuilder, usernameInput.getText().toString().isEmpty() ? usernameInput : passwordInput);
    }

    protected void keyDialog() {
        final View keyView = getLayoutInflater().inflate(R.layout.dialog_key, null);
        final EditText nameInput = keyView.findViewById(R.id.input_name);
        final EditText descriptionInput = keyView.findViewById(R.id.input_description);
        final String date = new SimpleDateFormat("yyyy/MM/dd", Locale.FRANCE).format(new Date());

        nameInput.setText("Téléphone de " + casConnexion.getUsername() + " - " + date);

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder
            .setTitle(R.string.key_registration)
            .setView(keyView)
            .setCancelable(false)
            .setPositiveButton(R.string.register, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int id) {
                    if (nameInput.getText().toString().equals("")) {
                        Toast.makeText(MainActivity.this, R.string.key_name_required, Toast.LENGTH_SHORT).show();
                        dialogInterface.cancel();
                        keyDialog();
                    }
                    else {
                        dialogInterface.cancel();

                        final ProgressDialog loading = ProgressDialog.show(MainActivity.this, getResources().getString(R.string.nemopay_connection), getResources().getString(R.string.nemopay_registering), true);
                        loading.setCancelable(false);
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    nemopaySession.registerApp(nameInput.getText().toString() + (nameInput.getText().toString().matches("^.* - ([0-9]{4})([/-])([0-9]{2})\\2([0-9]{2})$") ? "" : " - " + date), descriptionInput.getText().toString(), service);
                                    Thread.sleep(100);
                                } catch (Exception e) {
                                    Log.e(LOG_TAG, e.getMessage());
                                }

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        loading.dismiss();

                                        if (nemopaySession.getKey().isEmpty())
                                            dialog.errorDialog(getResources().getString(R.string.nemopay_connection), getResources().getString(R.string.nemopay_error_registering));
                                        else
                                            setKey(nemopaySession.getKey());
                                    }
                                });
                            }
                        }.start();
                    }
                }
            });

        dialog.createDialog(alertDialogBuilder, nameInput);
    }

    protected void addKeyDialog() {
        final View keyView = getLayoutInflater().inflate(R.layout.dialog_key_force, null);
        final EditText keyInput = keyView.findViewById(R.id.input_key);

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder
                .setTitle(R.string.key_registration)
                .setView(keyView)
                .setCancelable(false)
                .setPositiveButton(R.string.register, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int id) {
                        setKey(keyInput.getText().toString());
                    }
                });

        dialog.createDialog(alertDialogBuilder, keyInput);
    }
}
