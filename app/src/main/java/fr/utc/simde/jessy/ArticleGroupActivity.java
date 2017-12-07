package fr.utc.simde.jessy;

import android.content.DialogInterface;
import android.os.Bundle;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import fr.utc.simde.jessy.adapters.LocationsAdapter;
import fr.utc.simde.jessy.fragments.ArticleGroupFragment;
import fr.utc.simde.jessy.tools.HTTPRequest;

/**
 * Created by Samy on 27/10/2017.
 */

public abstract class ArticleGroupActivity extends BaseActivity {
    private static final String LOG_TAG = "_ArticleGroupActivity";

    protected ImageButton optionButton;
    protected ImageButton deleteButton;
    protected TabHost tabHost;

    protected List<ArticleGroupFragment> groupFragmentList;
    protected int nbrGroups;

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_group);

        this.optionButton = findViewById(R.id.image_param);
        this.deleteButton = findViewById(R.id.image_delete);
        this.tabHost = findViewById(R.id.tab_categories);
        this.tabHost.setup();

        this.groupFragmentList = new ArrayList<ArticleGroupFragment>();
        this.nbrGroups = 0;
    }

    protected void generate() {
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

        setOptionButton();
        setDeleteButton();
    }

    protected abstract void setOptionButton();
    protected abstract void setDeleteButton();

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

            if (articlesForThisCategory == null)
                articlesForThisCategory = new ObjectMapper().createArrayNode();

            createNewGroup(category.get("name").textValue(), category.get("id").intValue(), articlesForThisCategory);
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
                if (article.has("itm_id") && article.has("name")) {
                    boolean in = false;
                    for (JsonNode articleInList : articleList) {
                        if (articleInList.get("id").intValue() == article.get("itm_id").intValue()) {
                            JsonNode articleToAdd = new ObjectMapper().readTree(articleInList.toString());

                            if (!article.get("name").textValue().isEmpty())
                                ((ObjectNode) articleToAdd).put("name", article.get("name").textValue());

                            articlesForThisKeyboard.add(articleToAdd);
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

            createNewGroup(keyboard.get("name").textValue(), keyboard.get("id").intValue(), articlesForThisKeyboard, keyboard.get("data").get("nbColumns").isInt() ? keyboard.get("data").get("nbColumns").intValue() : Integer.valueOf(keyboard.get("data").get("nbColumns").textValue()));
        }
    }

    protected abstract void createNewGroup(final String name, final Integer id, final ArrayNode articleList) throws Exception;
    protected abstract void createNewGroup(final String name, final Integer id, final ArrayNode articleList, int gridColumns) throws Exception;
}
