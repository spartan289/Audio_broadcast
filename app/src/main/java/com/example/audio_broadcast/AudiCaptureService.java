package com.example.audio_broadcast;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat.Builder;

import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.Buffer;

public class AudiCaptureService extends Service {
    private static final int SERVICE_ID = 123;
    private static final Object NOTIFICATION_CHANNEL_ID = "AudioCapture channel" ;
    private MediaProjection mediaProjection;
    private MediaProjectionManager mediaProjectionManager;
    private static final String LOG_TAG = "AudioCall";
    private static final int SAMPLE_RATE = 8000; // Hertz
    private static final int SAMPLE_INTERVAL = 20; // Milliseconds
    private static final int SAMPLE_SIZE = 2; // Bytes
    private static final int BUF_SIZE = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE * 2; //Bytes
    private InetAddress address; // Address to call
    private int port = 50000; // Port the packets are addressed to
    private boolean mic = false; // Enable mic?
    private boolean speakers = false; // Enable speakers?
    private final IBinder binder = new LocalBinder();
    private boolean mAlive;


    public class LocalBinder extends Binder {
        public AudiCaptureService getServerInstance() {
            // Return this instance of LocalService so clients can call public methods
            return AudiCaptureService.this;
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String word = intent.getAction();
        if(word=="AudioCaptureService:Start"){

            mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK,
                    intent.getParcelableExtra("AudioCaptureService:Extra:ResultData"));
//            Intent
            System.out.println(mediaProjection);
        mAlive=true;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        this.createNotificationChannel();
        Notification notification = new Notification.
                Builder(getApplicationContext(), "channel1")
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentTitle("Hello Guys")
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            this.startForeground(123,notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        }
        mediaProjectionManager = (MediaProjectionManager) getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        try {
            address = getBroadcastAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel(){
        String channelId = "channel1";
        NotificationChannel channel = new NotificationChannel(channelId, "ChannelOne", NotificationManager.IMPORTANCE_HIGH);

        NotificationManager manager = (NotificationManager) getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);

    }
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
    public void startCall() {

//        startMic();
        startSpeakers();
    }

    public void endCall() {

        Log.i(LOG_TAG, "Ending call!");
        muteMic();
        muteSpeakers();
    }

    public void muteMic() {

        mic = false;
    }

    public void muteSpeakers() {

        speakers = false;
    }

    public void startMic(ParcelFileDescriptor file) {
        // Creates the thread for capturing and transmitting audio
        OutputStream outputStream = new ParcelFileDescriptor.AutoCloseOutputStream(file);
        mic = true;
        Thread thread = new Thread(new Runnable() {

            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void run() {
                // Create an instance of the AudioRecord class
                Log.i(LOG_TAG, "Send thread started. Thread id: " + Thread.currentThread().getId());
                InputStream audioStream;

                AudioFormat audioFormat = new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO).build();

                AudioPlaybackCaptureConfiguration config = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    config = new AudioPlaybackCaptureConfiguration.Builder(mediaProjection).addMatchingUsage(AudioAttributes.USAGE_MEDIA).build();
                }


                AudioRecord audioRecorder = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    audioRecorder = new AudioRecord.Builder()
                            .setAudioFormat(audioFormat)
                            .setBufferSizeInBytes(AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT))
                            .setAudioPlaybackCaptureConfig(config)
                            .build();


                }

                AudiCaptureService.Buffer buffer  = new AudiCaptureService.Buffer();

//                try {
//                    // Create a socket and start recording
//                    Log.i(LOG_TAG, "Packet destination: " + address.toString());
//                    DatagramSocket socket = new DatagramSocket();
//                    socket.setBroadcast(true);
//                    assert audioRecorder != null;
//                    audioRecorder.startRecording();
//                    while(mic) {
//                        // Capture audio from the mic and transmit it
//                        bytes_read = audioRecorder.read(buf, 0, BUF_SIZE);
//                        DatagramPacket packet = new DatagramPacket(buf, bytes_read, address, port);
//                        socket.send(packet);
//                        bytes_sent += bytes_read;
//                        Log.i(LOG_TAG, "Total bytes sent: " + bytes_sent);
//                        Thread.sleep(SAMPLE_INTERVAL, 0);
//                    }
//                    // Stop recording and release resources
//                    audioRecorder.stop();
//                    audioRecorder.release();
//                    socket.disconnect();
//                    socket.close();
//                    mic = false;
//                    return;
//                }
//                catch(InterruptedException e) {
//
//                    Log.e(LOG_TAG, "InterruptedException: " + e.toString());
//                    mic = false;
//                }
//                catch(SocketException e) {
//
//                    Log.e(LOG_TAG, "SocketException: " + e.toString());
//                    mic = false;
//                }
//                catch(UnknownHostException e) {
//
//                    Log.e(LOG_TAG, "UnknownHostException: " + e.toString());
//                    mic = false;
//                }
//                catch(IOException e) {
//
//                    Log.e(LOG_TAG, "IOException: " + e.toString());
//                    mic = false;
//                }
                assert audioRecorder != null;
                audioRecorder.startRecording();
                try {
                    while (isRecording()) {
                        int len = audioRecorder.read(buffer.data, 0, buffer.size);
                        if (len >= 0 && len <= buffer.size) {
                            Log.w(LOG_TAG, String.valueOf(buffer.data));
                            outputStream.write(buffer.data, 0, len);
                            outputStream.flush();
                        } else {
                            Log.w(LOG_TAG, "Unexpected length returned: " + len);
                        }
                    }
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Exception with recording stream", e);
                } finally {
                    stopInternal();
                    try {
                        audioRecorder.stop();
                    } catch (IllegalStateException e) {
                        Log.e(LOG_TAG, "Failed to stop AudioRecord", e);
                    }
                    audioRecorder.release();
                }
            }

            private boolean isRecording() {
                return mAlive;
            }
            private void stopInternal() {
                mAlive = false;
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Failed to close output stream", e);
                }
            }
        });
        thread.start();
    }


    public void startSpeakers() {
        // Creates the thread for receiving and playing back audio
        if(!speakers) {

            speakers = true;
            Thread receiveThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    // Create an instance of AudioTrack, used for playing back audio
                    Log.i(LOG_TAG, "Receive thread started. Thread id: " + Thread.currentThread().getId());
                    AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, BUF_SIZE, AudioTrack.MODE_STREAM);
                    track.play();
                    try {
                        // Define a socket to receive the audio
                        DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName("0.0.0.0"));
                        socket.setBroadcast(true);
                        byte[] buf = new byte[BUF_SIZE];
                        while(speakers) {
                            // Play back the audio received from packets
                            DatagramPacket packet = new DatagramPacket(buf, BUF_SIZE);
                            socket.receive(packet);
                            Log.i(LOG_TAG, "Packet received: " + packet.getLength());
                            track.write(packet.getData(), 0, BUF_SIZE);
                        }
                        // Stop playing back and release resources
                        socket.disconnect();
                        socket.close();
                        track.stop();
                        track.flush();
                        track.release();
                        speakers = false;
                        return;
                    }
                    catch(SocketException e) {

                        Log.e(LOG_TAG, "SocketException: " + e.toString());
                        speakers = false;
                    }
                    catch(IOException e) {

                        Log.e(LOG_TAG, "IOException: " + e.toString());
                        speakers = false;
                    }
                }
            });
            receiveThread.start();
        }
    }
    private static class Buffer extends AudioBuffer {
        @Override
        protected boolean validSize(int size) {
            return size != AudioRecord.ERROR && size != AudioRecord.ERROR_BAD_VALUE;
        }

        @Override
        protected int getMinBufferSize(int sampleRate) {
            return AudioRecord.getMinBufferSize(
                    sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        }
    }
}


