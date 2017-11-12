package fr.utc.simde.jessy.tools;

import android.content.SharedPreferences;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Created by Samy on 04/11/2017.
 */

public class Config {
    private static final String LOG_TAG = "_Config";

    private SharedPreferences sharedPreferences;

    private String foundationName;
    private Integer foundationId;
    private JsonNode groupList;
    private ArrayNode optionList;

    private Boolean canCancel;
    private Boolean canSell;
    private Boolean inKeyboard;
    private Boolean inGrid;
    private Boolean printCotisant;
    private Boolean print18;

    public Config(final SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;

        this.foundationName = sharedPreferences.getString("config_foundation_name", "");
        this.foundationId = sharedPreferences.getInt("config_foundation_id", -1);

        try {
            this.groupList = new ObjectMapper().readTree(sharedPreferences.getString("config_group_list", "{}"));
        } catch (Exception e) {
            Log.e(LOG_TAG, "error: " + e.getMessage());
        }

        try {
            this.optionList = (ArrayNode) new ObjectMapper().readTree(sharedPreferences.getString("config_option_list", "[]"));
        } catch (Exception e) {
            Log.e(LOG_TAG, "error: " + e.getMessage());
        }

        this.canCancel = sharedPreferences.getBoolean("config_can_cancel", true);
        this.canSell = sharedPreferences.getBoolean("config_can_sell", true);
        this.inKeyboard = sharedPreferences.getBoolean("config_in_keyboard", true);
        this.inGrid = sharedPreferences.getBoolean("config_in_grid", true);
        this.printCotisant = sharedPreferences.getBoolean("config_print_cotisant", false);
        this.print18 = sharedPreferences.getBoolean("config_print_18", false);
    }

    public Integer getFoundationId() { return this.foundationId; }
    public String getFoundationName() { return this.foundationName; }
    public void setFoundation(final int foundationId, final String foundationName) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("config_foundation_id", foundationId);
        editor.putString("config_foundation_name", foundationName);
        editor.apply();

        this.foundationId = foundationId;
        this.foundationName = foundationName;
    }

    public JsonNode getGroupList() { return this.groupList; }
    public void setGroupList(final JsonNode groupList) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("config_group_list", groupList.toString());
        editor.apply();

        this.groupList = groupList;
    }

    public ArrayNode getOptionList() { return this.optionList; }
    public void setOptionList(final ArrayNode optionList) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("config_option_list", optionList.toString());
        editor.apply();

        this.optionList = optionList;
    }

    public Boolean getCanCancel() { return this.canCancel; }
    public void setCanCancel(final Boolean canCancel) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("config_can_cancel", canCancel);
        editor.apply();

        this.canCancel = canCancel;
    }

    public Boolean getCanSell() { return this.canSell; }
    public void setCanSell(final Boolean canSell) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("config_can_sell", canSell);
        editor.apply();

        this.canSell = canSell;
    }

    public Boolean getInKeyboard() { return this.inKeyboard; }
    public void setInKeyboard(final Boolean inKeyboard) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("config_in_keyboard", inKeyboard);
        editor.apply();

        this.inKeyboard = inKeyboard;
    }

    public Boolean getInGrid() { return this.inGrid; }
    public void setInGrid(final Boolean inGrid) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("config_in_grid", inGrid);
        editor.apply();

        this.inGrid = inGrid;
    }

    public Boolean getPrintCotisant() { return this.printCotisant; }
    public void setPrintCotisant(final Boolean printCotisant) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("config_print_cotisant", printCotisant);
        editor.apply();

        this.printCotisant = printCotisant;
    }

    public Boolean getPrint18() { return this.print18; }
    public void setPrint18(final Boolean print18) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("config_print_18", print18);
        editor.apply();

        this.print18 = print18;
    }
}