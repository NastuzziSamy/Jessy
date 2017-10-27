package fr.utc.simde.payutc.fragments;

import android.app.Activity;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;

import fr.utc.simde.payutc.ArticleCategoryActivity;

/**
 * Created by Samy on 27/10/2017.
 */

public class ArticleGroupFragment implements TabHost.TabContentFactory {
    private TextView nameText;

    public ArticleGroupFragment(Activity activity) {
        nameText = new TextView(activity);
    }

    @Override
    public View createTabContent(String tag) {
        nameText.setText(tag);
        return nameText;
    }
}
