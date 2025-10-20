package com.example.remotecontrol;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import java.nio.ByteBuffer;

public class ScreenEncoder {
    private static final String TAG = "ScreenEncoder";

    private final Context context;
    private final int resultCode;
    private final Intent resultData;
    private final NetworkClient networkClient;

    private MediaProjection mediaProjection;
    private MediaCodec encoder;
    private Surface inputSurface;
    private VirtualDisplay virtualDisplay;
    private Thread encodingThread;
    private volatile boolean running = false;

    public ScreenEncoder(Context context, int resultCode, Intent resultData, NetworkClient client) {
        this.context = context.getApplicationContext();
        this.resultCode = resultCode;
        this.resultData = resultData;
        this.networkClient = client;
    }

    public void start() {
        if (encodingThread != null) return;
        encodingThread = new Thread(this::encodeLoop, "ScreenEncoder");
        encodingThread.start();
    }

    public void stop() {
        running = false;
        try { if (encodingThread != null) encodingThread.join(500); } catch (InterruptedException ignored) {}
        encodingThread = null;
        try { if (virtualDisplay != null) virtualDisplay.release(); } catch (Exception ignore) {}
        try { if (encoder != null) { encoder.stop(); encoder.release(); } } catch (Exception ignore) {}
        try { if (mediaProjection != null) mediaProjection.stop(); } catch (Exception ignore) {}
        mediaProjection = null;
        encoder = null;
        inputSurface = null;
        virtualDisplay = null;
    }

    private void encodeLoop() {
        try {
            DisplayMetrics dm = new DisplayMetrics();
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            display.getRealMetrics(dm);
            int width = dm.widthPixels;
            int height = dm.heightPixels;
            int density = dm.densityDpi;
            // scale down if too large
            int maxDim = Math.max(width, height);
            if (maxDim > 1280) {
                float scale = 1280f / maxDim;
                width = Math.round(width * scale);
                height = Math.round(height * scale);
            }

            MediaProjectionManager mpm = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            mediaProjection = mpm.getMediaProjection(resultCode, resultData);

            MediaFormat format = MediaFormat.createVideoFormat("video/avc", width, height);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, Math.max(2_000_000, width * height * 4));
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

            encoder = MediaCodec.createEncoderByType("video/avc");
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            inputSurface = encoder.createInputSurface();
            encoder.start();

            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "RemoteControl-VD",
                    width,
                    height,
                    density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    inputSurface,
                    null,
                    null
            );

            running = true;
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            while (running) {
                int outIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000);
                if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    continue;
                } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.i(TAG, "Encoder format changed: " + encoder.getOutputFormat());
                } else if (outIndex >= 0) {
                    ByteBuffer outputBuffer = encoder.getOutputBuffer(outIndex);
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        byte[] bytes = new byte[bufferInfo.size];
                        outputBuffer.position(bufferInfo.offset);
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                        outputBuffer.get(bytes);
                        networkClient.sendBinary(bytes, 0, bytes.length);
                    }
                    encoder.releaseOutputBuffer(outIndex, false);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Encoding error", e);
        } finally {
            stop();
        }
    }
}
