package com.six15.myapplication;

import android.content.Context;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by George on 5/9/2016.
 */
public class GlRenderer  implements GLSurfaceView.Renderer{

    private Context context;
   /*
    public GlRenderer(Context ctx){
        this.context = ctx;
    }
    */
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

    }

    @Override
    public void onDrawFrame(GL10 gl) {

    }
}
