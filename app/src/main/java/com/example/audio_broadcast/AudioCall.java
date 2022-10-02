package com.example.audio_broadcast;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.util.Log;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class AudioCall {

	private static final String LOG_TAG = "AudioCall";
	private static final int SAMPLE_RATE = 8000; // Hertz
	private static final int SAMPLE_INTERVAL = 20; // Milliseconds
	private static final int SAMPLE_SIZE = 2; // Bytes
	private static final int BUF_SIZE = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE * 2; //Bytes
	private InetAddress address; // Address to call
	private int port = 50000; // Port the packets are addressed to
	private boolean mic = false; // Enable mic?
	private boolean speakers = false; // Enable speakers?
	Context mcontext;
	private MediaProjection mediaProjection;

	public AudioCall(InetAddress address, View.OnClickListener mcontext, MediaProjection mediaProjection) {
		this.mcontext = (Context) mcontext;
		this.address = address;
		this.mediaProjection = mediaProjection;
	}

	public AudioCall(InetAddress address, Context mcontext) {
		this.mcontext = (Context) mcontext;
		this.address = address;
	}

	public void startCall() {

		startMic();
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

	public void startMic() {
		// Creates the thread for capturing and transmitting audio
		mic = true;
		Thread thread = new Thread(new Runnable() {

			@RequiresApi(api = Build.VERSION_CODES.M)
			@Override
			public void run() {
				// Create an instance of the AudioRecord class
				Log.i(LOG_TAG, "Send thread started. Thread id: " + Thread.currentThread().getId());


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
					if (ActivityCompat.checkSelfPermission(mcontext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
						// TODO: Consider calling
						//    ActivityCompat#requestPermissions
						// here to request the missing permissions, and then overriding
						//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
						//                                          int[] grantResults)
						// to handle the case where the user grants the permission. See the documentation
						// for ActivityCompat#requestPermissions for more details.
						return;
					}
					audioRecorder = new AudioRecord.Builder()
							.setAudioFormat(audioFormat)
							.setBufferSizeInBytes(AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 10)
							.setAudioPlaybackCaptureConfig(config)
							.build();
				}

//                AudioRecord audioRecorder = new AudioRecord.Builder().setAudioPlaybackCaptureConfig(config).(MediaRecorder.AudioSource.DEFAULT, SAMPLE_RATE,
//                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
//                        AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 10);


//				AudioRecord audioRecorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLE_RATE,
//						AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
//						AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 10);
				int bytes_read = 0;
				int bytes_sent = 0;
				byte[] buf = new byte[BUF_SIZE];
				try {
					// Create a socket and start recording
					Log.i(LOG_TAG, "Packet destination: " + address.toString());
					DatagramSocket socket = new DatagramSocket();
					socket.setBroadcast(true);
					assert audioRecorder != null;
					audioRecorder.startRecording();
					while(mic) {
						// Capture audio from the mic and transmit it
						bytes_read = audioRecorder.read(buf, 0, BUF_SIZE);
						DatagramPacket packet = new DatagramPacket(buf, bytes_read, address, port);
						socket.send(packet);
						bytes_sent += bytes_read;
						Log.i(LOG_TAG, "Total bytes sent: " + bytes_sent);
						Thread.sleep(SAMPLE_INTERVAL, 0);
					}
					// Stop recording and release resources
					audioRecorder.stop();
					audioRecorder.release();
					socket.disconnect();
					socket.close();
					mic = false;
					return;
				}
				catch(InterruptedException e) {

					Log.e(LOG_TAG, "InterruptedException: " + e.toString());
					mic = false;
				}
				catch(SocketException e) {

					Log.e(LOG_TAG, "SocketException: " + e.toString());
					mic = false;
				}
				catch(UnknownHostException e) {

					Log.e(LOG_TAG, "UnknownHostException: " + e.toString());
					mic = false;
				}
				catch(IOException e) {

					Log.e(LOG_TAG, "IOException: " + e.toString());
					mic = false;
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
						DatagramSocket socket = new DatagramSocket(port,InetAddress.getByName("0.0.0.0"));
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
}
