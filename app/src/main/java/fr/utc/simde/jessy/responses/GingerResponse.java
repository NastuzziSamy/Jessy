package fr.utc.simde.jessy.responses;

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

    public String getLogin() { return login; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getMail() { return mail; }
    public String getType() { return type; }
    public boolean getIs_adulte() { return is_adulte; }
    public boolean getIs_cotisant() { return is_cotisant; }
    public String getBadge_uid() { return badge_uid; }
}
