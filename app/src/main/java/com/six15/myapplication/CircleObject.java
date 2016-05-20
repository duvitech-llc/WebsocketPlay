package com.six15.myapplication;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Created by George on 5/19/2016.
 */
public class CircleObject extends BaseObject {
    private float originX;
    private float originY;
    private float radius;

    public CircleObject(float originX, float originY, float radius, int color) {
        super(DrawType.CIRCLE, color);
        this.originX = originX;
        this.originY = originY;
        this.radius = radius;
    }

    public float getOriginX() {
        return originX;
    }

    public void setOriginX(float originX) {
        this.originX = originX;
    }

    public float getOriginY() {
        return originY;
    }

    public void setOriginY(float originY) {
        this.originY = originY;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    @Override
    public void drawObject(Canvas canvas, Paint paint) {
        float xTranslateFactor = 1.0f;
        float yTranslateFactor = 1.0f;
        float rTranslateFactor = 1.0f;

        paint.setColor(this.getObjColor());
        if(this.hasCanvasDimensions() && this.hasSourceDimensions()){
            xTranslateFactor = this.getCanvasWidth()/this.getSrcWidth();
            yTranslateFactor = this.getCanvasHeight()/this.getSrcHeight();
            rTranslateFactor = (xTranslateFactor + yTranslateFactor)/2;
        }

        canvas.drawCircle(originX * xTranslateFactor, originY * yTranslateFactor,
                radius * rTranslateFactor, paint);
    }
}
