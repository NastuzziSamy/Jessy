package fr.utc.simde.payutc.tools;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;

import java.io.IOException;

import fr.utc.simde.payutc.MainActivity;

/**
 * Created by Samy on 24/10/2017.
 */

public class CASConnexion {
    private static final String LOG_TAG = "CASConnexion";
    private static final String casUrl = "https://api.nemopay.net/services/POSS3/getCasUrl?system_id=payutc";
    private static final String service = "http://localhost";
    private String url = "";
    private String username = "";
    private String location = "";
    private String ticket = "";

    public CASConnexion() {
        new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    HTTPRequest http = new HTTPRequest(casUrl);
                    http.post();
                    url = http.getResponse();
                    url = url.substring(1, url.length() - 1);
                } catch (Exception e) {
                    Log.e(LOG_TAG, e.getMessage());
                }
            }
        }).start();
    }

    public String getUsername() { return this.username; }
    public String getUrl() { return this.url; }

    public void connect(final String username, final String password) throws Exception {
        if (!username.isEmpty())
            this.username = username;

        if (this.url.isEmpty() || username.isEmpty() || password.isEmpty()) {
            throw new RuntimeException("Elements required");
        }

        HTTPRequest http = new HTTPRequest(this.url + "v1/tickets/");
        http.setArg("username", username);
        http.setArg("password", password);

        if (http.post() == 201) {
            Log.d(LOG_TAG, http.getHeader("Location"));
            this.location = http.getHeader("Location");
        }
        else
            throw new RuntimeException("Not Connected");
    }

    public Boolean isConnected() { return !this.location.isEmpty(); }

    public void addService() throws Exception {
        if (!isConnected())
            throw new RuntimeException("Not Connected");

        if (this.url.isEmpty() || this.username.isEmpty() || this.location.isEmpty())
            throw new RuntimeException("Elements required");

        HTTPRequest http = new HTTPRequest(this.location);
        http.setArg("service", this.service);

        if (http.post() == 200) {
            this.ticket = http.getResponse();
            Log.d(LOG_TAG, this.ticket);
        }
        else
            throw new RuntimeException("Service not added");
    }

    public Boolean isServiceAdded() { return !this.ticket.isEmpty(); }
    public String getTicket() { return this.ticket; }
}
