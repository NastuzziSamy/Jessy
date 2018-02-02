package fr.utc.simde.jessy.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Created by Samy on 10/11/2017.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class APIResponse {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public class APICommand {
        // Informations nécessaires
        protected String command;
        // Informations complémentaires
        protected String name;
        protected String description;
        protected Map<String, String> arguments;

        public String getCommand() { return command; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public Map<String, String> getArguments() { return arguments; }
    }

    // Informations nécessaires
    protected String id;
    protected String username;
    // Informations complémentaires
    protected Long creationDate;
    protected Long expirationDate;
    // Affiche soit un message, soit une liste de données rangée par catégorie
    protected String message;
    protected Map<String, Map<String, String>> data;
    // Permet d'exécuter des commandes suites à l'affichage (rien d'obligatoire mais button Ok par défaut)
    protected APICommand neutralCommand;
    protected APICommand negativeCommand;
    protected APICommand positiveCommand;

    public String getId() { return this.id; }
    public String getUsername() { return this.username; }
    public String getMessage() { return message; }
    public Map<String, Map<String, String>> getData() { return this.data; }
    public Long getCreationDate() { return this.creationDate; }
    public Long getExpirationDate() { return this.expirationDate; }
    public void removeExpirationDate() { this.expirationDate = null; }
    public APICommand getNeutralCommand() { return neutralCommand; }
    public APICommand getNegativeCommand() { return negativeCommand; }
    public APICommand getPositiveCommand() { return positiveCommand; }
}
