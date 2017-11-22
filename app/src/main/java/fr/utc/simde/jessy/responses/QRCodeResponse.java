package fr.utc.simde.jessy.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by Samy on 21/11/2017.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class QRCodeResponse {
    protected String username;
    protected String id;

    public String getUsername() { return this.username; }
    public String getId() { return this.id; }
}
