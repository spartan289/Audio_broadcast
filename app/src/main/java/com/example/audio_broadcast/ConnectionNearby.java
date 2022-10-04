package com.example.audio_broadcast;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public abstract class ConnectionNearby extends AppCompatActivity {
    private static final String[] REQUIRED_PERMISSIONS;
    private static final String TAG = "Nearby";

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            REQUIRED_PERMISSIONS =
                    new String[] {
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
                    new String[] {
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                    };
        } else {
            REQUIRED_PERMISSIONS =
                    new String[] {
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                    };
        }
    }

    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    private ConnectionsClient connectionsClient;
    private final Map<String,Endpoint> DiscoveredEndpoints = new HashMap<>();

    private final Map<String,Endpoint> PendingConnections = new HashMap<>();
    private final Map<String, Endpoint> EstablishedConnections = new HashMap<>();

    private boolean isConnecting=false;
    private boolean isDiscovering=false;
    private  boolean isAdvertising=false;

    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {}
    protected void onEndpointDisconnected(Endpoint endpoint) {}
    protected void onConnectionFailed(Endpoint endpoint) {}
    protected void onEndpointConnected(Endpoint endpoint) {}
    protected void onEndpointDiscovered(Endpoint endpoint) {}

    private void disconnectedFromEndpoint(Endpoint endpoint) {
        Log.d(TAG,String.format("disconnectedFromEndpoint(endpoint=%s)", endpoint));
        try {
            EstablishedConnections.remove(endpoint.getId());

        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
        onEndpointDisconnected(endpoint);
    }
    /** Disconnects from the given endpoint. */
    protected void disconnect(Endpoint endpoint) {
        connectionsClient.disconnectFromEndpoint(endpoint.getId());
        EstablishedConnections.remove(endpoint.getId());
    }

    /** Disconnects from all currently connected endpoints. */
    protected void disconnectFromAllEndpoints() {
        for (Endpoint endpoint : EstablishedConnections.values()) {
            connectionsClient.disconnectFromEndpoint(endpoint.getId());
        }
        EstablishedConnections.clear();
    }

    private void connectedToEndpoint(Endpoint endpoint) {
        Log.d(TAG,String.format("connectedToEndpoint(endpoint=%s)", endpoint));
        EstablishedConnections.put(endpoint.getId(), endpoint);
        onEndpointConnected(endpoint);
    }
    protected void stopAllEndpoints() {
        connectionsClient.stopAllEndpoints();
        IsAdvertising = false;
        IsDiscovering = false;
        isConnecting = false;
        DiscoveredEndpoints.clear();
        PendingConnections.clear();
        EstablishedConnections.clear();
    }

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo connectionInfo) {
                    Log.d("Nearby",String.format(
                            "onConnectionInitiated(endpointId=%s, endpointName=%s)",
                            endpointId, connectionInfo.getEndpointName()));
                    Endpoint endpoint = new Endpoint(endpointId,connectionInfo.getEndpointName());
                    PendingConnections.put(endpointId,endpoint);
                    ConnectionNearby.this.onConnectionInitiated(endpoint, connectionInfo);
                }

                @Override
                public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution result) {
                    Log.d("Nearby",String.format("onConnectionResponse(endpointId=%s, result=%s)", endpointId, result));
                    isConnecting=false;
                    if(!result.getStatus().isSuccess()){
                        Log.w("Nearby", "onConnectionResult: "+ConnectionNearby.toString(result.getStatus()) );
                        onConnectionFailed(PendingConnections.remove(endpointId));

                    }
                    connectedToEndpoint(PendingConnections.remove(endpointId));

                }
                public String TAG = "Nearby Conn";
                @Override
                public void onDisconnected(@NonNull String s) {
                    if(EstablishedConnections.containsKey(s)){
                        Log.w("Nearby", "onDisconnected: "+s );
                    }
                    disconnectedFromEndpoint(EstablishedConnections.get(s));
                }
            };

    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
                    Log.d(TAG,String.format("onPayloadReceived(endpointId=%s, payload=%s)", endpointId, payload));
                    onReceive(EstablishedConnections.get(endpointId), payload);

                }

                @Override
                public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {
                    Log.d(TAG,String.format(
                            "onPayloadTransferUpdate(endpointId=%s, update=%s)", endpointId, update));

                }
            };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectionsClient = Nearby.getConnectionsClient(getApplicationContext());
    }
    public static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    @Override
    protected void onStart() {
        super.onStart();
        if (!hasPermissions(this, getRequiredPermissions())) {
            if (Build.VERSION.SDK_INT < 23) {
                ActivityCompat.requestPermissions(
                        this, getRequiredPermissions(), REQUEST_CODE_REQUIRED_PERMISSIONS);
            } else {
                requestPermissions(getRequiredPermissions(), REQUEST_CODE_REQUIRED_PERMISSIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_REQUIRED_PERMISSIONS) {
            int i = 0;
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    Log.w(TAG,"Failed to request the permission " + permissions[i]);
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
    private boolean IsAdvertising=false;
    protected void startAdvertising(){
        IsAdvertising=true;
        final String localEndpointName = mname;

        AdvertisingOptions.Builder advertisingOptions = new AdvertisingOptions.Builder();
        advertisingOptions.setStrategy(Strategy.P2P_STAR);

        connectionsClient.startAdvertising(
                localEndpointName,
                String.valueOf(12001),
                connectionLifecycleCallback,
                advertisingOptions.build()
        ).addOnSuccessListener(unused -> {
           Log.v(TAG,localEndpointName);
           onAdvertisingStarted();
        }).addOnFailureListener(e -> {
            Log.w(TAG,"Advertising Failed",e);
            onAdvertisingFailed();
        });

    }
    protected void onAdvertisingStarted() {}

    /** Called when advertising fails to start. Override this method to act on the event. */
    protected void onAdvertisingFailed() {}

    protected void stopAdvertising(){
        IsAdvertising=false;
        connectionsClient.stopAdvertising();
    }

    protected void acceptConnection(final Endpoint endpoint){
        connectionsClient
                .acceptConnection(endpoint.getId(),payloadCallback)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG,"Failed Connection",e);
                    }
                });
    }
    protected void rejectConnection(Endpoint endpoint) {
        connectionsClient
                .rejectConnection(endpoint.getId())
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG,"rejectConnection() failed.", e);
                            }
                        });
    }
    private static boolean IsDiscovering=false;
    protected void startDiscovering(){
        IsDiscovering=true;
        DiscoveredEndpoints.clear();
        DiscoveryOptions.Builder discoBuilder = new DiscoveryOptions.Builder();
        discoBuilder.setStrategy(Strategy.P2P_STAR);
        connectionsClient.startDiscovery(
                String.valueOf(12001),
                new EndpointDiscoveryCallback() {
                    @Override
                    public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo info) {
                        Log.d(TAG,
                                String.format(
                                        "onEndpointFound(endpointId=%s, serviceId=%s, endpointName=%s)",
                                        endpointId, info.getServiceId(), info.getEndpointName()));

                        if ("12001".equals(info.getServiceId())) {
                            Endpoint endpoint = new Endpoint(endpointId, info.getEndpointName());
                            DiscoveredEndpoints.put(endpointId, endpoint);
                            onEndpointDiscovered(endpoint);
                        }

                    }

                    @Override
                    public void onEndpointLost(@NonNull String endpointId) {
                        Log.d(TAG,String.format("onEndpointLost(endpointId=%s)", endpointId));

                    }
                },discoBuilder.build()
        ).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                onDiscoveryStarted();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                IsDiscovering = false;
                Log.w(TAG,"startDiscovering() failed.", e);
                onDiscoveryFailed();
            }
        })
        ;

    }
    protected void onDiscoveryStarted() {}

    protected void stopDiscovering() {
        IsDiscovering = false;
        connectionsClient.stopDiscovery();
    }

    protected void onDiscoveryFailed() {}

    private static String mname=generateRandomName();
    private static String generateRandomName() {
        String name = "";
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            name += random.nextInt(10);
        }
        return name;
    }
    protected void connectToEndpoint(final Endpoint endpoint) {
        Log.v(TAG,"Sending a connection request to endpoint " + endpoint);
        // Mark ourselves as connecting so we don't connect multiple times
        isConnecting = true;

        // Ask to connect
        connectionsClient
                .requestConnection(mname, endpoint.getId(), connectionLifecycleCallback)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG,"requestConnection() failed.", e);
                                isConnecting = false;
                                onConnectionFailed(endpoint);
                            }
                        });
    }
    protected final boolean isConnecting() {
        return isConnecting;
    }

    protected void send(Payload payload) {
        send(payload, EstablishedConnections.keySet());
    }

    private void send(Payload payload, Set<String> endpoints) {
        connectionsClient
                .sendPayload(new ArrayList<>(endpoints), payload)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG,"sendPayload() failed.", e);
                            }
                        });
    }

    /**
     * Someone connected to us has sent us data. Override this method to act on the event.
     *
     * @param endpoint The sender.
     * @param payload The data.
     */
    protected void onReceive(Endpoint endpoint, Payload payload) {}



    protected static class Endpoint{
        private final String id;
        private final String name;
        public Endpoint( String id, String name){
            this.id=id;
            this.name=name;
        }

        public String getId(){ return id;}

        public String getName(){ return name;}

        @Override
        public boolean equals(Object obj){
            if(obj instanceof Endpoint){
                Endpoint other=(Endpoint) obj;
                return id.equals(other.id);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return String.format("Endpoint{id=%s, name=%s}",id,name);
        }
    }

    private static String toString(Status status) {
        return String.format(
                Locale.US,
                "[%d]%s",
                status.getStatusCode(),
                status.getStatusMessage() != null
                        ? status.getStatusMessage()
                        : ConnectionsStatusCodes.getStatusCodeString(status.getStatusCode()));
    }

    protected String[] getRequiredPermissions() {
        return REQUIRED_PERMISSIONS;
    }

}
