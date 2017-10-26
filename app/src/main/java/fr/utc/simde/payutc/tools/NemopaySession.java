package fr.utc.simde.payutc.tools;

import android.app.Activity;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import fr.utc.simde.payutc.R;

import static java.lang.System.in;

/**
 * Created by Samy on 24/10/2017.
 */

public class NemopaySession {
    private static final String LOG_TAG = "_NemopaySession";
    private static final String url = "https://api.nemopay.net/services/";
    private static Map<String, String> allRights = new HashMap<String, String>();
    private String name;
    private String key;
    private String session;
    private String username;

    private HTTPRequest request;
    private String[] rightsNeeded;

    private Map<String, String> cookies = new HashMap<String, String>();
    private final Map<String, String> getArgs = new HashMap<String, String>() {{
        put("system_id", "payutc");
    }};

    public NemopaySession(Activity activity) {
        this.name = "";
        this.key = "";
        this.session = "";
        this.username = "";

        String[] keys = activity.getResources().getStringArray(R.array.rights_keys);
        String[] values = activity.getResources().getStringArray(R.array.rights_values);
        for (int i = 0; i < Math.min(keys.length, values.length); ++i)
            this.allRights.put(keys[i], values[i]);
    }

    public Boolean isConnected() { return !this.session.isEmpty() && !this.username.isEmpty(); }
    public Boolean isRegistered() { return !this.name.isEmpty() && !this.key.isEmpty() && !this.session.isEmpty(); }

    public void disconnect() {
        this.session = "";
        this.username = "";
    }

    public void unregister() {
        this.name = "";
        this.key = "";

        disconnect();
    }

    public String getName() { return this.name; }
    public String getKey() { return this.key; }
    public String getUsername() { return username; }
    public HTTPRequest getRequest() { return this.request; }

    public int getCASUrl() throws IOException {
        return request("POSS3", "getCasUrl");
    }

    public int registerApp(final String name, final String description, final String service) throws IOException, JSONException {
        int reponseCode = request("KEY", "registerApplication", new HashMap<String, String>() {{
            put("app_url", service);
            put("app_name", name);
            put("app_desc", description);
        }});

        if (reponseCode == 200 && this.request.isJsonResponse())
            this.key = this.request.getJsonResponse().getString("app_key");

        return reponseCode;
    }

    public int loginApp(final String key, CASConnexion casConnexion) throws Exception {
        int reponseCode = loginApp(key);

        JSONObject response = getRequest().getJsonResponse();
        if (response.has("config") && ((JSONObject) response.get("config")).has("cas_url"))
            casConnexion.setUrl(((JSONObject) response.get("config")).getString("cas_url"));
        else
            throw new Exception("No correct informations");

        return reponseCode;
    }
    public int loginApp(final String key) throws Exception {
        int reponseCode = request("POSS3", "loginApp", new HashMap<String, String>() {{
            put("key", key);
        }});

        JSONObject response;
        if (reponseCode == 200 && this.request.isJsonResponse())
            response = this.request.getJsonResponse();
        else
            throw new Exception("Not authentified");

        if (response.has("sessionid") && response.has("name")) {
            this.session = response.getString("sessionid");
            this.name = response.getString("name");
            this.key = key;
        }
        else
            throw new Exception("No correct informations");

        return reponseCode;
    }

    public int loginBadge(final String idBadge, final String pin) throws Exception {
        int reponseCode = request("POSS3", "loginBadge2", new HashMap<String, String>() {{
            put("badge_id", idBadge);
            put("pin", pin);
        }}, new String[]{
            "sale"
        });

        JSONObject response;
        if (reponseCode == 200 && this.request.isJsonResponse())
            response = this.request.getJsonResponse();
        else
            throw new Exception("Not connected");

        if (response.has("sessionid") && response.has("username")) {
            this.session = response.getString("sessionid");
            this.username = response.getString("username");
        }
        else
            throw new Exception("No correct informations");

        return reponseCode;
    }

    public int loginCas(final String ticket, final String service) throws Exception {
        int reponseCode = request("POSS3", "loginCas2", new HashMap<String, String>() {{
            put("ticket", ticket);
            put("service", service);
        }});

        JSONObject response;
        if (reponseCode == 200 && this.request.isJsonResponse())
            response = this.request.getJsonResponse();
        else
            throw new Exception("Not connected");

        if (response.has("sessionid") && response.has("username")) {
            this.session = response.getString("sessionid");
            this.username = response.getString("username");
        }
        else
            throw new Exception("No correct informations");

        return reponseCode;
    }

    public String needRights(Activity activity) {
        String result;
        if (this.rightsNeeded.length == 0)
            return activity.getString(R.string.no_need_rights);
        else if (this.rightsNeeded.length == 1)
            result = activity.getString(R.string.no_right);
        else
            result = activity.getString(R.string.no_rights);

        for (String right : this.rightsNeeded) {
            if (allRights.containsKey(right))
                result += " " + allRights.get(right) + ",";
            else {
                result += " " + right + ",";
                Log.e(LOG_TAG, "\"" + right + "\" does not exist");
            }
        }

        return result.substring(0, result.length() - 1) + ".";
    }

    protected int request(final String method, final String service) throws IOException { return request(method, service, new HashMap<String, String>(), new String[]{}); }
    protected int request(final String method, final String service, final String[] rightsNeeded) throws IOException { return request(method, service, new HashMap<String, String>(), rightsNeeded); }
    protected int request(final String method, final String service, final Map<String, String> postArgs) throws IOException { return request(method, service, postArgs, new String[]{}); }
    protected int request(final String method, final String service, final Map<String, String> postArgs, final String[] rightsNeeded) throws IOException {
        Log.d(LOG_TAG, "url: " + url + method + "/" + service);
        this.request = new HTTPRequest(url + method + "/" + service);
        this.request.setGet(getArgs);
        this.request.setPost(postArgs);
        this.request.setCookies(this.cookies);

        int reponseCode = this.request.post();
        this.cookies = request.getCookies();
        this.rightsNeeded = rightsNeeded;

        return reponseCode;
    }
}
