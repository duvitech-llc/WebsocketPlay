package com.six15.myapplication;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Created by George on 5/19/2016.
 */
public abstract class BaseObject {
    private DrawType drawType;
    private int objColor;
    private int srcWidth;
    private int srcHeight;
    private int canvasWidth;
    private int canvasHeight;

    public BaseObject(DrawType drawType, int color) {
        this.srcHeight = 0;
        this.srcWidth = 0;
        this.canvasWidth = 0;
        this.canvasHeight = 0;
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

    public int getSrcWidth() {
        return srcWidth;
    }

    public void setSrcWidth(int srcWidth) {
        this.srcWidth = srcWidth;
    }

    public int getSrcHeight() {
        return srcHeight;
    }

    public void setSrcHeight(int srcHeight) {
        this.srcHeight = srcHeight;
    }

    public boolean hasSourceDimensions(){
        if(this.srcWidth>0 && this.srcHeight>0)
            return true;
        else
            return false;
    }

    public boolean hasCanvasDimensions(){
        if(this.canvasWidth>0 && this.canvasHeight>0)
            return true;
        else
            return false;
    }

    public int getCanvasWidth() {
        return canvasWidth;
    }

    public void setCanvasWidth(int canvasWidth) {
        this.canvasWidth = canvasWidth;
    }

    public int getCanvasHeight() {
        return canvasHeight;
    }

    public void setCanvasHeight(int canvasHeight) {
        this.canvasHeight = canvasHeight;
    }
}

