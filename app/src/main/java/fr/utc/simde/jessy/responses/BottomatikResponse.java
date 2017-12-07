package fr.utc.simde.jessy.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Samy on 10/11/2017.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class BottomatikResponse extends APIResponse {
    protected List<List<String>> articles;

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
