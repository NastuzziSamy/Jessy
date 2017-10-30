package fr.utc.simde.payutc;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import fr.utc.simde.payutc.articles.GroupFragment;
import fr.utc.simde.payutc.tools.HTTPRequest;

/**
 * Created by Samy on 27/10/2017.
 */

public class ArticleCategoryActivity extends BaseActivity {
    private static final String LOG_TAG = "_ArticleCategoryActivit";

    private ImageButton paramButton;
    private ImageButton deleteButton;
    private TabHost tabHost;

    private Panier panier;

    private List<GroupFragment> groupFragmentList;
    private int nbrCategories;

    public class Panier {
        private int totalPrice;
        private List<Integer> articleList = new ArrayList<Integer>();

        private TextView textView;

        public Panier(TextView textView) {
            this.totalPrice = 0;
            this.textView = textView;

            setText();
        }

        public void setText() {
            if (this.articleList.size() == 0)
                this.textView.setText("Panier vide");
            else
                this.textView.setText("Total: " + String.format("%.2f", new Float(totalPrice) / 100.00f) + "€");
        }

        public List<Integer> getArticleList() { return this.articleList; }

        public void addArticle(final int id, final int price) {
            this.articleList.add(id);
            this.totalPrice += price;

            setText();
        }

        public void clear() {
            this.articleList.clear();

            this.totalPrice = 0;
            setText();
        }

        public Boolean isEmpty() {
            return this.articleList.isEmpty();
        }
    }

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_category);

        TextView textView = findViewById(R.id.text_price);
        this.panier = new Panier(textView);
        this.paramButton = findViewById(R.id.image_param);
        this.deleteButton = findViewById(R.id.image_delete);
        this.tabHost = findViewById(R.id.tab_categories);
        this.tabHost.setup();

        this.groupFragmentList = new ArrayList<GroupFragment>();
        this.nbrCategories = 0;

        try {
            createCategories(new ObjectMapper().readTree(getIntent().getExtras().getString("categoryList")), new ObjectMapper().readTree(getIntent().getExtras().getString("articleList")));
        } catch (Exception e) {
            Log.e(LOG_TAG, "error: " + e.getMessage());
            dialog.errorDialog(this, getResources().getString(R.string.information_collection), getResources().getString(R.string.error_view), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int id) {
                    finish();
                }
            });
        }

        if (nbrCategories == 0) {
            dialog.errorDialog(this, getResources().getString(R.string.information_collection), getResources().getString(R.string.article_error_0_categorie_not_0), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int id) {
                    finish();
                }
            });
        }

        this.paramButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                config.setInGrid(!config.getInGrid());
                startCategoryArticlesActivity(ArticleCategoryActivity.this);
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
        if (this.panier.isEmpty())
            startBuyerInfoActivity(ArticleCategoryActivity.this, badgeId);
        else
            pay(badgeId);
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
                            Toast.makeText(ArticleCategoryActivity.this, "Paiement effectué", Toast.LENGTH_LONG).show();
                            setBackgroundColor(getResources().getColor(R.color.success));
                            ((Vibrator) getSystemService(ArticleCategoryActivity.VIBRATOR_SERVICE)).vibrate(250);
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
                                    dialog.errorDialog(ArticleCategoryActivity.this, getString(R.string.paiement), response.get("error").get("message").textValue());
                                    setBackgroundColor(getResources().getColor(R.color.error));
                                    ((Vibrator) getSystemService(ArticleCategoryActivity.VIBRATOR_SERVICE)).vibrate(500);
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
                                dialog.errorDialog(ArticleCategoryActivity.this, getString(R.string.paiement), e.getMessage());
                                setBackgroundColor(getResources().getColor(R.color.error));
                                ((Vibrator) getSystemService(ArticleCategoryActivity.VIBRATOR_SERVICE)).vibrate(500);
                            }
                        });
                    }
                }
            }
        }.start();
    }

    protected void createCategories(final JsonNode categoryList, final JsonNode articleList) throws Exception {
        HashMap<Integer, ArrayList<JsonNode>> articlesPerCategory = new HashMap<Integer, ArrayList<JsonNode>>();
        final int foundationId = nemopaySession.getFoundationId();

        for (final JsonNode article : articleList) {
            if (!article.has("id") || !article.has("price") || !article.has("name") || !article.has("active") || !article.has("cotisant") || !article.has("alcool") || !article.has("categorie_id") || !article.has("image_url") || !article.has("fundation_id") || article.get("fundation_id").intValue() != foundationId)
                throw new Exception("Unexpected JSON");

            if (!article.get("active").booleanValue())
                continue;

            if (articlesPerCategory.containsKey(article.get("categorie_id").intValue()))
                articlesPerCategory.get(article.get("categorie_id").intValue()).add(article);
            else
                articlesPerCategory.put(article.get("categorie_id").intValue(), new ArrayList<JsonNode>(){{ add(article); }});
        }

        for (JsonNode category : categoryList) {
            if (!category.has("id") || !category.has("name") || !category.has("fundation_id") || category.get("fundation_id").intValue() != foundationId)
                throw new Exception("Unexpected JSON");

            ArrayList<JsonNode> articlesForThisCategory = articlesPerCategory.get(category.get("id").intValue());
            if (articlesForThisCategory == null || articlesForThisCategory.size() == 0)
                continue;

            createNewCategory(category.get("name").textValue(), (ArrayNode) new ObjectMapper().readTree(articlesForThisCategory.toString()));
        }
    }

    protected void createNewCategory(final String name, final ArrayNode articleList) throws Exception {
        GroupFragment articleGroupFragment = new GroupFragment(ArticleCategoryActivity.this, articleList, this.panier, this.config.getInGrid());

        TabHost.TabSpec newTabSpec = this.tabHost.newTabSpec(name);
        newTabSpec.setIndicator(name);
        newTabSpec.setContent(articleGroupFragment);

        this.groupFragmentList.add(articleGroupFragment);

        this.tabHost.addTab(newTabSpec);
        nbrCategories++;
    }
}
