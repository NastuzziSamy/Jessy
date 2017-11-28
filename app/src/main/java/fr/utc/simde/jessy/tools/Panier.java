package fr.utc.simde.jessy.tools;

import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Samy on 05/11/2017.
 */

public class Panier {
    private int totalPrice;
    private List<List<Integer>> articleList = new ArrayList<List<Integer>>();

    private TextView textView;

    public Panier(TextView textView) {
        this.totalPrice = 0;
        this.textView = textView;

        setText();
    }

    public void setText() {
        if (this.articleList.size() == 0)
            this.textView.setText("Panier vide");
        else
            this.textView.setText("Total: " + String.format("%.2f", new Float(totalPrice) / 100.00f) + "€");
    }

    public List<List<Integer>> getArticleList() { return this.articleList; }

    public void addArticle(final int id, final int price) { addArticle(id, price, 1); }
    public void addArticle(final int id, final int price, final int quantity) {
        this.totalPrice += price * quantity;

        for (int i = 0; i < this.articleList.size(); i++) {
            if (this.articleList.get(i).get(0) == id) {
                this.articleList.get(i).set(1, this.articleList.get(i).get(1) + quantity);

                setText();
                return;
            }
        }

        this.articleList.add(new ArrayList<Integer>() {{
            add(id);
            add(quantity);
        }});

        setText();
    }

    public void clear() {
        this.articleList.clear();

        this.totalPrice = 0;
        setText();
    }

    public Boolean isEmpty() {
        return this.articleList.isEmpty();
    }
}