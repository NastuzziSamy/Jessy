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

public abstract class ArticleGroupFragment implements TabHost.TabContentFactory {
    private static final String LOG_TAG = "_GroupFragment";

    protected Activity activity;

    protected int nbrColumns;

    protected LayoutInflater layoutInflater;
    protected View view;
    protected GridView gridView;
    protected ListView listView;

    protected ArticlesAdapter articlesAdapter;
    protected Config config;

    public ArticleGroupFragment(final Activity activity, final ArrayNode articleList, final Config config) throws Exception {
        this.activity = activity;
        this.layoutInflater = LayoutInflater.from(activity);
        this.config = config;

        if (config.getInGrid()) {
            this.view = this.layoutInflater.inflate(R.layout.fragment_article_group_grid, null);
            this.gridView = this.view.findViewById(R.id.grid_articles);
            createArticleGrid(activity, articleList);
        }
        else {
            this.view = this.layoutInflater.inflate(R.layout.fragment_article_group_list, null);
            this.listView = this.view.findViewById(R.id.list_articles);
            createArticleList(activity, articleList);
        }
    }
    public ArticleGroupFragment(final Activity activity, final ArrayNode articleList, final Config config, final int gridColumns) throws Exception {
        this.activity = activity;
        this.layoutInflater = LayoutInflater.from(activity);
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

    protected void setGridLayout(final int nbrColumns) {
        this.nbrColumns = nbrColumns;
        this.gridView.setNumColumns(nbrColumns);
    }

    protected abstract void setOnArticleClick(View view);

    protected void setOnArticleLongClick(View view) {
        ((GridView) view).setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                articlesAdapter.toast(position, Toast.LENGTH_LONG);

                return true;
            }
        });
    }

    protected void createArticleGrid(final Activity activity, final ArrayNode articleList) throws Exception {
        this.articlesAdapter = new GridAdapter(activity, articleList, this.nbrColumns, this.config.getPrintCotisant(), this.config.getPrint18());

        setOnArticleClick(this.gridView);
        setOnArticleLongClick(this.gridView);

        this.gridView.setAdapter(this.articlesAdapter);
    }

    protected void createArticleList(final Activity activity, final ArrayNode articleList) throws Exception {
        this.articlesAdapter = new ListAdapater(activity, articleList, this.config.getPrintCotisant(), this.config.getPrint18());

        setOnArticleClick(this.listView);
        setOnArticleLongClick(this.listView);

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
