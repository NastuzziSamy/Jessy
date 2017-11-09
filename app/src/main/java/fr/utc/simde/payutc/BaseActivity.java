package fr.utc.simde.payutc;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.utc.simde.payutc.tools.CASConnexion;
import fr.utc.simde.payutc.tools.Config;
import fr.utc.simde.payutc.tools.Dialog;
import fr.utc.simde.payutc.tools.HTTPRequest;
import fr.utc.simde.payutc.tools.NemopaySession;

/**
 * Created by Samy on 26/10/2017.
 */

public abstract class BaseActivity extends NFCActivity {
    private static final String LOG_TAG = "_BaseActivity";

    protected static NemopaySession nemopaySession;
    protected static CASConnexion casConnexion;
    protected static Config config;

    protected static Dialog dialog;

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

                    if (rights.size() == sameRights.size())
                        runOnUiThread(runnable);
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
                    int responseCode = nemopaySession.getFoundations();
                    Thread.sleep(100);

                    // Toute une série de vérifications avant de lancer l'activité
                    final HTTPRequest request = nemopaySession.getRequest();
                    final JsonNode foundationList = request.getJSONResponse();

                    if (!request.isJSONResponse() || !foundationList.isArray())
                        throw new Exception("Malformed JSON");

                    if (foundationList.size() == 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.stopLoading();
                                fatal(activity, getString(R.string.information_collection), nemopaySession.getUsername() + " " + getString(R.string.user_no_rights));
                            }
                        });

                        return;
                    }

                    for (final JsonNode foundation : foundationList) {
                        if (!foundation.has("name") || !foundation.has("fun_id"))
                            throw new Exception("Unexpected JSON");
                    }

                    if (foundationList.size() == 1) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.stopLoading();
                                startArticlesActivity(activity, foundationList.get(0).get("fun_id").intValue(), foundationList.get(0).get("name").textValue());
                            }
                        });

                        return;
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
        dialog.startLoading(activity, activity.getResources().getString(R.string.information_collection), activity.getResources().getString(config.getInKeyboard() ? R.string.keyboard_list_collecting : R.string.category_list_collecting));
        final Intent intent = new Intent(activity, config.getInKeyboard() ? ArticleKeyboardActivity.class : ArticleCategoryActivity.class);

        new Thread() {
            @Override
            public void run() {
                try {
                    if (config.getInKeyboard())
                        nemopaySession.getKeyboards();
                    else
                        nemopaySession.getCategories();
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

                                dialog.errorDialog(activity, getString(R.string.information_collection), nemopaySession.getFoundationName() + " " + getString(config.getInKeyboard() ? R.string.keyboard_error_0 : R.string.category_error_0));
                            }
                        });

                        return;
                    }

                    intent.putExtra("groupList", request.getResponse());
                } catch (final Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fatal(activity, getString(config.getInKeyboard() ? R.string.keyboard_list_collecting : R.string.category_list_collecting), e.getMessage());
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

    protected void startArticlesActivity(final Activity activity, final int foundationId, final String foundationName) {
        nemopaySession.setFoundation(foundationId, foundationName);
        Log.d(LOG_TAG, String.valueOf(foundationId));

        startArticleGroupActivity(activity);
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
}
