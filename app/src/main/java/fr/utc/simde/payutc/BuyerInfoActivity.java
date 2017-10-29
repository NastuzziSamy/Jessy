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

            generatePurchases(buyerInfo.get("last_purchases"));
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

    protected void generatePurchases(final JsonNode purchaseList) throws Exception {
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
            ArrayNode articleList = new ObjectMapper().createArrayNode();
            Log.d(LOG_TAG, purchaseList.toString());
            for (JsonNode purchase : purchaseList) {
                articleList.add(new ObjectMapper().readTree("{\"name\":\"" + Integer.toString(purchase.get("obj_id").intValue()) + "\", " +
                        "\"price\":" + Integer.toString(purchase.get("pur_price").intValue()) + ", " +
                        "\"image_url\":\"\"}"));
            }

            Log.d(LOG_TAG, articleList.toString());

            this.listAdapater = new ListAdapater(BuyerInfoActivity.this, articleList);
            this.listView = new ListView(this);
            this.listView.setAdapter(this.listAdapater);

            this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick (AdapterView parent, View view,int position, long id){
                    JsonNode article = ((JsonNode) listAdapater.getArticle(position));
                            /*
                            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(BuyerInfoActivity.this);
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

                            dialog.createDialog(alertDialogBuilder, usernameInput.getText().toString().isEmpty() ? usernameInput : passwordInput);*/
                }
            });

            this.listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()

            {
                @Override
                public boolean onItemLongClick (AdapterView < ? > adapterView, View view,
                                                int position, long id){
                    listAdapater.toast(position, Toast.LENGTH_LONG);

                    return true;
                }
            });

            this.linearLayout.addView(listView);
        }
    }
}
