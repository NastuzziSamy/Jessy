package fr.utc.simde.jessy.tools;

import android.app.Activity;

import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import fr.utc.simde.jessy.R;

/**
 * Created by Samy on 29/11/2017.
 */

public class API {
    private static final String LOG_TAG = "_API";

    private String name;
    private String url;
    private String key;

    private HTTPRequest request;

    private String noKey;
    private String noRight;
    private String serviceText;
    private String notFound;
    private String badRequest;
    private String internalError;
    private String goneRequest;
    private String errorRequest;

    public API(final Activity activity, final String name, final String url) {
        this.name = name;
        this.url = url;
        this.key = "";

        this.noKey = activity.getString(R.string.ginger_no_key);
        this.noRight = activity.getString(R.string.ginger_no_rights);
        this.serviceText = activity.getString(R.string.service);
        this.notFound = activity.getString(R.string.not_found);
        this.badRequest = activity.getString(R.string.bad_request);
        this.internalError = activity.getString(R.string.internal_error);
        this.errorRequest = activity.getString(R.string.error_request);
        this.goneRequest = activity.getString(R.string.gone_request);
    }

    public void setKey(final String key) { this.key = key; }

    public HTTPRequest getRequest() { return this.request; }

    public int interact(final String before, final String command) throws Exception {
        return request(
            before + (before == null || before.isEmpty() || command == null || command.isEmpty() ? "" : "/") + command
        );
    }
    public int interact(final String before, final String command, Map<String, Object> postArgs) throws Exception {
        return request(
            before + (before == null || before.isEmpty() || command == null || command.isEmpty() ? "" : "/") + command,
            postArgs
        );
    }

    public int getInfosFromId(final String id) throws Exception {
        return request(
            id
        );
    }

    public int getInfosFromUsername(final String username) throws Exception {
        return request(
            "user/" + username
        );
    }

    protected int request(final String request) throws Exception { return request(request, new HashMap<String, Object>()); }
    protected int request(final String request, Map<String, Object> postArgs) throws Exception {
        this.request = new HTTPRequest(url + request);

        int responseCode;

        if (!this.key.equals(""))
            this.request.setGet(new HashMap<String, String>(){{ put("app_key", key); }});

        if (postArgs.size() == 0)
            responseCode = this.request.get();
        else {
            this.request.setPost(postArgs);
            responseCode = this.request.post(false);
        }

        if (responseCode == 200)
            return 200;
        else if (responseCode == 401)
            throw new Exception(this.noKey);
        else if (responseCode == 403)
            throw new Exception(this.noRight);
        else if (responseCode == 404)
            throw new Exception(this.serviceText + " " + this.name + " " + this.notFound);
        else if (responseCode == 400)
            throw new Exception(this.serviceText + " " + this.name + " " + this.badRequest);
        else if (responseCode == 500 || responseCode == 503)
            throw new Exception(this.serviceText + " " + this.name + " " + this.internalError);
        else if (responseCode == 410)
                throw new Exception(this.serviceText + " " + this.name + " " + this.goneRequest);
        else
            throw new Exception(this.serviceText + " " + this.name + " " + this.errorRequest + " " + responseCode);
    }
}
