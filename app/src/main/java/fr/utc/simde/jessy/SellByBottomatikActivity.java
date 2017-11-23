package fr.utc.simde.jessy;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Vibrator;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.zxing.Result;

import java.util.List;

import fr.utc.simde.jessy.adapters.ListAdapater;
import fr.utc.simde.jessy.responses.ArticleResponse;
import fr.utc.simde.jessy.responses.BottomatikResponse;
import fr.utc.simde.jessy.responses.GingerResponse;
import fr.utc.simde.jessy.responses.QRCodeResponse;

public class SellByBottomatikActivity extends QRCodeReaderActivity {
    private static final String LOG_TAG = "_SellByQRCodeActivity";

    @Override
    public void handleResult(final Result result) {
        if (result.getBarcodeFormat().toString().equals("QR_CODE")) {
            try {
                handleQRCode(new ObjectMapper().readValue(result.getText(), QRCodeResponse.class));
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());

                dialog.infoDialog(SellByBottomatikActivity.this, getString(R.string.qrcode_reading), e.getMessage(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
            }
        }
        else
            resumeReading();
    }

    protected void handleQRCode(final QRCodeResponse qrCodeResponse) {
        try {
            dialog.startLoading(SellByBottomatikActivity.this, getString(R.string.qrcode_reading), getString(R.string.user_ginger_info_collecting));

            new Thread() {
                @Override
                public void run() {
                    GingerResponse gingerResponse = null;
                    try {
                        ginger.getInfo(qrCodeResponse.getUsername());
                        Thread.sleep(100);

                        gingerResponse = new ObjectMapper().readValue(ginger.getRequest().getResponse(), GingerResponse.class);
                    }
                    catch (final Exception e) {
                        Log.e(LOG_TAG, e.getMessage());

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.infoDialog(SellByBottomatikActivity.this, getString(R.string.qrcode_reading), e.getMessage(), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        resumeReading();
                                    }
                                });
                            }
                        });

                        return;
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.changeLoading(getString(R.string.bottomatik_get_transaction));
                        }
                    });

                    BottomatikResponse bottomatikResponse = null;
                    try {
                        bottomatik.getTransactionFromId(qrCodeResponse.getId());
                        Thread.sleep(100);

                        bottomatikResponse = new ObjectMapper().readValue(bottomatik.getRequest().getResponse(), BottomatikResponse.class);

                        if (bottomatikResponse.getFoundationId() != nemopaySession.getFoundationId())
                            throw new Exception(getString(R.string.can_not_sell_other_foundation));

                        if (bottomatikResponse.isValidated())
                            throw new Exception(getString(R.string.bottomatik_already_validated));
                    }
                    catch (final Exception e) {
                        Log.e(LOG_TAG, e.getMessage());

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.infoDialog(SellByBottomatikActivity.this, getString(R.string.qrcode_reading), e.getMessage(), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        resumeReading();
                                    }
                                });
                            }
                        });

                        return;
                    }

                    pay(bottomatikResponse, gingerResponse.getBadgeId());
                }
            }.start();
        }
        catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());

            dialog.infoDialog(SellByBottomatikActivity.this, getString(R.string.qrcode_reading), e.getMessage(), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
        }
    }

    @Override
    public void onIdentification(final String badgeId) {

        dialog.startLoading(SellByBottomatikActivity.this, getString(R.string.badge_read), getString(R.string.user_ginger_info_collecting));

        new Thread() {
            @Override
            public void run() {
                try {
                    nemopaySession.getBuyerInfo(badgeId);
                    Thread.sleep(100);

                    // Toute une série de vérifications avant de lancer l'activité
                    final JsonNode buyerInfo = nemopaySession.getRequest().getJSONResponse();

                    if (!buyerInfo.has("lastname") || !buyerInfo.has("username") || !buyerInfo.has("firstname") || !buyerInfo.has("solde") || !buyerInfo.has("last_purchases") || !buyerInfo.get("last_purchases").isArray())
                        throw new Exception("Unexpected JSON");
                } catch (final Exception e) {
                    Log.e(LOG_TAG, "error: " + e.getMessage());

                    try {
                        if (nemopaySession.getRequest().getResponseCode() == 400)
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.errorDialog(SellByBottomatikActivity.this, getString(R.string.information_collection), getString(R.string.badge_error_not_recognized));
                                }
                            });
                        else
                            throw new Exception("");

                        return;
                    } catch (Exception e1) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                fatal(SellByBottomatikActivity.this, getString(R.string.information_collection), e.getMessage());
                            }
               });
                }
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog.changeLoading(getString(R.string.bottomatik_get_transaction));
                }
            });

                try {
                bottomatik.getTransactionFromUsername(nemopaySession.getRequest().getJSONResponse().get("username").textValue());
                Thread.sleep(100);

                JsonNode response = new ObjectMapper().readTree(bottomatik.getRequest().getResponse());

                if (response.get("transactions").size() == 0)
                    throw new Exception(getString(R.string.transaction_no));
                else {
                    final BottomatikResponse bottomatikResponse = new ObjectMapper().readValue(response.get("transactions").get(response.get("transactions").size() - 1).toString(), BottomatikResponse.class);

                    if (bottomatikResponse.getFoundationId() != nemopaySession.getFoundationId())
                        throw new Exception(getString(R.string.can_not_sell_other_foundation));

                    if (bottomatikResponse.isValidated())
                        throw new Exception(getString(R.string.bottomatik_already_validated));

                    pay(bottomatikResponse, badgeId);
                }
            }
                catch (final Exception e) {
                Log.e(LOG_TAG, e.getMessage());

                runOnUiThread(new Runnable() {
                    @Override
                        public void run() {
                            dialog.infoDialog(SellByBottomatikActivity.this, getString(R.string.information_collection), e.getMessage(), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    resumeReading();
                                }
                            });
                        }
                    });

                    return;
                }
            }
        }.start();
    }

    public void pay(final BottomatikResponse bottomatikResponse, final String badgeId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.changeLoading(getString(R.string.article_list_collecting));
            }
        });

        List<ArticleResponse> articleResponseList = null;
        List<Integer> articleIdList = bottomatikResponse.getArticleList();
        JsonNode articleList = null;
        final ArrayNode purchaseList = new ObjectMapper().createArrayNode();
        try {
            nemopaySession.getArticles();
            Thread.sleep(100);

            TypeReference<List<ArticleResponse>> articleListType = new TypeReference<List<ArticleResponse>>(){};
            articleResponseList = new ObjectMapper().readValue(nemopaySession.getRequest().getResponse(), articleListType);
            articleList = nemopaySession.getRequest().getJSONResponse();

            int i = 0;
            for (ArticleResponse articleResponse : articleResponseList) {
                if (articleIdList.contains(articleResponse.getId())) {
                    purchaseList.add(articleList.get(i));

                    int j = 0;
                    int quantity = 0;
                    for (Integer articleId : articleIdList) {
                        if (articleId == articleResponse.getId()) {
                            articleIdList.remove(j);
                            quantity++;
                        }

                        j++;
                    }

                    ((ObjectNode) purchaseList.get(purchaseList.size() - 1)).put("quantity", quantity);
                }

                i++;
            }

            if (articleIdList.size() != 0)
                throw new Exception(getString(R.string.article_not_available));
        }
        catch (final Exception e) {
            Log.e(LOG_TAG, e.getMessage());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog.infoDialog(SellByBottomatikActivity.this, getString(R.string.qrcode_reading), e.getMessage(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            resumeReading();
                        }
                    });
                }
            });

            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.changeLoading(getString(R.string.transaction_in_progress));
            }
        });

        try {
            if (!bottomatikResponse.isPaid()) {
                nemopaySession.setTransaction(badgeId, bottomatikResponse.getArticleList());
                Thread.sleep(100);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog.stopLoading();
                    Toast.makeText(SellByBottomatikActivity.this, getString(R.string.transaction_realized), Toast.LENGTH_LONG).show();
                    ((Vibrator) getSystemService(SellByBottomatikActivity.VIBRATOR_SERVICE)).vibrate(250);

                    final LayoutInflater layoutInflater = LayoutInflater.from(SellByBottomatikActivity.this);
                    final View popupView = layoutInflater.inflate(R.layout.dialog_list, null);
                    final ListView listView = popupView.findViewById(R.id.list_groups);

                    try {
                        listView.setAdapter(new ListAdapater(SellByBottomatikActivity.this, purchaseList, config.getPrintCotisant(), config.getPrint18()));
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "error: " + e.getMessage());

                        fatal(SellByBottomatikActivity.this, getString(R.string.qrcode_reading), e.getMessage());
                    }

                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(SellByBottomatikActivity.this);
                    alertDialogBuilder
                            .setTitle(R.string.panier)
                            .setView(popupView)
                            .setCancelable(false)
                            .setPositiveButton(R.string.validate, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int which) {
                                    dialog.startLoading(SellByBottomatikActivity.this, getResources().getString(R.string.paiement), getResources().getString(R.string.transaction_in_validation));

                                    new Thread() {
                                        @Override
                                        public void run() {
                                            try {
                                                bottomatik.setTransaction(bottomatikResponse.getId(), true, true);
                                                Thread.sleep(100);

                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        dialog.stopLoading();
                                                        Toast.makeText(SellByBottomatikActivity.this, getString(R.string.transaction_validated), Toast.LENGTH_LONG).show();

                                                        resumeReading();
                                                    }
                                                });
                                            } catch (final Exception e) {
                                                Log.e(LOG_TAG, "error: " + e.getMessage());

                                                dialog.errorDialog(SellByBottomatikActivity.this, getString(R.string.qrcode_reading), e.getMessage());

                                                resumeReading();
                                            }
                                        }
                                    }.start();
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int which) {
                                    dialog.startLoading(SellByBottomatikActivity.this, getResources().getString(R.string.qrcode_reading), getResources().getString(R.string.transaction_in_cancelation));

                                    new Thread() {
                                        @Override
                                        public void run() {
                                            try {
                                                nemopaySession.cancelTransaction(nemopaySession.getRequest().getJSONResponse().get("transaction_id").intValue());
                                                Thread.sleep(100);

                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        dialog.stopLoading();
                                                        Toast.makeText(SellByBottomatikActivity.this, getString(R.string.transaction_refunded), Toast.LENGTH_LONG).show();
                                                        ((Vibrator) getSystemService(SellByBottomatikActivity.VIBRATOR_SERVICE)).vibrate(250);

                                                        resumeReading();
                                                    }
                                                });
                                            } catch (final Exception e) {
                                                Log.e(LOG_TAG, "error: " + e.getMessage());

                                                try {
                                                    final JsonNode response = nemopaySession.getRequest().getJSONResponse();

                                                    if (response.has("error") && response.get("error").has("message")) {
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                dialog.stopLoading();
                                                                dialog.errorDialog(SellByBottomatikActivity.this, getString(R.string.qrcode_reading), response.get("error").get("message").textValue());
                                                                ((Vibrator) getSystemService(SellByBottomatikActivity.VIBRATOR_SERVICE)).vibrate(500);

                                                                resumeReading();
                                                            }
                                                        });
                                                    }
                                                    else
                                                        throw new Exception("");
                                                } catch (Exception e1) {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            dialog.stopLoading();
                                                            dialog.errorDialog(SellByBottomatikActivity.this, getString(R.string.qrcode_reading), e.getMessage());
                                                            ((Vibrator) getSystemService(SellByBottomatikActivity.VIBRATOR_SERVICE)).vibrate(500);

                                                            resumeReading();
                                                        }
                                                    });
                                                }
                                            }
                                        }
                                    }.start();
                                }
                            });

                    dialog.createDialog(alertDialogBuilder);
                }
            });
        } catch (final Exception e) {
            Log.e(LOG_TAG, "error: " + e.getMessage());

            try {
                final JsonNode response = nemopaySession.getRequest().getJSONResponse();

                if (response.has("error") && response.get("error").has("message")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.stopLoading();

                            dialog.errorDialog(SellByBottomatikActivity.this, getString(R.string.paiement), response.get("error").get("message").textValue());
                            ((Vibrator) getSystemService(SellByBottomatikActivity.VIBRATOR_SERVICE)).vibrate(500);

                            resumeReading();
                        }
                    });
                }
                else
                    throw new Exception("");
            } catch (Exception e1) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.stopLoading();
                        dialog.errorDialog(SellByBottomatikActivity.this, getString(R.string.paiement), e.getMessage(), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                resumeReading();
                            }
                        });
                        ((Vibrator) getSystemService(SellByBottomatikActivity.VIBRATOR_SERVICE)).vibrate(500);
                    }
                });
            }

            return;
        }
    }
}
