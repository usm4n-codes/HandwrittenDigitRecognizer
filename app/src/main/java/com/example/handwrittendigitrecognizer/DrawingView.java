package com.example.handwrittendigitrecognizer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/** A white drawing pad that records the user's finger strokes in an Android Path. */
public class DrawingView extends View {
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path strokePath = new Path();
    private boolean hasDrawing;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Paint controls how the Path looks: black ink with a wide, rounded stroke.
        strokePaint.setColor(Color.BLACK);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(24f);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
    }

    /** Android calls onDraw after invalidate() to repaint the custom View. */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);       // Paper-like background.
        canvas.drawPath(strokePath, strokePaint); // Draw all saved finger strokes.
    }

    /** Convert touch events into Path points and ask Android to redraw the View. */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Begin a new line where the finger first touches the pad.
                strokePath.moveTo(x, y);
                hasDrawing = true;
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                // Add any intermediate samples, then the newest sample, to keep the line smooth.
                for (int i = 0; i < event.getHistorySize(); i++) {
                    strokePath.lineTo(event.getHistoricalX(i), event.getHistoricalY(i));
                }
                strokePath.lineTo(x, y);
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                strokePath.lineTo(x, y);
                invalidate();
                performClick();
                return true;

            case MotionEvent.ACTION_CANCEL:
                invalidate();
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }

    /** Accessibility click hook paired with touch handling. */
    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    public boolean hasDrawing() {
        return hasDrawing;
    }

    /** Reset both the saved path and the flag that represents non-empty input. */
    public void clear() {
        strokePath.reset();
        hasDrawing = false;
        invalidate();
    }

    /**
     * Render the visible Path into pixels. DigitClassifier will crop and resize
     * this Bitmap to the model's smaller 28 by 28 input image.
     */
    public Bitmap getDrawingBitmap() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            throw new IllegalStateException("Drawing surface is not ready.");
        }
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas bitmapCanvas = new Canvas(bitmap);
        bitmapCanvas.drawColor(Color.WHITE);
        bitmapCanvas.drawPath(strokePath, strokePaint);
        return bitmap;
    }
}