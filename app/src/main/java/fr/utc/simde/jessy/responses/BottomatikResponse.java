package fr.utc.simde.jessy.responses;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Samy on 10/11/2017.
 */

public class BottomatikResponse extends APIResponse {
    protected String _id;
    protected Integer fun_id;
    protected boolean paid;
    protected boolean validated;
    protected List<List<String>> articles;

    public String get_id() { return this._id; }
    public Integer getFun_id() { return this.fun_id; }
    public boolean isPaid() { return this.paid; }
    public boolean isValidated() { return this.validated; }
    public List<List<String>> getArticles() { return this.articles; }

    public List<List<Integer>> getArticleList() {
        List<List<Integer>> articleList = new ArrayList<List<Integer>>();

        for (final List<String> article : this.articles)
            articleList.add(new ArrayList<Integer>() {{
                add(Integer.parseInt(article.get(0)));
                add(Integer.parseInt(article.get(1)));
            }});

        return articleList;
    }
}
