package fr.utc.simde.jessy;

import android.content.Context;
import android.content.DialogInterface;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.utc.simde.jessy.adapters.ListAdapater;
import fr.utc.simde.jessy.responses.APICommand;
import fr.utc.simde.jessy.responses.APIResponse;
import fr.utc.simde.jessy.responses.ArticleResponse;
import fr.utc.simde.jessy.responses.BottomatikResponse;
import fr.utc.simde.jessy.responses.GingerResponse;
import fr.utc.simde.jessy.responses.QRCodeResponse;
import fr.utc.simde.jessy.tools.API;
import fr.utc.simde.jessy.tools.ExtendedScannerView;
import me.dm7.barcodescanner.core.IViewFinder;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;

/**
 * Created by Samy on 18/11/2017.
 */

public class APIActivity extends BaseActivity implements ZXingScannerView.ResultHandler {
    private static final String LOG_TAG = "_APIActivity";

    protected ZXingScannerView scannerView;

    protected String badgeId;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.scannerView = new ZXingScannerView(APIActivity.this) {
            @Override
            protected IViewFinder createViewFinderView(Context context) {
                return new ExtendedScannerView(context);
            }
        };

        this.scannerView.setResultHandler(APIActivity.this);
        this.scannerView.startCamera(CAMERA_FACING_BACK);
        this.scannerView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                final LayoutInflater layoutInflater = LayoutInflater.from(APIActivity.this);
                final View popupView = layoutInflater.inflate(R.layout.dialog_tag, null);
                final EditText inputApi = popupView.findViewById(R.id.input_api);
                final EditText inputInfo = popupView.findViewById(R.id.input_info);
                final RadioButton buttonTag = popupView.findViewById(R.id.radio_tag);
                inputApi.setText(config.getCurrentApi());

                final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(APIActivity.this);
                alertDialogBuilder
                        .setTitle(R.string.getting_informations_from)
                        .setView(popupView)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Map<String, String> apiInfo = config.getApi(inputApi.getText().toString());

                                        if (apiInfo == null) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Log.e(LOG_TAG, getString(R.string.api_not_recognized));

                                                    dialog.infoDialog(APIActivity.this, getString(R.string.badge_read), getString(R.string.api_not_recognized));
                                                }
                                            });
                                        } else
                                            handleAPI(buttonTag.isChecked() ? inputInfo.getText().toString().toUpperCase() : inputInfo.getText().toString(), apiInfo, null, null, buttonTag.isChecked());
                                    }
                                }).start();
                            }
                        })
                        .setNeutralButton(R.string.api_assign, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Map<String, String> apiInfo = config.getApi(inputApi.getText().toString());
                                    }
                                }).start();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null);

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

        if (apiInfo == null) {
            dialog.infoDialog(APIActivity.this, getString(R.string.badge_read), getString(R.string.no_api), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    resumeReading();
                }
            });

            return;
        }

        dialog.startLoading(APIActivity.this, getString(R.string.badge_read), getString(R.string.user_ginger_info_collecting));

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
                            dialog.infoDialog(APIActivity.this, getString(R.string.badge_error_not_recognized), e.getMessage(), new DialogInterface.OnClickListener() {
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
        scannerView.resumeCameraPreview(APIActivity.this);
    }

    public void handleResult(final Result result) {
        if (result.getBarcodeFormat().toString().equals("QR_CODE")) {
            try {
                Log.d(LOG_TAG, result.getText());
                QRCodeResponse qrCodeResponse = new ObjectMapper().readValue(result.getText(), QRCodeResponse.class);
                final Map<String, String> apiInfo = config.getApi(qrCodeResponse.getSystem());

                if (apiInfo == null)
                    dialog.infoDialog(APIActivity.this, result.getBarcodeFormat().toString() + ": " + getString(R.string.not_understood), result.getText(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            resumeReading();
                        }
                    });
                else
                    handleQRCode(qrCodeResponse, apiInfo);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());

                dialog.infoDialog(APIActivity.this, getString(R.string.qrcode_reading), e.getMessage(), new DialogInterface.OnClickListener() {
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
        dialog.startLoading(APIActivity.this, getString(R.string.qrcode_reading), getString(R.string.user_ginger_info_collecting));

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
                                    dialog.infoDialog(APIActivity.this, getString(R.string.qrcode_reading), e.getMessage(), new DialogInterface.OnClickListener() {
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

        final API api = new API(APIActivity.this, apiInfo.get("name"), apiInfo.get("url"));
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

                            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(APIActivity.this);
                            alertDialogBuilder
                                    .setTitle(finalApiResponse.getId().isEmpty() ? getString(R.string.reservation_number) : (getString(R.string.reservation_number) + finalApiResponse.getId()))
                                    .setMessage(getString(R.string.ticket_validated) + (finalApiResponse.getExpirationDate() == null ? "" : " (" + DateUtils.formatDateTime(APIActivity.this, finalApiResponse.getExpirationDate() * 1000, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME) + ")"))
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
                            ((Vibrator) getSystemService(APIActivity.VIBRATOR_SERVICE)).vibrate(500);
                        }
                    });
                } catch (final Exception e1) {
                    Log.e(LOG_TAG, e.getMessage());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.infoDialog(APIActivity.this, getString(R.string.getting_informations_from) + " " + apiInfo.get("name"), e.getMessage(), new DialogInterface.OnClickListener() {
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
                        dialog.infoDialog(APIActivity.this, getString(R.string.getting_informations_from) + " " + apiInfo.get("name"), e.getMessage(), new DialogInterface.OnClickListener() {
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
                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(APIActivity.this);
                    alertDialogBuilder
                            .setTitle(apiResponse.getId().isEmpty() ? getString(R.string.reservation) : (getString(R.string.reservation_number) + apiResponse.getId()))
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
                    ((Vibrator) getSystemService(APIActivity.VIBRATOR_SERVICE)).vibrate(500);
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
                    dialog.infoDialog(APIActivity.this, getString(R.string.qrcode_reading), e.getMessage(), new DialogInterface.OnClickListener() {
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
                    Toast.makeText(APIActivity.this, getString(R.string.ticket_realized), Toast.LENGTH_LONG).show();
                    ((Vibrator) getSystemService(APIActivity.VIBRATOR_SERVICE)).vibrate(250);

                    final LayoutInflater layoutInflater = LayoutInflater.from(APIActivity.this);
                    final View popupView = layoutInflater.inflate(R.layout.dialog_list, null);
                    final ListView listView = popupView.findViewById(R.id.list_groups);

                    try {
                        listView.setAdapter(new ListAdapater(APIActivity.this, purchaseList, config.getPrintCotisant(), config.getPrint18()));
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "error: " + e.getMessage());

                        fatal(APIActivity.this, getString(R.string.qrcode_reading), e.getMessage());
                    }

                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(APIActivity.this);
                    alertDialogBuilder
                            .setTitle(R.string.panier)
                            .setView(popupView)
                            .setCancelable(false)
                            .setPositiveButton(R.string.validate, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int which) {
                                    dialog.startLoading(APIActivity.this, getResources().getString(R.string.paiement), getResources().getString(R.string.ticket_in_validation));

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
                                                        Toast.makeText(APIActivity.this, getString(R.string.ticket_validated), Toast.LENGTH_LONG).show();

                                                        resumeReading();
                                                    }
                                                });
                                            } catch (final Exception e) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Log.e(LOG_TAG, "error: " + e.getMessage());
                                                        dialog.errorDialog(APIActivity.this, getString(R.string.qrcode_reading), e.getMessage());

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
                                    dialog.startLoading(APIActivity.this, getResources().getString(R.string.qrcode_reading), getResources().getString(R.string.ticket_in_cancelation));

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
                                                        Toast.makeText(APIActivity.this, getString(R.string.ticket_refunded), Toast.LENGTH_LONG).show();
                                                        ((Vibrator) getSystemService(APIActivity.VIBRATOR_SERVICE)).vibrate(250);

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
                                                                dialog.errorDialog(APIActivity.this, getString(R.string.qrcode_reading), response.get("error").get("message").textValue());
                                                                ((Vibrator) getSystemService(APIActivity.VIBRATOR_SERVICE)).vibrate(500);

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
                                                            dialog.errorDialog(APIActivity.this, getString(R.string.qrcode_reading), e.getMessage());
                                                            ((Vibrator) getSystemService(APIActivity.VIBRATOR_SERVICE)).vibrate(500);

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

                            dialog.errorDialog(APIActivity.this, getString(R.string.paiement), response.get("error").get("message").textValue());
                            ((Vibrator) getSystemService(APIActivity.VIBRATOR_SERVICE)).vibrate(500);

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
                        dialog.errorDialog(APIActivity.this, getString(R.string.paiement), e.getMessage(), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                resumeReading();
                            }
                        });
                        ((Vibrator) getSystemService(APIActivity.VIBRATOR_SERVICE)).vibrate(500);
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
                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(APIActivity.this);
                    alertDialogBuilder
                            .setTitle(apiResponse.getId().isEmpty() ? getString(R.string.reservation) : (getString(R.string.reservation_number) + apiResponse.getId()))
                            .setMessage(getString(R.string.ticket_not_created_yet) + " (" + DateUtils.formatDateTime(APIActivity.this, apiResponse.getCreationDate() * 1000, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME) + ")")
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
                    ((Vibrator) getSystemService(APIActivity.VIBRATOR_SERVICE)).vibrate(500);
                }
            });

            return;
        }

        if (apiResponse.getExpirationDate() != null && apiResponse.getExpirationDate() != -1 && currentTimestamp > apiResponse.getExpirationDate()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(APIActivity.this);
                    alertDialogBuilder
                            .setTitle(apiResponse.getId().isEmpty() ? getString(R.string.reservation) : (getString(R.string.reservation_number) + apiResponse.getId()))
                            .setMessage(getString(R.string.ticket_expired) + " (" + DateUtils.formatDateTime(APIActivity.this, apiResponse.getExpirationDate() * 1000, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME) + ")")
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
                    ((Vibrator) getSystemService(APIActivity.VIBRATOR_SERVICE)).vibrate(500);
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
        dialog.startLoading(APIActivity.this, getString(R.string.api_execution), getString(R.string.api_execution));

        new Thread() {
            @Override
            public void run() {
                final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(APIActivity.this);
                alertDialogBuilder.setTitle(apiResponse.getId().isEmpty() ? getString(R.string.reservation) : (getString(R.string.reservation_number) + apiResponse.getId())).setCancelable(false);
                Map<String, Map<String, String>> data = apiResponse.getData();
                List<ArticleResponse> articleResponseList = null;
                List<List<Integer>> articleIdList = apiResponse.getArticleList();
                final ArrayNode purchaseList = new ObjectMapper().createArrayNode();

                final LinearLayout popupView = new LinearLayout(APIActivity.this);
                LinearLayout.LayoutParams popupParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                popupParams.setMargins(75, 0, 75, 0);
                LinearLayout.LayoutParams categoryParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                categoryParams.setMargins(75, 50, 75, 0);
                popupView.setOrientation(LinearLayout.VERTICAL);
                popupView.setLayoutParams(popupParams);

                if (!(apiResponse.getArticleList().isEmpty() || apiResponse.getFoundationId() == null)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.changeLoading(getString(R.string.article_list_collecting));
                        }
                    });

                    try {
                        if (nemopaySession.getFoundationId() != -1 && apiResponse.getFoundationId() != nemopaySession.getFoundationId())
                            throw new Exception(getString(R.string.can_not_sell_other_foundation));

                        nemopaySession.getArticles(apiResponse.getFoundationId());
                        Thread.sleep(100);

                        articleResponseList = new ObjectMapper().readValue(nemopaySession.getRequest().getResponse(), new TypeReference<List<ArticleResponse>>(){});
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
                                dialog.infoDialog(APIActivity.this, getString(R.string.qrcode_reading), e.getMessage(), new DialogInterface.OnClickListener() {
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

                if (data != null) {
                    if (gingerResponse != null)
                        data.put(getString(R.string.ginger_information), new HashMap<String, String>() {{
                            put(getString(R.string.adult), gingerResponse.getIs_adulte() ? getString(R.string.yes) : getString(R.string.no));
                            put(getString(R.string.cotisant), gingerResponse.getIs_cotisant() ? getString(R.string.yes) : getString(R.string.no));
                        }});

                    for (String category : data.keySet()) {
                        TextView categoryText = new TextView(APIActivity.this);
                        LinearLayout categoryView = new LinearLayout(APIActivity.this);

                        categoryText.setText(category);
                        categoryText.setTextSize(16);
                        categoryText.setTypeface(null, Typeface.BOLD);
                        categoryText.setLayoutParams(categoryParams);
                        categoryView.setOrientation(LinearLayout.VERTICAL);
                        categoryView.setLayoutParams(popupParams);

                        for (String name : apiResponse.getData().get(category).keySet()) {
                            LinearLayout elementView = new LinearLayout(APIActivity.this);
                            TextView nameText = new TextView(APIActivity.this);
                            TextView valueText = new TextView(APIActivity.this);

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
                }
                else if (apiResponse.getMessage() != null) {
                    final LinearLayout messageLayout = new LinearLayout(APIActivity.this);
                    TextView messageText = new TextView(APIActivity.this);

                    messageText.setText(apiResponse.getMessage());

                    messageLayout.setOrientation(LinearLayout.VERTICAL);
                    messageLayout.setLayoutParams(categoryParams);
                    messageLayout.addView(messageText);
                    popupView.addView(messageLayout);
                }
                else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.errorDialog(APIActivity.this, apiResponse.getId().isEmpty() ? getString(R.string.reservation) : (getString(R.string.reservation_number) + apiResponse.getId()), getString(R.string.api_no_data), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    resumeReading();
                                }
                            });
                        }
                    });

                    return;
                }

                if (!(apiResponse.getArticleList().isEmpty() || apiResponse.getFoundationId() == null)) {
                    final LinearLayout listLayout = new LinearLayout(APIActivity.this);
                    final LinearLayout payLayout = new LinearLayout(APIActivity.this);
                    final ListView listView = new ListView(APIActivity.this);

                    try {
                        listView.setAdapter(new ListAdapater(APIActivity.this, purchaseList, config.getPrintCotisant(), config.getPrint18()));
                    } catch (final Exception e) {
                        Log.e(LOG_TAG, "error: " + e.getMessage());

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                fatal(APIActivity.this, getString(R.string.qrcode_reading), e.getMessage());
                            }
                        });
                    }

                    TextView payText = new TextView(APIActivity.this);
                    payText.setText(getString(R.string.api_pay_caution).replace("%s", apiResponse.getPositiveCommand() == null || apiResponse.getPositiveCommand().getName() == null ? getString(R.string.pay) : apiResponse.getPositiveCommand().getName()));

                    listLayout.setOrientation(LinearLayout.VERTICAL);
                    listLayout.setLayoutParams(categoryParams);
                    payLayout.setOrientation(LinearLayout.VERTICAL);
                    payLayout.setLayoutParams(categoryParams);
                    listLayout.addView(listView);
                    payLayout.addView(payText);
                    popupView.addView(listLayout);
                    popupView.addView(payLayout);
                }

                alertDialogBuilder.setView(popupView);

                if (apiResponse.getArticleList().isEmpty() || apiResponse.getFoundationId() == null) {
                    alertDialogBuilder.setPositiveButton(apiResponse.getPositiveCommand() == null || apiResponse.getPositiveCommand().getName() == null ? getString(R.string.ok) : apiResponse.getPositiveCommand().getName(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (apiResponse.getPositiveCommand() != null && apiResponse.getPositiveCommand().getCommand() != null)
                                startCommand(api, apiResponse.getId(), apiResponse.getUsername(), apiResponse.getPositiveCommand(), gingerResponse);
                            else
                                resumeReading();
                        }
                    });
                }
                else {
                    alertDialogBuilder.setPositiveButton(apiResponse.getPositiveCommand() == null || apiResponse.getPositiveCommand().getName() == null ? getString(R.string.pay) : apiResponse.getPositiveCommand().getName(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            pay(api, apiResponse, gingerResponse);
                        }
                    });
                }

                if (!(apiResponse.getArticleList().isEmpty() || apiResponse.getFoundationId() == null) || (apiResponse.getNegativeCommand() != null && apiResponse.getNegativeCommand() != null))
                    alertDialogBuilder.setNegativeButton(apiResponse.getNegativeCommand() == null || apiResponse.getNegativeCommand().getName() == null ? getString(R.string.cancel) : apiResponse.getNegativeCommand().getName(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startCommand(api, apiResponse.getId(), apiResponse.getUsername(), apiResponse.getNegativeCommand(), gingerResponse);
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
                            startCommand(api, apiResponse.getId(), apiResponse.getUsername(), apiResponse.getNeutralCommand(), gingerResponse);
                        }
                    });

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.createDialog(alertDialogBuilder);
                    }
                });
            }
        }.start();
    }

    public void pay(final API api, final APIResponse apiResponse, final GingerResponse gingerResponse) {
        dialog.startLoading(APIActivity.this, getString(R.string.api_execution), getString(R.string.transaction_in_progress));

        new Thread() {
            @Override
            public void run() {
                try {
                    nemopaySession.setTransaction(gingerResponse.getBadge_uid(), apiResponse.getArticleList(), apiResponse.getFoundationId());
                    Thread.sleep(100);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.stopLoading();
                            Toast.makeText(APIActivity.this, getString(R.string.ticket_realized), Toast.LENGTH_LONG).show();
                            ((Vibrator) getSystemService(APIActivity.VIBRATOR_SERVICE)).vibrate(250);

                            APICommand apiCommand = new APICommand();
                            if (apiResponse.getPositiveCommand() == null) {
                                apiCommand.setCommand("pay");
                                apiCommand.setArguments(new HashMap<String, String>(){{
                                    put("paid", "true");
                                }});
                            }
                            else {
                                if (apiResponse.getPositiveCommand().getCommand() == null)
                                    apiCommand.setCommand("pay");
                                else
                                    apiCommand.setCommand(apiResponse.getPositiveCommand().getCommand());

                                if (apiResponse.getPositiveCommand().getArguments() == null)
                                    apiCommand.setArguments(new HashMap<String, String>(){{
                                        put("paid", "1");
                                    }});
                                else
                                    apiCommand.setArguments(apiResponse.getPositiveCommand().getArguments());

                                if (apiResponse.getPositiveCommand().getUseUsername() != null)
                                    apiCommand.setUseUsername(apiResponse.getPositiveCommand().getUseUsername());
                            }

                            startCommand(api, apiResponse.getId(), apiResponse.getUsername(), apiCommand, gingerResponse);
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

                                    dialog.errorDialog(APIActivity.this, getString(R.string.paiement), response.get("error").get("message").textValue());
                                    ((Vibrator) getSystemService(APIActivity.VIBRATOR_SERVICE)).vibrate(500);

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
                                dialog.errorDialog(APIActivity.this, getString(R.string.paiement), e.getMessage(), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        resumeReading();
                                    }
                                });
                                ((Vibrator) getSystemService(APIActivity.VIBRATOR_SERVICE)).vibrate(500);
                            }
                        });
                    }

                    return;
                }
            }
        }.start();
    }

    protected void startCommand(final API api, final String id, final String username, final APICommand apiCommand, final GingerResponse gingerResponse) {
        if (apiCommand == null) {
            resumeReading();

            return;
        }

        dialog.startLoading(APIActivity.this, id.isEmpty() ? getString(R.string.reservation) : (getString(R.string.reservation_number) + id), apiCommand == null || apiCommand.getDescription() == null ? getString(R.string.api_execution) : apiCommand.getDescription());

        new Thread() {
            @Override
            public void run() {
                try {
                    if (apiCommand.getArguments() == null)
                        api.interact(apiCommand.getUseUsername() == null || apiCommand.getUseUsername() == false ? id : ("user/" + username), apiCommand.getCommand());
                    else
                        api.interact(apiCommand.getUseUsername() == null || apiCommand.getUseUsername() == false ? id : ("user/" + username), apiCommand.getCommand(), mapArgs(apiCommand.getArguments()));
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

                            dialog.errorDialog(APIActivity.this, id.isEmpty() ? getString(R.string.reservation) : (getString(R.string.reservation_number) + id), message, new DialogInterface.OnClickListener() {
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
                        postArgs.put(postArg, arg)
                                ;
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

