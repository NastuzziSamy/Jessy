package fr.utc.simde.jessy.fragments;

import android.app.Activity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.utc.simde.jessy.tools.Config;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.utc.simde.jessy.MainActivity;
import fr.utc.simde.jessy.R;
import fr.utc.simde.jessy.tools.Config;
import fr.utc.simde.jessy.tools.Dialog;
import fr.utc.simde.jessy.tools.Panier;

/**
 * Created by Samy on 19/11/2017.
 */

public class SellFragment extends ArticleGroupFragment {
    private static final String LOG_TAG = "_SellFragment";

    protected Panier panier;
    protected Dialog dialog;

    public SellFragment(final Activity activity, final Dialog dialog, final ArrayNode articleList, final Panier panier, final Config config) throws Exception {
        super(activity, articleList, config);

        this.dialog = dialog;
        this.panier = panier;
    }
    public SellFragment(final Activity activity, final Dialog dialog, final ArrayNode articleList, final Panier panier, final Config config, final int gridColumns) throws Exception {
        super(activity, articleList, config, gridColumns);

        this.dialog = dialog;
        this.panier = panier;
    }

    @Override
    protected void setOnArticleClick(View view) {
        AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                JsonNode article = articlesAdapter.getArticle(position);

                if (article.get("variable_price").booleanValue()) {
                    priceDialog(position);
                } else {
                    articlesAdapter.onClick(position, 100);
                    panier.addArticle(article.get("id").intValue(), article.get("price").intValue());
                }
            }
        };

        if (this.config.getInGrid())
            ((GridView) view).setOnItemClickListener(onItemClickListener);
        else
            ((ListView) view).setOnItemClickListener(onItemClickListener);
    }

    protected void priceDialog(final Integer position) {
        final View priceView = activity.getLayoutInflater().inflate(R.layout.dialog_price, null);
        final EditText priceInput = priceView.findViewById(R.id.input_price);

        InputFilter twoDecimalsOnly = new InputFilter() {
            final int maxDigitsBeforeDecimalPoint = 3;
            final int maxDigitsAfterDecimalPoint = 2;

            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                StringBuilder builder = new StringBuilder(dest);
                builder.replace(dstart, dend, source
                        .subSequence(start, end).toString());
                if (!builder.toString().matches("((([1-9]{1})([0-9]{0,"+(maxDigitsBeforeDecimalPoint-1)+"})?)|0)?(\\.[0-9]{0,"+maxDigitsAfterDecimalPoint+"})?")) {
                    if (source.length() == 0)
                        return dest.subSequence(dstart, dend);
                    return "";
                }

                return null;
            }
        };
        priceInput.setFilters(new InputFilter[] { twoDecimalsOnly });

        if (articlesAdapter.getNbr(position) != 0)
            priceInput.setText(Float.toString(articlesAdapter.getNbr(position) / 100.0f));

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
        alertDialogBuilder
                .setTitle(R.string.price_variable)
                .setView(priceView)
                .setCancelable(false)
                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int id) {
                        if (priceInput.getText().toString().equals("")) {
                            Toast.makeText(activity, R.string.price_required, Toast.LENGTH_SHORT).show();

                            priceDialog(position);
                        }
                        else {
                            JsonNode article = articlesAdapter.getArticle(position);

                            panier.addArticle(article.get("id").intValue(), article.get("price").intValue(), Math.round(Float.parseFloat(priceInput.getText().toString()) * 100.0f) * article.get("price").intValue() - articlesAdapter.getNbr(position));
                            articlesAdapter.onClick(position, Math.round(Float.parseFloat(priceInput.getText().toString()) * 100.0f)  - articlesAdapter.getNbr(position));
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null);

        dialog.createDialog(alertDialogBuilder, priceInput);
    }
}
