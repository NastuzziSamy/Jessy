package fr.utc.simde.jessy;

import android.content.DialogInterface;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.Result;

import fr.utc.simde.jessy.objects.GingerResponse;
import fr.utc.simde.jessy.objects.QRCodeResponse;

public class SellByQRCodeActivity extends QRCodeReaderActivity {
    private static final String LOG_TAG = "_SellByQRCodeActivity";

    @Override
    public void handleResult(Result result) {
        Log.d(LOG_TAG, result.getText());
        Log.d(LOG_TAG, result.getBarcodeFormat().toString());

        try {
            final QRCodeResponse qrCodeResponse = new ObjectMapper().readValue(result.getText(), QRCodeResponse.class);

            dialog.startLoading(SellByQRCodeActivity.this, getString(R.string.qrcode_reading), getString(R.string.user_ginger_info_collecting));
            new Thread() {
                @Override
                public void run() {
                    try {
                        ginger.getInfo(qrCodeResponse.getUsername());
                        Thread.sleep(100);
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
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                dialog.infoDialog(SellByQRCodeActivity.this, getString(R.string.qrcode_reading), (new ObjectMapper().readValue(ginger.getRequest().getResponse(), GingerResponse.class)).toString(), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        resumeReading();
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
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
