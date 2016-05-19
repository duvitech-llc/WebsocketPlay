package com.six15.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.opengl.GLSurfaceView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity  implements WebRtcClient.RtcListener, SurfaceHolder.Callback{

    public static final String sServerID = "AAAAAAAAAAAAAAAAAAAAAA";
    private static final String TAG = "MainActivity";
    private String callerId = null;
    private final static int VIDEO_CALL_SENT = 666;
    private static final String VIDEO_CODEC_VP9 = "VP9";
    private static final String AUDIO_CODEC_OPUS = "opus";
    // Local preview screen position before call is connected.
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;
    // Local preview screen position after call is connected.
    private static final int LOCAL_X_CONNECTED = 72;
    private static final int LOCAL_Y_CONNECTED = 72;
    private static final int LOCAL_WIDTH_CONNECTED = 25;
    private static final int LOCAL_HEIGHT_CONNECTED = 25;
    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;
    private VideoRendererGui.ScalingType scalingType = VideoRendererGui.ScalingType.SCALE_ASPECT_FILL;
    private GLSurfaceView vsv;
    private VideoRenderer.Callbacks remoteRender;
    private VideoRenderer.Callbacks localRender;
    private WebRtcClient client;
    private String mSignalingServerAddress;
    private SurfaceView ov;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private SurfaceHolder sh;

    private ImageView mIndicator;
    private ArrayList<TextMessage> mMessageList;

    private ArrayList<BaseObject> drawObjectList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mSignalingServerAddress = "ws://" + getResources().getString(R.string.local_host);
        mIndicator = (ImageView)findViewById(R.id.imgDisplay);
        mIndicator.setImageResource(R.drawable.stop_icn);

        mMessageList = new ArrayList<>();
        drawObjectList = new ArrayList<>();

        vsv = (GLSurfaceView) findViewById(R.id.glview_call);
        vsv.setPreserveEGLContextOnPause(true);
        vsv.setKeepScreenOn(true);
        VideoRendererGui.setView(vsv, new Runnable() {
            @Override
            public void run() {
                init();
            }
        });

        vsv.setBackgroundColor(Color.BLACK);

        ov = (SurfaceView) findViewById(R.id.overlaySurface);
        ov.setZOrderMediaOverlay(true);
        ov.setBackgroundColor(Color.TRANSPARENT);
        ov.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //drawSquare(event.getRawX(), event.getRawY(),event.getRawX()+40, event.getRawY()+40,Color.YELLOW);
                drawCircle(event.getRawX(), event.getRawY(), 60, Color.BLUE);
                return false;
            }
        });

        sh = ov.getHolder();
        sh.addCallback(this);
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);

        // local and remote render
        remoteRender = VideoRendererGui.create(
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);

        localRender = VideoRendererGui.create(
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, scalingType, true);

        final Intent intent = getIntent();
        final String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
            final List<String> segments = intent.getData().getPathSegments();
            callerId = segments.get(0);
        }
    }

    private void init() {
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        PeerConnectionParameters params = new PeerConnectionParameters(
                true, false, displaySize.x, displaySize.y, 30, 1, VIDEO_CODEC_VP9, true, 1, AUDIO_CODEC_OPUS, true);

       client = new WebRtcClient(this, mSignalingServerAddress, params, VideoRendererGui.getEGLContext());
    }

    @Override
    public void onStart()
    {
        super.onStart();
    }

    @Override
    public void onStop() {
        if(client != null) {
            client.onStop();
        }
        super.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        vsv.onPause();
        if(client != null) {
            client.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        vsv.onResume();
        if(client != null) {
            client.onResume();
        }
    }

    @Override
    public void onDestroy() {
        if(client != null) {
            client.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    public void onCallReady(String callId) {
        Log.i(TAG, "onCallReady");
        if (callerId != null) {
            try {
                answer(callerId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            call(callId);
        }
    }

    public void answer(String callerId) throws JSONException {
        Log.i(TAG, "answer Caller: " + callerId);
        client.sendMessage(callerId, "init", null);
        startCam();
    }

    public void call(String callId) {
        Log.i(TAG, "call Caller: " + callId);
        // no need to do this unless we are placing a call
        // this can be startcam or client.start(friendlyName)
        String name = callId;

        //TODO: need to get friendly name of this device
        client.start("WIFI_Glasses");
/*
        Intent msg = new Intent(Intent.ACTION_SEND);
        // TODO: probably need to make this a message
        msg.putExtra(Intent.EXTRA_TEXT, mSignalingServerAddress + "/" + callId);
        msg.setType("text/plain");
        startActivityForResult(Intent.createChooser(msg, "Call someone :"), VIDEO_CALL_SENT);
*/
    }

    private void updateDisplay(){

        Canvas canvas = sh.lockCanvas();
        int x = canvas.getWidth();
        int y = canvas.getHeight();
        // clear screen and render objects
        canvas.drawColor(Color.BLACK);
        for(int c=0; c<drawObjectList.size(); c++){
            drawObjectList.get(c).drawObject(canvas,paint);
        }

        sh.unlockCanvasAndPost(canvas);

    }

    @Override
    public void drawCircle(float originX, float originY, float radius, int col){
        Log.i(TAG, "Draw Circle");
        drawObjectList.add(new CircleObject(originX, originY, radius, col));
        updateDisplay();
    }

    @Override
    public void drawSquare(float left, float top, float right, float bottom, int col){
        Log.i(TAG, "Draw Square");

        drawObjectList.add(new SquareObject(top, left, bottom, right, col));
        updateDisplay();
    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2, int col){
        Log.i(TAG, "Draw Line");

        drawObjectList.add(new LineObject(x1, y1, x2, y2, col));
        updateDisplay();

    }

    @Override
    public void clearScreen(){
        Log.i(TAG, "Clear Screen");
        drawObjectList.clear();
        updateDisplay();
    }

    public void startCam() {
        // Camera settings
        client.start("android_test");
    }

    @Override
    public void onStatusChanged(final String newStatus) {
        Log.i(TAG, "onStatusChanged: " + newStatus);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), newStatus, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMessage(String from, String msg) {
        final String dispMessage = from + ": " + msg;
        mMessageList.add(new TextMessage(from, msg));
        if(mMessageList.size()>4){
            // remove oldest
            mMessageList.remove(0);
        }

        // show messages
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), dispMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onLocalStream(MediaStream localStream) {
        Log.i(TAG, "onLocalStream");
        // render to screen

/*
        localStream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING,
                scalingType);
*/
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //ov.setBackgroundColor(Color.TRANSPARENT);
                mIndicator.setImageResource(R.drawable.play_icn);
            }
        });

    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream, int endPoint) {
        Log.i(TAG, "onAddRemoteStream");
        remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
        VideoRendererGui.update(remoteRender,
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType);

        /*
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED,
                LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED,
                scalingType);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ov.setBackgroundColor(Color.TRANSPARENT);
                mIndicator.setImageResource(R.drawable.play_icn);
            }
        });
        */
    }

    @Override
    public void onRemoveRemoteStream(int endPoint) {
        Log.i(TAG, "onRemoveRemoteStream");
        /*
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING,
                scalingType);
        */
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mIndicator.setImageResource(R.drawable.stop_icn);
               // ov.setBackgroundColor(Color.BLACK);
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return true;
            case KeyEvent.KEYCODE_MENU:
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Closing Telestration")
                .setMessage("Press Back to exit or select to cancel?")
                .setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        switch (keyCode) {
                            case KeyEvent.KEYCODE_VOLUME_UP:
                                return false;
                            case KeyEvent.KEYCODE_VOLUME_DOWN:
                                return false;
                            case KeyEvent.KEYCODE_MENU:
                                return true;
                            case KeyEvent.KEYCODE_BACK:
                                dialog.dismiss();
                                finish();
                                return true;
                            default:
                                return false;
                        }
                    }
                })
                .setPositiveButton("Exit", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }

                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void sendMessage(View view) {
        EditText editText = (EditText)findViewById(R.id.message);
        String message = editText.getText().toString();

        try {
            String to = null;
            client.sendTextMessage(to,"message",message);
        }catch(Exception ex){
            Log.e("ERROR", ex.getMessage());
        }

        editText.setText("");
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        Log.i(TAG, "surfaceCreated");
        Canvas canvas = sh.lockCanvas();
        canvas.drawColor(Color.BLACK);
        sh.unlockCanvasAndPost(canvas);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        Log.i(TAG, "surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        Log.i(TAG, "surfaceDestroyed");
    }
}
