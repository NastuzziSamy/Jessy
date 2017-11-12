package fr.utc.simde.jessy.tools;

import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Samy on 05/11/2017.
 */

public class Panier {
    private int totalPrice;
    private List<Integer> articleList = new ArrayList<Integer>();

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

    public List<Integer> getArticleList() { return this.articleList; }

    public void addArticle(final int id, final int price) {
        this.articleList.add(id);
        this.totalPrice += price;

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