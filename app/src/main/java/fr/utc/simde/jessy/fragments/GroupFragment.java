package fr.utc.simde.jessy.fragments;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;


import fr.utc.simde.jessy.R;
import fr.utc.simde.jessy.adapters.ArticlesAdapter;
import fr.utc.simde.jessy.adapters.GridAdapter;
import fr.utc.simde.jessy.adapters.ListAdapater;
import fr.utc.simde.jessy.tools.Config;
import fr.utc.simde.jessy.tools.Panier;

/**
 * Created by Samy on 27/10/2017.
 */

public class GroupFragment implements TabHost.TabContentFactory {
    private static final String LOG_TAG = "_ArticleGroupFragment";

    private int nbrColumns;

    private LayoutInflater layoutInflater;
    private View view;

    private GridView gridView;
    private ListView listView;

    private ArticlesAdapter articlesAdapter;
    private Panier panier;
    private Config config;

    public GroupFragment(final Activity activity, final ArrayNode articleList, final Panier panier, final Config config) throws Exception { new GroupFragment(activity, articleList, panier, config, 3); }
    public GroupFragment(final Activity activity, final ArrayNode articleList, final Panier panier, final Config config, final int gridColumns) throws Exception {
        this.layoutInflater = LayoutInflater.from(activity);
        this.panier = panier;
        this.config = config;

        if (config.getInGrid()) {
            this.view = this.layoutInflater.inflate(R.layout.fragment_article_group_grid, null);
            this.gridView = this.view.findViewById(R.id.grid_articles);
            setGridLayout(gridColumns);
            createArticleGrid(activity, articleList);
        }
        else {
            this.view = this.layoutInflater.inflate(R.layout.fragment_article_group_list, null);
            this.listView = this.view.findViewById(R.id.list_articles);
            createArticleList(activity, articleList);
        }

    }

    public void setGridLayout(final int nbrColumns) {
        this.nbrColumns = nbrColumns;
        this.gridView.setNumColumns(nbrColumns);
    }

    public void createArticleGrid(final Activity activity, final ArrayNode articleList) throws Exception {
        this.articlesAdapter = new GridAdapter(activity, articleList, this.nbrColumns, this.config.getPrintCotisant(), this.config.getPrint18());

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

        this.gridView.setAdapter(this.articlesAdapter);
    }

    public void createArticleList(final Activity activity, final ArrayNode articleList) throws Exception {
        this.articlesAdapter = new ListAdapater(activity, articleList, this.config.getPrintCotisant(), this.config.getPrint18());

        this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                JsonNode article = ((JsonNode) articlesAdapter.getArticle(position));
                articlesAdapter.onClick(position);
                panier.addArticle(article.get("id").intValue(), article.get("price").intValue());
            }
        });

        this.listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                articlesAdapter.toast(position, Toast.LENGTH_LONG);

                return true;
            }
        });

        this.listView.setAdapter(this.articlesAdapter);
    }

    public void clear() {
        articlesAdapter.clear();
    }

    @Override
    public View createTabContent(final String tag) {
        return this.view;
    }
}
