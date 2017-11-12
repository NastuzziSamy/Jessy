package fr.utc.simde.jessy.adapters;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Created by Samy on 29/10/2017.
 */

public class OptionChoicesAdapter extends BaseAdapter {
    private static final String LOG_TAG = "_OptionChoicesAdapter";

    protected Activity activity;

    protected ArrayNode optionList;

    protected CheckBox[] checkBoxList;

    public OptionChoicesAdapter(final Activity activity, final ArrayNode optionList) throws Exception {
        this.activity = activity;
        this.optionList = optionList;
        this.checkBoxList = new CheckBox[optionList.size()];
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = new LinearLayout(this.activity);
            CheckBox checkBox = new CheckBox(this.activity);
            checkBox.setText(this.optionList.get(position).textValue());

            ((LinearLayout) view).addView(checkBox);

            if (this.checkBoxList[position] == null)
                this.checkBoxList[position] = checkBox;
        }

        return view;
    }

    @Override
    public int getCount() { return this.optionList.size(); }

    @Override
    public Object getItem(int position) { return 0; }

    @Override
    public long getItemId(int position) { return position; }

    public ArrayNode getList() {
        ArrayNode optionList = new ObjectMapper().createArrayNode();

        int i = 0;
        for (CheckBox checkBox : this.checkBoxList) {
            if (checkBox.isChecked())
                optionList.add(this.optionList.get(i).textValue());

            i++;
        }

        return optionList;
    }
}
