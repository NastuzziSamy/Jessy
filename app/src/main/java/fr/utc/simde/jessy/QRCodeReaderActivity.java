package fr.utc.simde.jessy;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.zxing.Result;

import java.util.ArrayList;
import java.util.List;

import fr.utc.simde.jessy.adapters.ListAdapater;
import fr.utc.simde.jessy.responses.ArticleResponse;
import fr.utc.simde.jessy.responses.BottomatikResponse;
import fr.utc.simde.jessy.responses.ReservationResponse;
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

public class QRCodeReaderActivity extends BaseActivity implements ZXingScannerView.ResultHandler {
    private static final String LOG_TAG = "_QRCodeReaderActivity";

    protected ZXingScannerView scannerView;

    protected SharedPreferences sharedPreferences;

    protected List<String> apiName;
    protected List<String> apiUrl;
    protected List<Boolean> apiNeedKey;
    protected List<Boolean> apiNeedGinger;
    protected List<Class> apiResponseClass;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.sharedPreferences = getSharedPreferences("payutc", Activity.MODE_PRIVATE);

        this.apiName = new ArrayList<String>() {{
            add("bottomatik");
            add("comedmus");
            add("reservations");
        }};

        this.apiUrl = new ArrayList<String>() {{
            add("https://picasso.bottomatik.com/bot/transactions/");
            add("https://www.lacomutc.fr/qr/" + sharedPreferences.getString("key_" + apiName.get(1), "no_key") + "/");
            add("https://assos.utc.fr/simde/reservations/");
        }};

        this.apiNeedKey = new ArrayList<Boolean>() {{
            add(true);
            add(false);
            add(true);
        }};

        this.apiNeedGinger = new ArrayList<Boolean>() {{
            add(true);
            add(false);
            add(true);
        }};

        this.apiResponseClass = new ArrayList<Class>() {{
            add(BottomatikResponse.class);
            add(ReservationResponse.class);
            add(ReservationResponse.class);
        }};

        this.scannerView = new ZXingScannerView(QRCodeReaderActivity.this) {
            @Override
            protected IViewFinder createViewFinderView(Context context) {
                return new ExtendedScannerView(context);
            }
        };
        setContentView(this.scannerView);

        scannerView.setResultHandler(QRCodeReaderActivity.this);
        this.scannerView.startCamera(CAMERA_FACING_BACK);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!dialog.isShowing())
            resumeReading();
    }

    @Override
    public void onIdentification(final String badgeId) {
        this.scannerView.stopCamera();

        new Thread() {
            @Override
            public void run() {
                Integer apiIndex = 2;
                GingerResponse gingerResponse = null;
                if (apiNeedGinger.get(apiIndex)) {
                    try {
                        ginger.getInfoFromBadge(badgeId);
                        Thread.sleep(100);

                        gingerResponse = new ObjectMapper().readValue(ginger.getRequest().getResponse(), GingerResponse.class);
                    }
                    catch (final Exception e) {
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
                }

                handleAPI(gingerResponse.getLogin(), apiIndex, gingerResponse, false);
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

                if (!this.apiName.contains(qrCodeResponse.getSystem())) {
                    dialog.infoDialog(QRCodeReaderActivity.this, result.getBarcodeFormat().toString() + ": " + getString(R.string.not_understood), result.getText(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            resumeReading();
                        }
                    });
                }
                else
                    handleQRCode(qrCodeResponse, this.apiName.indexOf(qrCodeResponse.getSystem()));
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());

                dialog.infoDialog(QRCodeReaderActivity.this, getString(R.string.qrcode_reading), e.getMessage(), new DialogInterface.OnClickListener() {
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

    protected void handleQRCode(final QRCodeResponse qrCodeResponse, final Integer apiIndex) {
        dialog.startLoading(QRCodeReaderActivity.this, getString(R.string.qrcode_reading), getString(R.string.user_ginger_info_collecting));

        new Thread() {
            @Override
            public void run() {
                GingerResponse gingerResponse = null;
                if (apiNeedGinger.get(apiIndex)) {
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

                handleAPI(qrCodeResponse.getId(), apiIndex, gingerResponse, true);
            }
        }.start();
    }

    protected void handleAPI(final String info, final Integer apiIndex, final GingerResponse gingerResponse, final boolean byQRCode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.changeLoading(getString(R.string.getting_informations_from) + " " + apiName.get(apiIndex));
            }
        });

        API api = new API(QRCodeReaderActivity.this, apiName.get(apiIndex), apiUrl.get(apiIndex));

        if (apiNeedKey.get(apiIndex))
            api.setKey(sharedPreferences.getString("key_" + apiName.get(apiIndex), ""));

        Object apiResponse;
        try {
            if (byQRCode)
                api.getInfosFromId(info);
            else
                api.getInfosFromUsername(info);
            Thread.sleep(100);

            apiResponse = new ObjectMapper().readValue(api.getRequest().getResponse(), apiResponseClass.get(apiIndex));

            if (api.getRequest().getJSONResponse().has("type") && api.getRequest().getJSONResponse().get("type").textValue().equals("error") && api.getRequest().getJSONResponse().has("message"))
                throw new Exception(api.getRequest().getJSONResponse().get("message").textValue());
        }
        catch (final Exception e) {
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

        switch (apiIndex) {
            case 0:
                payWithBottomatik(api, (BottomatikResponse) apiResponse, gingerResponse);
                break;

            case 1:
            case 2:
                checkReservation(api, (ReservationResponse) apiResponse, gingerResponse);
                break;
        }
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
            if (nemopaySession.getFoundationId() != -1 && bottomatikResponse.getFun_id() != nemopaySession.getFoundationId())
                throw new Exception(getString(R.string.can_not_sell_other_foundation));

            if (bottomatikResponse.isValidated())
                throw new Exception(getString(R.string.already_validated));

            nemopaySession.getArticles();
            Thread.sleep(100);

            TypeReference<List<ArticleResponse>> articleListType = new TypeReference<List<ArticleResponse>>(){};
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
        }
        catch (final Exception e) {
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
                nemopaySession.setTransaction(gingerResponse.getBadge_uid(), bottomatikResponse.getArticleList(), bottomatikResponse.getFun_id());
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
                                                api.validate(bottomatikResponse.getId(), true, true);
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
                                                    }
                                                    else
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
                }
                else
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

    protected void checkReservation(final API api, final ReservationResponse reservationResponse, final GingerResponse gingerResponse) {
        long currentTimestamp = (System.currentTimeMillis() / 1000);
        Log.d(LOG_TAG, "Current time: " + currentTimestamp);

        if (currentTimestamp < reservationResponse.getCreation_date()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(QRCodeReaderActivity.this);
                    alertDialogBuilder
                            .setTitle(getString(R.string.reservation_number) + reservationResponse.getReservation_id())
                            .setMessage(getString(R.string.ticket_not_created_yet))
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
                                    seeReservation(api, reservationResponse, gingerResponse);
                                }
                            });

                    dialog.createDialog(alertDialogBuilder);
                    ((Vibrator) getSystemService(QRCodeReaderActivity.VIBRATOR_SERVICE)).vibrate(500);
                }
            });

            return;
        }

        if (currentTimestamp > reservationResponse.getExpires_at()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(QRCodeReaderActivity.this);
                    alertDialogBuilder
                            .setTitle(getString(R.string.reservation_number) + reservationResponse.getReservation_id())
                            .setMessage(getString(R.string.ticket_expired))
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
                                    seeReservation(api, reservationResponse, gingerResponse);
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
                seeReservation(api, reservationResponse, gingerResponse);
            }
        });
    }

    protected void seeReservation(final API api, final ReservationResponse reservationResponse, final GingerResponse gingerResponse) {
        final View keyView = getLayoutInflater().inflate(R.layout.dialog_reservation_info, null);
        final TextView nameText = keyView.findViewById(R.id.text_name);
        final TextView seanceText = keyView.findViewById(R.id.text_seance);
        final TextView priceText = keyView.findViewById(R.id.text_price);
        final TextView adultText = keyView.findViewById(R.id.text_adult);
        final TextView contributerText = keyView.findViewById(R.id.text_cotisant);

        nameText.setText(reservationResponse.getUsername());
        seanceText.setText(reservationResponse.getSeance());
        priceText.setText(reservationResponse.getType());

        if (gingerResponse == null) {
            adultText.setVisibility(View.GONE);
            contributerText.setVisibility(View.GONE);
        }
        else {
            if (gingerResponse.getIs_adulte())
                adultText.setText(getString(R.string.yes));
            else {
                adultText.setText(getString(R.string.no));
                adultText.setTextColor(Color.RED);
            }

            if (gingerResponse.getIs_adulte())
                contributerText.setText(getString(R.string.yes));
            else {
                contributerText.setText(getString(R.string.no));
                contributerText.setTextColor(Color.RED);
            }
        }

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(QRCodeReaderActivity.this);
        alertDialogBuilder
                .setTitle(getString(R.string.reservation_number) + reservationResponse.getReservation_id())
                .setView(keyView)
                .setCancelable(false)
                .setPositiveButton(R.string.validate, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int id) {dialog.startLoading(QRCodeReaderActivity.this, getResources().getString(R.string.paiement), getResources().getString(R.string.ticket_in_validation));
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    api.validate(reservationResponse.getId());
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
                .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        resumeReading();
                    }
                });

        dialog.createDialog(alertDialogBuilder);
    }
}

