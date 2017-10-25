package fr.utc.simde.payutc.tools;

import android.util.Log;

/**
 * Created by Samy on 24/10/2017.
 */

public class CASConnexion {
    private static final String LOG_TAG = "CASConnexion";
    private static final String service = "http://api.nemopay.net/";
    private String url;
    private String username;
    private String location;
    private String ticket;

    public CASConnexion(final NemopaySession nemopaySession) {
        this.url = "";
        this.username = "";
        this.location = "";
        this.ticket = "";

        new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    HTTPRequest request = nemopaySession.getCasUrl();
                    url = request.getResponse();
                    url = url.substring(1, url.length() - 1);
                } catch (Exception e) {
                    Log.e(LOG_TAG, e.getMessage());
                }
            }
        }).start();
    }

    public void setUsername(final String username) { this.username = username; }
    public String getUsername() { return this.username; }
    public String getUrl() { return this.url; }

    public void connect(final String username, final String password) throws Exception {
        this.username = username;
        this.location = "";
        this.ticket = "";

        if (this.url.isEmpty() || username.isEmpty() || password.isEmpty())
            throw new RuntimeException("Elements required");

        HTTPRequest request = new HTTPRequest(this.url + "v1/tickets/");
        request.setArg("username", username);
        request.setArg("password", password);

        if (request.post() == 201)
            this.location = request.getHeader("Location");
        else
            throw new RuntimeException("Not Connected");
    }

    public Boolean isConnected() { return !this.location.isEmpty(); }

    public void addService() throws Exception {
        this.ticket = "";

        if (!isConnected())
            throw new RuntimeException("Not Connected");

        if (this.url.isEmpty() || this.username.isEmpty() || this.location.isEmpty())
            throw new RuntimeException("Elements required");

        HTTPRequest request = new HTTPRequest(this.location);
        request.setArg("service", this.service);

        if (request.post() == 200)
            this.ticket = request.getResponse();
        else
            throw new RuntimeException("Service not added");
    }

    public Boolean isServiceAdded() { return !this.ticket.isEmpty(); }
}
