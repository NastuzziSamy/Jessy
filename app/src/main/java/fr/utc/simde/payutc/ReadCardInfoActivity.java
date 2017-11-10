package fr.utc.simde.payutc;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.utc.simde.payutc.adapters.ListAdapater;
import fr.utc.simde.payutc.tools.HTTPRequest;

/**
 * Created by Samy on 29/10/2017.
 */

public class ReadCardInfoActivity extends BaseActivity {
    private static final String LOG_TAG = "_ReadCardInfoActivity";

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_card_info);

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

        dialog.startLoading(ReadCardInfoActivity.this, getString(R.string.information_collection), getString(R.string.badge_waiting));
    }

    @Override
    protected void onIdentification(final String badgeId) {
        dialog.startLoading(ReadCardInfoActivity.this, getResources().getString(R.string.information_collection), getResources().getString(R.string.buyer_info_collecting));

        new Thread() {
            @Override
            public void run() {
                try {
                    nemopaySession.getBuyerInfo(badgeId);
                    Thread.sleep(100);

                    // Toute une série de vérifications avant de lancer l'activité
                    final JsonNode buyerInfo = nemopaySession.getRequest().getJSONResponse();

                    if (!buyerInfo.has("lastname") || !buyerInfo.has("username") || !buyerInfo.has("firstname") || !buyerInfo.has("solde") || !buyerInfo.has("last_purchases") || !buyerInfo.get("last_purchases").isArray())
                        throw new Exception("Unexpected JSON");
                } catch (final Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());

                    try {
                        if (nemopaySession.getRequest().getResponseCode() == 400)
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.errorDialog(ReadCardInfoActivity.this, getString(R.string.information_collection), getString(R.string.badge_error_not_recognized));
                                }
                            });
                        else
                            throw new Exception("");
                    } catch (Exception e1) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                fatal(ReadCardInfoActivity.this, getString(R.string.information_collection), e.getMessage());
                            }
                        });
                    }

                    return;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.changeLoading(getResources().getString(R.string.user_searching));
                    }
                });

                try {
                    nemopaySession.foundUser(nemopaySession.getRequest().getJSONResponse().get("username").textValue());
                    Thread.sleep(100);
                } catch (final Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());

                    try {
                        if (nemopaySession.getRequest().getResponseCode() == 400)
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.errorDialog(ReadCardInfoActivity.this, getString(R.string.information_collection), getString(R.string.user_not_recognized));
                                }
                            });
                        else
                            throw new Exception("");
                    } catch (Exception e1) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                fatal(ReadCardInfoActivity.this, getString(R.string.information_collection), e.getMessage());
                            }
                        });
                    }

                    return;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.changeLoading(getResources().getString(R.string.user_info_collecting));
                    }
                });

                try {
                    final JsonNode usersFounded = nemopaySession.getRequest().getJSONResponse();

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
                            setUserInfo(userInfo.get("user"), userInfo.get("tag"));
                            dialog.stopLoading();
                        }
                    });
                } catch (final Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());

                    try {
                        if (nemopaySession.getRequest().getResponseCode() == 400)
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.errorDialog(ReadCardInfoActivity.this, getString(R.string.information_collection), getString(R.string.user_error_collecting));
                                }
                            });
                        else
                            throw new Exception("");
                    } catch (Exception e1) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                fatal(ReadCardInfoActivity.this, getString(R.string.information_collection), e.getMessage());
                            }
                        });
                    }

                    return;
                }
            }
        }.start();
    }

    void setUserInfo(JsonNode userInfo, JsonNode tagInfo) {
        try {
            this.textUserId.setText(Integer.toString(userInfo.get("id").intValue()));
            this.textUsername.setText(userInfo.get("username").textValue());
            this.textFirstname.setText(userInfo.get("first_name").textValue());
            this.textLastname.setText(userInfo.get("last_name").textValue());
            this.textEmail.setText(userInfo.get("email").textValue());
            this.textAdult.setText(userInfo.get("adult").booleanValue() ? getString(R.string.yes) :  getString(R.string.no));
            this.textCotisant.setText(userInfo.get("cotisant").booleanValue() ? getString(R.string.yes) :  getString(R.string.no));

            this.textTagId.setText(Integer.toString(tagInfo.get("id").intValue()));
            this.textTag.setText(tagInfo.get("tag").textValue());
            this.textShortTag.setText(tagInfo.get("short_tag").textValue() == null ? getString(R.string.none) : tagInfo.get("short_tag").textValue());
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
}
