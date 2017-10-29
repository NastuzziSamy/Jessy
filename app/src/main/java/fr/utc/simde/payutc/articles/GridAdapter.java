package fr.utc.simde.payutc.articles;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;

import fr.utc.simde.payutc.R;

/**
 * Created by Samy on 29/10/2017.
 */

public class GridAdapter extends ArticlesAdapter {
    private static final String LOG_TAG = "_GridAdapter";

    private int size;

    public GridAdapter(final Activity activity, final JsonNode articleList, final int nbrColumns) throws Exception {
        super(activity, articleList);

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
            LayoutInflater layoutInflater = LayoutInflater.from(this.activity);
            view = layoutInflater.inflate(R.layout.fragment_article_grid, null);
        }

        ImageView imageView = view.findViewById(R.id.image_article);

        if (clickViewList[position] == null)
            clickViewList[position] = view.findViewById(R.id.text_nbr_clicks);

        TextView textView = view.findViewById(R.id.text_article);
        textView.setText(article.get("name").textValue());

        int imageSize = this.size;
        RelativeLayout.LayoutParams parms = new RelativeLayout.LayoutParams(imageSize, imageSize);
        imageView.setLayoutParams(parms);

        setImage(imageView, article.get("image_url").textValue(), position);
        setClickView(position);

        return view;
    }
}
