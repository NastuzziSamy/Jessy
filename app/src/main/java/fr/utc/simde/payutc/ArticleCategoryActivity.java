package fr.utc.simde.payutc;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;

import fr.utc.simde.payutc.fragments.ArticleGroupFragment;

/**
 * Created by Samy on 27/10/2017.
 */

public class ArticleCategoryActivity extends BaseActivity {

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView (R.layout.activity_articles_category);

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
