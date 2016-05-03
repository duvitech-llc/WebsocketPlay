package com.six15.myapplication;

import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketState;

import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String sServerID = "AAAAAAAAAAAAAAAAAAAAAA";
    private String sClientID = "";

    Gson gson = new Gson();
    WebSocketFactory factory = new WebSocketFactory();
    WebSocket ws = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onStart()
    {
        new Thread() {
            @Override
            public void run() {
                connectWebSocket();
            }
        }.start();
        super.onStart();
    }

    @Override
    public void onPause(){
        if(ws != null) {
            ws.disconnect();
            ws = null;
        }

        super.onPause();
    }

    private void connectWebSocket() {

        try {
            ws = new WebSocketFactory().createSocket("ws://six15wifiportal.azurewebsites.net/ws");
            ws.addListener(new WebSocketAdapter(){
                @Override
                public void onFrame(WebSocket websocket, WebSocketFrame frame) throws Exception
                {
                    Log.i("MESSAGE", "onFrame");
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
                    // need to handle messaging here for now
                    Log.i("MESSAGE", "onTextMessage ");
                    MessageData msg = gson.fromJson(text, MessageData.class);
                    switch (msg.getType()){
                        case "id":
                            sClientID = msg.getText();
                            Log.i("MESSAGE", "Our Client ID is: " + sClientID);
                            break;
                        case "message":
                            if(msg.getFrom() != null){
                                msgFrom = msg.getFrom();
                            }else if(msg.getClientID() != null){
                                msgFrom = msg.getClientID();
                            }
                            else
                                msgFrom = "Unknown";

                            Log.i("MESSAGE", "Message From: " + msgFrom + " Data: " + msg.getText());
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

    public void sendMessage(View view) {
        EditText editText = (EditText)findViewById(R.id.message);
        String message = editText.getText().toString();
        try {
            if (ws != null) {
                JSONObject string = new JSONObject();
                string.put("clientID", "Android");
                string.put("type", "message");
                string.put("text", message);
                string.put("to", null);
                string.put("from", sClientID);
                string.put("date", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
                ws.sendText(string.toString());
            }
        }catch(Exception ex){

            Log.e("ERROR", ex.getMessage());
        }

        editText.setText("");
    }
}
