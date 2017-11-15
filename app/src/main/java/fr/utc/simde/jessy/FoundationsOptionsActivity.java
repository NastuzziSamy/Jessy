package fr.utc.simde.jessy;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.utc.simde.jessy.adapters.FoundationsAdapter;
import fr.utc.simde.jessy.adapters.OptionChoicesAdapter;
import fr.utc.simde.jessy.adapters.OptionsAdapter;

/**
 * Created by Samy on 26/10/2017.
 */

public class FoundationsOptionsActivity extends BaseActivity {
    private static final String LOG_TAG = "_FoundationsOptionsActi";

    TabHost tabHost;
    ListView foundationList;
    ListView optionList;

    FoundationsAdapter foundationsAdapter;
    OptionsAdapter optionsAdapter;

    List<String> allOptionList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_foundations_options);

        this.tabHost = findViewById(R.id.tabs_foundations_options);
        this.foundationList = findViewById(R.id.list_foundations);
        this.optionList = findViewById(R.id.list_options);

        this.tabHost.setup();

        try {
            ArrayNode foundationList = (ArrayNode) new ObjectMapper().readTree(getIntent().getExtras().getString("foundationList"));

            if (config.getCanSell() && foundationList.size() != 0)
                this.tabHost.addTab(this.tabHost.newTabSpec(getString(R.string.foundations)).setIndicator(getString(R.string.foundations)).setContent(R.id.list_foundations));
            this.tabHost.addTab(this.tabHost.newTabSpec(getString(R.string.options)).setIndicator(getString(R.string.options)).setContent(R.id.list_options));

            this.allOptionList = Arrays.asList(getResources().getStringArray(R.array.options));

            if (config.getCanSell() && foundationList.size() != 0)
                setFoundationList(foundationList);

            setOptionList(config.getOptionList().size() == 0 ? (ArrayNode) new ObjectMapper().valueToTree(this.allOptionList) : config.getOptionList());
        } catch (Exception e) {
            Log.wtf(LOG_TAG, "error: " + e.getMessage());
            fatal(this, getResources().getString(R.string.information_collection), getResources().getString(R.string.error_view));
        }
    }

    @Override
    protected void onIdentification(final String badgeId) {}

    protected void setFoundationList(final ArrayNode foundationList) throws Exception {
        this.foundationsAdapter = new FoundationsAdapter(FoundationsOptionsActivity.this, foundationList);

        this.foundationList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                nemopaySession.setFoundation(foundationsAdapter.getFoundationId(position), foundationsAdapter.getFoundationName(position), -1);
                startArticleGroupActivity(FoundationsOptionsActivity.this);
            }
        });

        this.foundationList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                foundationsAdapter.toast(position);

                return true;
            }
        });

        this.foundationList.setAdapter(this.foundationsAdapter);
    }

    protected boolean isOption(final int position, final int option) {
        return optionsAdapter.getOptionName(position).equals(this.allOptionList.get(option));
    }

    protected void setOptionList(ArrayNode optionList) throws Exception {
        ArrayNode optionListAdded = (ArrayNode) new ObjectMapper().readTree(optionList.toString());
        optionListAdded.add(getString(R.string.configurate));
        this.optionsAdapter = new OptionsAdapter(FoundationsOptionsActivity.this, optionListAdded);

        this.optionList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                if (isOption(position,0))
                    dialog.infoDialog(FoundationsOptionsActivity.this, "Non encore fait", "Pour la version 0.9");
                else if (isOption(position,1))
                    dialog.infoDialog(FoundationsOptionsActivity.this, "Non encore fait", "Pour la version 0.11");
                else if (isOption(position,2))
                    dialog.infoDialog(FoundationsOptionsActivity.this, "Non encore fait", "Pour la version 0.12");
                else if (isOption(position,1))
                    dialog.infoDialog(FoundationsOptionsActivity.this, "Non encore fait", "Pour la version 0.12");
                else if (isOption(position,2))
                    dialog.infoDialog(FoundationsOptionsActivity.this, "Non encore fait", "Pour la version 0.10");
                else if (isOption(position,5))
                    startCardManagementActivity(FoundationsOptionsActivity.this);
                else if (isOption(position,6))
                    keyNemopayDialog();
                else if (isOption(position,7))
                    keyGingerDialog();
                else
                    configDialog();
            }
        });

        this.optionList.setAdapter(this.optionsAdapter);
    }

    protected void keyNemopayDialog() {
        hasRights(getString(R.string.nemopay), new String[]{}, new Runnable(){
            @Override
            public void run() {
                final View keyView = getLayoutInflater().inflate(R.layout.dialog_key_force, null);
                final EditText keyInput = keyView.findViewById(R.id.input_key);

                final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(FoundationsOptionsActivity.this);
                alertDialogBuilder
                        .setTitle(getString(R.string.key_registration) + " " + getString(R.string.nemopay))
                        .setView(keyView)
                        .setCancelable(false)
                        .setPositiveButton(R.string.register, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int id) {
                                if (!keyInput.getText().toString().equals(""))
                                    setNemopayKey(keyInput.getText().toString());
                            }
                        })
                        .setNegativeButton(R.string.cancel, null);

                dialog.createDialog(alertDialogBuilder, keyInput);
            }
        });
    }

    protected void keyGingerDialog() {
        hasRights(getString(R.string.ginger), new String[]{}, new Runnable(){
            @Override
            public void run() {
                final View keyView = getLayoutInflater().inflate(R.layout.dialog_key_force, null);
                final EditText keyInput = keyView.findViewById(R.id.input_key);

                final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(FoundationsOptionsActivity.this);
                alertDialogBuilder
                        .setTitle(getString(R.string.key_registration) + " " + getString(R.string.ginger))
                        .setView(keyView)
                        .setCancelable(false)
                        .setPositiveButton(R.string.register, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int id) {
                                if (!keyInput.getText().toString().equals(""))
                                    setGingerKey(keyInput.getText().toString());
                            }
                        })
                        .setNegativeButton(R.string.cancel, null);

                dialog.createDialog(alertDialogBuilder, keyInput);
            }
        });
    }

    protected void configDialog() {
        if (config.getFoundationId() == -1 && config.getOptionList().size() == 0) {
            final View popupView = LayoutInflater.from(FoundationsOptionsActivity.this).inflate(R.layout.dialog_config, null, false);
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

            configButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    hasRights(getString(R.string.configuration), new String[]{
                            "STAFF",
                            "GESAPPLICATIONS"
                    }, new Runnable(){
                        @Override
                        public void run() {
                            final LayoutInflater layoutInflater = LayoutInflater.from(FoundationsOptionsActivity.this);
                            final View popupView = layoutInflater.inflate(R.layout.dialog_group, null);
                            final ListView listView = popupView.findViewById(R.id.list_groups);
                            final Switch canSellSwitch = popupView.findViewById(R.id.switch_cancel);
                            ((TextView) popupView.findViewById(R.id.text_to_print)).setText(R.string.option_list);
                            canSellSwitch.setText(R.string.can_sell);

                            OptionChoicesAdapter allOptionsAdapter = null;
                            try {
                                allOptionsAdapter = new OptionChoicesAdapter(FoundationsOptionsActivity.this, (ArrayNode) new ObjectMapper().valueToTree(allOptionList));

                                listView.setAdapter(allOptionsAdapter);
                            } catch (Exception e) {
                                Log.e(LOG_TAG, "error: " + e.getMessage());
                                fatal(FoundationsOptionsActivity.this, getResources().getString(R.string.information_collection), getResources().getString(R.string.error_view));
                            }

                            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(FoundationsOptionsActivity.this);
                            final OptionChoicesAdapter finalAllOptionsAdapter = allOptionsAdapter;
                            alertDialogBuilder
                                    .setTitle(R.string.configuration)
                                    .setView(popupView)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.applicate, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialogInterface, int id) {
                                            config.setCanSell(canSellSwitch.isChecked());
                                            ArrayNode optionList = finalAllOptionsAdapter.getList();

                                            if (optionList == null || optionList.size() == 0) {
                                                Toast.makeText(FoundationsOptionsActivity.this, getString(R.string.option_0_selected), Toast.LENGTH_LONG).show();
                                                configDialog();
                                            }
                                            else {
                                                config.setOptionList(optionList);
                                                startMainActivity(FoundationsOptionsActivity.this);
                                            }
                                        }
                                    })
                                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialogInterface, int id) {
                                            config.setCanSell(true);
                                        }
                                    });

                            dialog.createDialog(alertDialogBuilder);
                        }
                    });
                }
            });

            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(FoundationsOptionsActivity.this);
            alertDialogBuilder
                    .setTitle(R.string.configuration)
                    .setView(popupView)
                    .setCancelable(false)
                    .setPositiveButton(R.string.applicate, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int id) {
                            config.setInCategory(radioCategory.isChecked());
                            config.setInGrid(radioGrid.isChecked());
                            config.setPrintCotisant(switchCotisant.isChecked());
                            config.setPrint18(swtich18.isChecked());
                        }
                    })
                    .setNegativeButton(R.string.cancel, null);

            dialog.createDialog(alertDialogBuilder);
        }
        else {
            final View popupView = LayoutInflater.from(FoundationsOptionsActivity.this).inflate(R.layout.dialog_config_restore, null, false);
            final Switch switchCotisant = popupView.findViewById(R.id.swtich_cotisant);
            final Switch swtich18 = popupView.findViewById(R.id.swtich_18);
            final Button configButton = popupView.findViewById(R.id.button_config);

            switchCotisant.setChecked(config.getPrintCotisant());
            swtich18.setChecked(config.getPrint18());

            configButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    hasRights(getString(R.string.configurate_by_default), new String[]{
                            "STAFF",
                            "GESAPPLICATIONS"
                    }, new Runnable() {
                        @Override
                        public void run() {
                            config.setOptionList(new ObjectMapper().createArrayNode());
                            config.setCanSell(true);

                            startMainActivity(FoundationsOptionsActivity.this);
                        }
                    });
                }
            });

            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(FoundationsOptionsActivity.this);
            alertDialogBuilder
                    .setTitle(R.string.configuration)
                    .setView(popupView)
                    .setCancelable(false)
                    .setPositiveButton(R.string.applicate, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int id) {
                            config.setPrintCotisant(switchCotisant.isChecked());
                            config.setPrint18(swtich18.isChecked());

                            startArticleGroupActivity(FoundationsOptionsActivity.this);
                        }
                    })
                    .setNegativeButton(R.string.cancel, null);

            dialog.createDialog(alertDialogBuilder);
        }
    }
}