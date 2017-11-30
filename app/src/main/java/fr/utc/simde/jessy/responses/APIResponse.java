package fr.utc.simde.jessy.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Samy on 10/11/2017.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class APIResponse {
    protected String id;
    protected String username;
    protected String type;
    protected Integer fun_id;
    protected long expires_at;
    protected long creation_date;
    protected boolean paid;
    protected boolean validated;

    public String getId() { return this.id; }
    public String getUsername() { return this.username; }
    public String getType() { return this.type; }
    public Integer getFoundationId() { return this.fun_id; }
    public long getExpiresAt() { return this.expires_at; }
    public long getCreatedAt() { return this.creation_date; }
    public boolean isPaid() { return this.paid; }
    public boolean isValidated() { return this.validated; }
}
