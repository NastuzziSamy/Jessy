package fr.utc.simde.jessy.fragments;

import android.app.Activity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.utc.simde.jessy.tools.Config;
import fr.utc.simde.jessy.tools.Panier;

/**
 * Created by Samy on 19/11/2017.
 */

public class SellFragment extends ArticleGroupFragment {
    private static final String LOG_TAG = "_SellFragment";

    protected Panier panier;

    public SellFragment(final Activity activity, final ArrayNode articleList, final Panier panier, final Config config) throws Exception {
        super(activity, articleList, config);

        this.panier = panier;
    }
    public SellFragment(final Activity activity, final ArrayNode articleList, final Panier panier, final Config config, final int gridColumns) throws Exception {
        super(activity, articleList, config, gridColumns);

        this.panier = panier;
    }

    @Override
    protected void setOnArticleClick(View view) {
        ((GridView) view).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                JsonNode article = articlesAdapter.getArticle(position);
                articlesAdapter.onClick(position);
                panier.addArticle(article.get("id").intValue(), article.get("price").intValue());
            }
        });
    }
}
