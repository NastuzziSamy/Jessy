package fr.utc.simde.payutc;

import android.app.ProgressDialog;
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

        dialog.startLoading(FoundationListActivity.this, getString(R.string.nemopay_connection), getString(R.string.nemopay_authentification));
        new Thread() {
            @Override
            public void run() {
                try {
                    nemopaySession.getFoundations();
                    Thread.sleep(100);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        HTTPRequest request = nemopaySession.getRequest();
                        dialog.stopLoading();

                        try {
                            if (request.getResponseCode() != 200)
                                throw new Exception("Malformed JSON");

                            JsonNode foundationList = request.getJsonResponse();

                            if (!request.isJsonResponse() || !foundationList.isArray())
                                throw new Exception("JSON unexpected");

                            setFoundationList(foundationList);
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "error: " + e.getMessage());
                            dialog.errorDialog(getString(R.string.information_collection), getString(R.string.foundation_error_get_list));
                        }
                    }
                });
            }
        }.start();
    }

    @Override
    protected void onIdentification(String idBadge) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();

        disconnect();
    }

    protected void setFoundationList(JsonNode foundationList) throws Exception {
        LinearLayout linearLayout = findViewById(R.id.foundationList);

        for (final JsonNode foundation : foundationList) {
            Button foundationButton = new Button(this);

            if (!foundation.has("name") || !foundation.has("fun_id"))
                throw new Exception("Unexpected JSON");

            foundationButton.setText(foundation.get("name").textValue());
            final String idFoundation = foundation.get("fun_id").textValue();
            foundationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                   //Log.d(LOG_TAG, idFoundation);
                }
            });

            linearLayout.addView(foundationButton);
        }
    }
}
