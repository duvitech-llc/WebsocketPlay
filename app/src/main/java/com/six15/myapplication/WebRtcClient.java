package com.six15.myapplication;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoSource;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.graphics.Color;
import android.opengl.EGLContext;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketState;

import org.webrtc.*;

/**
 * Created by George on 5/3/2016.
 */
public class WebRtcClient {
    private final static String TAG = WebRtcClient.class.getCanonicalName();
    private final static int MAX_PEER = 2;
    private boolean[] endPoints = new boolean[MAX_PEER];
    private PeerConnectionFactory factory;
    private HashMap<String, Peer> peers = new HashMap<>();
    private LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<>();
    private PeerConnectionParameters pcParams;
    private MediaConstraints pcConstraints = new MediaConstraints();
    private MediaStream localMS;
    private VideoSource videoSource;
    private RtcListener mListener;
    private String assignedId = "UNKNOWN";
    private HashMap<String, Command> commandMap;
    private String friendlyName = null;

    WebSocket ws = null;

    Gson gson = new Gson();
    JsonParser parser = new JsonParser();

    /**
     * Implement this interface to be notified of events.
     */
    public interface RtcListener{
        void onCallReady(String callId);

        void onStatusChanged(String newStatus);

        void onMessage(String from, String msg);

        void onLocalStream(MediaStream localStream);

        void onAddRemoteStream(MediaStream remoteStream, int endPoint);

        void onRemoveRemoteStream(int endPoint);

        void clearScreen();

        void drawLine(LineObject lo);
        void drawCircle(CircleObject co);
        void drawSquare(SquareObject so);
    }

    private interface Command{
        void execute(String peerId, JsonObject payload) throws JSONException;
    }
    private class CreateOfferCommand implements Command{
        public void execute(String peerId, JsonObject payload) throws JSONException {
            Log.d(TAG,"CreateOfferCommand");
            Peer peer = peers.get(peerId);
            peer.pc.createOffer(peer, pcConstraints);
        }
    }

    private class CreateAnswerCommand implements Command{
        public void execute(String peerId, JsonObject payload) throws JSONException {
            Log.d(TAG,"CreateAnswerCommand");
            Peer peer = peers.get(peerId);
            Log.d(TAG,"payload: " + payload.toString());
            SessionDescription sdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(payload.get("type").toString()),
                    payload.get("sdp").toString()
            );
            peer.pc.setRemoteDescription(peer, sdp);
            peer.pc.createAnswer(peer, pcConstraints);
        }
    }

    private class SetRemoteSDPCommand implements Command{
        public void execute(String peerId, JsonObject payload) throws JSONException {
            Log.d(TAG,"SetRemoteSDPCommand");
            Log.d(TAG,"SetRemoteSDPCommand PeerId: " + peerId);
            Peer peer = peers.get(peerId);
            Log.d(TAG,"payload: " + payload.toString());
            Log.d(TAG,"type: " + payload.get("type"));
            String sType = payload.get("type").getAsString();

            SessionDescription sdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(sType),
                    payload.get("sdp").getAsString()
            );

            peer.pc.setRemoteDescription(peer, sdp);
        }
    }

    private class AddIceCandidateCommand implements Command{
        public void execute(String peerId, JsonObject payload) throws JSONException {
            Log.d(TAG,"AddIceCandidateCommand");
            PeerConnection pc = peers.get(peerId).pc;
            Log.d(TAG,"payload: " + payload.toString());
            if (pc.getRemoteDescription() != null) {
                IceCandidate candidate = new IceCandidate(
                        payload.get("id").getAsString(),
                        payload.get("label").getAsInt(),
                        payload.get("candidate").getAsString()
                );
                pc.addIceCandidate(candidate);
            }
        }
    }

    /**
     * Send a message through the signaling server
     *
     * @param to id of recipient
     * @param type type of message
     * @param payload payload of message
     * @throws JSONException
     */
    public void sendMessage(String to, String type, JsonElement payload) throws JSONException {
        if(ws != null && ws.isOpen()) {
            JSONObject message = new JSONObject();
            message.put("clientId", assignedId);
            message.put("handler", "message");
            message.put("dispname", friendlyName);
            message.put("type", type);
            message.put("datatype", "json");
            message.put("to", to);
            message.put("from", assignedId);
            message.put("payload", payload.toString());
            message.put("data", null);
            message.put("date", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));

            ws.sendText(message.toString());
        }else{
            Log.e(TAG, "Websocket null or closed Error.");
        }
    }

    public void sendTextMessage(String to, String type, String data) throws JSONException {
        if(ws != null && ws.isOpen()) {
            JSONObject message = new JSONObject();
            message.put("clientId", assignedId);
            message.put("handler", "message");
            message.put("dispname", friendlyName);
            message.put("type", type);
            message.put("datatype", "text");
            message.put("to", to);
            message.put("from", assignedId);
            message.put("payload", null);
            message.put("data", data);
            message.put("date", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));

            ws.sendText(message.toString());
        }else{
            Log.e(TAG, "Websocket null or closed Error.");
        }
    }

    private class Peer implements SdpObserver, PeerConnection.Observer{
        private PeerConnection pc;
        private String id;
        private int endPoint;

        @Override
        public void onCreateSuccess(final SessionDescription sdp) {
            // TODO: modify sdp to use pcParams prefered codecs
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("type", sdp.type.canonicalForm());
                payload.addProperty("sdp", sdp.description);
                sendMessage(id, sdp.type.canonicalForm(), payload);
                pc.setLocalDescription(Peer.this, sdp);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSetSuccess() {}

        @Override
        public void onCreateFailure(String s) {}

        @Override
        public void onSetFailure(String s) {}

        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {}

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            if(iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
                removePeer(id);
                mListener.onStatusChanged("DISCONNECTED");
            }
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {}

        @Override
        public void onIceCandidate(final IceCandidate candidate) {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("label", candidate.sdpMLineIndex);
                payload.addProperty("id", candidate.sdpMid);
                payload.addProperty("candidate", candidate.sdp);
                sendMessage(id, "candidate", payload);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.d(TAG,"onAddStream "+mediaStream.label());
            // remote streams are displayed from 1 to MAX_PEER (0 is localStream)
            mListener.onAddRemoteStream(mediaStream, endPoint+1);
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            Log.d(TAG,"onRemoveStream "+mediaStream.label());
            removePeer(id);
        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {}

        @Override
        public void onRenegotiationNeeded() {

        }

        public Peer(String id, int endPoint) {
            Log.d(TAG,"new Peer: "+id + " " + endPoint);
            this.pc = factory.createPeerConnection(iceServers, pcConstraints, this);
            this.id = id;
            this.endPoint = endPoint;

            pc.addStream(localMS); //, new MediaConstraints()

            mListener.onStatusChanged("CONNECTING");
        }
    }

    private Peer addPeer(String id, int endPoint) {
        Peer peer = new Peer(id, endPoint);
        peers.put(id, peer);

        endPoints[endPoint] = true;
        return peer;
    }

    private void removePeer(String id) {
        Peer peer = peers.get(id);
        mListener.onRemoveRemoteStream(peer.endPoint);
        peer.pc.close();
        peers.remove(peer.id);
        endPoints[peer.endPoint] = false;
    }

    private void connectWebSocket(String serverAddress) {

        this.commandMap = new HashMap<>();

        commandMap.put("init", new CreateOfferCommand());
        commandMap.put("offer", new CreateAnswerCommand());
        commandMap.put("answer", new SetRemoteSDPCommand());
        commandMap.put("candidate", new AddIceCandidateCommand());

        try {
            ws = new WebSocketFactory().createSocket(serverAddress);
            ws.addListener(new WebSocketAdapter(){
                @Override
                public void onFrame(WebSocket websocket, WebSocketFrame frame) throws Exception
                {
                    Log.i("MESSAGE", "onFrame");
                    Log.i("MESSAGE", frame.toString());
                }


                @Override
                public void onContinuationFrame(WebSocket websocket, WebSocketFrame frame) throws Exception
                {
                    Log.i("MESSAGE", "onContinuationFrame");
                }


                @Override
                public void onTextFrame(WebSocket websocket, WebSocketFrame frame) throws Exception
                {
                    Log.i("MESSAGE", "onTextFrame");
                }


                @Override
                public void onBinaryFrame(WebSocket websocket, WebSocketFrame frame) throws Exception
                {
                    Log.i("MESSAGE", "onBinaryFrame");
                }


                @Override
                public void onCloseFrame(WebSocket websocket, WebSocketFrame frame) throws Exception
                {
                    Log.i("MESSAGE", "onCloseFrame");
                }


                @Override
                public void onPingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception
                {
                    Log.i("MESSAGE", "onPingFrame");
                }


                @Override
                public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception
                {
                    Log.i("MESSAGE", "onPongFrame");
                }


                @Override
                public void onTextMessage(WebSocket websocket, String text) throws Exception
                {
                    String msgFrom;
                    String msgType;

                    // need to handle messaging here for now
                    Log.i("Handler", "onMessage " + text);
                    MessageData msg = gson.fromJson(text, MessageData.class);
                    switch (msg.getHandler()){
                        case "id":
                            assignedId = (String)msg.getData();
                            Log.i("Handler", "Our ID is: " + assignedId);
                            mListener.onCallReady(assignedId);
                            break;
                        case "message":
                            if(msg.getFrom() != null){
                                msgFrom = msg.getFrom();
                            }else if(msg.getClientID() != null){
                                msgFrom = msg.getClientID();
                            }
                            else
                                msgFrom = "Unknown";
                            msgType = msg.getType();
                            if(msgType != null) {
                                Log.i("WEBRTC", "MessageHandler");
                                if(msgType.compareTo("message")==0 && msg.getDatatype() != null && msg.getDatatype().compareTo("text")==0){
                                    // text message
                                    Log.i("TextMessage", msgFrom + ": " + msg.getData());
                                    mListener.onMessage(msgFrom, (String) msg.getData());
                                }else{
                                    JsonObject payload = msg.getPayload().isJsonNull()? new JsonObject():msg.getPayload().getAsJsonObject();
                                    try {
                                        if (msgType.compareTo("init")!=0) {
                                            Log.i(TAG, "Init call");
                                        }
                                        // if peer is unknown and is not the server, try to add him
                                        // though at some point the server may stream directly to the HUD
                                        if (!peers.containsKey(msgFrom) && msgFrom.compareTo(MainActivity.sServerID)!=0) {
                                            // if MAX_PEER is reach, ignore the call
                                            Log.i(TAG, "Add Peer");
                                            int endPoint = findEndPoint();
                                            if (endPoint != MAX_PEER) {
                                                Peer peer = addPeer(msgFrom, endPoint);
                                                peer.pc.addStream(localMS);
                                                commandMap.get(msgType).execute(msgFrom, payload);
                                            }
                                        } else {
                                            Log.i(TAG, "Command Map: " + msgType);
                                            commandMap.get(msgType).execute(msgFrom, payload);
                                        }
                                    }catch(JSONException e){
                                        Log.e("Handler", "Payload Problem: " + e.getStackTrace());
                                    }
                                }
                            }else{
                                Log.e("Handler", "Message type is NULL: " + text);
                            }

                            break;
                        case "stream":
                            Log.i("stream", "Stream Handler: " + text);
                            break;
                        case "draw":
                            Log.i("draw", "Draw Handler: " + text);
                            msgType = msg.getType();
                            if(msgType != null) {
                                switch (msgType){
                                    case "clear":
                                        Log.d("clear", "Clear Display");
                                        mListener.clearScreen();
                                        break;
                                    case "line":
                                        Log.d("draw", "line: ");
                                        if(msg.getData() != null)
                                        {
                                            JsonObject o = parser.parse(msg.getData().toString()).getAsJsonObject();
                                            if(o.get("x1") != null && o.get("y1") != null && o.get("x2") != null && o.get("y2") != null) {

                                                LineObject lo = new LineObject(o.get("x1").getAsFloat(), o.get("y1").getAsFloat(),
                                                        o.get("x2").getAsFloat(), o.get("y2").getAsFloat(), Color.GREEN);

                                                if(o.get("w") != null && o.get("h") != null){
                                                    // we have width and height lets translate
                                                    lo.setSrcWidth(o.get("w").getAsInt());
                                                    lo.setSrcHeight(o.get("h").getAsInt());
                                                }

                                                mListener.drawLine(lo);

                                            }else{
                                                Log.d("draw", "line: Data received error");
                                            }
                                        }
                                        break;
                                    case "circle":
                                        Log.d("draw", "circle: ");
                                        if(msg.getData() != null)
                                        {
                                            JsonObject o = parser.parse(msg.getData().toString()).getAsJsonObject();
                                            if(o.get("x1") != null && o.get("y1") != null && o.get("r1") != null) {

                                                CircleObject co = new CircleObject(o.get("x1").getAsFloat(), o.get("y1").getAsFloat(),
                                                        o.get("r1").getAsFloat(), Color.YELLOW);

                                                if(o.get("w") != null && o.get("h") != null){
                                                    // we have width and height lets translate
                                                    co.setSrcWidth(o.get("w").getAsInt());
                                                    co.setSrcHeight(o.get("h").getAsInt());
                                                }

                                                mListener.drawCircle(co);

                                            }else{
                                                Log.d("draw", "circle: Data received error");
                                            }
                                        }
                                        break;
                                    case "square":
                                        Log.d("draw", "square: ");
                                        if(msg.getData() != null)
                                        {
                                            JsonObject o = parser.parse(msg.getData().toString()).getAsJsonObject();
                                            if(o.get("x1") != null && o.get("y1") != null && o.get("x2") != null && o.get("y2") != null) {

                                                SquareObject so = new SquareObject(o.get("y1").getAsFloat(), o.get("x1").getAsFloat(),
                                                        o.get("y2").getAsFloat(), o.get("x2").getAsFloat(), Color.RED);


                                                if(o.get("w") != null && o.get("h") != null){
                                                    // we have width and height lets translate
                                                    so.setSrcWidth(o.get("w").getAsInt());
                                                    so.setSrcHeight(o.get("h").getAsInt());
                                                }

                                                mListener.drawSquare(so);

                                            }else{
                                                Log.d("draw", "square: Data received error");
                                            }
                                        }
                                        break;
                                    default:
                                        Log.d("draw", "Unkown Drawing Command");
                                        break;
                                }
                            }
                            break;
                        default:
                            Log.e("MESSAGE", "UNKNOWN MESSAGE type data received: " + text);
                    }

                }


                @Override
                public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception
                {
                    Log.i("MESSAGE", "onBinaryMessage");
                }


                @Override
                public void onSendingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception
                {
                    Log.i("MESSAGE", "onSendingFrame");
                    Log.i("MESSAGE", frame.toString());
                }


                @Override
                public void onFrameSent(WebSocket websocket, WebSocketFrame frame) throws Exception
                {
                    Log.i("MESSAGE", "onFrameSent");
                }


                @Override
                public void onFrameUnsent(WebSocket websocket, WebSocketFrame frame) throws Exception
                {
                    Log.i("MESSAGE", "onFrameUnsent");
                }


                @Override
                public void onError(WebSocket websocket, WebSocketException cause) throws Exception
                {
                    Log.e("ERROR", "onError");
                }


                @Override
                public void onFrameError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception
                {
                    Log.e("ERROR", "onFrameError");
                }


                @Override
                public void onMessageError(WebSocket websocket, WebSocketException cause, List<WebSocketFrame> frames) throws Exception
                {
                    Log.e("ERROR", "onMessageError");
                }


                @Override
                public void onMessageDecompressionError(WebSocket websocket, WebSocketException cause, byte[] compressed) throws Exception
                {
                    Log.i("ERROR", "onMessageDecompressionError");
                }


                @Override
                public void onTextMessageError(WebSocket websocket, WebSocketException cause, byte[] data) throws Exception
                {
                    Log.e("ERROR", "onTextMessageError");
                }


                @Override
                public void onSendError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception
                {
                    Log.e("ERROR", "onSendError");
                }


                @Override
                public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception
                {
                    Log.e("ERROR", "onUnexpectedError");
                }


                @Override
                public void handleCallbackError(WebSocket websocket, Throwable cause) throws Exception
                {
                    Log.e("ERROR", "handleCallbackError");
                }


                @Override
                public void onSendingHandshake(WebSocket websocket, String requestLine, List<String[]> headers) throws Exception
                {

                    Log.i("MESSAGE", "onSendingHandshake");
                }

                @Override
                public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception
                {
                    // Received a text message.
                    Log.i("MESSAGE", "CONNNECTED");
                }

                @Override
                public void onStateChanged(WebSocket websocket, WebSocketState newState) throws Exception
                {
                    Log.i("MESSAGE", "STATE CHANGED");
                }

                @Override
                public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception
                {
                    Log.i("MESSAGE", "Error " + exception.getMessage());
                }


                @Override
                public void onDisconnected(WebSocket websocket,
                                           WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame,
                                           boolean closedByServer) throws Exception
                {
                    Log.i("MESSAGE", "DisCONNECTED");
                }

            });

            ws.connect();
        }catch (Exception ex){
            Log.e("ERROR", ex.getMessage());
        }

    }

    public WebRtcClient(RtcListener listener, String host, PeerConnectionParameters params, EGLContext mEGLcontext) {
        mListener = listener;
        pcParams = params;
        PeerConnectionFactory.initializeAndroidGlobals(listener, true, true,
                params.videoCodecHwAcceleration, mEGLcontext);
        factory = new PeerConnectionFactory();

        iceServers.add(new PeerConnection.IceServer("stun:23.21.150.121"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));

        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        pcConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));

        connectWebSocket(host);

    }

    /**
     * Call this method in Activity.onPause()
     */
    public void onPause() {
        Log.i(TAG, "WebRtcClient:onPause");
        if(videoSource != null) videoSource.stop();
    }

    public void onStop() {
        Log.i(TAG, "WebRtcClient:onStop");
    }

    /**
     * Call this method in Activity.onResume()
     */
    public void onResume() {
        Log.i(TAG, "WebRtcClient:onResume");
        if(videoSource != null) videoSource.restart();
    }

    /**
     * Call this method in Activity.onDestroy()
     */
    public void onDestroy() {
        Log.i(TAG, "WebRtcClient:onDestroy");
        for (Peer peer : peers.values()) {
            peer.pc.dispose();
        }
        if(videoSource != null)
            videoSource.stop();

        factory.dispose();

        if(ws != null){
            ws.disconnect();
        }

    }

    private int findEndPoint() {
        for(int i = 0; i < MAX_PEER; i++) if (!endPoints[i]) return i;
        return MAX_PEER;
    }

    /**
     * Start the client.
     *
     * Set up the local stream and notify the signaling server.
     * Call this method after onCallReady.
     *
     * @param name client name
     */
    public void start(String name){
        friendlyName = name;
        setCamera();
        try {
            JSONObject message = new JSONObject();
            message.put("clientId", assignedId);
            message.put("handler", "readyToStream");
            message.put("dispname", friendlyName);
            message.put("type", "readyToStream");
            message.put("message", "readyToStream");
            message.put("to", null);
            message.put("from", assignedId);
            message.put("payload", null);
            message.put("date", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
            ws.sendText(message.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setCamera(){
        localMS = factory.createLocalMediaStream("ARDAMS");
        if(pcParams.videoCallEnabled){
            MediaConstraints videoConstraints = new MediaConstraints();
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", Integer.toString(pcParams.videoHeight)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", Integer.toString(pcParams.videoWidth)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", Integer.toString(pcParams.videoFps)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", Integer.toString(pcParams.videoFps)));

            videoSource = factory.createVideoSource(getVideoCapturer(), videoConstraints);
            localMS.addTrack(factory.createVideoTrack("ARDAMSv0", videoSource));
        }

        AudioSource audioSource = factory.createAudioSource(new MediaConstraints());
        localMS.addTrack(factory.createAudioTrack("ARDAMSa0", audioSource));

        mListener.onLocalStream(localMS);
    }

    private VideoCapturer getVideoCapturer() {
        String nameOfBackFacingDevice = VideoCapturerAndroid.getNameOfBackFacingDevice();

        /*
        if(VideoCapturerAndroid.getNameOfFrontFacingDevice() != null)
            nameOfBackFacingDevice = VideoCapturerAndroid.getNameOfFrontFacingDevice();
        */

        if( nameOfBackFacingDevice == null){
            Log.e(TAG, "error getting camera");
        }
        /* front camera used for development */
        //
        return VideoCapturerAndroid.create(nameOfBackFacingDevice);
    }


}
