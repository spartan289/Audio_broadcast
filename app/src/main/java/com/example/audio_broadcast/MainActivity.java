package com.example.audio_broadcast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class MainActivity extends Activity {

    static final String LOG_TAG = "UDPchat";
    private static final int LISTENER_PORT = 50003;
    private static final int BUF_SIZE = 1024;
    private ContactManager contactManager;
    private String displayName;
    private boolean STARTED = false;
    private boolean IN_CALL = false;
    private boolean LISTEN = false;
    private MediaProjectionManager mediaProjectionManager;
    public final static String EXTRA_CONTACT = "hw.dt83.udpchat.CONTACT";
    public final static String EXTRA_IP = "hw.dt83.udpchat.IP";
    public final static String EXTRA_DISPLAYNAME = "hw.dt83.udpchat.DISPLAYNAME";
    public MediaProjection mediaProjection;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        mConnectionsClient = Nearby.getConnectionsClient(this);
//        Log.d(LOG_TAG,n)
        Intent audioCaptureIntent = new Intent(this, AudiCaptureService.class);
        audioCaptureIntent.setAction("AudioCaptureService:Start");
        audioCaptureIntent.putExtra("AudioCaptureService:Extra:ResultData",data);
        this.startForegroundService(audioCaptureIntent);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }sa
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    1234);
        }

        Log.i(LOG_TAG, "UDPChat started");
        Button broadcast = findViewById(R.id.broadcast);
        Button receive = findViewById(R.id.recieve);


        broadcast.setOnClickListener(new OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),13);
//                Intent intent = new Intent(MainActivity.this,startBroadcast.class);
//                startActivity(intent);
//                try {
////                    InetAddress broadcast = getBroadcastAddress();
////                    Log.d(LOG_TAG, broadcast.toString());
////                    InetAddress broadcast1 = broadcast;
////                    AudioCall call = new AudioCall(broadcast, this,mediaProjection);
////
////                    call.startMic();
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }



            }
        });
        receive.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,startReceive.class);
                startActivity(intent);

            }
        });
        // START BUTTON
        // Pressing this buttons initiates the main functionality

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private InetAddress getBroadcastAddress() throws IOException {
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) (broadcast >> (k * 8));
        return InetAddress.getByAddress(quads);
    }
    public static final Strategy STRATEGY = Strategy.P2P_STAR;
    public static final String SERVICE_ID="120001";

    private void startAdvertising(){
        AdvertisingOptions advertisingOptions = new AdvertisingOptions.Builder().setStrategy(STRATEGY).build();
        Nearby.getConnectionsClient(getApplicationContext()).startAdvertising("Device A", SERVICE_ID, new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(@NonNull String endPointId, @NonNull ConnectionInfo connectionInfo) {
                Nearby.getConnectionsClient(getApplicationContext())
                        .acceptConnection(endPointId,mPayloadCallback);
            }

            @Override
            public void onConnectionResult(@NonNull String endPointId, @NonNull ConnectionResolution connectionResolution) {
                switch (connectionResolution.getStatus().getStatusCode()){
                    case ConnectionsStatusCodes.STATUS_OK:
                        //We are connected
                        String strendPointId = endPointId;
                        sendPayload(strendPointId);
                    case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                        // The connection was rejected by one or both sides.
                        break;
                    case ConnectionsStatusCodes.STATUS_ERROR:
                        // The connection broke before it was able to be accepted.
                        break;
                    default:
                        // Unknown status code

                }
            }

            @Override
            public void onDisconnected(@NonNull String s) {
                Object strendPointId = null;
            }
        },advertisingOptions);
    }

    private void sendPayload(final String endPointId){
        Payload bytesPaylod=Payload.fromBytes(String.valueOf(timestamp).getBytes(StandardCharsets.UTF_8));
        Nearby.getConnectionsClient(getApplicationContext()).sendPayload(endPointId,bytesPaylod).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    private void startDiscovery(){
        DiscoveryOptions discoveryOptions = new DiscoveryOptions.Builder().setStrategy(STRATEGY).build();
        Nearby.getConnectionsClient(getApplicationContext())
                .startDiscovery(SERVICE_ID, new EndpointDiscoveryCallback() {
                    @Override
                    public void onEndpointFound(@NonNull String endPointId, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
                        Nearby.getConnectionsClient(getApplicationContext())
                                .requestConnection("Device B", endPointId, new ConnectionLifecycleCallback() {
                                    @Override
                                    public void onConnectionInitiated(@NonNull String s, @NonNull ConnectionInfo connectionInfo) {
                                        Nearby.getConnectionsClient(getApplicationContext())
                                                .acceptConnection(endPointId,mPayloadCallback);
                                    }

                                    @Override
                                    public void onConnectionResult(@NonNull String s, @NonNull ConnectionResolution connectionResolution) {
                                        switch (connectionResolution.getStatus().getStatusCode()) {
                                            case ConnectionsStatusCodes.STATUS_OK:
                                                // We're connected! Can now start sending and receiving data.
                                                break;
                                            case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                                                // The connection was rejected by one or both sides.
                                                break;
                                            case ConnectionsStatusCodes.STATUS_ERROR:
                                                // The connection broke before it was able to be accepted.
                                                break;
                                            default:
                                                // Unknown status code
                                        }
                                    }

                                    @Override
                                    public void onDisconnected(@NonNull String s) {

                                    }
                                });
                    }

                    @Override
                    public void onEndpointLost(@NonNull String s) {

                    }
                },discoveryOptions);
    }
    private final PayloadCallback mPayloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String s, @NonNull Payload payload) {
            final byte[] receivedBytes = payload.asBytes();
            Thread th = new Thread() {
                @Override
                public void run() {
                    long timestamp = System.currentTimeMillis();
//                    kib
                }
            };
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
            if (payloadTransferUpdate.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                // Do something with is here...
            }
        }
    };
}
