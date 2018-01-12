package fr.utc.simde.jessy.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by Samy on 21/11/2017.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class QRCodeResponse {
    protected String username;
    protected String id;
    protected String system;

    public void setUsername(String username) { this.username = username; }
    public void setId(String id) { this.id = id; }
    public void setSystem(String system) { this.system = system; }

    public String getUsername() { return this.username; }
    public String getId() { return this.id; }
    public String getSystem() { return this.system; }
}
