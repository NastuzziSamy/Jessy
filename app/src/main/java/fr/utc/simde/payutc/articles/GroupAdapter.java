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
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import fr.utc.simde.payutc.ArticleCategoryActivity;
import fr.utc.simde.payutc.R;
import fr.utc.simde.payutc.tools.HTTPRequest;

/**
 * Created by Samy on 29/10/2017.
 */

public class GroupAdapter extends BaseAdapter {
    private static final String LOG_TAG = "_GroupAdapter";

    protected Activity activity;

    protected JsonNode groupList;

    protected CheckBox[] checkBoxList;

    public GroupAdapter(final Activity activity, final JsonNode groupList) throws Exception {
        this.activity = activity;
        this.groupList = groupList;
        this.checkBoxList = new CheckBox[groupList.size()];
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        JsonNode group = this.groupList.get(position);

        if (view == null) {
            view = new LinearLayout(this.activity);
            CheckBox checkBox = new CheckBox(this.activity);
            checkBox.setText(group.get("name").textValue());

            ((LinearLayout) view).addView(checkBox);

            if (this.checkBoxList[position] == null)
                this.checkBoxList[position] = checkBox;
        }

        return view;
    }

    @Override
    public int getCount() { return this.groupList.size(); }

    @Override
    public Object getItem(int position) { return 0; }

    @Override
    public long getItemId(int position) { return position; }

    public JsonNode getList() {
        ObjectNode groupList = new ObjectMapper().createObjectNode();

        Integer i = 0;
        for (CheckBox checkBox : this.checkBoxList) {
            if (checkBox.isChecked())
                groupList.put(Integer.toString(this.groupList.get(i).get("id").intValue()), this.groupList.get(i).get("name").textValue());

            i++;
        }

        return groupList;
    }
}
