package fr.utc.simde.payutc;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.json.JSONArray;
import org.json.JSONObject;

import fr.utc.simde.payutc.tools.HTTPRequest;

/**
 * Created by Samy on 26/10/2017.
 */

public class FoundationListActivity extends BaseActivity {
    private static final String LOG_TAG = "_FoundationListActivity";

    LinearLayout listLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_foundation_list);

        listLayout = findViewById(R.id.foundationList);

        try {
            setFoundationList(new ObjectMapper().readTree(getIntent().getExtras().getString("foundationList")));
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
    public void onRestart() {
        super.onRestart();

        startFoundationListActivity(FoundationListActivity.this);
    }

    @Override
    protected void onIdentification(final String badgeId) {}

    protected void setFoundationList(final JsonNode foundationList) throws Exception {
        for (final JsonNode foundation : foundationList) {
            Button foundationButton = new Button(this);

            if (!foundation.has("name") || !foundation.has("fun_id"))
                throw new Exception("Unexpected JSON");

            foundationButton.setText(foundation.get("name").textValue());
            foundationButton.setOnClickListener(new onClickFoundation(foundation.get("fun_id").intValue(), foundation.get("name").textValue()));

            this.listLayout.addView(foundationButton);
        }
    }

    public class onClickFoundation implements View.OnClickListener {
        final int foundationId;
        final String foundationName;

        public onClickFoundation(final int foundationId, final String foundationName) {
            this.foundationId = foundationId;
            this.foundationName = foundationName;
        }

        @Override
        public void onClick(View view) {
            startArticlesActivity(FoundationListActivity.this, this.foundationId, this.foundationName);
        }

    };
}
