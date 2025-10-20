package com.example.remotecontrol;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "com.example.remotecontrol/remote_control";

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler(this::onMethodCall);
    }

    private void onMethodCall(MethodCall call, MethodChannel.Result result) {
        Context ctx = this;
        switch (call.method) {
            case "setServerUrl":
                String url = call.argument("url");
                RemoteControlService.setServerUrl(url);
                result.success(null);
                break;
            case "start":
                Intent s = new Intent(ctx, RemoteControlService.class);
                ctx.startService(s);
                result.success(null);
                break;
            case "stop":
                Intent st = new Intent(ctx, RemoteControlService.class);
                ctx.stopService(st);
                result.success(null);
                break;
            case "showCircle":
                Integer x = call.argument("x");
                Integer y = call.argument("y");
                RemoteControlService.sendShowCircleBroadcast(ctx, x != null ? x : 0, y != null ? y : 0);
                result.success(null);
                break;
            default:
                result.notImplemented();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            if (data != null && "poclink".equals(data.getScheme())) {
                if ("start".equalsIgnoreCase(data.getHost())) {
                    Intent s = new Intent(this, RemoteControlService.class);
                    startService(s);
                }
            }
        }
    }
}
