package fr.utc.simde.jessy;

import android.content.DialogInterface;
import android.os.Vibrator;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Toast;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.zxing.Result;

import java.io.IOException;
import java.util.List;

import fr.utc.simde.jessy.adapters.GridAdapter;
import fr.utc.simde.jessy.adapters.ListAdapater;
import fr.utc.simde.jessy.responses.ArticleResponse;
import fr.utc.simde.jessy.responses.BottomatikResponse;
import fr.utc.simde.jessy.responses.GingerResponse;
import fr.utc.simde.jessy.responses.QRCodeResponse;

public class SellByQRCodeActivity extends QRCodeReaderActivity {
    private static final String LOG_TAG = "_SellByQRCodeActivity";

    @Override
    public void handleResult(Result result) {
        if (result.getBarcodeFormat().toString().equals("QR_CODE")) {
            try {
                handleQRCode(new ObjectMapper().readValue(result.getText(), QRCodeResponse.class));
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());

                dialog.infoDialog(SellByQRCodeActivity.this, getString(R.string.qrcode_reading), e.getMessage(), new DialogInterface.OnClickListener() {
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
            dialog.startLoading(SellByQRCodeActivity.this, getString(R.string.qrcode_reading), getString(R.string.user_ginger_info_collecting));
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
                                dialog.infoDialog(SellByQRCodeActivity.this, getString(R.string.qrcode_reading), e.getMessage(), new DialogInterface.OnClickListener() {
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
                    }
                    catch (final Exception e) {
                        Log.e(LOG_TAG, e.getMessage());

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.infoDialog(SellByQRCodeActivity.this, getString(R.string.qrcode_reading), e.getMessage(), new DialogInterface.OnClickListener() {
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
                                articleIdList.remove(articleIdList.indexOf(articleResponse.getId()));

                                if (articleIdList.size() == 0)
                                    break;
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
                                dialog.infoDialog(SellByQRCodeActivity.this, getString(R.string.qrcode_reading), e.getMessage(), new DialogInterface.OnClickListener() {
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
                        nemopaySession.setTransaction(gingerResponse.getBadgeId(), bottomatikResponse.getArticleList());
                        Thread.sleep(100);

                        final BottomatikResponse finalBottomatikResponse = bottomatikResponse;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.stopLoading();
                                Toast.makeText(SellByQRCodeActivity.this, getString(R.string.transaction_realized), Toast.LENGTH_LONG).show();
                                ((Vibrator) getSystemService(SellByQRCodeActivity.VIBRATOR_SERVICE)).vibrate(250);

                                final LayoutInflater layoutInflater = LayoutInflater.from(SellByQRCodeActivity.this);
                                final View popupView = layoutInflater.inflate(R.layout.dialog_list, null);
                                final ListView listView = popupView.findViewById(R.id.list_groups);

                                try {
                                    listView.setAdapter(new ListAdapater(SellByQRCodeActivity.this, purchaseList, config.getPrintCotisant(), config.getPrint18()));
                                } catch (Exception e) {
                                    Log.e(LOG_TAG, "error: " + e.getMessage());

                                    fatal(SellByQRCodeActivity.this, getString(R.string.qrcode_reading), e.getMessage());
                                }

                                final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(SellByQRCodeActivity.this);
                                alertDialogBuilder
                                        .setTitle(R.string.panier)
                                        .setView(popupView)
                                        .setCancelable(false)
                                        .setPositiveButton(R.string.validate, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int which) {
                                                dialog.startLoading(SellByQRCodeActivity.this, getResources().getString(R.string.paiement), getResources().getString(R.string.transaction_in_validation));

                                                new Thread() {
                                                    @Override
                                                    public void run() {
                                                        try {
                                                            bottomatik.setTransaction(finalBottomatikResponse.getId(), true, true);
                                                            Thread.sleep(100);

                                                            runOnUiThread(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    dialog.stopLoading();
                                                                    Toast.makeText(SellByQRCodeActivity.this, getString(R.string.transaction_validated), Toast.LENGTH_LONG).show();

                                                                    resumeReading();
                                                                }
                                                            });
                                                        } catch (final Exception e) {
                                                            Log.e(LOG_TAG, "error: " + e.getMessage());

                                                            dialog.errorDialog(SellByQRCodeActivity.this, getString(R.string.qrcode_reading), e.getMessage());

                                                            resumeReading();
                                                        }
                                                    }
                                                }.start();
                                            }
                                        })
                                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int which) {
                                                dialog.startLoading(SellByQRCodeActivity.this, getResources().getString(R.string.qrcode_reading), getResources().getString(R.string.transaction_in_cancelation));

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
                                                                    Toast.makeText(SellByQRCodeActivity.this, getString(R.string.transaction_refunded), Toast.LENGTH_LONG).show();
                                                                    ((Vibrator) getSystemService(ArticleGroupActivity.VIBRATOR_SERVICE)).vibrate(250);

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
                                                                            dialog.errorDialog(SellByQRCodeActivity.this, getString(R.string.qrcode_reading), response.get("error").get("message").textValue());
                                                                            ((Vibrator) getSystemService(ArticleGroupActivity.VIBRATOR_SERVICE)).vibrate(500);

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
                                                                        dialog.errorDialog(SellByQRCodeActivity.this, getString(R.string.qrcode_reading), e.getMessage());
                                                                        ((Vibrator) getSystemService(ArticleGroupActivity.VIBRATOR_SERVICE)).vibrate(500);

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

                                        dialog.errorDialog(SellByQRCodeActivity.this, getString(R.string.paiement), response.get("error").get("message").textValue());
                                        ((Vibrator) getSystemService(SellByQRCodeActivity.VIBRATOR_SERVICE)).vibrate(500);

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
                                    dialog.errorDialog(SellByQRCodeActivity.this, getString(R.string.paiement), e.getMessage(), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            resumeReading();
                                        }
                                    });
                                    ((Vibrator) getSystemService(SellByQRCodeActivity.VIBRATOR_SERVICE)).vibrate(500);
                                }
                            });
                        }

                        return;
                    }
                }
            }.start();
        }
        catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());

            dialog.infoDialog(SellByQRCodeActivity.this, getString(R.string.qrcode_reading), e.getMessage(), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
        }
    }
}
