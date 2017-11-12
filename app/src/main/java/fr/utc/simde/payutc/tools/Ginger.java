package fr.utc.simde.payutc.tools;

import android.app.Activity;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import fr.utc.simde.payutc.R;

/**
 * Created by Samy on 10/11/2017.
 */

public class Ginger {
    private static final String LOG_TAG = "_Ginger";
    private static final String url = "https://assos.utc.fr/ginger/v1/";
    private String key;

    private HTTPRequest request;

    private String noKey;
    private String noRight;
    private String serviceText;
    private String notFound;
    private String badRequest;
    private String internalError;
    private String errorRequest;

    public Ginger(final Activity activity) {
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

    public int addCotisation(final String login, final String fin, final Integer paid) throws Exception {
        return request(
            login + "/cotisations",
            new HashMap<String, String>() {{
                put("debut", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
                put("fin", fin);
                put("montant", Integer.toString(paid));
            }}
        );
    }

    public int getInfoFromBadge(final String badgeId) throws Exception {
        return request(
            "badge/" + badgeId
        );
    }

    public int getInfo(final String login) throws Exception {
        return request(
            login
        );
    }

    protected int request(final String request) throws Exception { return request(request, new HashMap<String, String>()); }
    protected int request(final String request, Map<String, String> postArgs) throws Exception {
        Log.d(LOG_TAG, "url: " + url + request);

        this.request = new HTTPRequest(url + request);

        int responseCode;
        if (postArgs.size() == 0) {
            this.request.setGet(new HashMap<String, String>(){{ put("key", key); }});
            responseCode = this.request.get();
        }
        else {
            postArgs.put("key", key);
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
            throw new Exception("Ginger " + this.notFound);
        else if (responseCode == 400)
            throw new Exception("Ginger " + this.badRequest);
        else if (responseCode == 500 || responseCode == 503) {
            throw new Exception("Ginger " + this.internalError);
        }
        else
            throw new Exception("Ginger " + this.errorRequest + " " + responseCode);
    }
}
