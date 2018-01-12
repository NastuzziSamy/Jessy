package fr.utc.simde.jessy.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Samy on 10/11/2017.
 */

public class APIResponse {
    protected String id;
    protected String username;
    protected String type;
    protected long creation_date;
    protected long expires_at;

    public String getId() { return this.id; }
    public String getUsername() { return this.username; }
    public String getType() { return this.type; }
    public long getCreation_date() { return this.creation_date; }
    public long getExpires_at() { return this.expires_at; }
}
