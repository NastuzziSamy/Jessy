package fr.utc.simde.payutc.articles;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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
 * Created by Samy on 28/10/2017.
 */

public class ArticleAdapter extends BaseAdapter {
    private static final String LOG_TAG = "_ArticleAdapter";

    private Activity activity;
    private JsonNode articleList;
    private Bitmap[] imageList;
    private Integer[] nbrClicksList;
    private TextView[] clickViewList;
    private int size;

    public ArticleAdapter(final Activity activity, final JsonNode articleList, final int nbrColumns) throws Exception {
        this.activity = activity;
        this.articleList = articleList;
        this.imageList = new Bitmap[articleList.size()];
        this.nbrClicksList = new Integer[articleList.size()];
        this.clickViewList = new TextView[articleList.size()];

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
    public View getView(int position, View view, ViewGroup viewGroup) {
        JsonNode article = this.articleList.get(position);

        if (view == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(this.activity);
            view = layoutInflater.inflate(R.layout.fragment_article, null);
        }

        ImageView imageView = view.findViewById(R.id.image_article);

        if (clickViewList[position] == null)
            clickViewList[position] = view.findViewById(R.id.text_nbr_clicks);

        TextView textView = view.findViewById(R.id.text_article);

        int imageSize = this.size;
        RelativeLayout.LayoutParams parms = new RelativeLayout.LayoutParams(imageSize, imageSize);
        imageView.setLayoutParams(parms);
        setImage(imageView, article.get("image_url").textValue(), position);

        setClickView(position);

        textView.setText(article.get("name").textValue());

        return view;
    }

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

    public void onClick(final int position) {
        this.nbrClicksList[position]++;

        setClickView(position);
        toast(position, Toast.LENGTH_SHORT);
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
