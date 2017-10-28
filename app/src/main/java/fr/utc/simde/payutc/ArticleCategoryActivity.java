package fr.utc.simde.payutc;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.TabHost;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.utc.simde.payutc.fragments.ArticleGroupFragment;

/**
 * Created by Samy on 27/10/2017.
 */

public class ArticleCategoryActivity extends BaseActivity {
    private static final String LOG_TAG = "_ArticleCategoryActivit";

    private TabHost categoryTabList;

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView (R.layout.activity_articles_category);

        Log.d(LOG_TAG, "C: " + getIntent().getExtras().getString("categoryList"));
        Log.d(LOG_TAG, "A: " + getIntent().getExtras().getString("articleList"));

        this.categoryTabList = findViewById(R.id.tab_categories);
        this.categoryTabList.setup();

        try {
            createCategories(new ObjectMapper().readTree(getIntent().getExtras().getString("categoryList")));
        } catch (Exception e) {
            Log.wtf(LOG_TAG, "error: " + e.getMessage());
            dialog.errorDialog(this, getResources().getString(R.string.information_collection), getResources().getString(R.string.error_unexpected), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int id) {
                    finish();
                }
            });
        }
    }

    @Override
    protected void onIdentification(String idBadge) {}

    protected void createCategories(final JsonNode categoryList) throws Exception {
        for (JsonNode category : categoryList) {
            if (!category.has("id") || !category.has("name") || !category.has("fundation_id") || category.get("fundation_id").intValue() != nemopaySession.getFoundationId())
                throw new Exception("Unexpected JSON");

            createNewCategory(category.get("name").textValue());
        }
    }

    protected void createNewCategory(final String name) {
        TabHost.TabSpec newTabSpec = this.categoryTabList.newTabSpec(name);
        newTabSpec.setIndicator(name);
        newTabSpec.setContent(new ArticleGroupFragment(ArticleCategoryActivity.this));

        this.categoryTabList.addTab(newTabSpec);
    }
}
