package fr.utc.simde.payutc;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TabHost;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import fr.utc.simde.payutc.articles.ArticleGroupFragment;

/**
 * Created by Samy on 27/10/2017.
 */

public class ArticleCategoryActivity extends BaseActivity {
    private static final String LOG_TAG = "_ArticleCategoryActivit";

    private ImageButton paramButton;
    private ImageButton deleteButton;
    private TabHost tabHost;

    private Panier panier;

    private List<ArticleGroupFragment> articleGroupFragmentList;
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
            if (this.totalPrice == 0)
                this.textView.setText("Panier vide");
            else
                this.textView.setText("Total: " + String.format("%.2f", new Float(totalPrice) / 100.00f) + "€");
        }

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

        this.articleGroupFragmentList = new ArrayList<ArticleGroupFragment>();
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

        this.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (ArticleGroupFragment articleGroupFragment : articleGroupFragmentList)
                    articleGroupFragment.clear();

                panier.clear();
            }
        });
    }

    @Override
    protected void onIdentification(String idBadge) {}

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

            createNewCategory(category.get("name").textValue(), new ObjectMapper().readTree(articlesForThisCategory.toString()));
        }
    }

    protected void createNewCategory(final String name, final JsonNode articleList) throws Exception {
        ArticleGroupFragment articleGroupFragment = new ArticleGroupFragment(ArticleCategoryActivity.this, articleList, this.panier);

        TabHost.TabSpec newTabSpec = this.tabHost.newTabSpec(name);
        newTabSpec.setIndicator(name);
        newTabSpec.setContent(articleGroupFragment);

        this.articleGroupFragmentList.add(articleGroupFragment);

        this.tabHost.addTab(newTabSpec);
        nbrCategories++;
    }
}
