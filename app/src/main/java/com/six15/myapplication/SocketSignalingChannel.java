package com.six15.myapplication;

import android.util.Log;

import com.intel.webrtc.base.ActionCallback;
import com.intel.webrtc.base.WoogeenException;
import com.intel.webrtc.base.WoogeenIllegalArgumentException;
import com.intel.webrtc.p2p.SignalingChannelInterface;
import com.intel.webrtc.p2p.WoogeenP2PException;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Created by George on 8/19/2016.
 */
public class SocketSignalingChannel implements SignalingChannelInterface {

    private static final String TAG = "WooGeen-SocketClient";
    private final String CLIENT_CHAT_TYPE = "woogeen-message";
    private final String SERVERAUTHENTICATED = "server-authenticated";
    private final String FORCE_DISCONNECT = "server-disconnect";
    private Socket socketIOClient;
    private List<SignalingChannelObserver> signalingChannelObservers;
    private ActionCallback<String> connectCallback;
    private final String CLIENT_TYPE = "&clientType=";
    private final String CLIENT_TYPE_VALUE = "Android";
    private final String CLIENT_VERSION = "&clientVersion=";
    private final String CLIENT_VERSION_VALUE = "3.0";

    /**
     * Initialize the socket client.
     */
    public SocketSignalingChannel() {
        socketIOClient = null;
        connectCallback = null;
        this.signalingChannelObservers = new ArrayList<SignalingChannelObserver>();
    }

    @Override
    public void connect(String userInfo, ActionCallback<String> callback) {
        JSONObject loginObject;
        String token;
        String url;
        try {
            connectCallback = callback;
            loginObject = new JSONObject(userInfo);
            token = loginObject.getString("token");
            url = loginObject.getString("host");
            if (isValid(url) && !token.equals("")) {
                url += "?token=" + token + CLIENT_TYPE + CLIENT_TYPE_VALUE
                        + CLIENT_VERSION + CLIENT_VERSION_VALUE;
                IO.Options opt = new IO.Options();
                opt.forceNew = true;
                opt.reconnection = true;
                if(socketIOClient != null){
                    Log.d(TAG, "stop reconnecting the former url");
                    socketIOClient.disconnect();
                }
                socketIOClient = IO.socket(url, opt);
                bindCallbacks();
                socketIOClient.connect();
            } else {
                if (callback != null)
                    callback.onFailure(new WoogeenIllegalArgumentException(
                            "URL is invalid."));
            }
        } catch (JSONException e) {
            if (callback != null)
                callback.onFailure(new WoogeenIllegalArgumentException(e
                        .getMessage()));
        } catch (WoogeenIllegalArgumentException e) {
            if (callback != null)
                callback.onFailure(new WoogeenIllegalArgumentException(e
                        .getMessage()));
        } catch (URISyntaxException e) {
            if (callback != null)
                callback.onFailure(new WoogeenIllegalArgumentException(e
                        .getMessage()));
        }
    }

    @Override
    public void addObserver(SignalingChannelObserver signalingChannelObserver) {
        this.signalingChannelObservers.add(signalingChannelObserver);
    }

    @Override
    public void removeObserver(SignalingChannelObserver signalingChannelObserver) {
        this.signalingChannelObservers.remove(signalingChannelObserver);
    }

    boolean isValid(String urlString) throws WoogeenIllegalArgumentException {
        try {
            URL url = new URL(urlString);
            if (url.getPort() > 65535) {
                throw new WoogeenIllegalArgumentException(
                        "port cannot be larger than 65535.");
            } else
                return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new WoogeenIllegalArgumentException("Wrong URL.");
        }
    }


    @Override
    public void disconnect(ActionCallback<Void> callback) {
        if (socketIOClient != null) {
            Log.d(TAG, "Socket IO Disconnect.");
            socketIOClient.disconnect();
            socketIOClient = null;
        }
        if(callback!=null)
            callback.onSuccess(null);
    }

    /**
     * Send message to peer over WooGeen service.
     * @param message data which will be sent.
     * @param peerId peer id
     * @param callback callback function of sendMessage.
     */

    @Override
    public void sendMessage(String message, String peerId, final ActionCallback<Void> callback) {
        if (socketIOClient == null) {
            Log.d(TAG, "socketIOClient is not established.");
            return;
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("to", peerId);
            jsonObject.put("data", message);
            Ack ack = new Ack() {
                @Override
                public void call(Object... arg0) {
                    if (callback != null) {
                        if ((arg0 == null) || (arg0.length != 0)) {
                            callback.onFailure(new WoogeenException(
                                    "Errors occored during sending message."));
                        } else {
                            callback.onSuccess(null);
                        }
                    }
                }
            };
            socketIOClient.emit(CLIENT_CHAT_TYPE, jsonObject, ack);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Bind callback to the socket IO client.
     */
    private void bindCallbacks() {
        socketIOClient.on(CLIENT_CHAT_TYPE, onMessageCallback)
                .on(SERVERAUTHENTICATED, onServerAuthenticatedCallback)
                .on(FORCE_DISCONNECT, onForceDisconnectCallback)
                .on(Socket.EVENT_CONNECT_ERROR, onConnectFailedCallback)
                .on(Socket.EVENT_DISCONNECT, onDisconnectCallback)
                .on(Socket.EVENT_ERROR, onServerErrorCallback);
    };

    private Emitter.Listener onServerErrorCallback = new Emitter.Listener(){

        @Override
        public void call(Object... arg0) {
            if(connectCallback != null){
                Pattern pattern = Pattern.compile("[0-9]*");
                if(pattern.matcher(arg0[0].toString()).matches()){
                    connectCallback.onFailure(new WoogeenP2PException(
                            WoogeenP2PException.Code.valueOf(Integer.parseInt(arg0[0].toString()))));
                }else{
                    connectCallback.onFailure(new WoogeenException(arg0[0].toString()));
                }
            }
        }

    };
    private Emitter.Listener onMessageCallback = new Emitter.Listener() {
        @Override
        public void call(Object... arg0) {
            JSONObject argumentJsonObject = (JSONObject) arg0[0];
            for (SignalingChannelObserver observer : signalingChannelObservers)
                try {
                    observer.onMessage(argumentJsonObject.getString("from"),
                            argumentJsonObject.getString("data"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
        }
    };

    private Emitter.Listener onServerAuthenticatedCallback = new Emitter.Listener() {
        @Override
        public void call(Object... arg0) {
            if(connectCallback!=null){
                connectCallback.onSuccess(arg0[0].toString());
            }
        }
    };

    private Emitter.Listener onConnectFailedCallback = new Emitter.Listener() {

        @Override
        public void call(Object... arg0) {
            if (connectCallback != null) {
                connectCallback.onFailure(new WoogeenP2PException(
                        "Socket.IO reports connect to signaling server failed.",
                        WoogeenP2PException.Code.P2P_CONN_SERVER_UNKNOWN));
            }
        }
    };

    private Emitter.Listener onForceDisconnectCallback = new Emitter.Listener() {
        @Override
        public void call(Object... arg0) {
            if (socketIOClient != null) {
                socketIOClient.io().reconnection(false);
            }
        }
    };

    private Emitter.Listener onDisconnectCallback = new Emitter.Listener() {
        @Override
        public void call(Object... arg0) {
            for (SignalingChannelObserver observer : signalingChannelObservers)
                observer.onServerDisconnected();
        }
    };

}
