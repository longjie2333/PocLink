package com.example.remotecontrol;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;

public class AccessibilityControlService extends AccessibilityService {
    private static AccessibilityControlService instance;
    private WindowManager windowManager;
    private PointerOverlayView overlayView;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        ensureOverlay();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        if (overlayView != null) {
            try { windowManager.removeView(overlayView); } catch (Throwable ignore) {}
            overlayView = null;
        }
    }

    @Override
    public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    private void ensureOverlay() {
        if (overlayView != null) return;
        overlayView = new PointerOverlayView(this);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        windowManager.addView(overlayView, lp);
    }

    public static void showCircle(Context ctx, int x, int y) {
        if (instance == null) return;
        instance.ensureOverlay();
        instance.overlayView.showAt(x, y);
    }

    public static void performTap(Context ctx, int x, int y) {
        if (instance == null) return;
        Path p = new Path();
        p.moveTo(x, y);
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(p, 0, 50);
        GestureDescription gesture = new GestureDescription.Builder().addStroke(stroke).build();
        instance.dispatchGesture(gesture, null, null);
    }

    public static void performSwipe(Context ctx, int x1, int y1, int x2, int y2, int durationMs) {
        if (instance == null) return;
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(p, 0, Math.max(100, durationMs));
        GestureDescription gesture = new GestureDescription.Builder().addStroke(stroke).build();
        instance.dispatchGesture(gesture, null, null);
    }

    private static class PointerOverlayView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Handler handler = new Handler(Looper.getMainLooper());
        private int x = -1, y = -1;
        private boolean visible = false;

        public PointerOverlayView(Context context) {
            super(context);
            paint.setColor(Color.argb(180, 255, 64, 64));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(8f);
        }

        public void showAt(int x, int y) {
            this.x = x;
            this.y = y;
            this.visible = true;
            invalidate();
            handler.removeCallbacks(hideRunnable);
            handler.postDelayed(hideRunnable, 600);
        }

        private final Runnable hideRunnable = () -> {
            visible = false;
            invalidate();
        };

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (!visible || x < 0 || y < 0) return;
            float radius = 60f;
            canvas.drawCircle(x, y, radius, paint);
        }
    }
}
