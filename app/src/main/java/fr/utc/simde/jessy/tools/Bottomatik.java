package fr.utc.simde.jessy.tools;

import android.app.Activity;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import fr.utc.simde.jessy.R;

/**
 * Created by Samy on 21/11/2017.
 */

public class Bottomatik {
    private static final String LOG_TAG = "_Bottomatik";

    private static final String url = "https://picasso.bottomatik.com/bot/transactions/";
    private String key;

    private HTTPRequest request;

    private String noKey;
    private String noRight;
    private String serviceText;
    private String notFound;
    private String badRequest;
    private String internalError;
    private String errorRequest;

    public Bottomatik(final Activity activity) {
        this.key = "";

        this.noKey = activity.getString(R.string.ginger_no_key);
        this.noRight = activity.getString(R.string.ginger_no_rights);
        this.serviceText = activity.getString(R.string.service);
        this.notFound = activity.getString(R.string.not_found);
        this.badRequest = activity.getString(R.string.bad_request);
        this.internalError = activity.getString(R.string.internal_error);
        this.errorRequest = activity.getString(R.string.error_request);
    }

    public void setKey(final String key) { this.key = key; }

    public HTTPRequest getRequest() { return this.request; }

    public int setTransaction(final String id, final boolean paid, final boolean served) throws Exception {
        return request(
            id + "/validate",
            new HashMap<String, Object>() {{
                put("paid", paid);
                put("served", served);
            }}
        );
    }

    public int getTransactionFromId(final String id) throws Exception {
        return request(
            id
        );
    }

    public int getTransactionFromUsername(final String username) throws Exception {
        return request(
            "user/" + username
        );
    }

    protected int request(final String request) throws Exception { return request(request, new HashMap<String, Object>()); }
    protected int request(final String request, Map<String, Object> postArgs) throws Exception {
        this.request = new HTTPRequest(url + request);

        int responseCode;
        this.request.setGet(new HashMap<String, String>(){{ put("app_key", key); }});
        if (postArgs.size() == 0) {
            responseCode = this.request.get();
        }
        else {
            this.request.setPost(postArgs);
            responseCode = this.request.post();
        }

        if (responseCode == 200)
            return 200;
        else if (responseCode == 401)
            throw new Exception(this.noKey);
        else if (responseCode == 403)
            throw new Exception(this.noRight);
        else if (responseCode == 404)
            throw new Exception("Bottomatik " + this.notFound);
        else if (responseCode == 400)
            throw new Exception("Bottomatik " + this.badRequest);
        else if (responseCode == 500 || responseCode == 503) {
            throw new Exception("Bottomatik " + this.internalError);
        }
        else
            throw new Exception("Bottomatik " + this.errorRequest + " " + responseCode);
    }
}
