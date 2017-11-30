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
import com.fasterxml.jackson.databind.node.ArrayNode;

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
                            ((Vibrator) getSystemService(ArticleGroupActivity.VIBRATOR_SERVICE)).vibrate(250);
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
                                    ((Vibrator) getSystemService(ArticleGroupActivity.VIBRATOR_SERVICE)).vibrate(500);
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
                                ((Vibrator) getSystemService(ArticleGroupActivity.VIBRATOR_SERVICE)).vibrate(500);
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
                clearPanier();
            }
        });
    }

    @Override
    protected void createNewGroup(final String name, final ArrayNode articleList) throws Exception { createNewGroup(name, articleList, 3); }
    protected void createNewGroup(final String name, final ArrayNode articleList, int gridColumns) throws Exception {
        ArticleGroupFragment articleGroupFragment = new SellFragment(SellActivity.this, this.dialog, articleList, this.panier, this.config, gridColumns);

        TabHost.TabSpec newTabSpec = this.tabHost.newTabSpec(name);
        newTabSpec.setIndicator(name);
        newTabSpec.setContent(articleGroupFragment);

        this.groupFragmentList.add(articleGroupFragment);

        this.tabHost.addTab(newTabSpec);
        nbrGroups++;
    }
}
