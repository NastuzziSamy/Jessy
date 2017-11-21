package fr.utc.simde.jessy.objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by Samy on 21/11/2017.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class QRCodeResponse {
    private String username;
    private String id;

    public String getUsername() { return this.username; }
    public void setUsername(String username) { this.username = username; }

    public String getId() { return this.id; }
    public void setId(String id) { this.id = id; }
}
