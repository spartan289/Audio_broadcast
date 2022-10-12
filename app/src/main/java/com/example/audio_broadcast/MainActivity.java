package com.example.audio_broadcast;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {

    static final String LOG_TAG = "Nearby";
    private static final int LISTENER_PORT = 50003;
    private static final int BUF_SIZE = 1024;
    //    private State state = State.UNKNOWN;
//    private AudioRecorder mRecorder;
    private static final String[] REQUIRED_PERMISSIONS;
    private static final String TAG = "Nearby";
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            REQUIRED_PERMISSIONS =
                    new String[]{
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_ADVERTISE,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                    };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            REQUIRED_PERMISSIONS =
                    new String[]{
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                    };
        } else {
            REQUIRED_PERMISSIONS =
                    new String[]{
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                    };
        }
    }

    public MediaProjection mediaProjection;
    private MediaProjectionManager mediaProjectionManager;
    private AudiCaptureService audiCaptureService = null;
    private AudioRecord audioRecord;

    public static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

//        mConnectionsClient = Nearby.getConnectionsClient(this);
//        Log.d(LOG_TAG,n)
        Intent audioCaptureIntent = new Intent(this, AudiCaptureService.class);
        audioCaptureIntent.setAction("AudioCaptureService:Start");
        audioCaptureIntent.putExtra("AudioCaptureService:Extra:ResultData", data);
        this.startForegroundService(audioCaptureIntent);

    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    protected String[] getRequiredPermissions() {
        return REQUIRED_PERMISSIONS;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_REQUIRED_PERMISSIONS) {
            int i = 0;
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    Log.w(TAG, "Failed to request the permission " + permissions[i]);
                    Toast.makeText(this, "Cannot start without required permissions", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                i++;
            }
            recreate();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    @Override
    protected void onStop() {
//        setState(State.UNKNOWN);

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        stopService(new Intent(this, AudiCaptureService.class));
        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    1234);
        }
        if (!hasPermissions(this, getRequiredPermissions())) {
            if (Build.VERSION.SDK_INT < 23) {
                ActivityCompat.requestPermissions(
                        this, getRequiredPermissions(), REQUEST_CODE_REQUIRED_PERMISSIONS);
            } else {
                requestPermissions(getRequiredPermissions(), REQUEST_CODE_REQUIRED_PERMISSIONS);
            }
        }

        Intent nearby = new Intent(this, AudiCaptureService.class);
        nearby.setAction("nearby:start");
        startService(nearby);
        Log.i(LOG_TAG, "Nearby started");
        Button broadcast = findViewById(R.id.broadcast);
        Button receive = findViewById(R.id.recieve);


        broadcast.setOnClickListener(new OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {

                mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), 13);

            }
        });
        receive.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        // START BUTTON
        // Pressing this buttons initiates the main functionality
    }


//    };    private void startRecording() {
//        Log.v(LOG_TAG,"startRecording()");
//        Thread thread = new Thread(
//                new Runnable() {
//                    @Override
//                    public void run() {
//                        ParcelFileDescriptor[] payloadPipe = new ParcelFileDescriptor[0];
//                        try {
//                            payloadPipe = ParcelFileDescriptor.createPipe();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//
//                        // Send the first half of the payload (the read side) to Nearby Connections.
//                        send(Payload.fromStream(payloadPipe[0]));
//                        while (audiCaptureService==null){
//                            continue;
//                        }
//                        // Use the second half of the payload (the write side) in AudioRecorder.
//                        audiCaptureService.startMic(payloadPipe[1]);
//
//                    }
//                }
//        );
//        thread.start();
//    }
//    private void stopRecording() {
//        Log.v(LOG_TAG,"stopRecording()");
//
//    }
//    @Override
//    protected void onEndpointDiscovered(Endpoint endpoint) {
//        // We found an advertiser!
//        stopDiscovering();
//        connectToEndpoint(endpoint);
//    }
//
//    @Override
//    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {
//        // A connection to another device has been initiated! We'll use the auth token, which is the
//        // same on both devices, to pick a color to use when we're connected. This way, users can
//        // visually see which device they connected with.
////        mConnectedColor = COLORS[connectionInfo.getAuthenticationToken().hashCode() % COLORS.length];
//
//        // We accept the connection immediately.
//        acceptConnection(endpoint);
//    }
//    @Override
//    protected void onEndpointConnected(Endpoint endpoint) {
//        Toast.makeText(
//                        this, "Connected to %s"+endpoint.getName(), Toast.LENGTH_SHORT)
//                .show();
//        setState(State.CONNECTED);
//    }
//
//    private void setState(State ntate) {
////        state=connected;
//        State oldState = state;
//        state = ntate;
//        onStateChanged(oldState, ntate);
//    }
//    private TextView cstate;
//    @SuppressLint("SetTextI18n")
//    private void onStateChanged(State oldState, State newState) {
//        cstate=(TextView) findViewById(R.id.currentState);
//
//        // Update Nearby Connections to the new state.
//        switch (newState) {
//            case SEARCHING:
//                cstate.setText("Searching");
//                disconnectFromAllEndpoints();
//                startDiscovering();
//                startAdvertising();
//                break;
//            case CONNECTED:
//                cstate.setText("Connecting");
//
//                stopDiscovering();
//                stopAdvertising();
//                break;
//            case UNKNOWN:
//                cstate.setText("Unkown");
//
//                stopAllEndpoints();
//                break;
//            default:
//                // no-op
//                break;
//        }
//
//    }
//    @Override
//    protected void onEndpointDisconnected(Endpoint endpoint) {
//        Toast.makeText(
//                        this,  String.format("Disconnected from %s", endpoint.getName()), Toast.LENGTH_SHORT)
//                .show();
//        setState(State.SEARCHING);
//    }
//    @Override
//    protected void onConnectionFailed(Endpoint endpoint) {
//        if(state==State.SEARCHING){
//            startDiscovering();
//        }
//        super.onConnectionFailed(endpoint);
//    }
//
//    public static final Strategy STRATEGY = Strategy.P2P_STAR;
//    public static final String SERVICE_ID="120001";
//    public String EndPointId;
//    private void startStreaming(String endpoint){
//        Log.d(LOG_TAG,"start Streaming");
//        try{
//            ParcelFileDescriptor[] payloadPipe = ParcelFileDescriptor.createPipe();
//            sendPayload(endpoint,Payload.fromStream(payloadPipe[0]));
//            audiCaptureService.startMic(payloadPipe[1]);
//
////            audioRecord = ;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//    private void sendPayload(final String endPointId,Payload payload){
//
//
//
//        Nearby.getConnectionsClient(getApplicationContext()).sendPayload(endPointId,payload).addOnSuccessListener(new OnSuccessListener<Void>() {
//            @Override
//            public void onSuccess(Void unused) {
//
//            }
//        }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//
//            }
//        });
//    }
//
//    @Nullable
//    private AudioPlayer mAudioPlayer;
//
//    /**
//     * Someone connected to us has sent us data. Override this method to act on the event.
//     *
//     * @param endpoint The sender.
//     * @param payload  The data.
//     */
//    @Override
//    protected void onReceive(Endpoint endpoint, Payload payload) {
//        super.onReceive(endpoint, payload);
//        if (payload.getType() == Payload.Type.STREAM) {
//            if (mAudioPlayer != null) {
//                mAudioPlayer.stop();
//                mAudioPlayer = null;
//            }
//
//            AudioPlayer player =
//                    new AudioPlayer(Objects.requireNonNull(payload.asStream()).asInputStream()) {
//                        @WorkerThread
//                        @Override
//                        protected void onFinish() {
//                            runOnUiThread(
//                                    new Runnable() {
//                                        @UiThread
//                                        @Override
//                                        public void run() {
//                                            mAudioPlayer = null;
//                                        }
//                                    });
//                        }
//                    };
//            mAudioPlayer = player;
//            player.start();
//        }
//
//    }
//
//    public enum State {
//        UNKNOWN,
//        SEARCHING,
//        CONNECTED
//    }

}
