package fr.utc.simde.payutc;

import android.app.Activity;
import android.content.DialogInterface;
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

import org.w3c.dom.Text;

import java.io.IOException;

import fr.utc.simde.payutc.articles.GridAdapter;
import fr.utc.simde.payutc.articles.ListAdapater;
import fr.utc.simde.payutc.tools.HTTPRequest;

/**
 * Created by Samy on 29/10/2017.
 */

public class BuyerInfoActivity extends BaseActivity {
    private static final String LOG_TAG = "_BuyerInfoActivity";

    private String lastname;
    private String username;
    private String firstname;
    private JsonNode lastPurchases;

    private TextView textBuyerName;
    private TextView textSolde;
    private LinearLayout linearLayout;
    private ListView listView;

    private ListAdapater listAdapater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_info);

        this.textBuyerName = findViewById(R.id.text_buyer_name);
        this.textSolde = findViewById(R.id.text_solde);
        this.linearLayout = findViewById(R.id.layout_articles);

        try {
            JsonNode buyerInfo = new ObjectMapper().readTree(getIntent().getExtras().getString("buyerInfo"));

            if (!buyerInfo.has("lastname") || !buyerInfo.has("username") || !buyerInfo.has("firstname") || !buyerInfo.has("solde") || !buyerInfo.has("last_purchases") || !buyerInfo.get("last_purchases").isArray())
                throw new Exception("Unexpected JSON");

            this.textBuyerName.setText(buyerInfo.get("firstname").textValue() + " " + buyerInfo.get("lastname").textValue());
            this.textSolde.setText("Solde: " + String.format("%.2f", new Float(buyerInfo.get("solde").intValue()) / 100.00f) + "€");

            generatePurchases((ArrayNode) buyerInfo.get("last_purchases"));
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

    @Override
    protected void onIdentification(final String badgeId) {

    }

    protected void generatePurchases(final ArrayNode purchaseList) throws Exception {
        if (purchaseList.size() == 0) {
            String foundationName = nemopaySession.getFoundationName();
            TextView noPurchase = new TextView(this);
            noPurchase.setText(getString(R.string.no_purchases) + (foundationName.equals("") ? "" : "\n(" + foundationName + ")"));
            noPurchase.setTextSize(24);
            noPurchase.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,  LinearLayout.LayoutParams.MATCH_PARENT);
            layoutParams.setMargins(100, 100, 100, 100);
            noPurchase.setLayoutParams(layoutParams);

            this.linearLayout.addView(noPurchase);
        }
        else {
            generateArticleList(purchaseList);
            this.listView = new ListView(this);

            this.linearLayout.addView(listView);
        }
    }

    public void generateArticleList(final ArrayNode purchaseList) {
        dialog.startLoading(this, getString(R.string.information_collection), getString(R.string.article_list_collecting));

        new Thread() {
            @Override
            public void run() {
                ArrayNode articleFoundationList = new ObjectMapper().createArrayNode();
                if (nemopaySession.getFoundationId() != -1) {
                    try {
                        int responseCode = nemopaySession.getArticles();
                        Thread.sleep(100);

                        // Toute une série de vérifications avant de lancer l'activité
                        final HTTPRequest request = nemopaySession.getRequest();
                        articleFoundationList = (ArrayNode) request.getJSONResponse();

                        for (final JsonNode article : articleFoundationList) {
                            if (!article.has("id") || !article.has("price") || !article.has("name") || !article.has("active") || !article.has("cotisant") || !article.has("alcool") || !article.has("categorie_id") || !article.has("image_url") || !article.has("fundation_id") || article.get("fundation_id").intValue() != nemopaySession.getFoundationId())
                                throw new Exception("Unexpected JSON");
                        }
                    } catch (final Exception e) {
                        Log.e(LOG_TAG, "error: " + e.getMessage());

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.stopLoading();
                                dialog.errorDialog(BuyerInfoActivity.this, getString(R.string.article_list_collecting), e.getMessage());
                            }
                        });
                    }
                }

                final ArrayNode articleList = new ObjectMapper().createArrayNode();
                Boolean hasRight = true; //
                for (JsonNode purchase : purchaseList) {
                    int articleId = purchase.get("obj_id").intValue();

                    Boolean isIn = false;
                    for (JsonNode article : articleFoundationList) {
                        if (article.get("id").intValue() == articleId) {
                            ((ObjectNode) article).put("info", getString(R.string.realized) + " " + purchase.get("pur_date").textValue().substring(purchase.get("pur_date").textValue().length() - 8));
                            articleList.add(article);

                            isIn = true;
                            break;
                        }
                    }

                    if (!isIn) {
                        try {
                            articleList.add(new ObjectMapper().readTree("{" +
                                "\"name\":\"" + "N°: " + Integer.toString(purchase.get("obj_id").intValue()) + "\", " +
                                "\"price\":" + Integer.toString(purchase.get("pur_price").intValue()) + ", " +
                                "\"info\":\"Non annulable\", " +
                                "\"image_url\":\"\"}"
                            ));
                        }
                        catch (Exception e) {
                            Log.e(LOG_TAG, "error: " + e.getMessage());

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.stopLoading();
                                    dialog.errorDialog(BuyerInfoActivity.this, getResources().getString(R.string.information_collection), getResources().getString(R.string.error_view), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int id) {
                                            finish();
                                        }
                                    });
                                }
                            });
                        }
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            listAdapater = new ListAdapater(BuyerInfoActivity.this, articleList);
                            listView.setAdapter(listAdapater);
                            dialog.stopLoading();
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "error: " + e.getMessage());

                            dialog.errorDialog(BuyerInfoActivity.this, getResources().getString(R.string.information_collection), getResources().getString(R.string.error_view), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int id) {
                                    finish();
                                }
                            });
                        }
                    }
                });

            }
        }.start();
    }
}
