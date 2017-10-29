package fr.utc.simde.payutc.articles;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import fr.utc.simde.payutc.R;
import fr.utc.simde.payutc.tools.HTTPRequest;

/**
 * Created by Samy on 29/10/2017.
 */

public class ListAdapater extends ArticlesAdapter {
    private static final String LOG_TAG = "_ListAdapter";

    public ListAdapater(final Activity activity, final JsonNode articleList) throws Exception {
        super(activity, articleList);
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        JsonNode article = this.articleList.get(position);

        if (view == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(this.activity);
            view = layoutInflater.inflate(R.layout.fragment_article_list, null);
        }

        ImageView imageView = view.findViewById(R.id.image_article);

        if (clickViewList[position] == null)
            clickViewList[position] = view.findViewById(R.id.text_nbr_clicks);

        TextView nameText = view.findViewById(R.id.text_article);
        nameText.setText(article.get("name").textValue());

        TextView priceText = view.findViewById(R.id.text_price);
        priceText.setText(String.format("%.2f", new Float(articleList.get(position).get("price").intValue()) / 100.00f) + "€");

        if (article.has("info")) {
            TextView infoText = view.findViewById(R.id.text_info);
            infoText.setText(article.get("info").textValue());
        }

        setImage(imageView, article.get("image_url").textValue(), position);
        setClickView(position);

        return view;
    }

    public void setClickView(final int position) {
        if (this.clickViewList[position] != null) {
            if (this.nbrClicksList[position] == 0) {
                this.clickViewList[position].setText("");
                this.clickViewList[position].setAlpha(0.0f);
            } else {
                this.clickViewList[position].setText(Integer.toString(this.nbrClicksList[position]));
                this.clickViewList[position].setAlpha(1.0f);
            }
        }
    }
}
