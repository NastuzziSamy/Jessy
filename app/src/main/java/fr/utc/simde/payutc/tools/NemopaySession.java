package fr.utc.simde.payutc.tools;

import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Samy on 24/10/2017.
 */

public class NemopaySession {
    private static final String LOG_TAG = "_NemopaySession";
    private static final String url = "https://api.nemopay.net/services/";
    private String key;
    private String session;
    private String username;

    private final Map<String, String> getArgs = new HashMap<String, String>() {{
        put("system_id", "payutc");
    }};

    public NemopaySession() {
        this.session = "";
        this.username = "";
    }

    public HTTPRequest getCasUrl() throws IOException {
        return construct("POSS3", "getCasUrl");
    }

    public HTTPRequest loginCas(final String ticket, final String service) throws IOException {
        return construct("POSS3", "loginCas2", new HashMap<String, String>() {{
            put("ticket", ticket);
            put("service", service);
        }});
    }

    protected HTTPRequest construct(final String method, final String service) throws IOException { return construct(method, service, new HashMap<String, String>()); }
    protected HTTPRequest construct(final String method, final String service, final Map<String, String> postArgs) throws IOException {
        HTTPRequest request = new HTTPRequest(url + method + "/" + service);
        Log.d(LOG_TAG, "url: " + url + method + "/" + service);
        request.setGet(getArgs);
        request.setPost(postArgs);

        request.post();
        return request;
    }
}
