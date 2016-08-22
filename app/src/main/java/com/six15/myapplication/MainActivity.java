package com.six15.myapplication;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;


import com.intel.webrtc.base.ActionCallback;
import com.intel.webrtc.base.ClientContext;
import com.intel.webrtc.base.ConnectionStats;
import com.intel.webrtc.base.LocalCameraStream;
import com.intel.webrtc.base.LocalCameraStreamParameters;
import com.intel.webrtc.base.MediaCodec;
import com.intel.webrtc.base.RemoteStream;
import com.intel.webrtc.base.Stream;
import com.intel.webrtc.base.WoogeenException;
import com.intel.webrtc.base.WoogeenSurfaceRenderer;
import com.intel.webrtc.p2p.PeerClient;
import com.intel.webrtc.p2p.PeerClientConfiguration;
import com.intel.webrtc.p2p.PublishOptions;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.EglBase;
import org.webrtc.PeerConnection;
import org.webrtc.RendererCommon;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity  implements SurfaceHolder.Callback{

    public static final String sServerID = "AAAAAAAAAAAAAAAAAAAAAA";
    private static final String TAG = "MainActivity";
    private String callerId = null;

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

    private SurfaceView ov;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private String publishPeerId = "";
    private boolean isConnected = false;
    private boolean isSendingVideo = false;
    private boolean isReceivingVideo = false;

    private WoogeenSurfaceRenderer localSurfaceRenderer;
    private WoogeenSurfaceRenderer remoteSurfaceRenderer;
    private Stream.VideoRendererInterface localStreamRenderer;
    private Stream.VideoRendererInterface remoteStreamRenderer;

    private WoogeenSampleView localView;
    private WoogeenSampleView remoteView;

    // Intel Updates
    private String selfId = "";
    private String destId = "";
    private String server;
    private PeerClient peerClient;
    private LocalCameraStream localStream;
    private EglBase rootEglBase;
    private LinearLayout remoteViewContainer, localViewContainer;
    private HandlerThread peerThread;
    private PeerHandler peerHandler;
    private Message message;
    private String msgString;

    private final static int LOGIN = 1;
    private final static int LOGOUT = 2;
    private final static int INVITE = 3;
    private final static int STOP = 4;
    private final static int PUBLISH = 5;
    private final static int UNPUBLISH = 6;
    private final static int SWITCH_CAMERA = 7;
    private final static int SEND_DATA = 8;

    //default camera is the front camera
    private boolean mirror = true;

    private Timer statsTimer;

    private SurfaceHolder sh;

    private ImageView mIndicator;
    private ArrayList<TextMessage> mMessageList;

    private ArrayList<BaseObject> drawObjectList;

    /**
     * Id to identify a camera permission request.
     */
    private static final int REQUEST_CAMERA = 0;
    private static final int REQUEST_AUDIO = 0;

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

        AudioManager audioManager = ((AudioManager) getSystemService(AUDIO_SERVICE));
        audioManager.setSpeakerphoneOn(true);

        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            requestCameraPermission();

        }else{

            Log.i(TAG,
                    "CAMERA permission has already been granted.");
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            requestAudioPermission();

        }else{

            Log.i(TAG,
                    "RECORD_AUDIO permission has already been granted.");
        }

        localViewContainer = (LinearLayout) findViewById(R.id.local_view_container);
        remoteViewContainer = (LinearLayout) findViewById(R.id.remote_view_container);

      //  mSignalingServerAddress = "ws://" + getResources().getString(R.string.server_host);
      //  mIndicator = (ImageView)findViewById(R.id.imgDisplay);
      //  mIndicator.setImageResource(R.drawable.stop_icn);

        mMessageList = new ArrayList<>();
        drawObjectList = new ArrayList<>();

        ov = (SurfaceView) findViewById(R.id.overlaySurface);
        ov.setZOrderMediaOverlay(true);

        ov.setBackgroundColor(Color.TRANSPARENT);
/*
        ov.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //drawSquare(event.getRawX(), event.getRawY(),event.getRawX()+40, event.getRawY()+40,Color.YELLOW);
                //drawCircle(event.getRawX(), event.getRawY(), 60, Color.BLUE);
                return false;
            }
        });

*/

        sh = ov.getHolder();
        sh.setFormat(PixelFormat.TRANSPARENT);
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8);
        sh.addCallback(this);

        final Intent intent = getIntent();
        final String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
            final List<String> segments = intent.getData().getPathSegments();
            callerId = segments.get(0);
        }

        try {
            initVideoStreamsViews();
            initAudioControl();
            initPeerClient();
        } catch (WoogeenException e) {
            e.printStackTrace();
        }

    }

    private void initVideoStreamsViews() throws WoogeenException{

        localView = new WoogeenSampleView(this);
        localSurfaceRenderer = new WoogeenSurfaceRenderer(localView);
        localViewContainer.addView(localView);
        localStreamRenderer = localSurfaceRenderer.createVideoRenderer(0, 0, 100, 100, RendererCommon.ScalingType.SCALE_ASPECT_FILL, true);
        localView.setVisibility(View.INVISIBLE);

        remoteView = new WoogeenSampleView(this);
        remoteSurfaceRenderer = new WoogeenSurfaceRenderer(remoteView);
        remoteViewContainer.addView(remoteView);
        remoteStreamRenderer = remoteSurfaceRenderer.createVideoRenderer(0, 0, 100, 100, RendererCommon.ScalingType.SCALE_ASPECT_FILL, false);
        remoteView.setVisibility(View.INVISIBLE);
    }

    private void initPeerClient(){
        try{
            // Initialization work.
            rootEglBase = new EglBase();

            ClientContext.setApplicationContext(this, rootEglBase.getContext());
            List<PeerConnection.IceServer> iceServers = new ArrayList<PeerConnection.IceServer>();
            iceServers.add(new PeerConnection.IceServer("stun:23.21.150.121"));
            iceServers.add(new PeerConnection.IceServer(
                    "turn:61.152.239.60:4478?transport=udp", "woogeen",
                    "master"));
            iceServers.add(new PeerConnection.IceServer(
                    "turn:61.152.239.60:4478?transport=tcp", "woogeen",
                    "master"));
            PeerClientConfiguration config = new PeerClientConfiguration();
            config.setIceServers(iceServers);
            config.setVideoCodec(MediaCodec.VideoCodec.H264);
            peerClient = new PeerClient(config, new SocketSignalingChannel());
            peerClient.addObserver(observer);
            peerThread = new HandlerThread("PeerThread");
            peerThread.start();
            peerHandler = new PeerHandler(peerThread.getLooper());
        }catch(WoogeenException e1){
            e1.printStackTrace();
        }
    }

    private void initAudioControl(){
        try {
            Properties p = new Properties();
            InputStream s = this.getAssets().open("audio_control.properties");
            p.load(s);

            ClientContext.setAudioControlEnabled(Boolean.parseBoolean(p.getProperty("enable_audio_control")));
            ClientContext.setAudioLevelOverloud(Integer.parseInt(p.getProperty("audio_level_overloud")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void init() {
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
    }

    private void requestCameraPermission() {
        Log.i(TAG, "CAMERA permission has NOT been granted. Requesting permission.");


        // Camera permission has not been granted yet. Request it directly.
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                REQUEST_CAMERA);

    }

    private void requestAudioPermission() {
        Log.i(TAG, "RECORD_AUDIO permission has NOT been granted. Requesting permission.");


        // Camera permission has not been granted yet. Request it directly.
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_AUDIO);

    }

    @Override
    public void onStart()
    {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onPause() {
        if (localStream != null) {
            localStream.disableVideo();
            localStream.disableAudio();
            Toast.makeText(this, "Woogeen is running in the background.",
                    Toast.LENGTH_SHORT).show();
        }

        if(isConnected) {
            // log into server
            message = peerHandler.obtainMessage();
            message.what = LOGOUT;
            message.sendToTarget();
        }

        super.onPause();
    }

    @Override
    public void onResume() {

        if(!isConnected) {
            // log into server
            message = peerHandler.obtainMessage();
            message.what = LOGIN;
            message.sendToTarget();
        }

        if (localStream != null) {
            localStream.enableVideo();
            localStream.enableAudio();
            Toast.makeText(this, "Welcome back", Toast.LENGTH_SHORT).show();
        }
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private String getPhoneName() {
        BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
        String deviceName = myDevice.getName();
        return deviceName.replaceAll("\\s","");
    }

    private void updateDisplay(){

        Canvas canvas = sh.lockCanvas();
        int x = canvas.getWidth();
        int y = canvas.getHeight();
        // clear screen and render objects

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        for(int c=0; c<drawObjectList.size(); c++){
            if(drawObjectList.get(c).hasSourceDimensions()){
                // set width and height of canvas
                Log.d("updateDisplay","Canvas Width: " + x + " Height: " + y + "" );
                drawObjectList.get(c).setCanvasWidth(x);
                drawObjectList.get(c).setCanvasHeight(y);
            }
            drawObjectList.get(c).drawObject(canvas,paint);
        }

        sh.unlockCanvasAndPost(canvas);

    }

    private void fnDrawHandler(JSONObject obj){
        String sdat;
        JSONObject data;
        String typ;
        String cl;
        int c;
        float x1;
        float y1;
        float x2;
        float y2;
        float r1;

        try {
            String sType = obj.getString("type");
            switch(sType){
                case "clear":
                    Log.d(TAG,"fnDrawHandler:Clear Screen");
                    drawObjectList.clear();

                    break;
                case "line":
                    Log.d(TAG,"fnDrawHandler:Draw Line");
                    sdat = obj.getString("data");
                    data = new JSONObject(sdat);
                    typ = data.getString("p");
                    cl = data.getString("c");
                    c = Color.parseColor(cl);
                    x1 = BigDecimal.valueOf(data.getDouble("x1")).floatValue();
                    y1 = BigDecimal.valueOf(data.getDouble("y1")).floatValue();
                    x2 = BigDecimal.valueOf(data.getDouble("x2")).floatValue();
                    y2 = BigDecimal.valueOf(data.getDouble("y2")).floatValue();
                    LineObject lo = new LineObject(x1,y1,x2,y2,c);
                    // check if translation is needed
                    if(!data.isNull("w") && !data.isNull("h")){
                        lo.setSrcWidth(data.getInt("w"));
                        lo.setSrcHeight(data.getInt("h"));
                    }
                    drawObjectList.add(lo);
                    break;
                case "circle":
                    Log.d(TAG,"fnDrawHandler:Draw Circle");
                    sdat = obj.getString("data");
                    data = new JSONObject(sdat);
                    typ = data.getString("p");
                    cl = data.getString("c");
                    c = Color.parseColor(cl);
                    x1 = BigDecimal.valueOf(data.getDouble("x1")).floatValue();
                    y1 = BigDecimal.valueOf(data.getDouble("y1")).floatValue();
                    r1 = BigDecimal.valueOf(data.getDouble("r1")).floatValue();
                    CircleObject co = new CircleObject(x1,y1,r1,c);
                    // check if translation is needed
                    if(!data.isNull("w") && !data.isNull("h")){
                        co.setSrcWidth(data.getInt("w"));
                        co.setSrcHeight(data.getInt("h"));
                    }
                    drawObjectList.add(co);
                    break;
                case "square":
                    Log.d(TAG,"fnDrawHandler:Draw Square");
                    sdat = obj.getString("data");
                    data = new JSONObject(sdat);
                    typ = data.getString("p");
                    cl = data.getString("c");
                    c = Color.parseColor(cl);
                    x1 = BigDecimal.valueOf(data.getDouble("x1")).floatValue();
                    y1 = BigDecimal.valueOf(data.getDouble("y1")).floatValue();
                    x2 = BigDecimal.valueOf(data.getDouble("x2")).floatValue();
                    y2 = BigDecimal.valueOf(data.getDouble("y2")).floatValue();
                    SquareObject so = new SquareObject (y1,x1,y2,x2,  Color.argb(125, 255, 0, 0));
                    // check if translation is needed
                    if(!data.isNull("w") && !data.isNull("h")){
                        so.setSrcWidth(data.getInt("w"));
                        so.setSrcHeight(data.getInt("h"));
                    }
                    drawObjectList.add(so);

                    break;
                default:
                    Log.d(TAG,"fnDrawHandler: Unkown Draw Command");
                    break;
            }


            runOnUiThread(new Runnable() {
                public void run() {
                    updateDisplay();
                }
            });

        }catch(Exception e){
            Log.e(TAG, "fnDrawHandler Error: " + e.getLocalizedMessage());
        }
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
        /*
        EditText editText = (EditText)findViewById(R.id.message);
        String message = editText.getText().toString();

        try {
            String to = null;
            //client.sendTextMessage(to,"message",message);
        }catch(Exception ex){
            Log.e("ERROR", ex.getMessage());
        }

        editText.setText("");
        */
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        Log.i(TAG, "surfaceCreated");
        Canvas canvas = sh.lockCanvas();
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
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

    PeerClient.PeerClientObserver observer = new PeerClient.PeerClientObserver() {
        @Override
        public void onServerDisconnected() {
            remoteStreamRenderer.cleanFrame();
            localStreamRenderer.cleanFrame();
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this, "onServerDisconnected",
                            Toast.LENGTH_SHORT).show();
                    /*
                    loginBtn.setEnabled(true);
                    logoutBtn.setEnabled(false);
                    startVideoBtn.setEnabled(false);
                    stopVideoBtn.setEnabled(false);
                    switchCameraBtn.setEnabled(false);
                    connectBtn.setEnabled(false);
                    disconnectBtn.setEnabled(false);
                    sendMsgBtn.setEnabled(false);
                    selfIdEdTx.setEnabled(true);
                    serverEdTx.setEnabled(true);
                    */

                    isConnected = false;
                }
            });
        }

        @Override
        public void onInvited(final String peerId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    destId = peerId;
                    // auto accept
                    peerClient.accept(destId,
                            new ActionCallback<Void>() {

                                @Override
                                public void onSuccess(Void result) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(MainActivity.this,"Connected to " + peerId, Toast.LENGTH_SHORT).show();
                                                            /*
                                                            sendMsgBtn.setEnabled(true);
                                                            startVideoBtn.setEnabled(true);
                                                            destIdEdTx.setText(destId);
                                                            disconnectBtn.setEnabled(true);
                                                            */
                                        }});
                                }

                                @Override
                                public void onFailure(
                                        WoogeenException e) {
                                    Log.d(TAG, e.getMessage());
                                }

                            });
                }
            });
        }

        @Override
        public void onDenied(final String peerId) {
            Log.d(TAG, "onDenied:" + peerId);
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this,
                            "Receive Deny from " + peerId, Toast.LENGTH_SHORT)
                            .show();
                }
            });
        }

        @Override
        public void onAccepted(final String peerId) {
            Log.d(TAG, "onAccepted:" + peerId);
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this,
                            "Receive Accept from " + peerId, Toast.LENGTH_SHORT)
                            .show();
                    /*
                    sendMsgBtn.setEnabled(true);
                    startVideoBtn.setEnabled(true);
                    disconnectBtn.setEnabled(true);
                    */
                }
            });
        }

        @Override
        public void onChatStopped(final String peerId) {
            Log.d(TAG, "onChatStop:" + peerId);
            runOnUiThread(new Runnable() {
                public void run(){
                    /*
                    stopVideoBtn.setEnabled(false);
                    startVideoBtn.setEnabled(false);
                    */
                    Toast.makeText(MainActivity.this, "onChatStop:" + peerId,
                            Toast.LENGTH_SHORT).show();
                }
            });
            remoteStreamRenderer.cleanFrame();
            localStreamRenderer.cleanFrame();
            if(statsTimer != null){
                statsTimer.cancel();
            }
        }

        @Override
        public void onChatStarted(final String peerId) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this,
                            "onChatStart:" + peerId, Toast.LENGTH_SHORT).show();
                   /*
                    startVideoBtn.setEnabled(true);
                    sendMsgBtn.setEnabled(true);
                    */
                }
            });

            //This is a sample usage of get the statistic data for the peerconnection including all the streams
            //ever been published. If you would like to get the data for a specific stream,  please refer to the
            //sample code in the onSuccess callback of publish.
            //ATTENTION: DO NOT use getConnectionStats(), getConnectionStats(localstream)and getAudioLevels() at the same time.
            /*statsTimer = new Timer();
            statsTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    peerClient.getConnectionStats(destId, new ActionCallback<ConnectionStats>() {
                        @Override
                        public void onSuccess(ConnectionStats result) {
                            Log.d(TAG, "connection stats: " + result.timeStamp
                                      +" available transmit bitrate: " + result.videoBandwidthStats.transmitBitrate
                                      +" retransmit bitrate: " + result.videoBandwidthStats.reTransmitBitrate);
                        }

                        @Override
                        public void onFailure(WoogeenException e) {
                        }
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    peerClient.getAudioLevels(destId, new ActionCallback<AudioLevels>(){

                        @Override
                        public void onSuccess(AudioLevels audioLevels) {
                            Log.d(TAG, "audio input levels: ");
                            for(AudioLevels.AudioLevel al : audioLevels.getInputLevelList())
                                Log.d(TAG, al.ssrcId + ":" + al.level);
                            Log.d(TAG, "audio output levels: ");
                            for(AudioLevels.AudioLevel al : audioLevels.getOutputLevelList())
                                Log.d(TAG, al.ssrcId + ":" + al.level);
                        }

                        @Override
                        public void onFailure(WoogeenException e) {
                            Log.d(TAG, "Failed to get audio level:" + e.getMessage());
                        }

                    });
                }
            }, 0, 10000);*/
        }

        @Override
        public void onDataReceived(final String peerId, final String msg) {

            Log.d(TAG, "onDataReceived from: " + peerId + "data: " + msg );
            try {
                JSONObject data = new JSONObject(msg);
                String sHandler = data.getString("handler");
                if (sHandler.compareTo("message")==0){
                    final String fn = data.getString("dispname");
                    final String fmsg = data.getString("data");
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this,"" + fn + ": " + fmsg, Toast.LENGTH_LONG).show();
                        }
                    });

                }else if(sHandler.compareTo("request")==0){
                    String cmd = data.getString("type");

                    if(cmd.compareTo("play")==0){
                        destId = peerId;
                        message = peerHandler.obtainMessage();
                        message.what = PUBLISH;
                        message.sendToTarget();
                    }else{
                        destId = peerId;
                        message = peerHandler.obtainMessage();
                        message.what = UNPUBLISH;
                        message.sendToTarget();
                    }
                }else if(sHandler.compareTo("draw")==0){
                    fnDrawHandler(data);
                }

            }catch (Exception e){
                Log.e(TAG, "onDataReceived Error: " + e.getLocalizedMessage());
            }

        }

        @Override
        public void onStreamAdded(final RemoteStream stream) {
            Log.d(TAG, "onStreamAdded : from " + stream.getRemoteUserId());
            isReceivingVideo = true;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        stream.attach(remoteStreamRenderer);
                        remoteView.setVisibility(View.VISIBLE);
                        Toast.makeText(MainActivity.this, "Added remote stream "
                                        + "from "+stream.getRemoteUserId(),
                                Toast.LENGTH_LONG).show();

                    } catch (WoogeenException e) {
                        Log.d(TAG, e.getMessage());
                    }
                }
            });
        }

        @Override
        public void onStreamRemoved(final RemoteStream stream) {
            Log.d(TAG, "onStreamRemoved");
            isReceivingVideo = false;
            remoteStreamRenderer.cleanFrame();
            runOnUiThread(new Runnable() {
                public void run() {
                    remoteView.setVisibility(View.INVISIBLE);
                    Toast.makeText(MainActivity.this, "onStreamRemoved",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    class PeerHandler extends Handler {

        public PeerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LOGIN:
                    selfId = getPhoneName();
                    server = getResources().getString(R.string.serverIp);
                    JSONObject loginObject = new JSONObject();
                    try {
                        loginObject.put("host", server);
                        loginObject.put("token", selfId);
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                    }
                    peerClient.connect(loginObject.toString(),
                            new ActionCallback<String>() {

                                @Override
                                public void onSuccess(String result) {
                                    isConnected = true;
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            Toast.makeText(MainActivity.this,
                                                    "onServerConnected",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }

                                @Override
                                public void onFailure(WoogeenException e) {
                                    Log.d(TAG, "Failed to connect server:" + e.getMessage());

                                    isSendingVideo = false;
                                    isReceivingVideo = false;
                                    isConnected = false;

                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            Toast.makeText(MainActivity.this,
                                                    "onServerConnectFailed",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            });
                    break;
                case LOGOUT:
                    peerClient.disconnect(new ActionCallback<Void>() {

                        @Override
                        public void onSuccess(Void result) {
                            if (localStream != null) {
                                localStream.close();
                                localStream = null;
                                localStreamRenderer.cleanFrame();
                            }
                            isSendingVideo = false;
                            isReceivingVideo = false;
                            isConnected = false;
                            runOnUiThread(new Runnable() {
                                public void run() {
                                remoteView.setVisibility(View.INVISIBLE);
                                localView.setVisibility(View.INVISIBLE);
                                }
                            });
                        }

                        @Override
                        public void onFailure(WoogeenException e) {
                            Log.d(TAG, e.getMessage());
                        }
                    });
                    break;
                case INVITE:
                    destId = "TODO:ID_OF_TARGET";
                    peerClient.invite(destId, new ActionCallback<Void>() {

                        @Override
                        public void onSuccess(Void result) {
                        }

                        @Override
                        public void onFailure(WoogeenException e) {
                            Log.d(TAG, e.getMessage());
                        }

                    });
                    break;
                case STOP:
                    destId = "TODO:ID_OF_TARGET";
                    peerClient.stop(destId, new ActionCallback<Void>() {

                        @Override
                        public void onSuccess(Void result) {
                            if (localStream != null) {
                                localStream.close();
                                localStream = null;
                                localStreamRenderer.cleanFrame();
                            }
                        }

                        @Override
                        public void onFailure(WoogeenException e) {
                            Log.d(TAG, e.getMessage());
                        }

                    });
                    break;
                case PUBLISH:
                    if (localStream == null) {
                        LocalCameraStreamParameters msp;
                        try {
                            msp = new LocalCameraStreamParameters(
                                    true, true);
                            msp.setResolution(320, 240);
                            localStream = new LocalCameraStream(msp);
                            localStream.attach(localStreamRenderer);
                        } catch (WoogeenException e1) {
                            e1.printStackTrace();
                            if (localStream != null) {
                                localStream.close();
                                localStream = null;
                                localStreamRenderer.cleanFrame();
                            }
                        }
                    }
                    PublishOptions option = new PublishOptions();
                    option.setMaximumVideoBandwidth(200);
                    option.setMaximumAudioBandwidth(30);
                    peerClient.publish(localStream, destId, option,
                            new ActionCallback<Void>() {

                                @Override
                                public void onSuccess(Void result) {
                                    publishPeerId = destId;
                                    isSendingVideo = true;
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            localView.setVisibility(View.VISIBLE);
                                        }
                                    });

                                    //This is a sample usage of get the statistic data for a specific stream, in this sample, localStream
                                    //that has just been published. If you would like to get all the data for the peerconnection, including
                                    //the data for the streams had been published before, please refer to the sample code in onChatStarted.
                                    //ATTENTION: DO NOT use getConnectionStats(), getConnectionStats(localstream)and getAudioLevels() at the same time.
                                    statsTimer = new Timer();
                                    statsTimer.schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            peerClient.getConnectionStats(destId, localStream, new ActionCallback<ConnectionStats>() {
                                                @Override
                                                public void onSuccess(ConnectionStats result) {
                                                    Log.d(TAG, "result:" + result.mediaTracksStatsList.size());
                                                    Log.d(TAG, "connection stats: " + result.timeStamp
                                                            +" available transmit bitrate: " + result.videoBandwidthStats.transmitBitrate
                                                            +" retransmit bitrate: " + result.videoBandwidthStats.reTransmitBitrate);
                                                }

                                                @Override
                                                public void onFailure(WoogeenException e) {
                                                }
                                            });

                                        }
                                    }, 0, 10000);
                                }

                                @Override
                                public void onFailure(WoogeenException e) {
                                    Log.d(TAG, e.getMessage());
                                    if (localStream != null) {
                                        localStream.close();
                                        localStream = null;
                                        localStreamRenderer.cleanFrame();
                                    }
                                    isSendingVideo = false;
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            localView.setVisibility(View.INVISIBLE);
                                        }
                                    });
                                }
                            });
                    break;
                case UNPUBLISH:
                    if (localStream != null) {
                        peerClient.unpublish(localStream, publishPeerId,
                                new ActionCallback<Void>() {

                                    @Override
                                    public void onSuccess(Void result) {
                                        localStream.close();
                                        localStream = null;
                                        localStreamRenderer.cleanFrame();

                                        isSendingVideo = false;
                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                localView.setVisibility(View.INVISIBLE);
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(WoogeenException e) {
                                        Log.d(TAG, e.getMessage());
                                        isSendingVideo = false;
                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                localView.setVisibility(View.INVISIBLE);
                                            }
                                        });
                                    }

                                });
                    }
                    break;
                case SWITCH_CAMERA:
                    localStream.switchCamera(new ActionCallback<Boolean>(){

                        @Override
                        public void onSuccess(final Boolean isFrontCamera) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    // switchCameraBtn.setEnabled(true);
                                    Toast.makeText(MainActivity.this,
                                            "Switch to " + (isFrontCamera ? "front" : "back") + " camera.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                            //mirror = !mirror;
                            localSurfaceRenderer.update(localStreamRenderer, 0, 0, 100, 100, RendererCommon.ScalingType.SCALE_ASPECT_FILL, mirror);
                        }

                        @Override
                        public void onFailure(final WoogeenException e) {
                            runOnUiThread(new Runnable(){

                                @Override
                                public void run() {
                                    // switchCameraBtn.setEnabled(true);
                                    Toast.makeText(MainActivity.this,
                                            "Failed to switch camera. " + e.getLocalizedMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }

                            });
                        }

                    });
                    break;
                case SEND_DATA:
                    msgString = "TODO:MESSAGE_STRING";;
                    destId = "TODO:ID_OF_TARGET";
                    Log.d(TAG, "send data:" + msgString + " to " + destId);
                    peerClient.send(msgString, destId, new ActionCallback<Void>() {

                        @Override
                        public void onSuccess(Void result) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(MainActivity.this,
                                            "Sent successfully.",Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onFailure(final WoogeenException e) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(MainActivity.this, e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                            Log.d(TAG, e.getMessage());
                        }

                    });
            }
            super.handleMessage(msg);

        }
    }
}
