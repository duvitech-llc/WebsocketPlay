package com.six15.myapplication;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Created by George on 5/19/2016.
 */
public abstract class BaseObject {
    private DrawType drawType;
    private int objColor;

    public BaseObject(DrawType drawType, int color) {
        this.drawType = drawType;
        this.objColor = color;
    }

    public abstract void drawObject(Canvas canvas, Paint paint);

    public DrawType getDrawType() {
        return drawType;
    }

    public void setDrawType(DrawType drawType) {
        this.drawType = drawType;
    }

    public int getObjColor() {
        return objColor;
    }

    public void setObjColor(int objColor) {
        this.objColor = objColor;
    }
}

