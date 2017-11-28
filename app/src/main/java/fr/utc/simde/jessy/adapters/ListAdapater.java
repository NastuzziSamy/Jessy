package fr.utc.simde.jessy.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.utc.simde.jessy.R;

/**
 * Created by Samy on 29/10/2017.
 */

public class ListAdapater extends ArticlesAdapter {
    private static final String LOG_TAG = "_ListAdapter";

    public ListAdapater(final Activity activity, final ArrayNode articleList, final Boolean printCotisant, final Boolean print18) throws Exception {
        super(activity, articleList, printCotisant, print18);
    }

    @SuppressLint("ResourceAsColor")
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

            if (article.get("variable_price").booleanValue())
                priceText.setText("PV: " + String.format("%.2f", new Float(articleList.get(position).get("price").intValue() * articleList.get(position).get("quantity").intValue()) / 100.00f) + "€");
            else
                priceText.setText((article.has("quantity") ? Integer.toString(article.get("quantity").intValue()) + "x " : "") + String.format("%.2f", new Float(articleList.get(position).get("price").intValue()) / 100.00f) + "€");

            ImageView imageCotisant = this.viewList[position].findViewById(R.id.image_cotisant);
            ImageView image18 = this.viewList[position].findViewById(R.id.image_18);

            TextView infoText = this.viewList[position].findViewById(R.id.text_info);
            if (article.has("info")) {
                infoText.setText(article.get("info").textValue());

                imageCotisant.setVisibility(View.GONE);
                image18.setVisibility(View.GONE);
            }
            else
                setInfos(article, imageCotisant, image18);

            setImage(imageView, article.get("image_url").textValue(), position);

            if (article.has("quantity"))
                nbrList[position] = article.get("quantity").intValue() * (article.get("variable_price").booleanValue() ? 1 : 100);

            if (article.has("canceled") && article.get("canceled").booleanValue()) {
                nameText.setPaintFlags(nameText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                priceText.setPaintFlags(nameText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                infoText.setPaintFlags(nameText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                clickViewList[position].setPaintFlags(nameText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }

            if (this.nbrList[position] < 0)
                priceText.setTextColor(Color.RED);

            setClickView(position);
        }

        return this.viewList[position];
    }
}
