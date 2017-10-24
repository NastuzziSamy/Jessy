package fr.utc.simde.payutc.tools;

/**
 * Created by Samy on 24/10/2017.
 */

public class CASConnexion {
    private String url;
    private String username;
    private String password;
    private String ticket;

    public void CASConnexion(String url) {
        this.url = url;
    }

    public void setConnexion(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Boolean connect() {
        if (url.isEmpty() || username.isEmpty() || password.isEmpty())
            return false;

        return false;
    }
}
