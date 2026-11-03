package org.smoodri.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

/**
 * Optional native floating overlay attached to the *existing* monitoring foreground service.
 * The snapshot is rendered by the same NativePipView used for Android PiP.
 * Overlay has no separate sensor or GPS reader and is never created without permission.
 */
final class NativeOverlayController {
    static final String PREFS = "smoodri_overlay_geometry_v1";
    private final DriveMonitorService service;
    private final WindowManager manager;
    private final SharedPreferences preferences;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private FrameLayout root;
    private NativePipView content;
    private WindowManager.LayoutParams window;
    private int requestedWidth = 320, requestedHeight = 260;
    private String lastError = "";
    String lastError(){return lastError;}
    private final Runnable frameUpdater = new Runnable() {
        @Override public void run() {
            if (root != null && content != null) {
                content.invalidate();
                handler.postDelayed(this, 200);
            }
        }
    };

    NativeOverlayController(DriveMonitorService service) {
        this.service = service;
        manager = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
        preferences = service.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private int dp(float value) { return (int)(value * service.getResources().getDisplayMetrics().density + .5f); }
    private int toDp(float pixels) { return (int)(pixels / service.getResources().getDisplayMetrics().density + .5f); }
    private int screenWidth() { DisplayMetrics m = new DisplayMetrics(); manager.getDefaultDisplay().getMetrics(m); return m.widthPixels; }
    private int screenHeight() { DisplayMetrics m = new DisplayMetrics(); manager.getDefaultDisplay().getMetrics(m); return m.heightPixels; }
    private int allowedWidth(int value) { return Math.max(Math.min(dp(180), screenWidth()), Math.min(value, screenWidth())); }
    private int allowedHeight(int value) { return Math.max(Math.min(dp(145), screenHeight()), Math.min(value, screenHeight())); }
    private void keepOnScreen() {
        if (window == null) return;
        window.width=allowedWidth(window.width);
        window.height=allowedHeight(window.height);
        window.x=Math.max(0,Math.min(window.x,Math.max(0,screenWidth()-window.width)));
        window.y=Math.max(0,Math.min(window.y,Math.max(0,screenHeight()-window.height)));
    }
    private void persistGeometry() {
        if(window==null)return;
        preferences.edit().putInt("width",toDp(window.width)).putInt("height",toDp(window.height))
                .putInt("x",window.x).putInt("y",window.y).apply();
    }
    void configure(JSONObject settings) {
        if(settings==null)return;
        if(settings.has("overlayWidth")) requestedWidth=settings.optInt("overlayWidth",requestedWidth);
        if(settings.has("overlayHeight")) requestedHeight=settings.optInt("overlayHeight",requestedHeight);
        // A configuration changed in SETTING has precedence over previously dragged size.
        if(window!=null) {
            window.width=allowedWidth(dp(requestedWidth));
            window.height=allowedHeight(dp(requestedHeight));
            keepOnScreen();
            updateLayout();
            persistGeometry();
        }
    }
    private GradientDrawable background(int color, float radius, int border) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        drawable.setStroke(dp(1), border);
        return drawable;
    }
    private TextView control(String text) {
        TextView view = new TextView(service);
        view.setText(text);
        view.setTextSize(13);
        view.setTextColor(Color.WHITE);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(8), 0, dp(8), 0);
        view.setBackground(background(0xFF294058, 7, 0xFF47617A));
        view.setMinHeight(dp(34));
        return view;
    }
    private void updateLayout(){
        if(root==null || window==null)return;
        keepOnScreen();
        try { manager.updateViewLayout(root, window); }
        catch(IllegalArgumentException ignored) { /* overlay removed by Android */ }
    }
    private void enableDrag(View handle) {
        handle.setOnTouchListener(new View.OnTouchListener() {
            float initialX, initialY; int originX,originY;
            @Override public boolean onTouch(View view, MotionEvent event) {
                if(window==null)return false;
                switch(event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX=event.getRawX();initialY=event.getRawY();
                        originX=window.x;originY=window.y;return true;
                    case MotionEvent.ACTION_MOVE:
                        window.x=originX+Math.round(event.getRawX()-initialX);
                        window.y=originY+Math.round(event.getRawY()-initialY);
                        updateLayout();return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        persistGeometry();return true;
                    default:return false;
                }
            }
        });
    }
    private void enableResize(View handle) {
        handle.setOnTouchListener(new View.OnTouchListener() {
            float initialX, initialY; int initialWidth,initialHeight;
            @Override public boolean onTouch(View view, MotionEvent event) {
                if(window==null)return false;
                switch(event.getActionMasked()){
                    case MotionEvent.ACTION_DOWN:
                        initialX=event.getRawX();initialY=event.getRawY();
                        initialWidth=window.width;initialHeight=window.height;return true;
                    case MotionEvent.ACTION_MOVE:
                        window.width=allowedWidth(initialWidth+Math.round(event.getRawX()-initialX));
                        window.height=allowedHeight(initialHeight+Math.round(event.getRawY()-initialY));
                        updateLayout();return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        requestedWidth=toDp(window.width);requestedHeight=toDp(window.height);
                        persistGeometry();
                        preferences.edit().putLong("dragRevision",System.currentTimeMillis()).apply();
                        return true;
                    default:return false;
                }
            }
        });
    }
    boolean show() {
        if(root!=null){lastError="";return true;}
        if(manager==null){ lastError="WindowManager không khả dụng.";return false; }
        if(!Settings.canDrawOverlays(service)){
            lastError="Android chưa cấp quyền Hiển thị trên ứng dụng khác.";
            return false;
        }
        window = new WindowManager.LayoutParams(
                allowedWidth(dp(requestedWidth)),
                allowedHeight(dp(requestedHeight)),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        window.gravity=Gravity.TOP | Gravity.START;
        window.x=preferences.getInt("x",dp(12));
        window.y=preferences.getInt("y",dp(120));
        keepOnScreen();

        root=new FrameLayout(service);
        root.setBackground(background(0xFF101F31, 12, 0xFF6481A0));
        root.setClipToOutline(true);
        LinearLayout column=new LinearLayout(service);
        column.setOrientation(LinearLayout.VERTICAL);
        root.addView(column,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout titleBar=new LinearLayout(service);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setPadding(dp(8),dp(3),dp(4),dp(3));
        titleBar.setBackgroundColor(0xFF20374F);
        column.addView(titleBar,new LinearLayout.LayoutParams(-1,dp(41)));
        TextView dragLabel=control("≡  SmooDri  ·  OVERLAY");
        dragLabel.setBackgroundColor(Color.TRANSPARENT);
        dragLabel.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams dragParams=new LinearLayout.LayoutParams(0,-1,1f);
        titleBar.addView(dragLabel,dragParams);
        enableDrag(dragLabel);

        TextView open=control("MỞ");
        LinearLayout.LayoutParams controlParams=new LinearLayout.LayoutParams(-2,dp(34));
        controlParams.rightMargin=dp(5);
        titleBar.addView(open,controlParams);
        open.setOnClickListener(v->{
            hide();
            Intent i=new Intent(service,MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            service.startActivity(i);
        });
        TextView stop=control("×");
        stop.setTextSize(24);
        titleBar.addView(stop,new LinearLayout.LayoutParams(dp(38),dp(34)));
        stop.setOnClickListener(v->service.stopFromOverlay());

        content=new NativePipView(service);
        content.setOverlayMode(true);
        column.addView(content,new LinearLayout.LayoutParams(-1,0,1f));

        TextView resize=control("◢");
        resize.setTextSize(22);
        resize.setBackground(background(0xB8294058,5,0xFF47617A));
        FrameLayout.LayoutParams resizeParams=new FrameLayout.LayoutParams(dp(44),dp(39),Gravity.RIGHT | Gravity.BOTTOM);
        root.addView(resize,resizeParams);
        enableResize(resize);
        try {
            manager.addView(root,window);
            handler.removeCallbacks(frameUpdater);
            handler.post(frameUpdater);
            persistGeometry();
            lastError="";
            return true;
        } catch (RuntimeException error) {
            lastError="Android từ chối tạo Overlay ("+error.getClass().getSimpleName()+"): "+
                (error.getMessage()==null?"không có chi tiết":error.getMessage());
            Log.e("SmooDriOverlay", lastError, error);
            handler.removeCallbacks(frameUpdater);
            root=null;content=null;window=null;
            return false;
        }
    }
    void hide(){
        handler.removeCallbacks(frameUpdater);
        if(root!=null){
            try{manager.removeView(root);}catch(RuntimeException ignored){}
        }
        root=null;content=null;window=null;
    }
    boolean isVisible(){return root!=null;}
}
