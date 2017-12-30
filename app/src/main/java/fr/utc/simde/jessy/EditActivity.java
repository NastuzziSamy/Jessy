package fr.utc.simde.jessy;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

import fr.utc.simde.jessy.adapters.ArticlesAdapter;
import fr.utc.simde.jessy.fragments.ArticleGroupFragment;
import fr.utc.simde.jessy.fragments.EditFragment;

/**
 * Created by Samy on 20/11/2017.
 */

public class EditActivity extends ArticleGroupActivity {
    private static final String LOG_TAG = "_EditActivity";

    protected List<Integer> groupIdList;
    protected List<String> groupNameList;
    protected Integer currentId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.groupIdList = new ArrayList<Integer>();
        this.groupNameList = new ArrayList<String>();
        this.currentId = 0;

        generate();

        this.tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabName) {
                if (groupNameList.contains(tabName))
                    currentId = groupNameList.indexOf(tabName);
            }
        });

        final TabHost.TabSpec newTabSpec = this.tabHost.newTabSpec("JESPERE QUE PERSONNE AURA LIDEE DE METTRE CE NOM");
        newTabSpec.setIndicator("+ " + getString(R.string.category_add));
        newTabSpec.setContent(new TabHost.TabContentFactory() {
            @Override
            public View createTabContent(String tag) {
                return new View(EditActivity.this);
            }
        });

        this.tabHost.addTab(newTabSpec);
        this.tabHost.getTabWidget().getChildAt(nbrGroups).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final View keyView = getLayoutInflater().inflate(R.layout.dialog_category, null);
                final EditText nameInput = keyView.findViewById(R.id.input_name);

                final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(EditActivity.this);
                alertDialogBuilder
                        .setTitle(getString(R.string.category_add))
                        .setView(keyView)
                        .setCancelable(false)
                        .setPositiveButton(R.string.register, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int idClick) {
                                if (!nameInput.getText().toString().equals("")) {
                                    dialog.startLoading(EditActivity.this, getString(R.string.category_add), getString(R.string.category_adding));

                                    new Thread() {
                                        @Override
                                        public void run() {
                                            try {
                                                nemopaySession.setCategory(nameInput.getText().toString());
                                                Thread.sleep(100);

                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        dialog.stopLoading();

                                                        startEditActivity(EditActivity.this);
                                                    }
                                                });
                                            } catch (final Exception e) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Log.e(LOG_TAG, "error: " + e.getMessage());
                                                        dialog.errorDialog(EditActivity.this, getString(R.string.category_add), e.getMessage());
                                                    }
                                                });
                                            }
                                        }
                                    }.start();
                                }
                            }
                        })
                        .setNegativeButton(R.string.cancel, null);

                dialog.createDialog(alertDialogBuilder, nameInput);
            }
        });
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

                configButton.setVisibility(View.GONE);

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

                                startEditActivity(EditActivity.this);
                            }
                        })
                        .setNegativeButton(R.string.cancel, null);

                dialog.createDialog(alertDialogBuilder);
            }
        });
    }

    @Override
    protected void setDeleteButton() {
        this.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.choiceDialog(EditActivity.this, getString(R.string.category_delete), getString(R.string.category_delete_confirmation) + " " + groupNameList.get(currentId) + " ?", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        dialog.startLoading(EditActivity.this, getString(R.string.category_delete), getString(R.string.category_deleting));

                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    nemopaySession.delCategory(groupIdList.get(currentId));
                                    Thread.sleep(100);

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            dialog.stopLoading();

                                            startEditActivity(EditActivity.this);
                                        }
                                    });
                                } catch (final Exception e) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Log.e(LOG_TAG, "error: " + e.getMessage());
                                            dialog.errorDialog(EditActivity.this, getString(R.string.category_delete), e.getMessage());
                                        }
                                    });
                                }
                            }
                        }.start();
                    }
                }, null);
            }
        });
    }

    @Override
    protected void createNewGroup(final String name, final Integer id, final ArrayNode articleList) throws Exception { createNewGroup(name, id, articleList, 3); }
    protected void createNewGroup(final String name, final Integer id, final ArrayNode articleList, int gridColumns) throws Exception {
        ObjectNode editArticle = new ObjectMapper().createObjectNode();
        editArticle.put("id", -1);
        editArticle.put("name", getString(R.string.article_add_one));
        editArticle.put("fundation_id", nemopaySession.getFoundationId());
        editArticle.put("categorie_id", id);
        editArticle.put("image_url", "https://icon-icons.com/icons2/933/PNG/512/rounded-add-button_icon-icons.com_72592.png");
        editArticle.put("price", 0);
        editArticle.put("variable_price", false);
        editArticle.put("cotisant", false);
        editArticle.put("alcool", false);
        editArticle.put("active", true);

        articleList.add(editArticle);
        Log.d(LOG_TAG, articleList.toString());

        final ArticleGroupFragment articleGroupFragment = new EditFragment(EditActivity.this, articleList, this.dialog, this.config, gridColumns);

        final TabHost.TabSpec newTabSpec = this.tabHost.newTabSpec(name);
        newTabSpec.setIndicator(name);
        newTabSpec.setContent(articleGroupFragment);

        this.groupFragmentList.add(articleGroupFragment);

        this.tabHost.addTab(newTabSpec);

        this.groupIdList.add(id);
        this.groupNameList.add(name);

        this.tabHost.getTabWidget().getChildAt(nbrGroups++).setOnLongClickListener(new TabHost.OnLongClickListener() {
            public boolean onLongClick(View v) {
                setCategory(id, name);
                return false;
            }
        });
    }

    protected void setCategory(final int id, final String name) {
        final View keyView = getLayoutInflater().inflate(R.layout.dialog_category, null);
        final EditText nameInput = keyView.findViewById(R.id.input_name);
        nameInput.setText(name);

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(EditActivity.this);
        alertDialogBuilder
                .setTitle(getString(R.string.category_edit))
                .setView(keyView)
                .setCancelable(false)
                .setPositiveButton(R.string.register, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int idClick) {
                        if (!nameInput.getText().toString().equals("")) {
                            dialog.startLoading(EditActivity.this, getString(R.string.category_edit), getString(R.string.category_editing));

                            new Thread() {
                                @Override
                                public void run() {
                                    try {
                                        nemopaySession.setCategory(id, nameInput.getText().toString());
                                        Thread.sleep(100);

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                dialog.stopLoading();

                                                startEditActivity(EditActivity.this);
                                            }
                                        });
                                    } catch (final Exception e) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Log.e(LOG_TAG, "error: " + e.getMessage());
                                                dialog.errorDialog(EditActivity.this, getString(R.string.category_edit), e.getMessage());
                                            }
                                        });
                                    }
                                }
                            }.start();
                        }
                        else
                            setCategory(id, name);
                    }
                })
                .setNegativeButton(R.string.cancel, null);

        dialog.createDialog(alertDialogBuilder, nameInput);
    }

    public void setArticle(final ArticlesAdapter articlesAdapter, final int position) {
        InputFilter twoDecimalsOnly = new InputFilter() {
            final int maxDigitsBeforeDecimalPoint = 3;
            final int maxDigitsAfterDecimalPoint = 2;

            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                StringBuilder builder = new StringBuilder(dest);
                builder.replace(dstart, dend, source
                        .subSequence(start, end).toString());
                if (!builder.toString().matches("((([1-9]{1})([0-9]{0,"+(maxDigitsBeforeDecimalPoint-1)+"})?)|0)?(\\.[0-9]{0,"+maxDigitsAfterDecimalPoint+"})?")) {
                    if (source.length() == 0)
                        return dest.subSequence(dstart, dend);
                    return "";
                }

                return null;
            }
        };

        if (position == (articlesAdapter.getCount() - 1)) {
            final View popupView = LayoutInflater.from(EditActivity.this).inflate(R.layout.dialog_edit_article, null, false);
            final EditText nameInput = popupView.findViewById(R.id.input_name);
            final EditText urlInput = popupView.findViewById(R.id.input_url);
            final RadioButton radioVariablePrice = popupView.findViewById(R.id.radio_variable_price);
            final EditText priceInput = popupView.findViewById(R.id.input_price);
            final Switch switchCotisant = popupView.findViewById(R.id.swtich_cotisant);
            final Switch swtich18 = popupView.findViewById(R.id.swtich_18);
            final Button deleteButton = popupView.findViewById(R.id.button_delete);

            deleteButton.setVisibility(View.GONE);

            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(EditActivity.this);
            alertDialogBuilder
                    .setTitle(R.string.configuration)
                    .setView(popupView)
                    .setCancelable(false)
                    .setPositiveButton(R.string.reload, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int id) {
                            if (!nameInput.getText().toString().equals("") && (!radioVariablePrice.isChecked() || priceInput.getText().toString().equals(""))) {
                                dialog.startLoading(EditActivity.this, getString(R.string.article_add), getString(R.string.article_adding));

                                new Thread() {
                                    @Override
                                    public void run() {
                                        try {
                                            nemopaySession.setArticle(nameInput.getText().toString(), urlInput.getText().toString(), groupIdList.get(currentId), radioVariablePrice.isChecked() ? 1 : Math.round(Float.parseFloat(priceInput.getText().toString()) * 100.0f), swtich18.isChecked(), switchCotisant.isChecked(), radioVariablePrice.isChecked());
                                            Thread.sleep(100);

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    dialog.stopLoading();

                                                    startEditActivity(EditActivity.this);
                                                }
                                            });
                                        } catch (final Exception e) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Log.e(LOG_TAG, "error: " + e.getMessage());
                                                    dialog.errorDialog(EditActivity.this, getString(R.string.article_add), e.getMessage());
                                                }
                                            });
                                        }
                                    }
                                }.start();
                            }
                            else
                                setArticle(articlesAdapter, position);
                        }
                    })
                    .setNegativeButton(R.string.cancel, null);

            dialog.createDialog(alertDialogBuilder);
        }
        else {
            final JsonNode article = articlesAdapter.getArticle(position);
            final View popupView = LayoutInflater.from(EditActivity.this).inflate(R.layout.dialog_edit_article, null, false);
            final EditText nameInput = popupView.findViewById(R.id.input_name);
            final EditText urlInput = popupView.findViewById(R.id.input_url);
            final RadioButton radioVariablePrice = popupView.findViewById(R.id.radio_variable_price);
            final EditText priceInput = popupView.findViewById(R.id.input_price);
            final Switch switchCotisant = popupView.findViewById(R.id.swtich_cotisant);
            final Switch swtich18 = popupView.findViewById(R.id.swtich_18);
            final Button deleteButton = popupView.findViewById(R.id.button_delete);

            nameInput.setText(article.get("name").textValue());
            urlInput.setText(article.get("image_url").textValue());
            priceInput.setFilters(new InputFilter[] { twoDecimalsOnly });
            switchCotisant.setChecked(article.get("cotisant").booleanValue());
            swtich18.setChecked(article.get("alcool").booleanValue());

            if (article.get("variable_price").booleanValue())
                radioVariablePrice.setChecked(true);
            else
                priceInput.setText(String.format("%.2f", article.get("price").intValue() / 100.00f).replace(",", "."));

            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.choiceDialog(EditActivity.this, getString(R.string.article_delete), getString(R.string.article_delete_confirmation) + " " + article.get("name").textValue() + " ?", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int which) {
                            dialog.startLoading(EditActivity.this, getString(R.string.article_delete), getString(R.string.article_deleting));

                            new Thread() {
                                @Override
                                public void run() {
                                    try {
                                        nemopaySession.delArticle(article.get("id").intValue());
                                        Thread.sleep(100);

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                dialog.stopLoading();

                                                startEditActivity(EditActivity.this);
                                            }
                                        });
                                    } catch (final Exception e) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Log.e(LOG_TAG, "error: " + e.getMessage());
                                                dialog.errorDialog(EditActivity.this, getString(R.string.article_delete), e.getMessage());
                                            }
                                        });
                                    }
                                }
                            }.start();
                        }
                    }, null);
                }
            });

            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(EditActivity.this);
            alertDialogBuilder
                    .setTitle(R.string.configuration)
                    .setView(popupView)
                    .setCancelable(false)
                    .setPositiveButton(R.string.register, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int id) {
                            if (!nameInput.getText().toString().equals("") && (radioVariablePrice.isChecked() || !priceInput.getText().toString().equals(""))) {
                                dialog.startLoading(EditActivity.this, getString(R.string.article_edit), getString(R.string.article_editing));

                                new Thread() {
                                    @Override
                                    public void run() {
                                        try {
                                            nemopaySession.setArticle(articlesAdapter.getArticle(position).get("id").intValue(), nameInput.getText().toString(), urlInput.getText().toString(), groupIdList.get(currentId), radioVariablePrice.isChecked() ? 1 : Math.round(Float.parseFloat(priceInput.getText().toString()) * 100f), swtich18.isChecked(), switchCotisant.isChecked(), radioVariablePrice.isChecked());
                                            Thread.sleep(100);

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    dialog.stopLoading();

                                                    startEditActivity(EditActivity.this);
                                                }
                                            });
                                        } catch (final Exception e) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Log.e(LOG_TAG, "error: " + e.getMessage());
                                                    dialog.errorDialog(EditActivity.this, getString(R.string.article_edit), e.getMessage());
                                                }
                                            });
                                        }
                                    }
                                }.start();
                            }
                            else
                                setArticle(articlesAdapter, position);
                        }
                    })
                    .setNegativeButton(R.string.cancel, null);

            dialog.createDialog(alertDialogBuilder);
        }
    }
}
