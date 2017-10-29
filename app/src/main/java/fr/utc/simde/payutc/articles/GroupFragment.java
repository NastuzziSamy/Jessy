package fr.utc.simde.payutc.articles;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TabHost;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;


import fr.utc.simde.payutc.ArticleCategoryActivity;
import fr.utc.simde.payutc.R;

/**
 * Created by Samy on 27/10/2017.
 */

public class GroupFragment implements TabHost.TabContentFactory {
    private static final String LOG_TAG = "_ArticleGroupFragment";

    private int nbrColumns;

    private LayoutInflater layoutInflater;
    private View view;

    private GridView gridView;

    private ArticlesAdapter articlesAdapter;
    private ArticleCategoryActivity.Panier panier;

    public GroupFragment(final Activity activity, final JsonNode articleList, ArticleCategoryActivity.Panier panier) throws Exception {
        this.layoutInflater = LayoutInflater.from(activity);
        this.view = this.layoutInflater.inflate(R.layout.fragment_article_group_grid, null);
        this.gridView = this.view.findViewById(R.id.grid_articles);

        this.panier = panier;

        setGridLayout(3);
        createArticles(activity, articleList);

        this.gridView.setAdapter(this.articlesAdapter);
    }

    public void setGridLayout(final int nbrColumns) {
        this.nbrColumns = nbrColumns;
        this.gridView.setNumColumns(nbrColumns);
    }

    public void createArticles(final Activity activity, final JsonNode articleList) throws Exception {
        this.articlesAdapter = new GridAdapter(activity, articleList, this.nbrColumns);

        this.gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                JsonNode article = ((JsonNode) articlesAdapter.getArticle(position));
                articlesAdapter.onClick(position);
                panier.addArticle(article.get("id").intValue(), article.get("price").intValue());
            }
        });

        this.gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                articlesAdapter.toast(position, Toast.LENGTH_LONG);

                return true;
            }
        });
    }

    public void clear() {
        articlesAdapter.clear();
    }

    @Override
    public View createTabContent(final String tag) {
        return this.view;
    }
}
