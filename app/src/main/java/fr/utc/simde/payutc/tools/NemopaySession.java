package fr.utc.simde.payutc.tools;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Samy on 24/10/2017.
 */

public class NemopaySession {
    private static final String LOG_TAG = "_NemopaySession";
    private static final String url = "https://api.nemopay.net/services/";
    private String name;
    private String key;
    private String session;
    private String username;

    private HTTPRequest request;
    private Map<String, String> cookies = new HashMap<String, String>();

    private final Map<String, String> getArgs = new HashMap<String, String>() {{
        put("system_id", "payutc");
    }};

    public NemopaySession() {
        this.name = "";
        this.key = "";
        this.session = "";
        this.username = "";
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

    protected int request(final String method, final String service) throws IOException { return request(method, service, new HashMap<String, String>()); }
    protected int request(final String method, final String service, final Map<String, String> postArgs) throws IOException {
        Log.d(LOG_TAG, "url: " + url + method + "/" + service);
        this.request = new HTTPRequest(url + method + "/" + service);
        this.request.setGet(getArgs);
        this.request.setPost(postArgs);
        this.request.setCookies(this.cookies);

        int reponseCode = this.request.post();
        this.cookies = request.getCookies();

        return reponseCode;
    }
}
