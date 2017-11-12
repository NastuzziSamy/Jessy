package fr.utc.simde.jessy.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.utc.simde.jessy.R;

/**
 * Created by Samy on 28/10/2017.
 */

public class FoundationsAdapter extends BaseAdapter {
    private static final String LOG_TAG = "_FoundationsAdapter";

    protected Activity activity;
    protected ArrayNode foundationList;

    public FoundationsAdapter(final Activity activity, final ArrayNode foundationList) throws Exception {
        this.activity = activity;
        this.foundationList = foundationList;
    }

    @Override
    public int getCount() {
        return this.foundationList.size();
    }

    @Override
    public Object getItem(int position) {
        return this.foundationList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getFoundationId(position);
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        LayoutInflater layoutInflater = LayoutInflater.from(this.activity);
        view = layoutInflater.inflate(R.layout.fragment_list, null);

        TextView textView = view.findViewById(R.id.text_element);
        textView.setText(getFoundationName(position));

        return view;
    }

    public int getFoundationId(int position) {
        return this.foundationList.get(position).get("fun_id").intValue();
    }

    public String getFoundationName(int position) {
        return this.foundationList.get(position).get("name").textValue();
    }

    public void toast(final int position) {
        Toast.makeText(this.activity, String.valueOf(getFoundationId(position)) + ": " + getFoundationName(position), Toast.LENGTH_SHORT).show();
    }
}