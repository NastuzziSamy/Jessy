package fr.utc.simde.jessy.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.utc.simde.jessy.R;

/**
 * Created by Samy on 28/10/2017.
 */

public class OptionsAdapter extends BaseAdapter {
    private static final String LOG_TAG = "_OptionsAdapter";

    protected Activity activity;
    protected ArrayNode optionList;

    public OptionsAdapter(final Activity activity, final ArrayNode optionList) throws Exception {
        this.activity = activity;
        this.optionList = optionList;
    }

    @Override
    public int getCount() {
        return this.optionList.size();
    }

    @Override
    public Object getItem(int position) {
        return this.optionList.get(position).textValue();
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        LayoutInflater layoutInflater = LayoutInflater.from(this.activity);
        view = layoutInflater.inflate(R.layout.fragment_list, null);

        TextView textView = view.findViewById(R.id.text_element);
        textView.setText(this.optionList.get(position).textValue());

        return view;
    }

    public String getOptionName(int position) {
        return this.optionList.get(position).textValue();
    }
}