package fr.utc.simde.payutc.fragments;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;

import fr.utc.simde.payutc.ArticleCategoryActivity;
import fr.utc.simde.payutc.R;

/**
 * Created by Samy on 27/10/2017.
 */

public class ArticleGroupFragment implements TabHost.TabContentFactory {
    private LayoutInflater articleLayout;

    public ArticleGroupFragment(Activity activity) {
        this.articleLayout = LayoutInflater.from(activity);
    }

    @Override
    public View createTabContent(String tag) {
        View view = this.articleLayout.inflate(R.layout.fragment_article_group, null);

        return view;
    }
}
