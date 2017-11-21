package fr.utc.simde.jessy.objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by Samy on 10/11/2017.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class GingerResponse {
    protected String login;
    protected String nom;
    protected String prenom;
    protected String mail;
    protected String type;
    protected boolean is_adulte;
    protected boolean is_cotisant;
    protected String badge_uid;

    public String getUsername() { return login; }
    public String getLastname() { return nom; }
    public String getFirstname() { return prenom; }
    public String getEmail() { return mail; }
    public String getType() { return type; }
    public boolean getIsAdult() { return is_adulte; }
    public boolean getIsContributer() { return is_cotisant; }
    public String getBadgeId() { return badge_uid; }
}
