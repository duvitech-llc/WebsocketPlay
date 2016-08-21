package com.six15.myapplication;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
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


import com.intel.webrtc.base.ClientContext;
import com.intel.webrtc.base.LocalCameraStream;
import com.intel.webrtc.base.MediaCodec;
import com.intel.webrtc.base.RemoteStream;
import com.intel.webrtc.base.Stream;
import com.intel.webrtc.base.WoogeenException;
import com.intel.webrtc.base.WoogeenSurfaceRenderer;
import com.intel.webrtc.p2p.PeerClient;
import com.intel.webrtc.p2p.PeerClientConfiguration;

import org.webrtc.EglBase;
import org.webrtc.PeerConnection;
import org.webrtc.RendererCommon;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Timer;

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

    private GLSurfaceView vsv;
    private VideoRenderer.Callbacks remoteRender;
    private VideoRenderer.Callbacks localRender;
    private String mSignalingServerAddress;
    private SurfaceView ov;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

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


        mSignalingServerAddress = "ws://" + getResources().getString(R.string.server_host);
      //  mIndicator = (ImageView)findViewById(R.id.imgDisplay);
      //  mIndicator.setImageResource(R.drawable.stop_icn);

        mMessageList = new ArrayList<>();
        drawObjectList = new ArrayList<>();

        vsv.setBackgroundColor(Color.BLACK);

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
        sh.addCallback(this);
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8);

        // local and remote render
        remoteRender = VideoRendererGui.create(
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, null, false);

        localRender = VideoRendererGui.create(
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, null, true);

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

        remoteView = new WoogeenSampleView(this);
        remoteSurfaceRenderer = new WoogeenSurfaceRenderer(remoteView);
        remoteViewContainer.addView(remoteView);
        remoteStreamRenderer = remoteSurfaceRenderer.createVideoRenderer(0, 0, 100, 100, RendererCommon.ScalingType.SCALE_ASPECT_FILL, false);

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
        super.onPause();
    }

    @Override
    public void onResume() {
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
        return deviceName;
    }

    private void updateDisplay(){

        Canvas canvas = sh.lockCanvas();
        int x = canvas.getWidth();
        int y = canvas.getHeight();
        // clear screen and render objects

        canvas.drawColor(Color.BLACK);
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

    PeerClient.PeerClientObserver observer = new PeerClient.PeerClientObserver() {

        @Override
        public void onServerDisconnected() {

        }

        @Override
        public void onInvited(String s) {

        }

        @Override
        public void onDenied(String s) {

        }

        @Override
        public void onAccepted(String s) {

        }

        @Override
        public void onChatStopped(String s) {

        }

        @Override
        public void onChatStarted(String s) {

        }

        @Override
        public void onDataReceived(String s, String s1) {

        }

        @Override
        public void onStreamAdded(RemoteStream remoteStream) {

        }

        @Override
        public void onStreamRemoved(RemoteStream remoteStream) {

        }
    };

    class PeerHandler extends Handler {

        public PeerHandler(Looper looper) {
            super(looper);
        }
    }
}
