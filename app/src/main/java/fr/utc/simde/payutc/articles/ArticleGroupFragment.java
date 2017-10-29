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

public class ArticleGroupFragment implements TabHost.TabContentFactory {
    private static final String LOG_TAG = "_ArticleGroupFragment";

    private LayoutInflater layoutInflater;
    private View view;
    private GridView gridView;
    private ArticleAdapter articleAdapter;

    private int nbrColumns;

    private ArticleCategoryActivity.Panier panier;

    public ArticleGroupFragment(final Activity activity, final JsonNode articleList, ArticleCategoryActivity.Panier panier) throws Exception {
        this.layoutInflater = LayoutInflater.from(activity);
        this.view = this.layoutInflater.inflate(R.layout.fragment_article_group, null);
        this.gridView = this.view.findViewById(R.id.grid_articles);

        this.panier = panier;

        setGridLayout(3);
        createArticles(activity, articleList);

        gridView.setAdapter(this.articleAdapter);
    }

    public void setGridLayout(final int nbrColumns) {
        this.nbrColumns = nbrColumns;
        gridView.setNumColumns(nbrColumns);
    }

    public void createArticles(final Activity activity, final JsonNode articleList) throws Exception {
        this.articleAdapter = new ArticleAdapter(activity, articleList, this.nbrColumns);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                JsonNode article = ((JsonNode) articleAdapter.getArticle(position));
                articleAdapter.onClick(position);
                panier.addArticle(article.get("id").intValue(), article.get("price").intValue());
            }
        });

        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                JsonNode article = ((JsonNode) articleAdapter.getArticle(position));
                Toast.makeText(activity, article.get("name").textValue() + ": " + String.format("%.2f", new Float(article.get("price").intValue()) / 100.00f) + "€", Toast.LENGTH_LONG).show();

                return false;
            }
        });
    }

    public void clear() {
        articleAdapter.clear();
    }

    @Override
    public View createTabContent(final String tag) {
        return this.view;
    }
}
