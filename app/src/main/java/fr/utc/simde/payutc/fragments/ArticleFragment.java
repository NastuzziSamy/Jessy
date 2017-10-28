package fr.utc.simde.payutc.fragments;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;

import fr.utc.simde.payutc.R;

/**
 * Created by Samy on 28/10/2017.
 */

public class ArticleFragment extends View {
    private static final String LOG_TAG = "_ArticleFragment";

    private int articleId;
    private int price;
    private String name;
    private String url;

    private LayoutInflater layoutInflater;
    private View view;
    private TextView textView;

    private LinearLayout linearLayout;

    public ArticleFragment(Activity activity, final JsonNode article) {
        super(activity);
/*
        this.layoutInflater = LayoutInflater.from(activity);
        this.view = this.layoutInflater.inflate(R.layout.fragment_article, null);
        this.textView = view.findViewById(R.id.text_article);
        this.textView.setText(article.get("name").textValue());*/

        TextView t = new TextView(activity);
        ImageView iv = new ImageView(activity);
        iv.setImageResource(R.mipmap.ic_launcher);
        t.setText(article.get("name").textValue() + ": ");
        TextView t2 = new TextView(activity);
        t2.setText((article.get("price").intValue() / 100) + "€" + ((article.get("price").intValue() % 100) == 0 ? "" : (((article.get("price").intValue() % 100) < 10 ? "0" : "") + (article.get("price").intValue() % 100))));
        this.linearLayout = new LinearLayout(activity);

        linearLayout.addView(iv);
        linearLayout.addView(t);
        linearLayout.addView(t2);
    }

    public View getView() {
        return this.linearLayout;
    }
}
