package com.six15.myapplication;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Created by George on 5/19/2016.
 */
public class SquareObject extends BaseObject {
    private float top;
    private float left;
    private float bottom;
    private float right;

    public SquareObject(float top, float left, float bottom, float right, int color) {
        super(DrawType.SQUARE, color);
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
    }

    @Override
    public void drawObject(Canvas canvas, Paint paint) {
        float xTranslateFactor = 1.0f;
        float yTranslateFactor = 1.0f;

        paint.setColor(this.getObjColor());

        if(this.hasCanvasDimensions() && this.hasSourceDimensions()){
            // draw translated
            xTranslateFactor = (float) ((float) this.getCanvasWidth()/(float) this.getSrcWidth());
            yTranslateFactor = (float) ((float) this.getCanvasHeight()/(float) this.getSrcHeight());
        }

        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawRect(left * xTranslateFactor, top * yTranslateFactor,
                right * xTranslateFactor, bottom * yTranslateFactor, paint);
        paint.setStyle(Paint.Style.STROKE);
    }

    public float getTop() {
        return top;
    }

    public void setTop(float top) {
        this.top = top;
    }

    public float getLeft() {
        return left;
    }

    public void setLeft(float left) {
        this.left = left;
    }

    public float getBottom() {
        return bottom;
    }

    public void setBottom(float bottom) {
        this.bottom = bottom;
    }

    public float getRight() {
        return right;
    }

    public void setRight(float right) {
        this.right = right;
    }
}
