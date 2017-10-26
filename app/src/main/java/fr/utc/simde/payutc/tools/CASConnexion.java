package fr.utc.simde.payutc.tools;

import android.util.Log;

/**
 * Created by Samy on 24/10/2017.
 */

public class CASConnexion {
    private static final String LOG_TAG = "_CASConnexion";
    private static String url;
    private static String username;
    private static String location;
    private static String ticket;

    public CASConnexion(final NemopaySession nemopaySession) {
        this.url = "";
        this.username = "";
        this.location = "";
        this.ticket = "";
    }

    public void setUsername(final String username) { this.username = username; }
    public String getUsername() { return this.username; }
    public String getTicket() { return this.ticket; }
    public String getUrl() { return this.url; }
    public void setUrl(final String url) { this.url = url; }

    public void connect(final String username, final String password) throws Exception {
        this.username = username;
        this.location = "";
        this.ticket = "";

        if (this.url.isEmpty() || username.isEmpty() || password.isEmpty())
            throw new RuntimeException("Elements required");

        HTTPRequest request = new HTTPRequest(this.url + "v1/tickets/");
        request.addPost("username", username);
        request.addPost("password", password);

        if (request.post() == 201)
            this.location = request.getHeader("Location");
        else
            throw new RuntimeException("Not Connected");
    }

    public void disconnect() {
        this.username = "";
        this.location = "";
        this.ticket = "";
    }

    public Boolean isConnected() { return !this.location.isEmpty(); }

    public void addService(final String service) throws Exception {
        this.ticket = "";

        if (!isConnected())
            throw new RuntimeException("Not Connected");

        if (this.url.isEmpty() || this.username.isEmpty() || this.location.isEmpty())
            throw new RuntimeException("Elements required");

        HTTPRequest request = new HTTPRequest(this.location);
        request.addPost("service", service);

        if (request.post() == 200)
            this.ticket = request.getResponse();
        else
            throw new RuntimeException("Service not added");
    }

    public Boolean isServiceAdded() { return !this.ticket.isEmpty(); }
}
