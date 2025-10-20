package com.example.remotecontrol;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

public class ScreenCapturePermissionActivity extends Activity {
    private static final int REQ_CODE = 2001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent i = mpm.createScreenCaptureIntent();
        startActivityForResult(i, REQ_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE) {
            Intent b = new Intent(RemoteControlService.ACTION_CAPTURE_PERMISSION_RESULT);
            b.putExtra("resultCode", resultCode);
            b.putExtra("resultData", data);
            sendBroadcast(b);
        }
        finish();
    }
}
