package fr.utc.simde.payutc;

import android.os.Bundle;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.HashMap;

import fr.utc.simde.payutc.ArticleGroupActivity;

/**
 * Created by Samy on 27/10/2017.
 */

public class ArticleKeyboardActivity extends ArticleGroupActivity {
    private static final String LOG_TAG = "_ArticleKeyboardActivit";

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void createGroups(final JsonNode keyboardList, final JsonNode articleList) throws Exception {
        final int foundationId = nemopaySession.getFoundationId();
        final JsonNode authorizedList = config.getGroupList();

        for (final JsonNode article : articleList) {
            if (!article.has("id") || !article.has("price") || !article.has("name") || !article.has("active") || !article.has("cotisant") || !article.has("alcool") || !article.has("categorie_id") || !article.has("image_url") || !article.has("fundation_id") || article.get("fundation_id").intValue() != foundationId)
                throw new Exception("Unexpected JSON");
        }

        for (JsonNode keyboard : keyboardList) {
            ArrayNode articlesForThisKeyboard = new ObjectMapper().createArrayNode();

            if (!keyboard.has("id") || !keyboard.has("name") || !keyboard.has("fun_id") || keyboard.get("fun_id").intValue() != foundationId || !keyboard.has("data") || !keyboard.get("data").has("items") || !keyboard.get("data").get("items").isArray() || !keyboard.get("data").has("nbColumns"))
                throw new Exception("Unexpected JSON");

            if (config.getFoundationId() != -1) if (!authorizedList.has(Integer.toString(keyboard.get("id").intValue())))
                continue;
            else if (keyboard.get("data").get("items").size() == 0)
                continue;

            for (JsonNode article : keyboard.get("data").get("items")) {
                if (article.has("itm_id")) {
                    boolean in = false;
                    for (JsonNode articleInList : articleList) {
                        if (articleInList.get("id").intValue() == article.get("itm_id").intValue()) {
                            articlesForThisKeyboard.add(articleInList);
                            in = true;
                            break;
                        }
                    }

                    if (!in)
                        articlesForThisKeyboard.add(new ObjectMapper().createObjectNode());
                }
                else if (config.getInGrid())
                    articlesForThisKeyboard.add(new ObjectMapper().createObjectNode());
            }

            createNewGroup(keyboard.get("name").textValue(), articlesForThisKeyboard, keyboard.get("data").get("nbColumns").isInt() ? keyboard.get("data").get("nbColumns").intValue() : Integer.valueOf(keyboard.get("data").get("nbColumns").textValue()));
        }
    }
}
