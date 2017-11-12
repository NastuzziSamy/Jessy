package fr.utc.simde.jessy;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.utc.simde.jessy.tools.CASConnexion;
import fr.utc.simde.jessy.tools.Config;
import fr.utc.simde.jessy.tools.Dialog;
import fr.utc.simde.jessy.tools.Ginger;
import fr.utc.simde.jessy.tools.HTTPRequest;
import fr.utc.simde.jessy.tools.NemopaySession;

/**
 * Created by Samy on 26/10/2017.
 */

public abstract class BaseActivity extends NFCActivity {
    private static final String LOG_TAG = "_BaseActivity";

    protected static NemopaySession nemopaySession;
    protected static Ginger ginger;
    protected static CASConnexion casConnexion;
    protected static Config config;

    protected static Dialog dialog;

    protected static SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dialog = new Dialog(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        dialog.dismiss();
    }

    protected void disconnect() {
        nemopaySession.disconnect();
        casConnexion.disconnect();
    }

    protected void unregister(final Activity activity) {
        nemopaySession.unregister();
        disconnect();

        dialog.errorDialog(activity, getString(R.string.key_registration), getString(R.string.key_remove_temp));
    }

    protected void fatal(final Activity activity, final String title, final String message) {
        dialog.fatalDialog(activity, title, message, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                unregister(activity);
                startMainActivity(activity);
            }
        });
    }

    protected void hasRights(final String titre, final String[] rightList, final Runnable runnable) {
        dialog.startLoading(BaseActivity.this, getString(R.string.information_collection), getString(R.string.user_rights_list_collecting));
        new Thread() {
            @Override
            public void run() {
                try {
                    List<String> rights = Arrays.asList(rightList);
                    List<String> sameRights = new ArrayList<String>();
                    nemopaySession.getAllMyRights();
                    Thread.sleep(100);

                    JsonNode myRightList = nemopaySession.getRequest().getJSONResponse();

                    if (myRightList.has("0")) {
                        for (JsonNode myRight : myRightList.get("0")) {
                            if (rights.contains(myRight.textValue()))
                                sameRights.add(myRight.textValue());
                        }
                    }

                    if (myRightList.has(String.valueOf(nemopaySession.getFoundationId()))) {
                        for (JsonNode myRight : myRightList.get(String.valueOf(nemopaySession.getFoundationId()))) {
                            if (rights.contains(myRight.textValue()))
                                sameRights.add(myRight.textValue());
                        }
                    }

                    if ((rights.size() == sameRights.size() && rights.size() != 0) || (rights.size() == 0 && myRightList.has("0") && myRightList.get("0").size() > 75)) // Si on a plus de 75 droits sur toutes les fondations, on estime qu'on a le full access
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.stopLoading();
                                runnable.run();
                            }
                        });
                    else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.stopLoading();
                                dialog.errorDialog(BaseActivity.this, titre, nemopaySession.forbidden(rightList));
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.stopLoading();
                            dialog.errorDialog(BaseActivity.this, getString(R.string.user_rights_list_collecting), getString(R.string.error_rights));
                        }
                    });
                }

            }
        }.start();
    }

    protected void startMainActivity(final Activity activity) {
        disconnect();

        Intent intent = new Intent(activity, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }

    protected void startFoundationListActivity(final Activity activity) {
        if (config.getFoundationId() != -1) {
            startArticleGroupActivity(activity);
            return;
        }

        dialog.startLoading(activity, getString(R.string.information_collection), getString(R.string.foundation_list_collecting));
        final Intent intent = new Intent(activity, FoundationsOptionsActivity.class);

        new Thread() {
            @Override
            public void run() {
                try {
                    nemopaySession.getFoundations();
                    Thread.sleep(100);

                    // Toute une série de vérifications avant de lancer l'activité
                    final HTTPRequest request = nemopaySession.getRequest();
                    final JsonNode foundationList = request.getJSONResponse();

                    if (!request.isJSONResponse() || !foundationList.isArray())
                        throw new Exception("Malformed JSON");

                    for (final JsonNode foundation : foundationList) {
                        if (!foundation.has("name") || !foundation.has("fun_id"))
                            throw new Exception("Unexpected JSON");
                    }

                    intent.putExtra("foundationList", request.getResponse());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.stopLoading();

                            if (activity.getClass().getSimpleName().equals("FoundationsOptionsActivity"))
                                finish();

                            activity.startActivity(intent);
                        }
                    });
                } catch (final Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fatal(activity, getString(R.string.foundation_list_collecting), e.getMessage());
                        }
                    });
                }
            }
        }.start();
    }

    protected void startArticleGroupActivity(final Activity activity) {
        dialog.startLoading(activity, activity.getResources().getString(R.string.information_collection), getString(R.string.location_list_collecting));
        final Intent intent = new Intent(activity, ArticleGroupActivity.class);

        new Thread() {
            @Override
            public void run() {
                try {
                    if (config.getLocationId() == -1) {
                        intent.putExtra("categoryListAuthorized", new ArrayList<Integer>());
                        intent.putExtra("keyboardListAuthorized", new ArrayList<Integer>());
                    }
                    else {
                        nemopaySession.getLocations();
                        Thread.sleep(100);

                        // Toute une série de vérifications avant de lancer l'activité
                        final HTTPRequest request = nemopaySession.getRequest();
                        final JsonNode locationList = request.getJSONResponse();

                        if (locationList == null || locationList.size() == 0) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.stopLoading();

                                    fatal(activity, getString(R.string.information_collection), nemopaySession.getFoundationName() + " " + getString(R.string.location_error_0));
                                }
                            });

                            return;
                        }

                        if (!locationList.isArray())
                            throw new Exception("Malformed JSON");

                        ArrayList<Integer> categoryListAuthorized = new ArrayList<Integer>();
                        ArrayList<Integer> keyboardListAuthorized = new ArrayList<Integer>();
                        for (final JsonNode location : locationList) {
                            if (!location.has("id") || !location.has("name") || !location.has("enabled") || !location.has("categories") || !location.has("sales_keyboards"))
                                throw new Exception("Unexpected JSON");

                            if (location.get("id").intValue() == config.getLocationId()) {
                                for (JsonNode category : location.get("categories"))
                                    categoryListAuthorized.add(category.intValue());

                                for (JsonNode keyboard : location.get("sales_keyboards"))
                                    keyboardListAuthorized.add(keyboard.intValue());
                            }
                        }

                        intent.putIntegerArrayListExtra("categoryListAuthorized", categoryListAuthorized);
                        intent.putIntegerArrayListExtra("keyboardListAuthorized", keyboardListAuthorized);
                    }
                } catch (final Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fatal(activity, getString(config.getInCategory() ? R.string.category_list_collecting : R.string.keyboard_list_collecting), e.getMessage());
                        }
                    });
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.changeLoading(getString(config.getInCategory() ? R.string.category_list_collecting : R.string.keyboard_list_collecting));
                    }
                });

                try {
                    if (config.getInCategory())
                        nemopaySession.getCategories();
                    else
                        nemopaySession.getKeyboards();
                    Thread.sleep(100);

                    // Toute une série de vérifications avant de lancer l'activité
                    final HTTPRequest request = nemopaySession.getRequest();
                    final JsonNode groupList = request.getJSONResponse();

                    if (!groupList.isArray())
                        throw new Exception("Malformed JSON");

                    if (groupList.size() == 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.stopLoading();

                                dialog.errorDialog(activity, getString(R.string.information_collection), nemopaySession.getFoundationName() + " " + getString(config.getInCategory() ? R.string.category_error_0 : R.string.keyboard_error_0));
                            }
                        });

                        return;
                    }

                    intent.putExtra(config.getInCategory() ? "categoryList" : "keyboardList", request.getResponse());
                } catch (final Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fatal(activity, getString(config.getInCategory() ? R.string.category_list_collecting : R.string.keyboard_list_collecting), e.getMessage());
                        }
                    });
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.changeLoading(getResources().getString(R.string.article_list_collecting));
                    }
                });

                try {
                    int responseCode = nemopaySession.getArticles();
                    Thread.sleep(100);

                    // Toute une série de vérifications avant de lancer l'activité
                    final HTTPRequest request = nemopaySession.getRequest();
                    final JsonNode articleList = request.getJSONResponse();

                    if (!request.isJSONResponse() || !articleList.isArray())
                        throw new Exception("Malformed JSON");

                    if (articleList.size() == 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.stopLoading();
                                dialog.errorDialog(activity, getString(R.string.information_collection), nemopaySession.getFoundationName() + " " + getString(R.string.article_error_0));
                            }
                        });

                        return;
                    }

                    for (final JsonNode article : articleList) {
                        if (!article.has("id") || !article.has("price") || !article.has("name") || !article.has("active") || !article.has("cotisant") || !article.has("alcool") || !article.has("categorie_id") || !article.has("image_url") || !article.has("fundation_id") || article.get("fundation_id").intValue() != nemopaySession.getFoundationId())
                            throw new Exception("Unexpected JSON");
                    }

                    intent.putExtra("articleList", request.getResponse());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.stopLoading();

                            if (activity.getClass().getSimpleName().equals("ArticleKeyboardActivity") || activity.getClass().getSimpleName().equals("ArticleCategoryActivity"))
                                finish();

                            activity.startActivity(intent);
                        }
                    });
                } catch (final Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fatal(activity, getString(R.string.article_list_collecting), e.getMessage());
                        }
                    });
                }
            }
        }.start();
    }

    protected void startBuyerInfoActivity(final Activity activity, final String badgeId) {
        dialog.startLoading(activity, activity.getResources().getString(R.string.information_collection), activity.getResources().getString(R.string.buyer_info_collecting));
        final Intent intent = new Intent(activity, BuyerInfoActivity.class);

        new Thread() {
            @Override
            public void run() {
                try {
                    nemopaySession.getBuyerInfo(badgeId);
                    Thread.sleep(100);

                    // Toute une série de vérifications avant de lancer l'activité
                    final HTTPRequest request = nemopaySession.getRequest();
                    final JsonNode buyerInfo = request.getJSONResponse();

                    if (!request.isJSONResponse())
                        throw new Exception("Malformed JSON");

                    if (!buyerInfo.has("lastname") || !buyerInfo.has("username") || !buyerInfo.has("firstname") || !buyerInfo.has("solde") || !buyerInfo.has("last_purchases") || !buyerInfo.get("last_purchases").isArray())
                        throw new Exception("Unexpected JSON");

                    intent.putExtra("badgeId", badgeId);
                    intent.putExtra("buyerInfo", request.getResponse());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.stopLoading();

                            if (activity.getClass().getSimpleName().equals("BuyerInfoActivity"))
                                finish();

                            activity.startActivity(intent);
                        }
                    });
                } catch (final Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());

                    try {
                        if (nemopaySession.getRequest().getResponseCode() == 400)
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.errorDialog(activity, getString(R.string.information_collection), getString(R.string.badge_error_not_recognized));
                                }
                            });
                        else
                            throw new Exception("");
                    } catch (Exception e1) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                fatal(activity, getString(R.string.information_collection), e.getMessage());
                            }
                        });
                    }
                }
            }
        }.start();
    }

    protected void startCardManagementActivity(final Activity activity) {
        hasRights(getString(R.string.user_rights_list_collecting), new String[]{"STAFF", "POSS3", "GESUSERS"}, new Runnable() {
            @Override
            public void run() {
            activity.startActivity(new Intent(activity, CardManagementActivity.class));
            }
        });
    }

    protected void delKey() {
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.remove("key");
        edit.apply();

        unregister(BaseActivity.this);
    }

    protected void setNemopayKey(final String key) {
        dialog.startLoading(BaseActivity.this, getString(R.string.nemopay_connection), getString(R.string.nemopay_authentification));
        new Thread() {
            @Override
            public void run() {
                try {
                    nemopaySession.loginApp(key, casConnexion);
                    Thread.sleep(100);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.stopLoading();

                            if (nemopaySession.isRegistered()) {
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("key", key);
                                editor.apply();

                                TextView textView = findViewById(R.id.text_app_registered);
                                if (textView != null)
                                    textView.setText(nemopaySession.getName().substring(0, nemopaySession.getName().length() - (nemopaySession.getName().matches("^.* - ([0-9]{4})([/-])([0-9]{2})\\2([0-9]{2})$") ? 13 : 0)));
                            }
                            else
                                dialog.errorDialog(BaseActivity.this, getString(R.string.nemopay_connection), getString(R.string.nemopay_error_registering));
                        }
                    });
                } catch (Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.errorDialog(BaseActivity.this, getString(R.string.nemopay_connection), getString(R.string.nemopay_error_registering));
                        }
                    });
                }
            }
        }.start();
    }

    protected void setGingerKey(final String key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("key_ginger", key);
        editor.apply();

        ginger.setKey(key);
    }
}
