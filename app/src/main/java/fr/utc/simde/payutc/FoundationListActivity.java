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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_foundation_list);

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
    protected void onIdentification(String idBadge) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();

        disconnect();
    }

    protected void setFoundationList(final JsonNode foundationList) throws Exception {
        LinearLayout linearLayout = findViewById(R.id.foundationList);

        for (final JsonNode foundation : foundationList) {
            Button foundationButton = new Button(this);

            if (!foundation.has("name") || !foundation.has("fun_id"))
                throw new Exception("Unexpected JSON");

            foundationButton.setText(foundation.get("name").textValue());
            foundationButton.setOnClickListener(new onClickFoundation(foundation.get("fun_id").intValue()));

            linearLayout.addView(foundationButton);
        }
    }

    public class onClickFoundation implements View.OnClickListener {
        final int idFoundation;

        public onClickFoundation(int idFoundation) {
            this.idFoundation = idFoundation;
        }

        @Override
        public void onClick(View view) {
            nemopaySession.setFoundation(this.idFoundation);
            Log.d(LOG_TAG, String.valueOf(this.idFoundation));
        }

    };
}
