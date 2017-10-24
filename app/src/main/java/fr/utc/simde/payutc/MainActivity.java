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

import java.io.IOException;

import fr.utc.simde.payutc.tools.HTTPRequest;
import fr.utc.simde.payutc.tools.NFCActivity;

public class MainActivity extends NFCActivity {
    private static final String LOG_TAG = "MainActivity";
    private static Boolean registered = false;
    private static String login = "";
    private static String password = "";

    private static AlertDialog alertDialog;

    private static TextView AppConfigText;
    private static TextView AppRegisteredText;
    private static Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AppConfigText = (TextView) findViewById(R.id.text_app_config);
        AppRegisteredText = (TextView) findViewById(R.id.text_app_registered);
        loginButton = (Button) findViewById(R.id.button_login);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread thread = new Thread(new Runnable(){
                    @Override
                    public void run() {
                        try {
                            HTTPRequest test = new HTTPRequest("https://nastuzzi.fr");
                            test.addArg("coucou", "test");
                            Log.d(LOG_TAG, Integer.toString(test.get()));
                            Log.d(LOG_TAG, test.getResponse());
                        } catch (Exception e) {
                            Log.e(LOG_TAG, e.getMessage());
                        }
                    }
                });
                thread.start();
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
    protected void onIdentification(String id) {
        Log.d(LOG_TAG, id);
        badgeDialog(id);
    }

    protected void setRegistered(boolean p_registered) {
        registered = p_registered;
        AppRegisteredText.setText(registered ? R.string.app_registred : R.string.app_not_registred);
    }

    protected Boolean connectWithBadge(String idBadge, String pin) {
        Log.d(LOG_TAG, "ID: " + idBadge);
        Log.d(LOG_TAG, "PIN: " + pin);

        final ProgressDialog ringProgressDialog = ProgressDialog.show(MainActivity.this, "Please wait ...", "Downloading Image ...", true);
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

        return false;
    }

    protected void createDialog(AlertDialog.Builder alertDialogBuilder) {
        if (alertDialog != null)
            alertDialog.dismiss();

        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    protected void badgeDialog(String idBadge) {
        final View pinView = getLayoutInflater().inflate(R.layout.dialog_badge, null);
        final EditText pinInput = (EditText) pinView.findViewById(R.id.input_pin);
        final String uid = idBadge;

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
                        badgeDialog(uid);
                    }
                    else {
                        connectWithBadge(uid, pinInput.getText().toString());
                        dialog.cancel();
                    }
                }
            })
            .setNeutralButton(R.string.no_pin, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    connectWithBadge(uid, "0000");
                    dialog.cancel();
                }
            });

        createDialog(alertDialogBuilder);
    }

    protected void connectDialog() {
        final View loginView = getLayoutInflater().inflate(R.layout.dialog_login, null);
        final EditText loginInput = (EditText) loginView.findViewById(R.id.input_login);
        final EditText passwordInput = (EditText) loginView.findViewById(R.id.input_password);

        loginInput.setText(login);
        passwordInput.setText(password);

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder
            .setTitle(R.string.login_dialog)
            .setView(loginView)
            .setCancelable(true)
            .setPositiveButton(R.string.connexion, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    if (loginInput.getText().toString().equals("") || passwordInput.getText().toString().equals("")) {
                        Toast.makeText(MainActivity.this, R.string.login_and_password_required, Toast.LENGTH_SHORT).show();
                        dialog.cancel();
                        connectDialog();
                    }
                    else {
                        Log.d(LOG_TAG, "Login: " + loginInput.getText().toString());
                        Log.d(LOG_TAG, "Mdp: " + passwordInput.getText().toString());
                        passwordInput.setText("");
                        dialog.cancel();
                    }
                }
            })
            .setNeutralButton(R.string.erase, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    loginInput.setText("");
                    passwordInput.setText("");
                    dialog.cancel();
                    connectDialog();
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            })
            .setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(final DialogInterface dialog) {
                    login = loginInput.getText().toString();
                    password = passwordInput.getText().toString();
                }
            });

        createDialog(alertDialogBuilder);
    }
}
