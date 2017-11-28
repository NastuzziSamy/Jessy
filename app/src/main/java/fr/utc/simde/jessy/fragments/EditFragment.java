package fr.utc.simde.jessy.fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Switch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.utc.simde.jessy.BaseActivity;
import fr.utc.simde.jessy.R;
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
                final JsonNode article = articlesAdapter.getArticle(position);
                final View popupView = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_article, null, false);
                final EditText textName = popupView.findViewById(R.id.text_name);
                final EditText textPrice = popupView.findViewById(R.id.text_price);
                final Switch switchCotisant = popupView.findViewById(R.id.swtich_cotisant);
                final Switch swtich18 = popupView.findViewById(R.id.swtich_18);

                textName.setText(article.get("name").textValue());
                textPrice.setText(Float.toString(article.get("price").intValue() / 100.0f));
                switchCotisant.setChecked(article.get("cotisant").booleanValue());
                swtich18.setChecked(article.get("alcool").booleanValue());

                final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
                alertDialogBuilder
                        .setTitle(R.string.configuration)
                        .setView(popupView)
                        .setCancelable(false)
                        .setPositiveButton(R.string.reload, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int id) {
                                config.setPrintCotisant(switchCotisant.isChecked());
                                config.setPrint18(swtich18.isChecked());

                                ((BaseActivity) activity).startSellActivity(activity);
                            }
                        })
                        .setNegativeButton(R.string.cancel, null);

                dialog.createDialog(alertDialogBuilder);
            }
        };

        if (this.config.getInGrid())
            ((GridView) view).setOnItemClickListener(onItemClickListener);
        else
            ((ListView) view).setOnItemClickListener(onItemClickListener);
    }
}
