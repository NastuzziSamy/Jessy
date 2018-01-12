package fr.utc.simde.jessy;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

import fr.utc.simde.jessy.fragments.ArticleGroupFragment;
import fr.utc.simde.jessy.fragments.SellFragment;
import fr.utc.simde.jessy.tools.Panier;

/**
 * Created by Samy on 20/11/2017.
 */

public class SellActivity extends ArticleGroupActivity {
    private static final String LOG_TAG = "_SellActivity";

    protected Panier panier;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView panierText = findViewById(R.id.text_price);
        this.panier = new Panier(panierText);

        panierText.setOnLongClickListener(new TextView.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startQRCodeReaderActivity(SellActivity.this);

                return false;
            }
        });

        generate();
    }

    @Override
    protected void onIdentification(final String badgeId) {
        if (dialog.isShowing())
            return;

        if (this.panier.isEmpty())
            startBuyerInfoActivity(SellActivity.this, badgeId);
        else
            pay(badgeId);
    }

    public void clearPanier() {
        for (ArticleGroupFragment groupFragment : groupFragmentList)
            groupFragment.clear();

        panier.clear();
    }

    public void setBackgroundColor(int color) {
        this.tabHost.setBackgroundColor(color);

        new Thread(){
            @Override
            public void run() {
                try {
                    Thread.sleep(2500);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tabHost.setBackgroundColor(getResources().getColor(R.color.white));
                        }
                    });
                } catch (Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());
                }

            }
        }.start();
    }

    protected void pay(final String badgeId) {
        dialog.startLoading(this, getString(R.string.paiement), getString(R.string.transaction_in_progress));

        final List<List<Integer>> articleList = new ArrayList<List<Integer>>(panier.getArticleList());
        clearPanier();

        new Thread() {
            @Override
            public void run() {
                try {
                    nemopaySession.setTransaction(badgeId, articleList);
                    Thread.sleep(100);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.stopLoading();
                            Toast.makeText(SellActivity.this, getString(R.string.ticket_realized), Toast.LENGTH_LONG).show();
                            setBackgroundColor(getResources().getColor(R.color.success));
                            ((Vibrator) getSystemService(SellActivity.VIBRATOR_SERVICE)).vibrate(250);
                        }
                    });
                } catch (final Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());

                    try {
                        final JsonNode response = nemopaySession.getRequest().getJSONResponse();

                        if (response.has("error") && response.get("error").has("message")) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.stopLoading();
                                    dialog.errorDialog(SellActivity.this, getString(R.string.paiement), response.get("error").get("message").textValue());
                                    setBackgroundColor(getResources().getColor(R.color.error));
                                    ((Vibrator) getSystemService(SellActivity.VIBRATOR_SERVICE)).vibrate(500);
                                }
                            });
                        }
                        else
                            throw new Exception("");
                    } catch (Exception e1) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.stopLoading();
                                dialog.errorDialog(SellActivity.this, getString(R.string.paiement), e.getMessage());
                                setBackgroundColor(getResources().getColor(R.color.error));
                                ((Vibrator) getSystemService(SellActivity.VIBRATOR_SERVICE)).vibrate(500);
                            }
                        });
                    }
                }
            }
        }.start();
    }

    @Override
    protected void setOptionButton() {
        this.optionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (config.getFoundationId() == -1) {
                    final View popupView = LayoutInflater.from(SellActivity.this).inflate(R.layout.dialog_config, null, false);
                    final RadioButton radioKeyboard = popupView.findViewById(R.id.radio_keyboard);
                    final RadioButton radioCategory = popupView.findViewById(R.id.radio_category);
                    final RadioButton radioGrid = popupView.findViewById(R.id.radio_grid);
                    final RadioButton radioList = popupView.findViewById(R.id.radio_list);
                    final Switch switchCotisant = popupView.findViewById(R.id.swtich_cotisant);
                    final Switch swtich18 = popupView.findViewById(R.id.swtich_18);
                    final Button configButton = popupView.findViewById(R.id.button_config);

                    if (config.getInCategory())
                        radioCategory.setChecked(true);
                    else
                        radioKeyboard.setChecked(true);

                    if (config.getInGrid())
                        radioGrid.setChecked(true);
                    else
                        radioList.setChecked(true);

                    switchCotisant.setChecked(config.getPrintCotisant());
                    swtich18.setChecked(config.getPrint18());

                    configButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(final View view) {
                            hasRights(getString(R.string.configurate), new String[]{
                                    "STAFF",
                                    "GESAPPLICATIONS"
                            }, new Runnable() {
                                @Override
                                public void run() {
                                    configDialog();
                                }
                            });
                        }
                    });

                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(SellActivity.this);
                    alertDialogBuilder
                            .setTitle(R.string.configuration)
                            .setView(popupView)
                            .setCancelable(false)
                            .setPositiveButton(R.string.reload, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialogInterface, int id) {
                                    config.setInCategory(radioCategory.isChecked());
                                    config.setInGrid(radioGrid.isChecked());
                                    config.setPrintCotisant(switchCotisant.isChecked());
                                    config.setPrint18(swtich18.isChecked());

                                    startSellActivity(SellActivity.this);
                                }
                            })
                            .setNegativeButton(R.string.cancel, null);

                    dialog.createDialog(alertDialogBuilder);
                }
                else {
                    final View popupView = LayoutInflater.from(SellActivity.this).inflate(R.layout.dialog_config_restore, null, false);
                    final Switch switchCotisant = popupView.findViewById(R.id.swtich_cotisant);
                    final Switch swtich18 = popupView.findViewById(R.id.swtich_18);
                    final Button configButton = popupView.findViewById(R.id.button_config);

                    switchCotisant.setChecked(config.getPrintCotisant());
                    swtich18.setChecked(config.getPrint18());

                    configButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            hasRights(getString(R.string.configurate_by_default), new String[]{
                                    "STAFF",
                                    "GESAPPLICATIONS"
                            }, new Runnable() {
                                @Override
                                public void run() {
                                    config.setFoundation(-1, "");
                                    config.setLocation(-1, "");
                                    config.setCanCancel(true);

                                    startMainActivity(SellActivity.this);
                                }
                            });
                        }
                    });

                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(SellActivity.this);
                    alertDialogBuilder
                            .setTitle(R.string.configuration)
                            .setView(popupView)
                            .setCancelable(false)
                            .setPositiveButton(R.string.reload, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialogInterface, int id) {
                                    config.setPrintCotisant(switchCotisant.isChecked());
                                    config.setPrint18(swtich18.isChecked());

                                    startSellActivity(SellActivity.this);
                                }
                            })
                            .setNegativeButton(R.string.cancel, null);

                    dialog.createDialog(alertDialogBuilder);
                }
            }
        });
    }

    @Override
    protected void setDeleteButton() {
        this.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearPanier();
                }
        });
    }

    @Override
    protected void createNewGroup(final String name, final Integer id, final ArrayNode articleList) throws Exception { createNewGroup(name, id, articleList, 3); }
    protected void createNewGroup(final String name, final Integer id, final ArrayNode articleList, int gridColumns) throws Exception {
        ArticleGroupFragment articleGroupFragment = new SellFragment(SellActivity.this, this.dialog, articleList, this.panier, this.config, gridColumns);

        TabHost.TabSpec newTabSpec = this.tabHost.newTabSpec(name);
        newTabSpec.setIndicator(name);
        newTabSpec.setContent(articleGroupFragment);

        this.groupFragmentList.add(articleGroupFragment);

        this.tabHost.addTab(newTabSpec);
        nbrGroups++;
    }
}
