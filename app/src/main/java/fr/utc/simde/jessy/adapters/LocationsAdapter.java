package fr.utc.simde.jessy.adapters;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created by Samy on 29/10/2017.
 */

public class LocationsAdapter extends BaseAdapter {
    private static final String LOG_TAG = "_LocationsAdapter";

    protected Activity activity;

    protected JsonNode locationList;

    public LocationsAdapter(final Activity activity, final JsonNode locationList) throws Exception {
        this.activity = activity;
        this.locationList = locationList;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = new LinearLayout(this.activity);
            TextView textView = new TextView(this.activity);
            textView.setText(getLocationName(position));

            ((LinearLayout) view).addView(textView);
        }

        return view;
    }

    @Override
    public int getCount() { return this.locationList.size(); }

    @Override
    public Object getItem(int position) { return 0; }

    @Override
    public long getItemId(int position) { return position; }

    public int getLocationId(int position) {
        return this.locationList.get(position).get("id").intValue();
    }

    public String getLocationName(int position) {
        return this.locationList.get(position).get("name").textValue();
    }

    public ArrayNode getCategoryList(int position) {
        return (ArrayNode) this.locationList.get(position).get("categories");
    }

    public ArrayNode getKeyboardList(int position) {
        return (ArrayNode) this.locationList.get(position).get("sales_keyboards");
    }
}
