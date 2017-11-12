package fr.utc.simde.jessy.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import fr.utc.simde.jessy.tools.HTTPRequest;

/**
 * Created by Samy on 28/10/2017.
 */

public abstract class ArticlesAdapter extends BaseAdapter {
    private static final String LOG_TAG = "_ArticlesAdapter";

    protected Activity activity;
    protected Boolean printCotisant;
    protected Boolean print18;

    protected Bitmap[] imageList;
    protected Integer[] nbrClicksList;

    protected View[] viewList;
    protected TextView[] clickViewList;

    protected ArrayNode articleList;

    public ArticlesAdapter(final Activity activity, final ArrayNode articleList, final Boolean printCotisant, final Boolean print18) throws Exception {
        this.activity = activity;
        this.printCotisant = printCotisant;
        this.print18 = print18;

        this.articleList = articleList;
        this.imageList = new Bitmap[articleList.size()];
        this.nbrClicksList = new Integer[articleList.size()];

        this.viewList = new View[articleList.size()];
        this.clickViewList = new TextView[articleList.size()];

        for (int i = 0; i < this.nbrClicksList.length; i++)
            this.nbrClicksList[i] = 0;
    }

    @Override
    public int getCount() {
        return this.articleList.size();
    }

    @Override
    public Object getItem(int position) {
        return getArticle(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) { return view; }

    public void setClickView(final int position) {
        if (this.clickViewList[position] != null) {
            if (this.nbrClicksList[position] == 0) {
                this.clickViewList[position].setText("");
                this.clickViewList[position].setAlpha(0.0f);
            }
            else {
                this.clickViewList[position].setText(Integer.toString(this.nbrClicksList[position]));
                this.clickViewList[position].setAlpha(1.0f);
            }
        }
    }

    public void toast(final int position, int lengthLong) {
        Toast.makeText(this.activity, articleList.get(position).get("name").textValue() + ": " + String.format("%.2f", new Float(articleList.get(position).get("price").intValue()) / 100.00f) + "€", lengthLong).show();
    }

    public void setInfos(JsonNode article, ImageView imageCotisant, ImageView image18) {
        if (this.printCotisant) {
            if (article.get("cotisant").booleanValue())
                imageCotisant.setAlpha(1.0f);
            else
                imageCotisant.setAlpha(0.3f);
        }
        else
            imageCotisant.setVisibility(View.GONE);

        if (this.print18) {
            if (article.get("alcool").booleanValue())
                image18.setAlpha(1.0f);
            else
                image18.setAlpha(0.3f);
        }
        else
            image18.setVisibility(View.GONE);
    }

    public void onClick(final int position) {
        this.nbrClicksList[position]++;

        setClickView(position);
    }

    public void clear() {
        for (int i = 0; i < getCount(); i++) {
            this.nbrClicksList[i] = 0;
            setClickView(i);
        }
    }

    public JsonNode getArticle(final int position) {
        return this.articleList.get(position);
    }

    public void setImage(final ImageView imageView, final String url, final int position) {
        final HTTPRequest[] request = new HTTPRequest[1];

        if (imageList[position] != null)
            imageView.setImageBitmap(imageList[position]);
        else if (url != null && !url.equals("")) {
            new Thread(){
                @Override
                public void run() {
                    request[0] = new HTTPRequest(url);

                    if (request[0].get() == 200) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    imageList[position] = request[0].getImageResponse();
                                    imageView.setImageBitmap(imageList[position]);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }
            }.start();
            //new DownloadImageTask(imageView, imageList[position]).execute(url);
        }
    }

    public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        private ImageView imageView;
        private Bitmap image;

        public DownloadImageTask(ImageView imageView, Bitmap image) {
            this.imageView = imageView;
            this.image = image;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            try {
                URL url = new URL(urldisplay);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                image = BitmapFactory.decodeStream(input);
            } catch (Exception e) {
                image = null;
            }
            return image;
        }

        @SuppressLint("NewApi")
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                imageView.setImageBitmap(result);
            }
        }
    }
}
