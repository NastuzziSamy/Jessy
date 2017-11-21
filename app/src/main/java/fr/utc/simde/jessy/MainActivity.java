package fr.utc.simde.jessy;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.Image;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import fr.utc.simde.jessy.tools.CASConnexion;
import fr.utc.simde.jessy.tools.Config;
import fr.utc.simde.jessy.tools.Ginger;
import fr.utc.simde.jessy.tools.NemopaySession;

/**
 * Created by Samy on 24/10/2017.
 */

public class MainActivity extends BaseActivity {
    private static final String LOG_TAG = "_MainActivity";
    private static final String service = "https://assos.utc.fr";

    private static ImageView appImg;
    private static TextView appNameText;
    private static TextView appConfigText;
    private static TextView appRegisteredText;
    private static Button usernameButton;

    private boolean casConnexionDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("payutc", Activity.MODE_PRIVATE);

        nemopaySession = new NemopaySession(MainActivity.this);
        ginger = new Ginger(MainActivity.this);
        casConnexion = new CASConnexion(nemopaySession);
        config = new Config(sharedPreferences);

        String key = sharedPreferences.getString("key", "");
        if (!key.equals(""))
            setNemopayKey(key);

        key = sharedPreferences.getString("key_ginger", "");
        if (!key.equals(""))
            setGingerKey(key);

        appImg = findViewById(R.id.img_payutc);
        appNameText = findViewById(R.id.text_app_name);
        appConfigText = findViewById(R.id.text_app_config);
        appRegisteredText = findViewById(R.id.text_app_registered);
        usernameButton = findViewById(R.id.button_username);

        casConnexionDialog = false;

        appImg.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                config.reset();
                restartApp(MainActivity.this);
                return false;
            }
        });

        appRegisteredText.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                optionDialog();
                return false;
            }
        });

        usernameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                casDialog();
            }
        });

        setConfig();
        checkUpdate(false);

        startSellByQRCodeActivity(MainActivity.this);
    }

    @Override
    public void onRestart() {
        super.onRestart();

        disconnect();
        setConfig();

        if (!nemopaySession.isRegistered()) {
            final String key = sharedPreferences.getString("key", "");
            if (!key.equals(""))
                setNemopayKey(key);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        disconnect();
    }

    @Override
    protected void onIdentification(final String badgeId) {
        if (!dialog.isShowing() || casConnexionDialog)
            badgeDialog(badgeId);

        casConnexionDialog = false;
    }

    @Override
    protected void enableInternetDialog(final Context context) {
        Toast.makeText(context, R.string.internet_not_available, Toast.LENGTH_SHORT).show();

        dialog.infoDialog(MainActivity.this, getString(R.string.connection), getString(R.string.internet_accessibility), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!checkInternet(context))
                    enableInternetDialog(context);
                else
                    restartApp(MainActivity.this);
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (Integer.parseInt(android.os.Build.VERSION.SDK) > 5 && keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            onBackPressed();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        Intent setIntent = new Intent(Intent.ACTION_MAIN);
        setIntent.addCategory(Intent.CATEGORY_HOME);
        setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(setIntent);
    }

    @Override
    protected void unregister(final Activity activity) {
        super.unregister(activity);

        ((TextView) findViewById(R.id.text_app_registered)).setText(R.string.app_not_registred);
    }

    protected void setConfig() {
        if (config.getFoundationId() != -1) {
            appNameText.setText(config.getFoundationName());
            appConfigText.setText(config.getLocationName());
            nemopaySession.setFoundation(config.getFoundationId(), config.getFoundationName(), config.getLocationId());
        }
        else if (config.getOptionList().size() != 0) {
            String list = "";
            Iterator<JsonNode> options = config.getOptionList().elements();

            if (config.getCanSell())
                list = ", " + getString(R.string.sell);

            while (options.hasNext())
                list += ", " + options.next().textValue();

            appNameText.setText(R.string.app_name);
            appConfigText.setText(list.length() == 0 ? "" : list.substring(2));
            nemopaySession.setFoundation(-1, "", -1);
        }
        else {
            appNameText.setText(R.string.app_name);
            appConfigText.setText("");
            nemopaySession.setFoundation(-1, "", -1);
        }
    }

    protected void connectWithCAS(final String username, final String password) throws InterruptedException {
        dialog.startLoading(MainActivity.this, getString(R.string.cas_connection), getString(R.string.cas_in_url));

        casConnexionDialog = false;
        new Thread() {
            @Override
            public void run() {
                if (casConnexion.getUrl().equals("")) {
                    try {
                        nemopaySession.getCASUrl();
                        String url = nemopaySession.getRequest().getResponse();
                        casConnexion.setUrl(url.substring(1, url.length() - 1));
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "error: " + e.getMessage());
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (casConnexion.getUrl().equals("")) {
                            dialog.stopLoading();
                            dialog.errorDialog(MainActivity.this, getString(R.string.cas_connection), getString(R.string.cas_error_url));
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
                            dialog.errorDialog(MainActivity.this, getString(R.string.cas_connection), getString(R.string.cas_error_connection));
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
                            dialog.errorDialog(MainActivity.this, getString(R.string.cas_connection), getString(R.string.cas_error_service_adding));
                        }
                    }
                });

                if (!casConnexion.isServiceAdded())
                    return;

                try {
                    nemopaySession.loginCas(casConnexion.getTicket(), service);
                    Thread.sleep(100);
                } catch (Exception e) {
                    try { // Des fois, la session est null
                        nemopaySession.loginCas(casConnexion.getTicket(), service);
                        Thread.sleep(100);
                    } catch (Exception e1) {
                        Log.e(LOG_TAG, "error: " + e.getMessage());
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.stopLoading();

                        if (!nemopaySession.isConnected())
                            dialog.errorDialog(MainActivity.this, getString(R.string.cas_connection), getString(R.string.cas_error_service_linking));
                        else if (!nemopaySession.isRegistered())
                            keyDialog();
                        else
                            startFoundationListActivity(MainActivity.this);
                    }
                });
            }
        }.start();
    }

    protected void connectWithBadge(final String badgeId, final String pin) {
        if (!nemopaySession.isRegistered() || nemopaySession.isConnected())
            return;

        dialog.startLoading(MainActivity.this, getString(R.string.badge_dialog), getString(R.string.badge_recognization));
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    nemopaySession.loginBadge(badgeId, pin);
                    Thread.sleep(100);
                } catch (final Exception e) {
                    try { // Des fois, la session est null
                        nemopaySession.loginBadge(badgeId, pin);
                        Thread.sleep(100);
                    } catch (final Exception e1) {
                        Log.e(LOG_TAG, "error: " + e.getMessage());

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (nemopaySession.getRequest().getResponseCode() == 400)
                                    dialog.errorDialog(MainActivity.this, getString(R.string.badge_dialog), getString(R.string.badge_pin_error_not_recognized));
                                else
                                    dialog.errorDialog(MainActivity.this, getString(R.string.badge_dialog), e.getMessage());
                            }
                        });
                    }
                }

                if (nemopaySession.isConnected()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.stopLoading();

                            try {
                                startFoundationListActivity(MainActivity.this);
                            } catch (Exception e) {
                                Log.e(LOG_TAG, "error: " + e.getMessage());
                            }
                        }
                    });
                }
            }
        }).start();
    }

    protected void badgeDialog(final String badgeId) {
        if (!nemopaySession.isRegistered()) {
            dialog.errorDialog(MainActivity.this, getString(R.string.badge_connection), getString(R.string.badge_app_not_registered));
            return;
        }

        if (nemopaySession.isConnected()) {
            dialog.errorDialog(MainActivity.this, getString(R.string.badge_connection), getString(R.string.already_connected) + " " + nemopaySession.getUsername());
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
            .setPositiveButton(R.string.connect, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int id) {
                    if (pinInput.getText().toString().equals("")) {
                        Toast.makeText(MainActivity.this, R.string.pin_required, Toast.LENGTH_SHORT).show();
                        dialogInterface.cancel();
                        badgeDialog(badgeId);
                    }
                    else {
                        connectWithBadge(badgeId, pinInput.getText().toString());
                        dialogInterface.cancel();
                    }
                }
            })
            .setNegativeButton(R.string.cancel, null);

        noPinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                dialog.errorDialog(MainActivity.this, getResources().getString(R.string.badge_connection), getResources().getString(R.string.badge_pin_not_initialized));
            }
        });

        dialog.createDialog(alertDialogBuilder, pinInput);
    }

    protected void casDialog() {
        if (nemopaySession.isConnected()) {
            dialog.errorDialog(MainActivity.this, getString(R.string.cas_connection), getString(R.string.already_connected) + " " + nemopaySession.getUsername());
            return;
        }

        casConnexionDialog = true;

        final View usernameView = getLayoutInflater().inflate(R.layout.dialog_login, null);
        final EditText usernameInput = usernameView.findViewById(R.id.input_username);
        final EditText passwordInput = usernameView.findViewById(R.id.input_password);

        usernameInput.setText(casConnexion.getUsername());

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder
            .setTitle(R.string.username_dialog)
            .setView(usernameView)
            .setCancelable(false)
            .setPositiveButton(R.string.connect, new DialogInterface.OnClickListener() {
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
                                                dialog.errorDialog(MainActivity.this, getString(R.string.nemopay_connection), getString(R.string.nemopay_error_registering));
                                            else
                                                setNemopayKey(nemopaySession.getKey());
                                        }
                                    });
                                }
                            }.start();
                        }
                    }
                });

        dialog.createDialog(alertDialogBuilder, nameInput);
    }

    protected void optionDialog() {
        final View view = getLayoutInflater().inflate(R.layout.dialog_key_force, null);
        final EditText keyInput = view.findViewById(R.id.input_key);

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder
                .setTitle(R.string.key_registration)
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(R.string.register, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int id) {
                        unregister(MainActivity.this);
                        setNemopayKey(keyInput.getText().toString());
                    }
                })
                .setNegativeButton(R.string.cancel, null);

        dialog.createDialog(alertDialogBuilder, keyInput);
    }
}
