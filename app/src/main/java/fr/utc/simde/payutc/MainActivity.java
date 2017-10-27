package fr.utc.simde.payutc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import fr.utc.simde.payutc.tools.CASConnexion;
import fr.utc.simde.payutc.tools.Dialog;
import fr.utc.simde.payutc.tools.NemopaySession;

public class MainActivity extends BaseActivity {
    private static final String LOG_TAG = "_MainActivity";
    private static final String service = "https://assos.utc.fr";

    private static SharedPreferences sharedPreferences;

    private static TextView AppConfigText;
    private static TextView AppRegisteredText;
    private static Button usernameButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dialog = new Dialog(MainActivity.this);
        nemopaySession = new NemopaySession(MainActivity.this);
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
                else
                    unregister();

                return false;
            }
        });

        usernameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                casDialog();
            }
        });
    }

    @Override
    protected void onIdentification(final String idBadge) {
        if (!dialog.isShowing())
            badgeDialog(idBadge);
    }

    @Override
    protected void unregister() {
        super.unregister();

        ((TextView) findViewById(R.id.text_app_registered)).setText(R.string.app_not_registred);
    }

    protected void delKey() {
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.remove("key");
        edit.apply();

        unregister();
    }

    protected void setKey(final String key) {
        if (nemopaySession.isRegistered()) {
            dialog.errorDialog(getString(R.string.nemopay_connection), getString(R.string.nemopay_already_registered));
            return;
        }

        dialog.startLoading(MainActivity.this, getString(R.string.nemopay_connection), getString(R.string.nemopay_authentification));
        new Thread() {
            @Override
            public void run() {
                try {
                    nemopaySession.loginApp(key, casConnexion);
                    Thread.sleep(100);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.stopLoading();

                        if (nemopaySession.isRegistered()) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("key", key);
                            editor.apply();

                            ((TextView) findViewById(R.id.text_app_registered)).setText(nemopaySession.getName().substring(0, nemopaySession.getName().length() - (nemopaySession.getName().matches("^.* - ([0-9]{4})([/-])([0-9]{2})\\2([0-9]{2})$") ? 13 : 0)));
                        }
                        else
                            dialog.errorDialog(getString(R.string.nemopay_connection), getString(R.string.nemopay_error_registering));
                    }
                });
            }
        }.start();
    }

    protected void startFoundationListActivity() {
        MainActivity.this.startActivity(new Intent(MainActivity.this, FoundationListActivity.class));
    }

    protected void connectWithCAS(final String username, final String password) throws InterruptedException {
        dialog.startLoading(MainActivity.this, getString(R.string.cas_connection), getString(R.string.cas_in_url));
        new Thread() {
            @Override
            public void run() {
                if (casConnexion.getUrl().equals("")) {
                    try {
                        if (nemopaySession.getCASUrl() == 200) {
                            Log.d(LOG_TAG, nemopaySession.getRequest().getResponse());
                            String url = nemopaySession.getRequest().getResponse();
                            casConnexion.setUrl(url.substring(1, url.length() - 1));
                        }
                        else
                            throw new Exception("Impossible to get CAS url");
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "error: " + e.getMessage());
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (casConnexion.getUrl().equals("")) {
                            dialog.stopLoading();
                            dialog.errorDialog(getString(R.string.cas_connection), getString(R.string.cas_error_url));
                        }
                        else
                            dialog.changeLoading(getString(R.string.cas_in_connection));
                    }
                });

                if (casConnexion.getUrl().equals(""))
                    return;

                try {
                    casConnexion.connect(username, password);
                    Thread.sleep(100);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (casConnexion.isConnected())
                            dialog.changeLoading(getString(R.string.cas_in_service_adding));
                        else {
                            dialog.stopLoading();
                            dialog.errorDialog(getString(R.string.cas_connection), getString(R.string.cas_error_connection));
                        }
                    }
                });

                if (!casConnexion.isConnected())
                    return;

                try {
                    casConnexion.addService(service);
                    Thread.sleep(100);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (casConnexion.isServiceAdded())
                            dialog.changeLoading(getString(R.string.nemopay_connection));
                        else {
                            dialog.stopLoading();
                            dialog.errorDialog(getString(R.string.cas_connection), getString(R.string.cas_error_service_adding));
                        }
                    }
                });

                if (!casConnexion.isServiceAdded())
                    return;

                try {
                    nemopaySession.loginCas(casConnexion.getTicket(), service);
                    Thread.sleep(1000);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.stopLoading();

                        if (!nemopaySession.isConnected())
                            dialog.errorDialog(getString(R.string.cas_connection), getString(R.string.cas_error_service_linking));
                        else if (!nemopaySession.isRegistered())
                            keyDialog();
                        else
                            startFoundationListActivity();
                    }
                });
            }
        }.start();
    }

    protected void connectWithBadge(final String idBadge, final String pin) {
        if (!nemopaySession.isRegistered() || nemopaySession.isConnected())
            return;

        dialog.startLoading(MainActivity.this, getString(R.string.badge_dialog), getString(R.string.badge_recognization));
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    nemopaySession.loginBadge(idBadge, pin);
                    Thread.sleep(100);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.stopLoading();

                        try {
                            if (nemopaySession.isConnected())
                                startFoundationListActivity();
                            else if (nemopaySession.getRequest().getResponseCode() == 400)
                                dialog.errorDialog(getString(R.string.badge_dialog), getString(R.string.badge_pin_error_not_recognized));
                            else
                                dialog.errorDialog(getString(R.string.badge_dialog), getString(R.string.badge_error_no_rights) + ".\n" + nemopaySession.needRights(MainActivity.this));
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "error: " + e.getMessage());
                        }
                    }
                });
            }
        }).start();
    }

    protected void badgeDialog(final String idBadge) {
        if (!nemopaySession.isRegistered()) {
            dialog.errorDialog(getString(R.string.badge_connection), getString(R.string.badge_app_not_registered));
            return;
        }

        if (nemopaySession.isConnected()) {
            dialog.errorDialog(getString(R.string.badge_connection), getString(R.string.already_connected) + " " + nemopaySession.getUsername());
            return;
        }

        final View pinView = getLayoutInflater().inflate(R.layout.dialog_badge, null);
        final EditText pinInput = pinView.findViewById(R.id.input_pin);
        final Button noPinButton = pinView.findViewById(R.id.button_no_pin);

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder
            .setTitle(R.string.badge_dialog)
            .setView(pinView)
            .setCancelable(false)
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
            .setNegativeButton(R.string.cancel, null);

        noPinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                connectWithBadge(idBadge, "0000");
            }
        });

        dialog.createDialog(alertDialogBuilder, pinInput);
    }

    protected void casDialog() {
        if (nemopaySession.isConnected()) {
            dialog.errorDialog(getString(R.string.cas_connection), getString(R.string.already_connected) + " " + nemopaySession.getUsername());
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
                        casDialog();
                    }
                    else {
                        try {
                            connectWithCAS(usernameInput.getText().toString(), passwordInput.getText().toString());
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "error: " + e.getMessage());
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

                        final ProgressDialog loading = ProgressDialog.show(MainActivity.this, getString(R.string.nemopay_connection), getString(R.string.nemopay_registering), true);
                        loading.setCancelable(false);
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    nemopaySession.registerApp(nameInput.getText().toString() + (nameInput.getText().toString().matches("^.* - ([0-9]{4})([/-])([0-9]{2})\\2([0-9]{2})$") ? "" : " - " + date), descriptionInput.getText().toString(), service);
                                    Thread.sleep(100);
                                } catch (Exception e) {
                                    Log.e(LOG_TAG, "error: " + e.getMessage());
                                }

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        loading.dismiss();

                                        if (nemopaySession.getKey().isEmpty())
                                            dialog.errorDialog(getString(R.string.nemopay_connection), getString(R.string.nemopay_error_registering));
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
                })
                .setNegativeButton(R.string.cancel, null);

        dialog.createDialog(alertDialogBuilder, keyInput);
    }
}
