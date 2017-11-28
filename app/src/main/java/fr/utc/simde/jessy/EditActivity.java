package fr.utc.simde.jessy;

import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.List;

import fr.utc.simde.jessy.fragments.ArticleGroupFragment;
import fr.utc.simde.jessy.fragments.EditFragment;
import fr.utc.simde.jessy.fragments.SellFragment;
import fr.utc.simde.jessy.tools.Panier;

/**
 * Created by Samy on 20/11/2017.
 */

public class EditActivity extends ArticleGroupActivity {
    private static final String LOG_TAG = "_EditActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        generate();
    }

    @Override
    protected void setOptionButton() {
        this.optionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final View popupView = LayoutInflater.from(EditActivity.this).inflate(R.layout.dialog_config, null, false);
                final RadioButton radioKeyboard = popupView.findViewById(R.id.radio_keyboard);
                final RadioButton radioCategory = popupView.findViewById(R.id.radio_category);
                final RadioButton radioGrid = popupView.findViewById(R.id.radio_grid);
                final RadioButton radioList = popupView.findViewById(R.id.radio_list);
                final Switch switchCotisant = popupView.findViewById(R.id.swtich_cotisant);
                final Switch swtich18 = popupView.findViewById(R.id.swtich_18);
                final Button configButton = popupView.findViewById(R.id.button_config);

                if (config.getInCategory())
                    radioCategory.setChecked(true);
                else
                    radioKeyboard.setChecked(true);

                if (config.getInGrid())
                    radioGrid.setChecked(true);
                else
                    radioList.setChecked(true);

                switchCotisant.setChecked(config.getPrintCotisant());
                swtich18.setChecked(config.getPrint18());

                configButton.setText("Edit le nom");
                configButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        hasRights(getString(R.string.configurate_by_default), new String[]{
                                "STAFF",
                                "GESAPPLICATIONS"
                        }, new Runnable() {
                            @Override
                            public void run() {
                                configDialog();
                            }
                        });
                    }
                });

                final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(EditActivity.this);
                alertDialogBuilder
                        .setTitle(R.string.configuration)
                        .setView(popupView)
                        .setCancelable(false)
                        .setPositiveButton(R.string.reload, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int id) {
                                config.setInCategory(radioCategory.isChecked());
                                config.setInGrid(radioGrid.isChecked());
                                config.setPrintCotisant(switchCotisant.isChecked());
                                config.setPrint18(swtich18.isChecked());

                                startSellActivity(EditActivity.this);
                            }
                        })
                        .setNegativeButton(R.string.cancel, null);

                dialog.createDialog(alertDialogBuilder);
            }
        });
    }

    @Override
    protected void createNewGroup(final String name, final ArrayNode articleList) throws Exception { createNewGroup(name, articleList, 3); }
    protected void createNewGroup(final String name, final ArrayNode articleList, int gridColumns) throws Exception {
        ArticleGroupFragment articleGroupFragment = new EditFragment(EditActivity.this, articleList, this.dialog, this.config, gridColumns);

        TabHost.TabSpec newTabSpec = this.tabHost.newTabSpec(name);
        newTabSpec.setIndicator(name);
        newTabSpec.setContent(articleGroupFragment);

        this.groupFragmentList.add(articleGroupFragment);

        this.tabHost.addTab(newTabSpec);
        nbrGroups++;
    }
}
