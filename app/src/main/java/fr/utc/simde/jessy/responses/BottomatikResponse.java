package fr.utc.simde.jessy.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Samy on 10/11/2017.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class BottomatikResponse {
    protected String id;
    protected String username;
    protected String type;
    protected int fun_id;
    protected long expires_at;
    protected long creation_date;
    protected List<Integer> articles;

    public String getId() { return this.id; }
    public String getUsername() { return this.username; }
    public String getType() { return this.type; }
    public int getFoundationId() { return this.fun_id; }
    public long getExpiresAt() { return this.expires_at; }
    public long getCreatedAt() { return this.creation_date; }
    public List<Integer> getArticleList() { return this.articles; }
}
