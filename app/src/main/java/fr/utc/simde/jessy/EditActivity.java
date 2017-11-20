package fr.utc.simde.jessy;

import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.List;

import fr.utc.simde.jessy.fragments.ArticleGroupFragment;
import fr.utc.simde.jessy.fragments.EditFragment;
import fr.utc.simde.jessy.fragments.SellFragment;
import fr.utc.simde.jessy.tools.Panier;

/**
 * Created by Samy on 20/11/2017.
 */

public class EditActivity extends ArticleGroupActivity {
    private static final String LOG_TAG = "_EditActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        generate();
    }

    @Override
    protected void setOptionButton() {
        this.optionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

    @Override
    protected void createNewGroup(final String name, final ArrayNode articleList) throws Exception { createNewGroup(name, articleList, 3); }
    protected void createNewGroup(final String name, final ArrayNode articleList, int gridColumns) throws Exception {
        ArticleGroupFragment articleGroupFragment = new EditFragment(EditActivity.this, articleList, this.dialog, this.config, gridColumns);

        TabHost.TabSpec newTabSpec = this.tabHost.newTabSpec(name);
        newTabSpec.setIndicator(name);
        newTabSpec.setContent(articleGroupFragment);

        this.groupFragmentList.add(articleGroupFragment);

        this.tabHost.addTab(newTabSpec);
        nbrGroups++;
    }
}
