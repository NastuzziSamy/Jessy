package fr.utc.simde.payutc;

import android.os.Bundle;
import android.util.Log;
import android.widget.TabHost;

import fr.utc.simde.payutc.fragments.ArticleGroupFragment;

/**
 * Created by Samy on 27/10/2017.
 */

public class ArticleCategoryActivity extends BaseActivity {
    private static final String LOG_TAG = "_ArticleCategoryActivit";

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView (R.layout.activity_articles_category);

        Log.d(LOG_TAG, "C: " + getIntent().getExtras().getString("categoryList"));
        Log.d(LOG_TAG, "A: " + getIntent().getExtras().getString("articleList"));

        TabHost host = findViewById(R.id.tab_categories);
        host.setup();

        //Tab 1
        TabHost.TabSpec spec = host.newTabSpec("Softs");
        spec.setIndicator("Softs");
        spec.setContent(new ArticleGroupFragment(ArticleCategoryActivity.this));

        host.addTab(spec);

        //Tab 1
        TabHost.TabSpec spec2 = host.newTabSpec("Bières");
        spec2.setIndicator("Bières");
        spec2.setContent(new ArticleGroupFragment(ArticleCategoryActivity.this));

        host.addTab(spec2);
    }

    @Override
    protected void onIdentification(String idBadge) {

    }
}
