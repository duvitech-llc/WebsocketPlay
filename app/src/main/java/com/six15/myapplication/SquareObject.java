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
        paint.setColor(this.getObjColor());
        canvas.drawRect(left, top, right, bottom, paint);
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