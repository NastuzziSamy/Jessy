package fr.utc.simde.payutc.articles;

import android.app.Activity;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.utc.simde.payutc.R;

/**
 * Created by Samy on 29/10/2017.
 */

public class GridAdapter extends ArticlesAdapter {
    private static final String LOG_TAG = "_GridAdapter";

    private int size;

    public GridAdapter(final Activity activity, final ArrayNode articleList, final int nbrColumns, final Boolean printCotisant, final Boolean print18) throws Exception {
        super(activity, articleList, printCotisant, print18);

        switch (nbrColumns) {
            case 1:
                this.size = 250;
                break;
            case 2:
                this.size = 200;
                break;
            case 3:
                this.size = 150;
                break;
            case 4:
                this.size = 125;
                break;
            case 5:
            default:
                this.size = 100;
                break;
        }
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        JsonNode article = this.articleList.get(position);

        if (view == null) {
            if (article.size() == 0)
                view = new View(this.activity);
            else {
                LayoutInflater layoutInflater = LayoutInflater.from(this.activity);
                view = layoutInflater.inflate(R.layout.fragment_article_grid, null);

                ImageView imageView = view.findViewById(R.id.image_article);

                if (clickViewList[position] == null)
                    clickViewList[position] = view.findViewById(R.id.text_nbr_clicks);

                TextView textView = view.findViewById(R.id.text_article);
                textView.setText(article.get("name").textValue());

                RelativeLayout.LayoutParams parms = new RelativeLayout.LayoutParams(this.size, this.size);
                imageView.setLayoutParams(parms);

                ImageView imageCotisant = view.findViewById(R.id.image_cotisant);
                ImageView image18 = view.findViewById(R.id.image_18);

                LinearLayout.LayoutParams imageParms = new LinearLayout.LayoutParams(45, 45);
                imageParms.setMargins(0, this.size - 45, 0, 0);
                imageCotisant.setLayoutParams(imageParms);
                image18.setLayoutParams(imageParms);

                setInfos(article, imageCotisant, image18);
                setImage(imageView, article.get("image_url").textValue(), position);
                setClickView(position);
            }
        }

        return view;
    }
}
