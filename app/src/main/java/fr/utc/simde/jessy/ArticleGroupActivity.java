package fr.utc.simde.jessy;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import fr.utc.simde.jessy.adapters.LocationsAdapter;
import fr.utc.simde.jessy.fragments.GroupFragment;
import fr.utc.simde.jessy.tools.HTTPRequest;
import fr.utc.simde.jessy.tools.Panier;

/**
 * Created by Samy on 27/10/2017.
 */

public class ArticleGroupActivity extends BaseActivity {
    private static final String LOG_TAG = "_ArticleGroupActivity";

    protected ImageButton paramButton;
    protected ImageButton deleteButton;
    protected TabHost tabHost;

    protected Panier panier;

    protected List<GroupFragment> groupFragmentList;
    protected int nbrGroups;

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_group);

        TextView textView = findViewById(R.id.text_price);
        this.panier = new Panier(textView);
        this.paramButton = findViewById(R.id.image_param);
        this.deleteButton = findViewById(R.id.image_delete);
        this.tabHost = findViewById(R.id.tab_categories);
        this.tabHost.setup();

        this.groupFragmentList = new ArrayList<GroupFragment>();
        this.nbrGroups = 0;

        try {
            if (getIntent().getExtras().getString("categoryList") != null)
                createCategories(new ObjectMapper().readTree(getIntent().getExtras().getString("categoryList")), getIntent().getExtras().getIntegerArrayList("categoryListAuthorized"), new ObjectMapper().readTree(getIntent().getExtras().getString("articleList")));

            if (getIntent().getExtras().getString("keyboardList") != null)
                createKeyboards(new ObjectMapper().readTree(getIntent().getExtras().getString("keyboardList")), getIntent().getExtras().getIntegerArrayList("keyboardListAuthorized"), new ObjectMapper().readTree(getIntent().getExtras().getString("articleList")));
        } catch (Exception e) {
            Log.e(LOG_TAG, "error: " + e.getMessage());
            dialog.errorDialog(this, getResources().getString(R.string.information_collection), getResources().getString(R.string.error_view), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int id) {
                    finish();
                }
            });
        }

        if (this.nbrGroups == 0) {
            if (config.getLocationId() != -1) {
                config.setLocation(-1, "");
                config.setCanCancel(true);
            }

            dialog.errorDialog(this, getResources().getString(R.string.information_collection), nemopaySession.getFoundationName() + " " + getString(R.string.article_error_0), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int id) {
                    finish();
                }
            });
        }

        this.paramButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (config.getFoundationId() == -1) {
                    final View popupView = LayoutInflater.from(ArticleGroupActivity.this).inflate(R.layout.dialog_config, null, false);
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
                            hasRights(getString(R.string.configurate_by_default), new String[]{
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

                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ArticleGroupActivity.this);
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

                                    startArticleGroupActivity(ArticleGroupActivity.this);
                                }
                            })
                            .setNegativeButton(R.string.cancel, null);

                    dialog.createDialog(alertDialogBuilder);
                }
                else {
                    final View popupView = LayoutInflater.from(ArticleGroupActivity.this).inflate(R.layout.dialog_config_restore, null, false);
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

                                    startMainActivity(ArticleGroupActivity.this);
                                }
                            });
                        }
                    });

                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ArticleGroupActivity.this);
                    alertDialogBuilder
                            .setTitle(R.string.configuration)
                            .setView(popupView)
                            .setCancelable(false)
                            .setPositiveButton(R.string.reload, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialogInterface, int id) {
                                    config.setPrintCotisant(switchCotisant.isChecked());
                                    config.setPrint18(swtich18.isChecked());

                                    startArticleGroupActivity(ArticleGroupActivity.this);
                                }
                            })
                            .setNegativeButton(R.string.cancel, null);

                    dialog.createDialog(alertDialogBuilder);
                }
            }
        });

        this.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearPanier();
            }
        });
    }

    @Override
    protected void onIdentification(final String badgeId) {
        if (dialog.isShowing())
            return;

        if (this.panier.isEmpty())
            startBuyerInfoActivity(ArticleGroupActivity.this, badgeId);
        else
            pay(badgeId);
    }

    protected void configDialog() {
        dialog.startLoading(ArticleGroupActivity.this, getResources().getString(R.string.information_collection), getString(config.getInCategory() ? R.string.category_list_collecting : R.string.keyboard_list_collecting));

        new Thread() {
            @Override
            public void run() {
                try {
                    nemopaySession.getLocations();
                    Thread.sleep(100);

                    final HTTPRequest request = nemopaySession.getRequest();
                    final JsonNode locationList = request.getJSONResponse();

                    if (!locationList.isArray())
                        throw new Exception("Malformed JSON");

                    if (locationList == null || locationList.size() == 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.stopLoading();

                                dialog.errorDialog(ArticleGroupActivity.this, getString(R.string.information_collection), nemopaySession.getFoundationName() + " " + getString(R.string.location_error_0));
                            }
                        });

                        return;
                    }

                    for (final JsonNode location : locationList) {
                        if (!location.has("id") || !location.has("name") || !location.has("enabled") || !location.has("categories") || !location.has("sales_keyboards"))
                            throw new Exception("Unexpected JSON");
                    }
                } catch (final Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fatal(ArticleGroupActivity.this, getString(config.getInCategory() ? R.string.category_list_collecting : R.string.keyboard_list_collecting), e.getMessage());
                        }
                    });
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.stopLoading();

                        final LayoutInflater layoutInflater = LayoutInflater.from(ArticleGroupActivity.this);
                        final View popupView = layoutInflater.inflate(R.layout.dialog_group, null);
                        final ListView listView = popupView.findViewById(R.id.list_groups);
                        final Switch canCancelSwitch = popupView.findViewById(R.id.switch_cancel);
                        canCancelSwitch.setChecked(config.getCanCancel());

                        ArrayNode locationList = new ObjectMapper().createArrayNode();
                        final LocationsAdapter locationAdapter;
                        try {
                            for (JsonNode location : nemopaySession.getRequest().getJSONResponse()) {
                                if (location.get("enabled").booleanValue())
                                    locationList.add(location);
                            }

                            locationAdapter = new LocationsAdapter(ArticleGroupActivity.this, locationList);

                            listView.setOnItemClickListener(new ListView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    config.setCanCancel(canCancelSwitch.isChecked());
                                    config.setFoundation(nemopaySession.getFoundationId(), nemopaySession.getFoundationName());
                                    config.setLocation(locationAdapter.getLocationId(position), locationAdapter.getLocationName(position));
                                    startMainActivity(ArticleGroupActivity.this);
                                }
                            });

                            listView.setAdapter(locationAdapter);
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "error: " + e.getMessage());

                            fatal(ArticleGroupActivity.this, getString(config.getInCategory() ? R.string.category_list_collecting : R.string.keyboard_list_collecting), e.getMessage());
                        }

                        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ArticleGroupActivity.this);
                        alertDialogBuilder
                                .setTitle(R.string.configuration)
                                .setView(popupView)
                                .setCancelable(false)
                                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialogInterface, int id) {
                                        config.setCanCancel(true);
                                    }
                                });

                        dialog.createDialog(alertDialogBuilder);
                    }
                });
            }
        }.start();
    }

    public void clearPanier() {
        for (GroupFragment groupFragment : groupFragmentList)
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
        dialog.startLoading(this, getResources().getString(R.string.paiement), getResources().getString(R.string.transaction_in_progress));

        new Thread() {
            @Override
            public void run() {
                try {
                    nemopaySession.setTransaction(badgeId, panier.getArticleList());
                    Thread.sleep(100);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.stopLoading();
                            Toast.makeText(ArticleGroupActivity.this, "Paiement effectué", Toast.LENGTH_LONG).show();
                            setBackgroundColor(getResources().getColor(R.color.success));
                            ((Vibrator) getSystemService(ArticleGroupActivity.VIBRATOR_SERVICE)).vibrate(250);
                            clearPanier();
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
                                    dialog.errorDialog(ArticleGroupActivity.this, getString(R.string.paiement), response.get("error").get("message").textValue());
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
                                dialog.errorDialog(ArticleGroupActivity.this, getString(R.string.paiement), e.getMessage());
                                setBackgroundColor(getResources().getColor(R.color.error));
                                ((Vibrator) getSystemService(ArticleGroupActivity.VIBRATOR_SERVICE)).vibrate(500);
                            }
                        });
                    }
                }
            }
        }.start();
    }


    protected void createCategories(final JsonNode categoryList, final List<Integer> authorizedList, final JsonNode articleList) throws Exception {
        HashMap<Integer, ArrayNode> articlesPerCategory = new HashMap<Integer, ArrayNode>();
        final int foundationId = nemopaySession.getFoundationId();

        for (final JsonNode article : articleList) {
            if (!article.has("id") || !article.has("price") || !article.has("name") || !article.has("active") || !article.has("cotisant") || !article.has("alcool") || !article.has("categorie_id") || !article.has("image_url") || !article.has("fundation_id") || article.get("fundation_id").intValue() != foundationId)
                throw new Exception("Unexpected JSON");

            if (!article.get("active").booleanValue())
                continue;

            if (articlesPerCategory.containsKey(article.get("categorie_id").intValue()))
                articlesPerCategory.get(article.get("categorie_id").intValue()).add(article);
            else
                articlesPerCategory.put(article.get("categorie_id").intValue(), new ObjectMapper().createArrayNode().add(article));
        }

        for (JsonNode category : categoryList) {
            if (!category.has("id") || !category.has("name") || !category.has("fundation_id") || category.get("fundation_id").intValue() != foundationId)
                throw new Exception("Unexpected JSON");

            ArrayNode articlesForThisCategory = articlesPerCategory.get(category.get("id").intValue());
            if (config.getFoundationId() != -1) if (!authorizedList.contains(category.get("id").intValue()))
                continue;
            else if (articlesForThisCategory.size() == 0)
                continue;

            if (articlesForThisCategory == null)
                continue;

            createNewGroup(category.get("name").textValue(), articlesForThisCategory);
        }
    }

    protected void createKeyboards(final JsonNode keyboardList, final List<Integer> authorizedList, final JsonNode articleList) throws Exception {
        final int foundationId = nemopaySession.getFoundationId();

        for (final JsonNode article : articleList) {
            if (!article.has("id") || !article.has("price") || !article.has("name") || !article.has("active") || !article.has("cotisant") || !article.has("alcool") || !article.has("categorie_id") || !article.has("image_url") || !article.has("fundation_id") || article.get("fundation_id").intValue() != foundationId)
                throw new Exception("Unexpected JSON");
        }

        for (JsonNode keyboard : keyboardList) {
            ArrayNode articlesForThisKeyboard = new ObjectMapper().createArrayNode();

            if (config.getFoundationId() != -1) if (!authorizedList.contains(keyboard.get("id").intValue()))
                continue;

            if (!keyboard.has("id") || !keyboard.has("name") || !keyboard.has("fun_id") || keyboard.get("fun_id").intValue() != foundationId || !keyboard.has("data") || !keyboard.get("data").has("items") || !keyboard.get("data").get("items").isArray() || !keyboard.get("data").has("nbColumns"))
                throw new Exception("Unexpected JSON");

            for (JsonNode article : keyboard.get("data").get("items")) {
                if (article.has("itm_id")) {
                    boolean in = false;
                    for (JsonNode articleInList : articleList) {
                        if (articleInList.get("id").intValue() == article.get("itm_id").intValue()) {
                            articlesForThisKeyboard.add(articleInList);
                            in = true;
                            break;
                        }
                    }

                    if (!in && config.getInGrid())
                        articlesForThisKeyboard.add(new ObjectMapper().createObjectNode());
                }
                else if (config.getInGrid())
                    articlesForThisKeyboard.add(new ObjectMapper().createObjectNode());
            }

            if (articlesForThisKeyboard.size() == 0)
                continue;

            createNewGroup(keyboard.get("name").textValue(), articlesForThisKeyboard, keyboard.get("data").get("nbColumns").isInt() ? keyboard.get("data").get("nbColumns").intValue() : Integer.valueOf(keyboard.get("data").get("nbColumns").textValue()));
        }
    }

    protected void createNewGroup(final String name, final ArrayNode articleList) throws Exception { createNewGroup(name, articleList, 3); }
    protected void createNewGroup(final String name, final ArrayNode articleList, int gridColumns) throws Exception {
        GroupFragment articleGroupFragment = new GroupFragment(ArticleGroupActivity.this, articleList, this.panier, this.config, gridColumns);

        TabHost.TabSpec newTabSpec = this.tabHost.newTabSpec(name);
        newTabSpec.setIndicator(name);
        newTabSpec.setContent(articleGroupFragment);

        this.groupFragmentList.add(articleGroupFragment);

        this.tabHost.addTab(newTabSpec);
        nbrGroups++;
    }
}
