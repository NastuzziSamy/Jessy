package fr.utc.simde.jessy;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AlertDialog;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.zxing.Result;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.utc.simde.jessy.adapters.ListAdapater;
import fr.utc.simde.jessy.responses.APIResponse;
import fr.utc.simde.jessy.responses.ArticleResponse;
import fr.utc.simde.jessy.responses.BottomatikResponse;
import fr.utc.simde.jessy.responses.GingerResponse;
import fr.utc.simde.jessy.responses.QRCodeResponse;
import fr.utc.simde.jessy.tools.API;
import fr.utc.simde.jessy.tools.ExtendedScannerView;
import me.dm7.barcodescanner.core.IViewFinder;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

import static android.graphics.Typeface.BOLD;
import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;

/**
 * Created by Samy on 18/11/2017.
 */

public class QRCodeReaderActivity extends BaseActivity implements ZXingScannerView.ResultHandler {
    private static final String LOG_TAG = "_QRCodeReaderActivity";

    protected ZXingScannerView scannerView;

    protected String badgeId;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.scannerView = new ZXingScannerView(QRCodeReaderActivity.this) {
            @Override
            protected IViewFinder createViewFinderView(Context context) {
                return new ExtendedScannerView(context);
            }
        };

        this.scannerView.setResultHandler(QRCodeReaderActivity.this);
        this.scannerView.startCamera(CAMERA_FACING_BACK);
        this.scannerView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                final LayoutInflater layoutInflater = LayoutInflater.from(QRCodeReaderActivity.this);
                final View popupView = layoutInflater.inflate(R.layout.dialog_tag, null);
                final EditText inputApi = popupView.findViewById(R.id.input_api);
                final EditText inputInfo = popupView.findViewById(R.id.input_info);
                final RadioButton buttonTag = popupView.findViewById(R.id.radio_tag);
                inputApi.setText(config.getCurrentApi());

                final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(QRCodeReaderActivity.this);
                alertDialogBuilder
                        .setTitle(R.string.getting_informations_from)
                        .setView(popupView)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Map<String, String> apiInfo = config.getApi(inputApi.getText().toString());

                                        if (apiInfo == null) {
                                            Log.e(LOG_TAG, getString(R.string.api_not_recognized));

                                            dialog.infoDialog(QRCodeReaderActivity.this, getString(R.string.badge_read), getString(R.string.api_not_recognized));
                                        } else
                                            handleAPI(buttonTag.isChecked() ? inputInfo.getText().toString().toUpperCase() : inputInfo.getText().toString(), apiInfo, null, null, buttonTag.isChecked());
                                    }
                                }).start();
                            }
                        })
                        .setCancelable(true);

                dialog.createDialog(alertDialogBuilder, inputInfo);

                return false;
            }
        });

        setContentView(this.scannerView);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!dialog.isShowing())
            resumeReading();
    }

    @Override
    public void onIdentification(final String badgeId) {
        final Map<String, String> apiInfo = config.getApi(config.getCurrentApi());

        new Thread() {
            @Override
            public void run() {
                try {
                    ginger.getInfoFromBadge(badgeId);
                    Thread.sleep(100);

                    GingerResponse gingerResponse = new ObjectMapper().readValue(ginger.getRequest().getResponse(), GingerResponse.class);
                    handleAPI(gingerResponse.getLogin(), apiInfo, gingerResponse, null, false);
                } catch (final Exception e) {
                    Log.e(LOG_TAG, e.getMessage());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.infoDialog(QRCodeReaderActivity.this, getString(R.string.badge_error_not_recognized), e.getMessage(), new DialogInterface.OnClickListener() {
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

    protected void resumeReading() {
        scannerView.resumeCameraPreview(QRCodeReaderActivity.this);
    }

    public void handleResult(final Result result) {
        if (result.getBarcodeFormat().toString().equals("QR_CODE")) {
            try {
                Log.d(LOG_TAG, result.getText());
                QRCodeResponse qrCodeResponse = new ObjectMapper().readValue(result.getText(), QRCodeResponse.class);
                final Map<String, String> apiInfo = config.getApi(qrCodeResponse.getSystem());

                if (apiInfo == null)
                    dialog.infoDialog(QRCodeReaderActivity.this, result.getBarcodeFormat().toString() + ": " + getString(R.string.not_understood), result.getText(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            resumeReading();
                        }
                    });
                else
                    handleQRCode(qrCodeResponse, apiInfo);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());

                dialog.infoDialog(QRCodeReaderActivity.this, getString(R.string.qrcode_reading), e.getMessage(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
            }
        } else
            resumeReading();
    }

    protected void handleQRCode(final QRCodeResponse qrCodeResponse, final Map<String, String> apiInfo) {
        dialog.startLoading(QRCodeReaderActivity.this, getString(R.string.qrcode_reading), getString(R.string.user_ginger_info_collecting));

        new Thread() {
            @Override
            public void run() {
                GingerResponse gingerResponse = null;
                if (qrCodeResponse.getUsername() != null && !qrCodeResponse.getUsername().isEmpty()) {
                    try {
                        ginger.getInfo(qrCodeResponse.getUsername());
                        Thread.sleep(100);

                        gingerResponse = new ObjectMapper().readValue(ginger.getRequest().getResponse(), GingerResponse.class);
                    } catch (final Exception e) {
                        Log.e(LOG_TAG, e.getMessage());

                        if (ginger.getRequest().getResponseCode() != 404) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.infoDialog(QRCodeReaderActivity.this, getString(R.string.qrcode_reading), e.getMessage(), new DialogInterface.OnClickListener() {
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
                }

                handleAPI(qrCodeResponse.getId(), apiInfo, gingerResponse, qrCodeResponse, true);
            }
        }.start();
    }

    protected void handleAPI(final String info, final Map<String, String> apiInfo, final GingerResponse gingerResponse, final QRCodeResponse qrCodeResponse, final boolean byTag) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.changeLoading(getString(R.string.getting_informations_from) + " " + apiInfo.get("name"));
            }
        });

        final API api = new API(QRCodeReaderActivity.this, apiInfo.get("name"), apiInfo.get("url"));
        api.setKey(apiInfo.get("key"));

        APIResponse apiResponse;
        try {
            if (byTag)
                api.getInfosFromId(info);
            else
                api.getInfosFromUsername(info);
            Thread.sleep(100);

            apiResponse = new ObjectMapper().readValue(api.getRequest().getResponse(), apiInfo.get("api").equals("bottomatik") ? BottomatikResponse.class : APIResponse.class);

            if (api.getRequest().getJSONResponse().has("status") && api.getRequest().getJSONResponse().has("message"))
                throw new Exception(api.getRequest().getJSONResponse().get("message").textValue());
        } catch (final Exception e) {
            Log.e(LOG_TAG, e.getMessage());

            if (api.getRequest().getResponseCode() == 410) {
                try {
                    apiResponse = new ObjectMapper().readValue(api.getRequest().getResponse(), apiInfo.get("api").equals("bottomatik") ? BottomatikResponse.class : APIResponse.class);

                    if (api.getRequest().getJSONResponse().has("message"))
                        throw new Exception(api.getRequest().getJSONResponse().get("message").textValue());

                    final APIResponse finalApiResponse = apiResponse;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(QRCodeReaderActivity.this);
                            alertDialogBuilder
                                    .setTitle(getString(R.string.reservation_number) + finalApiResponse.getId())
                                    .setMessage(getString(R.string.ticket_validated) + " (" + DateUtils.formatDateTime(QRCodeReaderActivity.this, finalApiResponse.getExpirationDate() * 1000, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME) + ")")
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            resumeReading();
                                        }
                                    })
                                    .setNeutralButton(R.string.more, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finalApiResponse.removeExpirationDate();
                                            checkResult(api, finalApiResponse, apiInfo, gingerResponse, qrCodeResponse);
                                        }
                                    });

                            dialog.createDialog(alertDialogBuilder);
                            ((Vibrator) getSystemService(QRCodeReaderActivity.VIBRATOR_SERVICE)).vibrate(500);
                        }
                    });
                } catch (final Exception e1) {
                    Log.e(LOG_TAG, e.getMessage());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.infoDialog(QRCodeReaderActivity.this, getString(R.string.getting_informations_from) + " " + apiInfo.get("name"), e.getMessage(), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    resumeReading();
                                }
                            });
                        }
                    });
                }
            } else
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.infoDialog(QRCodeReaderActivity.this, getString(R.string.getting_informations_from) + " " + apiInfo.get("name"), e.getMessage(), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                resumeReading();
                            }
                        });
                    }
                });

            return;
        }

        checkResult(api, apiResponse, apiInfo, gingerResponse, qrCodeResponse);
    }

    protected void checkResult(final API api, final APIResponse apiResponse, final Map<String, String> apiInfo, final GingerResponse gingerResponse, final QRCodeResponse qrCodeResponse) {
        if (qrCodeResponse != null && (!apiResponse.getId().equals(qrCodeResponse.getId()) || !apiResponse.getUsername().equals(qrCodeResponse.getUsername()))) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(QRCodeReaderActivity.this);
                    alertDialogBuilder
                            .setTitle(getString(R.string.reservation_number) + apiResponse.getId())
                            .setMessage(getString(R.string.ticket_maybe_falsified) + "\n" +
                                    "QRCode:\n" +
                                    "   id: " + qrCodeResponse.getId() + "\n" +
                                    "   username: " + qrCodeResponse.getUsername() + "\n\n" +
                                    "API:\n" +
                                    "   id: " + apiResponse.getId() + "\n" +
                                    "   username: " + apiResponse.getUsername())
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    resumeReading();
                                }
                            })
                            .setNeutralButton(R.string.more, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    checkResult(api, apiResponse, apiInfo, gingerResponse, null);
                                }
                            });

                    dialog.createDialog(alertDialogBuilder);
                    ((Vibrator) getSystemService(QRCodeReaderActivity.VIBRATOR_SERVICE)).vibrate(500);
                }
            });

            return;
        }

        if (apiInfo.get("api").equals("bottomatik"))
            payWithBottomatik(api, (BottomatikResponse) apiResponse, gingerResponse);
        else
            checkInfo(api, apiResponse, gingerResponse);
    }

    public void payWithBottomatik(final API api, final BottomatikResponse bottomatikResponse, final GingerResponse gingerResponse) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.changeLoading(getString(R.string.article_list_collecting));
            }
        });

        List<ArticleResponse> articleResponseList = null;
        List<List<Integer>> articleIdList = bottomatikResponse.getArticleList();
        final ArrayNode purchaseList = new ObjectMapper().createArrayNode();
        try {
            if (nemopaySession.getFoundationId() != -1 && bottomatikResponse.getFunId() != nemopaySession.getFoundationId())
                throw new Exception(getString(R.string.can_not_sell_other_foundation));

            if (bottomatikResponse.isValidated())
                throw new Exception(getString(R.string.already_validated));

            nemopaySession.getArticles();
            Thread.sleep(100);

            TypeReference<List<ArticleResponse>> articleListType = new TypeReference<List<ArticleResponse>>() {
            };
            articleResponseList = new ObjectMapper().readValue(nemopaySession.getRequest().getResponse(), articleListType);
            JsonNode articleList = nemopaySession.getRequest().getJSONResponse();

            int id = 0;
            for (ArticleResponse articleResponse : articleResponseList) {
                for (int i = 0; i < articleIdList.size(); i++) {
                    if (articleIdList.get(i).get(0) == articleResponse.getId()) {
                        ((ObjectNode) articleList.get(id)).put("quantity", articleIdList.get(i).get(1));
                        articleIdList.remove(i);

                        purchaseList.add(articleList.get(id));
                        break;
                    }
                }

                id++;
            }

            if (articleIdList.size() != 0)
                throw new Exception(getString(R.string.article_not_available));
        } catch (final Exception e) {
            Log.e(LOG_TAG, e.getMessage());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog.infoDialog(QRCodeReaderActivity.this, getString(R.string.qrcode_reading), e.getMessage(), new DialogInterface.OnClickListener() {
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
                nemopaySession.setTransaction(gingerResponse.getBadge_uid(), bottomatikResponse.getArticleList(), bottomatikResponse.getFunId());
                Thread.sleep(100);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog.stopLoading();
                    Toast.makeText(QRCodeReaderActivity.this, getString(R.string.ticket_realized), Toast.LENGTH_LONG).show();
                    ((Vibrator) getSystemService(QRCodeReaderActivity.VIBRATOR_SERVICE)).vibrate(250);

                    final LayoutInflater layoutInflater = LayoutInflater.from(QRCodeReaderActivity.this);
                    final View popupView = layoutInflater.inflate(R.layout.dialog_list, null);
                    final ListView listView = popupView.findViewById(R.id.list_groups);

                    try {
                        listView.setAdapter(new ListAdapater(QRCodeReaderActivity.this, purchaseList, config.getPrintCotisant(), config.getPrint18()));
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "error: " + e.getMessage());

                        fatal(QRCodeReaderActivity.this, getString(R.string.qrcode_reading), e.getMessage());
                    }

                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(QRCodeReaderActivity.this);
                    alertDialogBuilder
                            .setTitle(R.string.panier)
                            .setView(popupView)
                            .setCancelable(false)
                            .setPositiveButton(R.string.validate, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int which) {
                                    dialog.startLoading(QRCodeReaderActivity.this, getResources().getString(R.string.paiement), getResources().getString(R.string.ticket_in_validation));

                                    new Thread() {
                                        @Override
                                        public void run() {
                                            try {
                                                api.interact(bottomatikResponse.getId(), "validate", new HashMap<String, Object>() {{
                                                    put("paid", true);
                                                    put("served", true);
                                                }});
                                                Thread.sleep(100);

                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        dialog.stopLoading();
                                                        Toast.makeText(QRCodeReaderActivity.this, getString(R.string.ticket_validated), Toast.LENGTH_LONG).show();

                                                        resumeReading();
                                                    }
                                                });
                                            } catch (final Exception e) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Log.e(LOG_TAG, "error: " + e.getMessage());
                                                        dialog.errorDialog(QRCodeReaderActivity.this, getString(R.string.qrcode_reading), e.getMessage());

                                                        resumeReading();
                                                    }
                                                });
                                            }
                                        }
                                    }.start();
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int which) {
                                    dialog.startLoading(QRCodeReaderActivity.this, getResources().getString(R.string.qrcode_reading), getResources().getString(R.string.ticket_in_cancelation));

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
                                                        Toast.makeText(QRCodeReaderActivity.this, getString(R.string.ticket_refunded), Toast.LENGTH_LONG).show();
                                                        ((Vibrator) getSystemService(QRCodeReaderActivity.VIBRATOR_SERVICE)).vibrate(250);

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
                                                                dialog.errorDialog(QRCodeReaderActivity.this, getString(R.string.qrcode_reading), response.get("error").get("message").textValue());
                                                                ((Vibrator) getSystemService(QRCodeReaderActivity.VIBRATOR_SERVICE)).vibrate(500);

                                                                resumeReading();
                                                            }
                                                        });
                                                    } else
                                                        throw new Exception("");
                                                } catch (Exception e1) {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            dialog.stopLoading();
                                                            dialog.errorDialog(QRCodeReaderActivity.this, getString(R.string.qrcode_reading), e.getMessage());
                                                            ((Vibrator) getSystemService(QRCodeReaderActivity.VIBRATOR_SERVICE)).vibrate(500);

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

                            dialog.errorDialog(QRCodeReaderActivity.this, getString(R.string.paiement), response.get("error").get("message").textValue());
                            ((Vibrator) getSystemService(QRCodeReaderActivity.VIBRATOR_SERVICE)).vibrate(500);

                            resumeReading();
                        }
                    });
                } else
                    throw new Exception("");
            } catch (Exception e1) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.stopLoading();
                        dialog.errorDialog(QRCodeReaderActivity.this, getString(R.string.paiement), e.getMessage(), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                resumeReading();
                            }
                        });
                        ((Vibrator) getSystemService(QRCodeReaderActivity.VIBRATOR_SERVICE)).vibrate(500);
                    }
                });
            }

            return;
        }
    }

    protected void checkInfo(final API api, final APIResponse apiResponse, final GingerResponse gingerResponse) {
        long currentTimestamp = (System.currentTimeMillis() / 1000);

        if (apiResponse.getCreationDate() != null && apiResponse.getCreationDate() != -1 && currentTimestamp < apiResponse.getCreationDate()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(QRCodeReaderActivity.this);
                    alertDialogBuilder
                            .setTitle(getString(R.string.reservation_number) + apiResponse.getId())
                            .setMessage(getString(R.string.ticket_not_created_yet) + " (" + DateUtils.formatDateTime(QRCodeReaderActivity.this, apiResponse.getCreationDate() * 1000, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME) + ")")
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    resumeReading();
                                }
                            })
                            .setNeutralButton(R.string.more, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    seeInfo(api, apiResponse, gingerResponse);
                                }
                            });

                    dialog.createDialog(alertDialogBuilder);
                    ((Vibrator) getSystemService(QRCodeReaderActivity.VIBRATOR_SERVICE)).vibrate(500);
                }
            });

            return;
        }

        if (apiResponse.getExpirationDate() != null && apiResponse.getExpirationDate() != -1 && currentTimestamp > apiResponse.getExpirationDate()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(QRCodeReaderActivity.this);
                    alertDialogBuilder
                            .setTitle(getString(R.string.reservation_number) + apiResponse.getId())
                            .setMessage(getString(R.string.ticket_expired) + " (" + DateUtils.formatDateTime(QRCodeReaderActivity.this, apiResponse.getExpirationDate() * 1000, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME) + ")")
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    resumeReading();
                                }
                            })
                            .setNeutralButton(R.string.more, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    seeInfo(api, apiResponse, gingerResponse);
                                }
                            });

                    dialog.createDialog(alertDialogBuilder);
                    ((Vibrator) getSystemService(QRCodeReaderActivity.VIBRATOR_SERVICE)).vibrate(500);
                }
            });

            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                seeInfo(api, apiResponse, gingerResponse);
            }
        });
    }

    protected void seeInfo(final API api, final APIResponse apiResponse, final GingerResponse gingerResponse) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(QRCodeReaderActivity.this);
        alertDialogBuilder.setTitle(getString(R.string.reservation_number) + apiResponse.getId()).setCancelable(false);
        Map<String, Map<String, String>> data = apiResponse.getData();

        if (data != null) {
            LinearLayout popupView = new LinearLayout(this);
            LinearLayout.LayoutParams popupParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            LinearLayout.LayoutParams categoryParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            popupParams.setMargins(75, 0, 75, 0);
            categoryParams.setMargins(75, 50, 75, 0);
            popupView.setOrientation(LinearLayout.VERTICAL);
            popupView.setLayoutParams(popupParams);


            if (gingerResponse != null)
                data.put(getString(R.string.ginger_information), new HashMap<String, String>() {{
                    put(getString(R.string.adult), gingerResponse.getIs_adulte() ? getString(R.string.yes) : getString(R.string.no));
                    put(getString(R.string.cotisant), gingerResponse.getIs_cotisant() ? getString(R.string.yes) : getString(R.string.no));
                }});

            for (String category : data.keySet()) {
                TextView categoryText = new TextView(this);
                LinearLayout categoryView = new LinearLayout(this);

                categoryText.setText(category);
                categoryText.setTextSize(16);
                categoryText.setTypeface(null, Typeface.BOLD);
                categoryText.setLayoutParams(categoryParams);
                categoryView.setOrientation(LinearLayout.VERTICAL);
                categoryView.setLayoutParams(popupParams);

                for (String name : apiResponse.getData().get(category).keySet()) {
                    LinearLayout elementView = new LinearLayout(this);
                    TextView nameText = new TextView(this);
                    TextView valueText = new TextView(this);

                    elementView.setOrientation(LinearLayout.HORIZONTAL);
                    nameText.setText(name);
                    valueText.setText(apiResponse.getData().get(category).get(name));
                    nameText.setWidth(250);

                    elementView.addView(nameText);
                    elementView.addView(valueText);
                    categoryView.addView(elementView);
                }

                popupView.addView(categoryText);
                popupView.addView(categoryView);
            }

            alertDialogBuilder.setView(popupView);
        }
        else if (apiResponse.getMessage() != null)
            alertDialogBuilder.setMessage(apiResponse.getMessage());
        else {
            dialog.errorDialog(QRCodeReaderActivity.this, getString(R.string.reservation_number) + apiResponse.getId(), getString(R.string.api_no_data), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    resumeReading();
                }
            });
        }

        alertDialogBuilder.setPositiveButton(apiResponse.getPositiveCommand() == null || apiResponse.getPositiveCommand().getName() == null ? getString(R.string.ok) : apiResponse.getPositiveCommand().getName(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (apiResponse.getPositiveCommand() != null && apiResponse.getPositiveCommand().getCommand() != null)
                    startCommand(api, apiResponse.getId(), apiResponse.getPositiveCommand(), gingerResponse);
                else
                    resumeReading();
            }
        });

        if (apiResponse.getNegativeCommand() != null && apiResponse.getNegativeCommand().getCommand() != null)
            alertDialogBuilder.setNegativeButton(apiResponse.getNegativeCommand().getName() == null ? getString(R.string.cancel) : apiResponse.getNegativeCommand().getName(), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    startCommand(api, apiResponse.getId(), apiResponse.getNegativeCommand(), gingerResponse);
                }
            });
        else if (apiResponse.getPositiveCommand() != null && apiResponse.getPositiveCommand().getCommand() != null) // Si le bouton ok est déjà défini et qu'on ne défini pas le bouton négatif, alors il s'agira du simple bouton ok
            alertDialogBuilder.setNegativeButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    resumeReading();
                }
            });

        if (apiResponse.getNeutralCommand() != null)
            alertDialogBuilder.setNeutralButton(apiResponse.getNeutralCommand().getName() == null ? getString(R.string.more) : apiResponse.getNeutralCommand().getName(), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    startCommand(api, apiResponse.getId(), apiResponse.getNeutralCommand(), gingerResponse);
                }
            });

        dialog.createDialog(alertDialogBuilder);
    }

    protected void startCommand(final API api, final String id, final APIResponse.APICommand apiCommand, final GingerResponse gingerResponse) {
        dialog.startLoading(QRCodeReaderActivity.this, getString(R.string.reservation_number) + id, apiCommand.getDescription() == null ? getString(R.string.api_execution) : apiCommand.getDescription());

        new Thread() {
            @Override
            public void run() {
                try {
                    if (apiCommand.getArguments() == null)
                        api.interact(id, apiCommand.getCommand());
                    else
                        api.interact(id, apiCommand.getCommand(), mapArgs(apiCommand.getArguments()));
                    Thread.sleep(100);

                    final APIResponse apiResponse = new ObjectMapper().readValue(api.getRequest().getResponse(), APIResponse.class);
                    dialog.stopLoading();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (apiResponse.getId() != null)
                                seeInfo(api, apiResponse, gingerResponse);
                            else
                                resumeReading();
                        }
                    });

                }
                catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String message = e.getMessage();
                            dialog.stopLoading();

                            try {
                                if (api.getRequest().getJSONResponse().has("message") && !api.getRequest().getJSONResponse().get("message").equals(""))
                                    message = api.getRequest().getJSONResponse().get("message").asText();
                            } catch (Exception e1) {
                                message = e.getMessage();
                            }

                            Log.e(LOG_TAG, "error: " + message);

                            dialog.errorDialog(QRCodeReaderActivity.this, getString(R.string.reservation_number) + id, message, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    resumeReading();
                                }
                            });
                        }
                    });
                }
            }
        }.start();
    }

    protected Map<String, Object> mapArgs(Map<String, String> args) {
        Map<String, Object> postArgs = new HashMap<String, Object>();

        for (String postArg : args.keySet()) {
            Object arg = null;
            try { arg = (Map<String, String>) new ObjectMapper().readValue(args.get(postArg), new TypeReference<HashMap<String, String>>(){}); }
            catch (Exception e) {}

            if (arg != null)
                postArgs.put(postArg, mapArgs((Map<String, String>) arg));
            else {
                try { arg = (List<String>) new ObjectMapper().readValue(args.get(postArg), new TypeReference<ArrayList<String>>(){}); }
                catch (Exception e) {}

                if (arg != null)
                    postArgs.put(postArg, arg);
                else {
                    try { arg = (Integer) Integer.valueOf(args.get(postArg)); }
                    catch (Exception e) {}

                    if (arg != null)
                        postArgs.put(postArg, arg);
                    else {
                        try { arg = (Float) Float.valueOf(args.get(postArg)); }
                        catch (Exception e) {}

                        if (arg != null)
                            postArgs.put(postArg, arg);
                        else
                            postArgs.put(postArg, args.get(postArg));
                    }
                }
            }
        }

        return postArgs;
    }
}

