package fr.utc.simde.jessy.tools;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import fr.utc.simde.jessy.R;
import me.dm7.barcodescanner.core.DisplayUtils;
import me.dm7.barcodescanner.core.IViewFinder;

/**
 * Created by Samy on 19/11/2017.
 */

public class ExtendedScannerView extends View implements IViewFinder {
    private Rect mFramingRect;

    private static final float PORTRAIT_WIDTH_RATIO = 0.9f;
    private static final float PORTRAIT_WIDTH_HEIGHT_RATIO = 1.0f;

    private static final float LANDSCAPE_HEIGHT_RATIO = 5f/8;
    private static final float LANDSCAPE_WIDTH_HEIGHT_RATIO = 1.4f;
    private static final int MIN_DIMENSION_DIFF = 50;

    private static final float SQUARE_DIMENSION_RATIO = 0.9f;

    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private int scannerAlpha;
    private static final int POINT_SIZE = 10;
    private static final long ANIMATION_DELAY = 80l;

    private final int mDefaultLaserColor = getResources().getColor(R.color.viewfinder_laser);
    private final int mDefaultMaskColor = getResources().getColor(R.color.viewfinder_mask);
    private final int mDefaultBorderColor = getResources().getColor(R.color.viewfinder_border);
    private final int mDefaultBorderStrokeWidth = getResources().getInteger(R.integer.viewfinder_border_width);
    private final int mDefaultBorderLineLength = getResources().getInteger(R.integer.viewfinder_border_length);

    protected Paint mFinderMaskPaint;
    protected Paint mBorderPaint;
    protected int mBorderLineLength;
    protected boolean mSquareViewFinder;

    public ExtendedScannerView(Context context) {
        super(context);

        init();
    }

    public ExtendedScannerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    private void init() {
        mFinderMaskPaint = new Paint();
        mFinderMaskPaint.setColor(mDefaultMaskColor);

        mBorderPaint = new Paint();
        mBorderPaint.setColor(mDefaultBorderColor);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(mDefaultBorderStrokeWidth);

        mBorderLineLength = mDefaultBorderLineLength;
    }

    public void setupViewFinder() {
        updateFramingRect();
        invalidate();
    }

    public Rect getFramingRect() {
        return mFramingRect;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (getFramingRect() == null) {
            return;
        }

        drawViewFinderMask(canvas);
        drawViewFinderBorder(canvas);
        drawLaser(canvas);
    }

    public void drawViewFinderMask(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        Rect framingRect = getFramingRect();

        canvas.drawRect(0, 0, width, framingRect.top, mFinderMaskPaint);
        canvas.drawRect(0, framingRect.top, framingRect.left, framingRect.bottom + 1, mFinderMaskPaint);
        canvas.drawRect(framingRect.right + 1, framingRect.top, width, framingRect.bottom + 1, mFinderMaskPaint);
        canvas.drawRect(0, framingRect.bottom + 1, width, height, mFinderMaskPaint);
    }

    public void drawViewFinderBorder(Canvas canvas) {
        Rect framingRect = getFramingRect();

        canvas.drawLine(framingRect.left - 1, framingRect.top - 1, framingRect.left - 1, framingRect.top - 1 + mBorderLineLength, mBorderPaint);
        canvas.drawLine(framingRect.left - 1, framingRect.top - 1, framingRect.left - 1 + mBorderLineLength, framingRect.top - 1, mBorderPaint);

        canvas.drawLine(framingRect.left - 1, framingRect.bottom + 1, framingRect.left - 1, framingRect.bottom + 1 - mBorderLineLength, mBorderPaint);
        canvas.drawLine(framingRect.left - 1, framingRect.bottom + 1, framingRect.left - 1 + mBorderLineLength, framingRect.bottom + 1, mBorderPaint);

        canvas.drawLine(framingRect.right + 1, framingRect.top - 1, framingRect.right + 1, framingRect.top - 1 + mBorderLineLength, mBorderPaint);
        canvas.drawLine(framingRect.right + 1, framingRect.top - 1, framingRect.right + 1 - mBorderLineLength, framingRect.top - 1, mBorderPaint);

        canvas.drawLine(framingRect.right + 1, framingRect.bottom + 1, framingRect.right + 1, framingRect.bottom + 1 - mBorderLineLength, mBorderPaint);
        canvas.drawLine(framingRect.right + 1, framingRect.bottom + 1, framingRect.right + 1 - mBorderLineLength, framingRect.bottom + 1, mBorderPaint);
    }

    public void drawLaser(Canvas canvas) {
        Rect framingRect = getFramingRect();

        scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;

        postInvalidateDelayed(ANIMATION_DELAY,
            framingRect.left - POINT_SIZE,
            framingRect.top - POINT_SIZE,
            framingRect.right + POINT_SIZE,
            framingRect.bottom + POINT_SIZE);
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        updateFramingRect();
    }

    public synchronized void updateFramingRect() {
        Point viewResolution = new Point(getWidth(), getHeight());
        int width;
        int height;
        int orientation = DisplayUtils.getScreenOrientation(getContext());

        if (mSquareViewFinder) {
            if (orientation != Configuration.ORIENTATION_PORTRAIT) {
                height = (int) (getHeight() * SQUARE_DIMENSION_RATIO);
                width = height;
            } else {
                width = (int) (getWidth() * SQUARE_DIMENSION_RATIO);
                height = width;
            }
        } else {
            if(orientation != Configuration.ORIENTATION_PORTRAIT) {
                height = (int) (getHeight() * LANDSCAPE_HEIGHT_RATIO);
                width = (int) (LANDSCAPE_WIDTH_HEIGHT_RATIO * height);
            } else {
                width = (int) (getWidth() * PORTRAIT_WIDTH_RATIO);
                height = (int) (PORTRAIT_WIDTH_HEIGHT_RATIO * width);
            }
        }

        if (width > getWidth()) {
            width = getWidth() - MIN_DIMENSION_DIFF;
        }

        if (height > getHeight()) {
            height = getHeight() - MIN_DIMENSION_DIFF;
        }

        int leftOffset = (viewResolution.x - width) / 2;
        int topOffset = (viewResolution.y - height) / 2;
        mFramingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
    }
}