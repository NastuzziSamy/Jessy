package fr.utc.simde.jessy.fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Switch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.utc.simde.jessy.BaseActivity;
import fr.utc.simde.jessy.EditActivity;
import fr.utc.simde.jessy.R;
import fr.utc.simde.jessy.SellActivity;
import fr.utc.simde.jessy.tools.Config;
import fr.utc.simde.jessy.tools.Dialog;

/**
 * Created by Samy on 19/11/2017.
 */

public class EditFragment extends ArticleGroupFragment {
    private static final String LOG_TAG = "_EditFragment";

    protected Dialog dialog;

    public EditFragment(final Activity activity, final ArrayNode articleList, final Dialog dialog, final Config config) throws Exception {
        super(activity, articleList, config);

        this.dialog = dialog;
    }
    public EditFragment(final Activity activity, final ArrayNode articleList, final Dialog dialog, final Config config, final int gridColumns) throws Exception {
        super(activity, articleList, config, gridColumns);

        this.dialog = dialog;
    }

    @Override
    protected void setOnArticleClick(View view) {
        AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                ((EditActivity) activity).setArticle(articlesAdapter, position);
            }
        };

        if (this.config.getInGrid())
            ((GridView) view).setOnItemClickListener(onItemClickListener);
        else
            ((ListView) view).setOnItemClickListener(onItemClickListener);
    }
}
