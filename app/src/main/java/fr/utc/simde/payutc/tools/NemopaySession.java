package fr.utc.simde.payutc.tools;

/**
 * Created by Samy on 24/10/2017.
 */

public class NemopaySession {
    private String session;
    private String username;

    public NemopaySession(final String session, final String username) {
        this.session = session;
        this.username = username;
    }
}
