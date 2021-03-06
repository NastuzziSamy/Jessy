package fr.utc.simde.jessy;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Samy on 29/10/2017.
 */

public class CardManagementActivity extends BaseActivity {
    private static final String LOG_TAG = "_CardManagementActivity";

    TextView textUserId;
    TextView textUsername;
    TextView textFirstname;
    TextView textLastname;
    TextView textEmail;
    TextView textAdult;
    TextView textCotisant;
    TextView textTagId;
    TextView textTag;
    TextView textShortTag;
    TextView textSolde;

    TextView textUsernameGinger;
    TextView textFirstnameGinger;
    TextView textLastnameGinger;
    TextView textTypeGinger;
    TextView textEmailGinger;
    TextView textAdultGinger;
    TextView textCotisantGinger;
    TextView textTagGinger;

    String username;
    String badgeId;
    Integer solde;
    Integer toRun;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_management);

        this.textUserId = findViewById(R.id.text_user_id);
        this.textUsername = findViewById(R.id.text_username);
        this.textFirstname = findViewById(R.id.text_firstname);
        this.textLastname = findViewById(R.id.text_lastname);
        this.textEmail = findViewById(R.id.text_email);
        this.textAdult = findViewById(R.id.text_adult);
        this.textCotisant = findViewById(R.id.text_cotisant);
        this.textTagId = findViewById(R.id.text_tag_id);
        this.textTag = findViewById(R.id.text_tag);
        this.textShortTag = findViewById(R.id.text_short_tag);
        this.textSolde = findViewById(R.id.text_solde);

        this.textUsernameGinger = findViewById(R.id.text_username_ginger);
        this.textFirstnameGinger = findViewById(R.id.text_firstname_ginger);
        this.textLastnameGinger = findViewById(R.id.text_lastname_ginger);
        this.textEmailGinger = findViewById(R.id.text_email_ginger);
        this.textTypeGinger = findViewById(R.id.text_type_ginger);
        this.textAdultGinger = findViewById(R.id.text_adult_ginger);
        this.textCotisantGinger = findViewById(R.id.text_cotisant_ginger);
        this.textTagGinger = findViewById(R.id.text_tag_ginger);

        findViewById(R.id.contribute).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.infoDialog(CardManagementActivity.this, getString(R.string.contribute), getString(R.string.badge_waiting), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int id) {
                        toRun = 0;
                    }
                });

                toRun = 2;
            }
        });

        this.toRun = 0;
    }

    @Override
    protected void onIdentification(final String badgeId) {
        if (this.toRun == null)
            return;

        this.badgeId = badgeId;
        if (this.toRun == 0) {
            dialog.startLoading(CardManagementActivity.this, getString(R.string.information_collection), getString(R.string.buyer_info_collecting));

            new Thread(){
                @Override
                public void run() {
                    readCard();
                }
            }.start();
        }
        else if (this.toRun == 1)
            dialog.infoDialog(CardManagementActivity.this, getString(R.string.information_collection), "Pas encore fait");
        else if (this.toRun == 2) {
            this.toRun = null;
            hasRights(getString(R.string.contribute), new String[]{"GESUSERS"}, true, new Runnable() {
                @Override
                public void run() {
                    dialog.dismiss();

                    final View view = getLayoutInflater().inflate(R.layout.dialog_contribute, null);
                    final RadioGroup radioGroup = view.findViewById(R.id.radio_type);
                    final RadioButton radioButton = view.findViewById(R.id.radio_student);
                    final EditText editText = view.findViewById(R.id.nbr_days);

                    radioButton.setChecked(true);

                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CardManagementActivity.this);
                    alertDialogBuilder
                            .setTitle(R.string.contribute)
                            .setView(view)
                            .setCancelable(false)
                            .setPositiveButton(R.string.contribute, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialogInterface, int id) {
                                    dialog.startLoading(CardManagementActivity.this, getString(R.string.information_collection), getString(R.string.buyer_info_collecting));

                                    new Thread(){
                                        @Override
                                        public void run() {
                                            int id = radioGroup.indexOfChild(radioGroup.findViewById(radioGroup.getCheckedRadioButtonId()));

                                            if (contributeCard(id == 3 ? new SimpleDateFormat("yyyy-MM-dd").format(new Date(new Date().getTime() + (Long.parseLong(String.valueOf(editText.getText())) * 86400000L))) : (Integer.parseInt(new SimpleDateFormat("MM").format(new Date())) > 8 ? Integer.toString(Integer.parseInt(new SimpleDateFormat("yyyy").format(new Date())) + 1) : new SimpleDateFormat("yyyy").format(new Date())) + "-08-31", id < 2 ? 20 : 1))
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Toast.makeText(CardManagementActivity.this, R.string.contribute_now, Toast.LENGTH_LONG).show();
                                                    }
                                                });

                                            readCard();
                                        }
                                    }.start();
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialogInterface, int id) {
                                toRun = 0;
                            }});

                    dialog.createDialog(alertDialogBuilder, editText);
                }
            });
        }
    }

    void setUserInfo(JsonNode userInfo, JsonNode tagInfo, JsonNode gingerInfo) {
        try {
            this.textUserId.setText(Integer.toString(userInfo.get("id").intValue()));
            this.textUsername.setText(userInfo.get("username").textValue());
            this.textFirstname.setText(userInfo.get("first_name").textValue());
            this.textLastname.setText(userInfo.get("last_name").textValue());
            this.textEmail.setText(userInfo.get("email").textValue());
            this.textAdult.setText(userInfo.get("adult").booleanValue() ? getString(R.string.yes) : getString(R.string.no));
            this.textAdult.setTextColor(userInfo.get("adult").booleanValue() ? Color.BLUE : Color.RED);
            this.textCotisant.setText(userInfo.get("cotisant").booleanValue() ? getString(R.string.yes) : getString(R.string.no));
            this.textCotisant.setTextColor(userInfo.get("cotisant").booleanValue() ? Color.BLUE : Color.RED);

            this.textTagId.setText(Integer.toString(tagInfo.get("id").intValue()));
            this.textTag.setText(tagInfo.get("tag").textValue());
            this.textShortTag.setText(tagInfo.get("short_tag").textValue() == null ? getString(R.string.none) : tagInfo.get("short_tag").textValue());
            this.textSolde.setText(this.solde == null ? nemopaySession.forbidden(new String[]{"POSS3"}, false) : String.format("%.2f", new Float(this.solde) / 100.00f) + "€");
            this.textSolde.setTextColor(this.solde == null || this.solde == 0 ? Color.RED : Color.BLUE);

            this.textUsernameGinger.setText(gingerInfo.get("login").textValue());
            this.textFirstnameGinger.setText(gingerInfo.get("prenom").textValue());
            this.textLastnameGinger.setText(gingerInfo.get("nom").textValue());
            this.textEmailGinger.setText(gingerInfo.get("mail").textValue());
            this.textTypeGinger.setText(gingerInfo.get("type").textValue());
            this.textAdultGinger.setText(gingerInfo.get("is_adulte").booleanValue() ? getString(R.string.yes) : getString(R.string.no));
            this.textAdultGinger.setTextColor(gingerInfo.get("is_adulte").booleanValue() ? Color.BLUE : Color.RED);
            this.textCotisantGinger.setText(gingerInfo.get("is_cotisant").booleanValue() ? getString(R.string.yes) : getString(R.string.no));
            this.textCotisantGinger.setTextColor(gingerInfo.get("is_cotisant").booleanValue() ? Color.BLUE : Color.RED);
            this.textTagGinger.setText(gingerInfo.get("badge_uid").textValue());
        } catch (Exception e) {
            Log.e(LOG_TAG, "error: " + e.getMessage());
            dialog.errorDialog(this, getResources().getString(R.string.information_collection), getResources().getString(R.string.error_view), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int id) {
                    finish();
                }
            });
        }
    }

    protected Boolean contributeCard(final String fin, final Integer paid) {
        try {
            ginger.addCotisation(username, fin, paid);
            Thread.sleep(100);
        } catch (final Exception e) {
            Log.e(LOG_TAG, "error: " + e.getMessage());

            try {
                if (ginger.getRequest().getResponseCode() == 404)
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.errorDialog(CardManagementActivity.this, getString(R.string.information_collection), getString(R.string.user_not_recognized));
                        }
                    });
                if (ginger.getRequest().getResponseCode() == 409)
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.errorDialog(CardManagementActivity.this, getString(R.string.cotisant), getString(R.string.contribute_already));
                        }
                    });
                else
                    throw new Exception("");
            } catch (Exception e1) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        fatal(CardManagementActivity.this, getString(R.string.information_collection), e.getMessage());
                    }
                });
            }

            return false;
        }

        return true;
    }

    protected void readCard() {
        try {
            nemopaySession.getBuyerInfo(badgeId);
            Thread.sleep(100);

            // Toute une série de vérifications avant de lancer l'activité
            final JsonNode buyerInfo = nemopaySession.getRequest().getJSONResponse();

            if (!buyerInfo.has("lastname") || !buyerInfo.has("username") || !buyerInfo.has("firstname") || !buyerInfo.has("solde") || !buyerInfo.has("last_purchases") || !buyerInfo.get("last_purchases").isArray())
                throw new Exception("Unexpected JSON");

            solde = buyerInfo.get("solde").intValue();
        } catch (final Exception e) {
            Log.e(LOG_TAG, "error: " + e.getMessage());

            try {
                if (nemopaySession.getRequest().getResponseCode() == 400) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.errorDialog(CardManagementActivity.this, getString(R.string.information_collection), getString(R.string.badge_error_not_recognized));
                            toRun = 0;
                        }
                    });

                    return;
                }
                if (nemopaySession.getRequest().getResponseCode() == 403)
                    solde = null;
                else
                    throw new Exception("");
            } catch (Exception e1) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        fatal(CardManagementActivity.this, getString(R.string.information_collection), e.getMessage());
                    }
                });

                return;
            }
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.changeLoading(getResources().getString(R.string.user_ginger_info_collecting));
            }
        });

        try {
            ginger.getInfoFromBadge(badgeId);
            Thread.sleep(100);

            // Toute une série de vérifications avant de lancer l'activité
            final JsonNode gingerInfo = new ObjectMapper().readTree(ginger.getRequest().getResponse()); // La réponse n'est pas considérée comme une réponse json.. (l'en-tête ne renvoie pas une application/json)

            if (!gingerInfo.has("login") || !gingerInfo.has("nom") || !gingerInfo.has("prenom") || !gingerInfo.has("mail") || !gingerInfo.has("type") || !gingerInfo.has("is_adulte") || !gingerInfo.has("is_cotisant"))
                throw new Exception("Unexpected JSON");

            username = gingerInfo.get("login").textValue();
        } catch (final Exception e) {
            Log.e(LOG_TAG, "error: " + e.getMessage());

            try {
                if (ginger.getRequest().getResponseCode() == 404)
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.errorDialog(CardManagementActivity.this, getString(R.string.information_collection), getString(R.string.user_not_recognized));
                            toRun = 0;
                        }
                    });
                else
                    throw new Exception("");
            } catch (Exception e1) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        fatal(CardManagementActivity.this, getString(R.string.information_collection), e.getMessage());
                    }
                });
            }

            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.changeLoading(getResources().getString(R.string.information_collection));
            }
        });

        try {
            nemopaySession.foundUser(username);
            Thread.sleep(100);
        } catch (final Exception e) {
            Log.e(LOG_TAG, "error: " + e.getMessage());

            try {
                if (nemopaySession.getRequest().getResponseCode() == 400)
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.errorDialog(CardManagementActivity.this, getString(R.string.information_collection), getString(R.string.user_not_recognized));
                            toRun = 0;
                        }
                    });
                else
                    throw new Exception("");
            } catch (Exception e1) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        fatal(CardManagementActivity.this, getString(R.string.information_collection), e.getMessage());
                    }
                });
            }

            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.changeLoading(getResources().getString(R.string.user_nemopay_info_collecting));
            }
        });

        try {
            final JsonNode usersFounded = nemopaySession.getRequest().getJSONResponse();
            final JsonNode gingerInfo = new ObjectMapper().readTree(ginger.getRequest().getResponse()); // La réponse n'est pas considérée comme une réponse json.. (l'en-tête ne renvoie pas une application/json)

            if (!usersFounded.has(0) || !usersFounded.get(0).has("id"))
                throw new Exception(getString(R.string.user_not_recognized));

            nemopaySession.getUser(usersFounded.get(0).get("id").intValue());
            Thread.sleep(100);

            // Toute une série de vérifications avant de lancer l'activité
            final JsonNode userInfo = nemopaySession.getRequest().getJSONResponse();

            if (!userInfo.has("user") || !userInfo.has("tag"))
                throw new Exception("Unexpected JSON");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setUserInfo(userInfo.get("user"), userInfo.get("tag"), gingerInfo);
                    dialog.stopLoading();
                    toRun = 0;
                }
            });
        } catch (final Exception e) {
            Log.e(LOG_TAG, "error: " + e.getMessage());

            try {
                if (nemopaySession.getRequest().getResponseCode() == 400)
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.errorDialog(CardManagementActivity.this, getString(R.string.information_collection), getString(R.string.user_error_collecting));
                            toRun = 0;
                        }
                    });
                else
                    throw new Exception("");
            } catch (Exception e1) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        fatal(CardManagementActivity.this, getString(R.string.information_collection), e.getMessage());
                    }
                });
            }

            return;
        }
    }
}
