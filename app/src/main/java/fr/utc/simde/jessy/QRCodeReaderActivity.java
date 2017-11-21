package fr.utc.simde.jessy;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import com.google.zxing.Result;

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

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
    public void onIdentification(final String badgeId) {
        this.scannerView.stopCamera();
        dialog.infoDialog(QRCodeReaderActivity.this, getString(R.string.badge_read), "Badge: " + badgeId, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                scannerView.startCamera(CAMERA_FACING_BACK);
            }
        });
    }

    @Override
    public void handleResult(Result result) {
        Log.d(LOG_TAG, result.getText());
        Log.d(LOG_TAG, result.getBarcodeFormat().toString());
        dialog.infoDialog(QRCodeReaderActivity.this, getString(R.string.qrcode_reading), result.getText(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                resumeReading();
            }
        });
    }

    protected void resumeReading() {
        scannerView.resumeCameraPreview(QRCodeReaderActivity.this);
    }
}
