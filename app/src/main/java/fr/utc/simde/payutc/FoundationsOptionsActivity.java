package fr.utc.simde.payutc;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import fr.utc.simde.payutc.adapters.FoundationsAdapter;
import fr.utc.simde.payutc.adapters.ListAdapater;

/**
 * Created by Samy on 26/10/2017.
 */

public class FoundationsOptionsActivity extends BaseActivity {
    private static final String LOG_TAG = "_FoundationsOptionsActivity";

    TabHost tabHost;
    ListView foundationList;
    ListView optionList;

    FoundationsAdapter foundationsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_foundations_options);

        this.tabHost = findViewById(R.id.tabs_foundations_options);
        this.foundationList = findViewById(R.id.list_foundations);
        this.optionList = findViewById(R.id.list_options);

        this.tabHost.setup();
        this.tabHost.addTab(this.tabHost.newTabSpec(getString(R.string.foundations)).setIndicator(getString(R.string.foundations)).setContent(R.id.list_foundations));
        this.tabHost.addTab(this.tabHost.newTabSpec(getString(R.string.options)).setIndicator(getString(R.string.options)).setContent(R.id.list_options));

        try {
            setFoundationList((ArrayNode) new ObjectMapper().readTree(getIntent().getExtras().getString("foundationList")));
            setOptionList();
        } catch (Exception e) {
            Log.wtf(LOG_TAG, "error: " + e.getMessage());
            dialog.errorDialog(this, getResources().getString(R.string.information_collection), getResources().getString(R.string.error_unexpected), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int id) {
                    finish();
                }
            });
        }
    }

    @Override
    protected void onIdentification(final String badgeId) {}

    protected void setFoundationList(final ArrayNode foundationList) throws Exception {
        this.foundationsAdapter = new FoundationsAdapter(FoundationsOptionsActivity.this, foundationList);

        this.foundationList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                startArticlesActivity(FoundationsOptionsActivity.this, foundationsAdapter.getFoundationId(position), foundationsAdapter.getFoundationName(position));
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

    protected void setOptionList() {
        this.optionList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                if (position == 2)
                    startReadCardInfoActivity(FoundationsOptionsActivity.this);
                else if (position == 6)
                    keyNemopayDialog();
                else if (position == 7)
                    keyGingerDialog();
                else
                    dialog.infoDialog(FoundationsOptionsActivity.this, "Non encore fait", "A faire");
            }
        });
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
}
