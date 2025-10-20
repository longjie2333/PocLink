package com.example.remotecontrol;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.projection.MediaProjection;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;

public class RemoteControlService extends Service implements NetworkClient.Listener {
    private static final String TAG = "RemoteControlService";
    public static final String ACTION_CAPTURE_PERMISSION_RESULT = "com.example.remotecontrol.CAPTURE_PERMISSION_RESULT";
    public static final String ACTION_SHOW_CIRCLE = "com.example.remotecontrol.ACTION_SHOW_CIRCLE";

    private static String serverUrl = "tcp://127.0.0.1:9002";

    private NetworkClient networkClient;
    private ScreenEncoder screenEncoder;

    public static void setServerUrl(String url) {
        if (url != null && !url.isEmpty()) {
            serverUrl = url;
        }
    }

    public static void sendShowCircleBroadcast(Context ctx, int x, int y) {
        Intent i = new Intent(ACTION_SHOW_CIRCLE);
        i.putExtra("x", x);
        i.putExtra("y", y);
        ctx.sendBroadcast(i);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startInForeground();
        registerReceiver(capturePermissionReceiver, new IntentFilter(ACTION_CAPTURE_PERMISSION_RESULT));
        registerReceiver(showCircleReceiver, new IntentFilter(ACTION_SHOW_CIRCLE));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        connectNetwork();
        requestScreenCapturePermission();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(capturePermissionReceiver); } catch (Throwable ignored) {}
        try { unregisterReceiver(showCircleReceiver); } catch (Throwable ignored) {}
        if (screenEncoder != null) {
            screenEncoder.stop();
            screenEncoder = null;
        }
        if (networkClient != null) {
            networkClient.close();
            networkClient = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void connectNetwork() {
        try {
            URI uri = parseServerUrl(serverUrl);
            networkClient = new NetworkClient(uri.getHost(), uri.getPort(), this);
            networkClient.connect();
        } catch (Exception e) {
            Log.e(TAG, "Network connect error", e);
        }
    }

    private URI parseServerUrl(String url) {
        try {
            URI u = new URI(url);
            if (u.getHost() != null && u.getPort() != -1) return u;
        } catch (Exception ignore) {}
        // Fallback: host:port
        try {
            if (url.contains(":")) {
                String[] parts = url.split(":");
                return new URI("tcp", null, parts[0], Integer.parseInt(parts[1]), null, null, null);
            }
        } catch (Exception ignored) {}
        return URI.create("tcp://127.0.0.1:9002");
    }

    private void requestScreenCapturePermission() {
        Intent i = new Intent(this, ScreenCapturePermissionActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    private final BroadcastReceiver capturePermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int resultCode = intent.getIntExtra("resultCode", 0);
            Intent resultData = intent.getParcelableExtra("resultData");
            if (resultData != null) {
                startScreenEncoding(resultCode, resultData);
            }
        }
    };

    private final BroadcastReceiver showCircleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int x = intent.getIntExtra("x", 0);
            int y = intent.getIntExtra("y", 0);
            AccessibilityControlService.showCircle(context, x, y);
        }
    };

    private void startScreenEncoding(int resultCode, Intent data) {
        stopScreenEncoding();
        screenEncoder = new ScreenEncoder(getApplicationContext(), resultCode, data, networkClient);
        screenEncoder.start();
    }

    private void stopScreenEncoding() {
        if (screenEncoder != null) {
            screenEncoder.stop();
            screenEncoder = null;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("remote_control", "Remote Control", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.createNotificationChannel(channel);
        }
    }

    private void startInForeground() {
        Intent launch = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(this, "remote_control")
                .setContentTitle("Remote Control Running")
                .setContentText("Streaming and control service active")
                .setSmallIcon(android.R.drawable.presence_online)
                .setContentIntent(pi)
                .build();
        startForeground(1, notification);
    }

    @Override
    public void onConnected() {
        Log.i(TAG, "Network connected");
    }

    @Override
    public void onTextMessage(String text) {
        try {
            JSONObject obj = new JSONObject(text);
            String type = obj.optString("type");
            if ("input".equals(type)) {
                String action = obj.optString("action");
                int x = obj.optInt("x");
                int y = obj.optInt("y");
                if ("tap".equals(action)) {
                    AccessibilityControlService.performTap(this, x, y);
                } else if ("swipe".equals(action)) {
                    int x2 = obj.optInt("x2", x);
                    int y2 = obj.optInt("y2", y);
                    AccessibilityControlService.performSwipe(this, x, y, x2, y2, 300);
                }
                AccessibilityControlService.showCircle(this, x, y);
            } else if ("start_capture".equals(type)) {
                requestScreenCapturePermission();
            }
        } catch (JSONException e) {
            Log.e(TAG, "Parse message error", e);
        }
    }

    @Override
    public void onClosed() {
        Log.i(TAG, "Network closed");
    }

    @Override
    public void onError(Exception e) {
        Log.e(TAG, "Network error", e);
    }
}
