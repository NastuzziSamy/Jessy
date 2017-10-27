package fr.utc.simde.payutc;

import android.app.Activity;
<<<<<<< HEAD
=======
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
>>>>>>> foundationListActivity

import fr.utc.simde.payutc.tools.CASConnexion;
import fr.utc.simde.payutc.tools.Dialog;
import fr.utc.simde.payutc.tools.HTTPRequest;
import fr.utc.simde.payutc.tools.NemopaySession;

/**
 * Created by Samy on 26/10/2017.
 */

public abstract class BaseActivity extends NFCActivity {
    private static final String LOG_TAG = "_LOG_TAG";
    protected static Dialog dialog;
    protected static NemopaySession nemopaySession;
    protected static CASConnexion casConnexion;

    protected void disconnect() {
        nemopaySession.disconnect();
        casConnexion.disconnect();
    }

    protected void unregister(Activity activity) {
        nemopaySession.unregister();
        disconnect();

        dialog.errorDialog(activity, getString(R.string.key_registration), getString(R.string.key_remove_temp));
    }

    protected void startFoundationListActivity(final Activity activity) {
        dialog.startLoading(activity, getString(R.string.information_collection), getString(R.string.foundation_list_collecting));
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

                        try {
                            if (request.getResponseCode() != 200)
                                throw new Exception("Malformed JSON");

                            JsonNode foundationList = request.getJsonResponse();
                            String response = request.getResponse();

                            if (!request.isJsonResponse() || !foundationList.isArray())
                                throw new Exception("JSON unexpected");

                            for (final JsonNode foundation : foundationList) {
                                if (!foundation.has("name") || !foundation.has("fun_id"))
                                    throw new Exception("Unexpected JSON");
                            }

                            getIntent().getSerializableExtra("MyClass");

                            Intent intent = new Intent(activity, FoundationListActivity.class);
                            intent.putExtra("foundationList", response);
                            dialog.stopLoading();
                            activity.startActivity(intent);
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "error: " + e.getMessage());
                            dialog.errorDialog(getString(R.string.information_collection), getString(R.string.foundation_error_get_list));
                        }
                    }
                });
            }
        }.start();
    }
}
