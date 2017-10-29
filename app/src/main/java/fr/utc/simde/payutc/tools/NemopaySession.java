package fr.utc.simde.payutc.tools;

import android.app.Activity;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;

import org.json.JSONException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import fr.utc.simde.payutc.R;

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
    private int foundationId;
    private String foundationName;

    private HTTPRequest request;

    private String noRight;
    private String noRights;
    private String noRightsNeeded;
    private String serviceText;
    private String notFound;
    private String badRequest;
    private String internalError;
    private String errorRequest;

    private Map<String, String> cookies = new HashMap<String, String>();
    private final Map<String, String> getArgs = new HashMap<String, String>() {{
        put("system_id", "payutc");
    }};

    public NemopaySession(final Activity activity) {
        this.name = "";
        this.key = "";
        this.session = "";
        this.username = "";
        this.foundationId = -1;
        this.foundationName = "";

        String[] keys = activity.getResources().getStringArray(R.array.rights_keys);
        String[] values = activity.getResources().getStringArray(R.array.rights_values);
        for (int i = 0; i < Math.min(keys.length, values.length); ++i)
            this.allRights.put(keys[i], values[i]);

        this.noRightsNeeded = activity.getString(R.string.no_need_rights);
        this.noRight = activity.getString(R.string.no_right);
        this.noRights = activity.getString(R.string.no_rights);
        this.serviceText = activity.getString(R.string.service);
        this.notFound = activity.getString(R.string.not_found);
        this.badRequest = activity.getString(R.string.bad_request);
        this.internalError = activity.getString(R.string.internal_error);
        this.errorRequest = activity.getString(R.string.error_request);
    }

    public Boolean isConnected() { return !this.session.isEmpty() && !this.username.isEmpty(); }
    public Boolean isRegistered() { return !this.name.isEmpty() && !this.key.isEmpty() && !this.session.isEmpty(); }

    public void disconnect() {
        this.username = "";

        if (!isRegistered())
            this.session = "";
    }

    public void unregister() {
        this.name = "";
        this.key = "";

        disconnect();
    }

    public void setFoundation(final int foundationId, final String foundationName) {
        this.foundationId = foundationId;
        this.foundationName = foundationName;
    }

    public String getName() { return this.name; }
    public String getKey() { return this.key; }
    public String getUsername() { return username; }
    public HTTPRequest getRequest() { return this.request; }
    public int getFoundationId() { return foundationId; }
    public String getFoundationName() { return foundationName; }

    public int getBuyerInfo(final String badgeId) throws Exception {
        if (!isConnected())
            throw new Exception("Not connected");

        return request(
                "POSS3",
                "getBuyerInfo",
                new HashMap<String, String>() {{
                    put("badge_id", badgeId);
                    if (foundationId != -1)
                        put("fun_id", Integer.toString(foundationId));
                }},
                new String[]{
                        "sale"
                }
        );
    }

    public int getArticles() throws Exception {
        if (!isConnected())
            throw new Exception("Not connected");

        if (this.foundationId == -1)
            throw new Exception("No foundation set");

        return request(
            "POSS3",
            "getProducts",
            new HashMap<String, String>() {{
                put("fun_id", Integer.toString(foundationId));
            }},
            new String[]{
                "sale"
            }
        );
    }

    public int getCategories() throws Exception {
        if (!isConnected())
            throw new Exception("Not connected");

        if (this.foundationId == -1)
            throw new Exception("No foundation set");

        return request(
            "POSS3",
            "getCategories",
            new HashMap<String, String>() {{
                put("fun_id", Integer.toString(foundationId));
            }},
            new String[]{
                "sale"
            }
        );
    }

    public int getFoundations() throws Exception {
        if (!isConnected())
            throw new Exception("Not connected");

        return request(
            "POSS3",
            "getFundations",
            new String[]{
                "sale"
            }
        );
    }

    public int getCASUrl() throws Exception {
        return request(
            "POSS3",
            "getCasUrl"
        );
    }

    public int registerApp(final String name, final String description, final String service) throws Exception {
        if (!isConnected())
            throw new Exception("Not connected");

        int reponseCode = request(
            "KEY",
            "registerApplication",
            new HashMap<String, String>() {{
                put("app_url", service);
                put("app_name", name);
                put("app_desc", description);
            }}
        );

        if (reponseCode != 200 || !this.request.isJSONResponse())
            throw new Exception("Not created");

        JsonNode response = this.request.getJSONResponse();
        if (response.has("app_key"))
            this.key = response.get("app_key").textValue();
        else
            throw new Exception("Unexpected JSON");

        return reponseCode;
    }

    public int loginApp(final String key, CASConnexion casConnexion) throws Exception {
        int reponseCode = loginApp(key);

        JsonNode response = this.request.getJSONResponse();
        if (response.has("config") && response.get("config").has("cas_url"))
            casConnexion.setUrl(response.get("config").get("cas_url").textValue());
        else
            throw new Exception("Unexpected JSON");

        return reponseCode;
    }
    public int loginApp(final String key) throws Exception {
        int reponseCode = request(
            "POSS3",
            "loginApp",
            new HashMap<String, String>() {{
                put("key", key);
            }}
        );

        JsonNode response;
        if (reponseCode == 200 && this.request.isJSONResponse())
            response = this.request.getJSONResponse();
        else
            throw new Exception("Not authentified");

        if (response.has("sessionid") && response.has("name")) {
            this.session = response.get("sessionid").textValue();
            this.name = response.get("name").textValue();
            this.key = key;
        }
        else
            throw new Exception("Unexpected JSON");

        return reponseCode;
    }

    public int loginBadge(final String badgeId, final String pin) throws Exception {
        if (!isRegistered())
            throw new Exception("Not registered");

        int reponseCode = request(
            "POSS3",
            "loginBadge2",
            new HashMap<String, String>() {{
                put("badge_id", badgeId);
                put("pin", pin);
            }},
            new String[]{
                "sale"
            }
        );

        JsonNode response;
        if (reponseCode == 200 && this.request.isJSONResponse())
            response = this.request.getJSONResponse();
        else
            throw new Exception("Not connected");

        if (response.has("sessionid") && response.has("username")) {
            this.session = response.get("sessionid").textValue();
            this.username = response.get("username").textValue();
        }
        else
            throw new Exception("Unexpected JSON");

        return reponseCode;
    }

    public int loginCas(final String ticket, final String service) throws Exception {
        int reponseCode = request(
            "POSS3",
            "loginCas2",
            new HashMap<String, String>() {{
                put("ticket", ticket);
                put("service", service);
            }}
        );

        JsonNode response;
        if (reponseCode == 200 && this.request.isJSONResponse())
            response = this.request.getJSONResponse();
        else
            throw new Exception("Not connected");

        if (response.has("sessionid") && response.has("username")) {
            this.session = response.get("sessionid").textValue();
            this.username = response.get("username").textValue();
        }
        else
            throw new Exception("Unexpected JSON");

        return reponseCode;
    }

    public String forbidden(final String[] rightsNeeded) {
        String result;
        if (rightsNeeded.length == 0)
            result = this.noRightsNeeded;
        else if (rightsNeeded.length == 1)
            result = this.noRight;
        else
            result = this.noRights;

        for (String right : rightsNeeded) {
            if (allRights.containsKey(right))
                result += " " + allRights.get(right) + ",";
            else {
                result += " " + right + ",";
                Log.e(LOG_TAG, "\"" + right + "\" does not exist");
            }
        }

        return result.substring(0, result.length() - 1) + ".";
    }

    protected int request(final String method, final String service) throws Exception { return request(method, service, new HashMap<String, String>(), new String[]{}); }
    protected int request(final String method, final String service, final String[] rightsNeeded) throws Exception { return request(method, service, new HashMap<String, String>(), rightsNeeded); }
    protected int request(final String method, final String service, final Map<String, String> postArgs) throws Exception { return request(method, service, postArgs, new String[]{}); }
    protected int request(final String method, final String service, final Map<String, String> postArgs, final String[] rightsNeeded) throws Exception {
        Log.d(LOG_TAG, "url: " + url + method + "/" + service);
        this.request = new HTTPRequest(url + method + "/" + service);
        this.request.setGet(getArgs);
        this.request.setPost(postArgs);
        this.request.setCookies(this.cookies);

        int responseCode = this.request.post();
        this.cookies = request.getCookies();

        if (responseCode == 200)
            return 200;
        else if (responseCode == 403)
            throw new Exception(forbidden(rightsNeeded));
        else if (responseCode == 404)
            throw new Exception(this.serviceText + " " + service + " " + this.notFound);
        else if (responseCode == 400)
            throw new Exception(this.serviceText + " " + service + " " + this.badRequest);
        else if (responseCode == 500 || responseCode == 503)
            throw new Exception(this.serviceText + " " + service + " " + this.internalError);
        else
            throw new Exception(this.serviceText + " " + service + " " + this.errorRequest + " " + responseCode);
    }
}
