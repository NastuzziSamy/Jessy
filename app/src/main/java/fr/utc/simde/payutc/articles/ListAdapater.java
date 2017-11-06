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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

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

    public ListAdapater(final Activity activity, final ArrayNode articleList, final Boolean printCotisant, final Boolean print18) throws Exception {
        super(activity, articleList, printCotisant, print18);
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        JsonNode article = this.articleList.get(position);

        if (this.viewList[position] == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(this.activity);
            this.viewList[position] = layoutInflater.inflate(R.layout.fragment_article_list, null);

            ImageView imageView = this.viewList[position].findViewById(R.id.image_article);

            clickViewList[position] = this.viewList[position].findViewById(R.id.text_nbr_clicks);

            TextView nameText = this.viewList[position].findViewById(R.id.text_article);
            nameText.setText(article.get("name").textValue());

            TextView priceText = this.viewList[position].findViewById(R.id.text_price);
            priceText.setText((article.has("quantity") ? Integer.toString(article.get("quantity").intValue()) + "x " : "") + String.format("%.2f", new Float(articleList.get(position).get("price").intValue()) / 100.00f) + "€");

            ImageView imageCotisant = this.viewList[position].findViewById(R.id.image_cotisant);
            ImageView image18 = this.viewList[position].findViewById(R.id.image_18);

            if (article.has("info")) {
                TextView infoText = this.viewList[position].findViewById(R.id.text_info);
                infoText.setText(article.get("info").textValue());

                imageCotisant.setVisibility(View.GONE);
                image18.setVisibility(View.GONE);
            }
            else
                setInfos(article, imageCotisant, image18);

            setImage(imageView, article.get("image_url").textValue(), position);

            if (article.has("quantity")) {
                nbrClicksList[position] = article.get("quantity").intValue();
            }

            setClickView(position);
        }

        return this.viewList[position];
    }

    public void toast(final int position, int lengthLong) {
        Toast.makeText(this.activity, (articleList.get(position).has("quantity") ? Integer.toString(articleList.get(position).get("quantity").intValue()) + "x " : "") + articleList.get(position).get("name").textValue() + ": " + String.format("%.2f", new Float((articleList.get(position).has("quantity") ? articleList.get(position).get("quantity").intValue() : 1) * articleList.get(position).get("price").intValue()) / 100.00f) + "€", lengthLong).show();
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
