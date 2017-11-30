package fr.utc.simde.jessy.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Samy on 10/11/2017.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class ArticleResponse {
    protected int id;
    protected String name;
    protected int fundation_id;
    protected int categorie_id;
    protected int price;
    protected boolean variable_price;
    protected boolean cotisant;
    protected boolean alcool;
    protected boolean active;

    public int getId() { return this.id; }
    public String getName() { return this.name; }
    public int getFoundationId() { return this.fundation_id; }
    public int getCategoryId() { return this.categorie_id; }
    public int getPrice() { return this.price; }
    public boolean getIsVariablePrice() { return this.variable_price; }
    public boolean getIsContributerOnly() { return this.cotisant; }
    public boolean getIsAdultOnly() { return this.alcool; }
    public boolean getIsActive() { return this.active; }
}
