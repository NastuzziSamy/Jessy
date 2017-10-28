package fr.utc.simde.payutc.fragments;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;


import fr.utc.simde.payutc.R;

/**
 * Created by Samy on 27/10/2017.
 */

public class ArticleGroupFragment implements TabHost.TabContentFactory {
    private static final String LOG_TAG = "_ArticleGroupFragment";

    private LayoutInflater layoutInflater;
    private View view;
    private GridLayout gridLayout;

    private int nbrColumns;

    public ArticleGroupFragment(final Activity activity, final JsonNode articleList) throws Exception {
        this.layoutInflater = LayoutInflater.from(activity);
        this.view = this.layoutInflater.inflate(R.layout.fragment_article_group, null);
        this.gridLayout = this.view.findViewById(R.id.grid_articles);
        setGridLayout(3);

        createGroups(activity, articleList);
    }

    public void setGridLayout(final int nbrColumns) {
        this.nbrColumns = nbrColumns;
        this.gridLayout.setColumnCount(nbrColumns);
    }

    public void createGroups(final Activity activity, final JsonNode articleList) throws Exception {
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        for (JsonNode article : articleList) {
            if (!article.has("id") || !article.has("price") || !article.has("name") || !article.has("active") || !article.has("cotisant") || !article.has("alcool") || !article.has("categorie_id") || !article.has("image_url") || !article.has("fundation_id"))
                throw new Exception("Unexpected JSON");

            if (!article.get("active").booleanValue())
                continue;

            this.gridLayout.addView(new ArticleFragment(activity, article).getView());
        }

    }

    @Override
    public View createTabContent(final String tag) {
        return this.view;
    }
}
