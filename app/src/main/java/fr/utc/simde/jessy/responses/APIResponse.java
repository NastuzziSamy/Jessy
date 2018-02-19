package fr.utc.simde.jessy.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class APIResponse {
    protected String id;
    protected String username;
    // Informations complémentaires
    protected Long creationDate;
    protected Long expirationDate;
    // Affiche soit un message, soit une liste de données rangée par catégorie
    protected String message;
    protected Map<String, Map<String, String>> data;
    // Fais payer la personne les articles donnés correspondant à la fondation (il est nécessaire que la personne possède les droits de ventes dessus) => Implique que la première commande fera payer les articles affichés (uniquelment le message peut-être affiché)    protected List<List<String>> articles;
    protected List<List<String>> articles;
    protected Integer foundationId;
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
    public List<List<String>> getArticles() { return this.articles; }
    public List<List<Integer>> getArticleList() {
        List<List<Integer>> articleList = new ArrayList<List<Integer>>();

        if (this.articles == null)
            return articleList;

        for (final List<String> article : this.articles)
            articleList.add(new ArrayList<Integer>() {{
                add(Integer.parseInt(article.get(0)));
                add(Integer.parseInt(article.get(1)));
            }});

        return articleList;
    }
    public Integer getFoundationId() { return this.foundationId; }
    public APICommand getNeutralCommand() { return neutralCommand; }
    public APICommand getNegativeCommand() { return negativeCommand; }
    public APICommand getPositiveCommand() { return positiveCommand; }
}
