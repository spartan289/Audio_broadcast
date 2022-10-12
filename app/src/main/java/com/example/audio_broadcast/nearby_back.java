//package com.example.audio_broadcast;
//
//import android.annotation.SuppressLint;
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.os.Binder;
//import android.os.Build;
//import android.os.IBinder;
//import android.os.ParcelFileDescriptor;
//import android.util.Log;
//import android.widget.Toast;
//
//import androidx.annotation.Nullable;
//import androidx.annotation.RequiresApi;
//import androidx.annotation.WorkerThread;
//import androidx.core.app.NotificationCompat;
//import androidx.core.app.NotificationManagerCompat;
//
//import com.google.android.gms.nearby.connection.ConnectionInfo;
//import com.google.android.gms.nearby.connection.Payload;
//import com.google.android.gms.nearby.connection.Strategy;
//
//import java.io.IOException;
//import java.util.Objects;
//
//public class nearby_back extends ConnectionNearby {
//    public static final Strategy STRATEGY = Strategy.P2P_STAR;
//    public static final String SERVICE_ID = "120001";
//    private static final String LOG_TAG = "Connection Nearby";
//    private final IBinder binder = new nearby_back.LocalBinder();
//    private final BroadcastReceiver rec_advertise = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
////            if(intent.getAction()=='')
//            startAdvertising();
//        }
//    };
//    private final BroadcastReceiver rec_discover = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
////            if(intent.getAction()=='')
//            startDiscovering();
//        }
//    };
//    private final BroadcastReceiver rec_stop = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
////            if(intent.getAction()=='')
//            stopDiscovering();
//            startAdvertising();
//
//        }
//    };
//    private State state = State.UNKNOWN;
//    @Nullable
//    private AudioPlayer mAudioPlayer;
//
//    public nearby_back() {
//    }
//
//    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) {
//        return binder;
//    }
//
//    private void startRecording() {
//        Log.v(LOG_TAG, "startRecording()");
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
//
//                        // Use the second half of the payload (the write side) in AudioRecorder.
////                        startMic(payloadPipe[1]);
//
//                    }
//                }
//        );
//        thread.start();
//    }
//
//    private void stopRecording() {
//        Log.v(LOG_TAG, "stopRecording()");
//    }
//
//    @Override
//    protected void onEndpointDiscovered(Endpoint endpoint) {
//        // We found an advertiser!
//        stopDiscovering();
//        connectToEndpoint(endpoint);
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//
//        String action = intent.getAction();
//        if (action == "nearby:start") {
//            setState(State.SEARCHING);
//        }
//        return super.onStartCommand(intent, flags, startId);
//    }
//
//    @RequiresApi(api = Build.VERSION_CODES.O)
//    private void createNotificationChannel() {
//        String channelId = "channel1";
//        NotificationChannel channel = new NotificationChannel(channelId, "ChannelOne", NotificationManager.IMPORTANCE_HIGH);
//
//        NotificationManager manager = (NotificationManager) getSystemService(NotificationManager.class);
//        manager.createNotificationChannel(channel);
//
//    }
//
//    @RequiresApi(api = Build.VERSION_CODES.O)
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        registerReceiver(rec_advertise, new IntentFilter("com.nearby.intent.action.advertise"));
//        registerReceiver(rec_discover, new IntentFilter("com.nearby.intent.action.discover"));
//        registerReceiver(rec_stop, new IntentFilter("com.nearby.intent.action.stop"));
//        this.createNotificationChannel();
//        Intent advertiseintent = new Intent("com.nearby.intent.action.advertise");
//        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent advertpi = PendingIntent.getBroadcast(getApplicationContext(), 0, advertiseintent, PendingIntent.FLAG_IMMUTABLE);
//
//        Intent discoverIntent = new Intent("com.nearby.intent.action.discover");
//        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent discoverpi = PendingIntent.getBroadcast(getApplicationContext(), 0, discoverIntent, PendingIntent.FLAG_IMMUTABLE);
//
//        Intent stopIntent = new Intent("com.nearby.intent.action.stop");
//        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent stoppi = PendingIntent.getBroadcast(getApplicationContext(), 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel1")
//                .setSmallIcon(R.mipmap.ic_launcher)
//                .setContentTitle("Nearby")
//                .addAction(R.dr, "Adv", advertpi)
//                .addAction(R.drawable.ic_stat_name, "DIS", discoverpi)
//                .addAction(R.drawable.ic_stat_stop, "STOP", stoppi);
//        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
//
//// notificationId is a unique int for each notification that you must define
//        notificationManager.notify(34543, builder.build());
//
//    }
//
//    @Override
//    public void onDestroy() {
//        setState(State.UNKNOWN);
//        super.onDestroy();
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
//
//    @Override
//    protected void onEndpointConnected(Endpoint endpoint) {
//        Toast.makeText(
//                        this, "Connected to %s" + endpoint.getName(), Toast.LENGTH_SHORT)
//                .show();
//        setState(State.CONNECTED);
//    }
//
//    private void setState(State ntate) {
////        state=connected;
//        State oldState = state;
//        state = ntate;
////        onStateChanged(oldState, ntate);
//    }
//
//    //    private TextView cstate;
//    @SuppressLint("SetTextI18n")
//    private void onStateChanged(State oldState, State newState) {
////        cstate=(TextView) findViewById(R.id.currentState);
//
//        // Update Nearby Connections to the new state.
//        switch (newState) {
//            case SEARCHING:
////                cstate.setText("Searching");
//                disconnectFromAllEndpoints();
//                startDiscovering();
//                startAdvertising();
//                break;
//            case CONNECTED:
////                cstate.setText("Connecting");
//
//                stopDiscovering();
//                stopAdvertising();
//                break;
//            case UNKNOWN:
////                cstate.setText("Unkown");
//
//                stopAllEndpoints();
//                break;
//            default:
//                // no-op
//                break;
//        }
//
//    }
//
//    @Override
//    protected void onEndpointDisconnected(Endpoint endpoint) {
//        Toast.makeText(
//                        this, String.format("Disconnected from %s", endpoint.getName()), Toast.LENGTH_SHORT)
//                .show();
//        setState(State.SEARCHING);
//    }
//
//    @Override
//    protected void onConnectionFailed(Endpoint endpoint) {
//        if (state == State.SEARCHING) {
//            startDiscovering();
//        }
//        super.onConnectionFailed(endpoint);
//    }
//
////    private void sendPayload(final String endPointId,Payload payload){
////
////
////
////        Nearby.getConnectionsClient(getApplicationContext()).sendPayload(endPointId,payload).addOnSuccessListener(new OnSuccessListener<Void>() {
////            @Override
////            public void onSuccess(Void unused) {
////
////            }
////        }).addOnFailureListener(new OnFailureListener() {
////            @Override
////            public void onFailure(@NonNull Exception e) {
////
////            }
////        });
////    }
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
//                            mAudioPlayer = null;
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
//
//    public class LocalBinder extends Binder {
//        public nearby_back getServerInstance() {
//            // Return this instance of LocalService so clients can call public methods
//            return nearby_back.this;
//        }
//    }
//}