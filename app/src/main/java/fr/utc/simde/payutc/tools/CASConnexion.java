package fr.utc.simde.payutc.tools;

import android.util.Log;

/**
 * Created by Samy on 24/10/2017.
 */

public class CASConnexion {
    private static final String LOG_TAG = "CASConnexion";
    private static String casUrl = "https://api.nemopay.net/services/POSS3/getCasUrl?system_id=payutc";
    private String url;
    private String username;
    private String ticket;

    public CASConnexion() {
        new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    HTTPRequest http = new HTTPRequest(casUrl);
                    http.post();
                    url = http.getResponse();
                } catch (Exception e) {
                    Log.e(LOG_TAG, e.getMessage());
                }
            }
        }).start();
    }

    public String getUsername() { return this.username; }
    public String getUrl() { return this.url; }

    public Boolean connect() {
        if (url.isEmpty() || username.isEmpty())
            return false;

        return false;
    }
}
