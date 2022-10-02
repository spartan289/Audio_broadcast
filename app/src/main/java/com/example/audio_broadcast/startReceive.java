package com.example.audio_broadcast;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import java.io.IOException;
import java.net.InetAddress;

public class startReceive extends AppCompatActivity {
    static final String LOG_TAG = "UDPchat";
    AudioCall call;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_receive);
        Button btn = findViewById(R.id.receiv);
        btn.setOnClickListener(view -> {
            try {
                InetAddress broadcast = getBroadcastAddress();
                Log.d(LOG_TAG,broadcast.toString());
                InetAddress broadcast1 = broadcast;
                AudioCall call = new AudioCall(broadcast, this);

                call.startSpeakers();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
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

}