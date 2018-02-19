package fr.utc.simde.jessy.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * Created by Samy on 10/11/2017.
 */

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

    public void setCommand(String command) { this.command = command; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setArguments(Map<String, String> arguments) { this.arguments = arguments; }
}
