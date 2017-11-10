package fr.utc.simde.payutc;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;

import fr.utc.simde.payutc.adapters.FoundationsAdapter;
import fr.utc.simde.payutc.adapters.ListAdapater;

/**
 * Created by Samy on 26/10/2017.
 */

public class FoundationsOptionsActivity extends BaseActivity {
    private static final String LOG_TAG = "_FoundationsOptionsActivity";

    TabHost tabHost;
    ListView foundationList;
    ListView optionList;

    FoundationsAdapter foundationsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_foundations_options);

        this.tabHost = findViewById(R.id.tabs_foundations_options);
        this.foundationList = findViewById(R.id.list_foundations);
        this.optionList = findViewById(R.id.list_options);

        this.tabHost.setup();
        this.tabHost.addTab(this.tabHost.newTabSpec(getString(R.string.foundations)).setIndicator(getString(R.string.foundations)).setContent(R.id.list_foundations));
        this.tabHost.addTab(this.tabHost.newTabSpec(getString(R.string.options)).setIndicator(getString(R.string.options)).setContent(R.id.list_options));

        try {
            setFoundationList((ArrayNode) new ObjectMapper().readTree(getIntent().getExtras().getString("foundationList")));
            setOptionList();
        } catch (Exception e) {
            Log.wtf(LOG_TAG, "error: " + e.getMessage());
            dialog.errorDialog(this, getResources().getString(R.string.information_collection), getResources().getString(R.string.error_unexpected), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int id) {
                    finish();
                }
            });
        }
    }

    @Override
    protected void onIdentification(final String badgeId) {}

    protected void setFoundationList(final ArrayNode foundationList) throws Exception {
        this.foundationsAdapter = new FoundationsAdapter(FoundationsOptionsActivity.this, foundationList);

        this.foundationList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                startArticlesActivity(FoundationsOptionsActivity.this, foundationsAdapter.getFoundationId(position), foundationsAdapter.getFoundationName(position));
            }
        });

        this.foundationList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                foundationsAdapter.toast(position);

                return true;
            }
        });

        this.foundationList.setAdapter(this.foundationsAdapter);
    }

    protected void setOptionList() {
        this.optionList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                if (position == 2)
                    startReadCardInfoActivity(FoundationsOptionsActivity.this);
            }
        });
    }
}
