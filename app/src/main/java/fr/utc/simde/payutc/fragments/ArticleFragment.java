package fr.utc.simde.payutc.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import fr.utc.simde.payutc.R;
import fr.utc.simde.payutc.tools.HTTPRequest;

import static android.content.ContentValues.TAG;

/**
 * Created by Samy on 28/10/2017.
 */

public class ArticleFragment extends View {
    private static final String LOG_TAG = "_ArticleFragment";

    private int id;
    private int price;
    private String name;
    private String imageUrl;

    private LayoutInflater layoutInflater;
    private View view;
    private ImageView imageView;
    private TextView textView;

    private HTTPRequest request;

    public ArticleFragment(final Activity activity, final JsonNode article) {
        super(activity);

        this.id = article.get("id").intValue();
        this.price = article.get("price").intValue();
        this.name = article.get("name").textValue();
        this.imageUrl = article.get("image_url").textValue();

        this.layoutInflater = LayoutInflater.from(activity);
        this.view = this.layoutInflater.inflate(R.layout.fragment_article, null);
        this.imageView = view.findViewById(R.id.image_article);
        this.textView = view.findViewById(R.id.text_article);

        RelativeLayout.LayoutParams parms = new RelativeLayout.LayoutParams(200,200);
        imageView.setLayoutParams(parms);

        setView(activity);
    }

    public void setView(final Activity activity) {/*
     this.textView.setText(article.get("name").textValue())
        TextView t = new TextView(activity);
        iv = new ImageView(activity);
        iv.setTag("image_article_" + Integer.toString(this.id));
        iv.setImageResource(R.mipmap.ic_launcher);
        LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(200,200);
        iv.setLayoutParams(parms);

        t.setText(this.name + ": ");
        TextView t2 = new TextView(activity);
        t2.setText((this.price / 100) + "€" + ((this.price % 100) == 0 ? "" : (((this.price % 100) < 10 ? "0" : "") + (this.price % 100))));
        this.linearLayout = new LinearLayout(activity);

        linearLayout.addView(iv);
        linearLayout.addView(t);
        linearLayout.addView(t2);
*/

        if (this.imageUrl != null && !this.imageUrl.equals("")) {
            new Thread(){
                @Override
                public void run() {
                    request = new HTTPRequest(imageUrl);

                    if (request.get() == 200) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    imageView.setImageBitmap(request.getImageResponse());

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }
            }.start();
            //new DownloadImageTask(iv).execute(this.imageUrl);
        }

        this.textView.setText(this.name);
    }
    public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        private ImageView imageView;
        private Bitmap image;

        public DownloadImageTask(ImageView imageView) {
            this.imageView = imageView;
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

    public View getView() {
        return this.view;
    }
}
