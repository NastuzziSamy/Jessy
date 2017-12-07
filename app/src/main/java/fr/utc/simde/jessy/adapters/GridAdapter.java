package fr.utc.simde.jessy.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.utc.simde.jessy.R;

/**
 * Created by Samy on 29/10/2017.
 */

public class GridAdapter extends ArticlesAdapter {
    private static final String LOG_TAG = "_GridAdapter";

    private int size;

    public GridAdapter(final Activity activity, final ArrayNode articleList, final int nbrColumns, final Boolean printCotisant, final Boolean print18) throws Exception {
        super(activity, articleList, printCotisant, print18);

        this.size = nbrColumns > 5 ? 100 : 225 - (25 * nbrColumns);
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        JsonNode article = this.articleList.get(position);

        if (this.viewList[position] == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(this.activity);
            this.viewList[position] = layoutInflater.inflate(R.layout.fragment_article_grid, null);

            ImageView imageView = this.viewList[position].findViewById(R.id.image_article);
            RelativeLayout.LayoutParams parms = new RelativeLayout.LayoutParams(this.size, this.size);
            imageView.setLayoutParams(parms);

            if (article == null || article.size() == 0)
                this.viewList[position].setVisibility(View.INVISIBLE);
            else {
                this.clickViewList[position] = this.viewList[position].findViewById(R.id.text_nbr_clicks);

                TextView textView = this.viewList[position].findViewById(R.id.text_article);
                textView.setText(article.get("name").textValue());

                ImageView imageCotisant = this.viewList[position].findViewById(R.id.image_cotisant);
                ImageView image18 = this.viewList[position].findViewById(R.id.image_18);

                LinearLayout.LayoutParams imageParms = new LinearLayout.LayoutParams(45, 45);
                imageParms.setMargins(0, this.size - 45, 0, 0);
                imageCotisant.setLayoutParams(imageParms);
                image18.setLayoutParams(imageParms);

                if (article.get("id").intValue() == -1) {
                    imageCotisant.setVisibility(View.GONE);
                    image18.setVisibility(View.GONE);
                }
                else
                    setInfos(article, imageCotisant, image18);

                setImage(imageView, article.get("image_url").textValue(), position);
                setClickView(position);
            }
        }

        return this.viewList[position];
    }
}
