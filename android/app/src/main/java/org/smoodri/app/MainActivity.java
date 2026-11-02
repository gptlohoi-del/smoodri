package org.smoodri.app;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.provider.Settings;
import android.net.Uri;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Rational;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.location.LocationManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Standalone Android SmooDri with native location service + native PiP. */
public final class MainActivity extends Activity {
    private static final String BASE_URL = "https://gptlohoi-del.github.io/smoodri/";
    private static final String HOST = "gptlohoi-del.github.io";
    private static final String WEB_PATH = "/smoodri/";
    private static final int REQ_LOCATION_START = 1901;
    private static final int REQ_NOTIFICATIONS = 1902;
    private WebView web;
    private NativePipView pipView;
    private FrameLayout root;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateLoop = new Runnable() {
        @Override public void run() {
            if (web != null) {
                boolean running = DriveMonitorService.running();
                if (running != previousRunning) { previousRunning=running; updatePipParams(); }
                String ratio=DriveMonitorService.snapshot().pipRatio;
                if(!ratio.equals(lastPipRatio)){ lastPipRatio=ratio; updatePipParams(); }
                if (running && !isInPictureInPictureMode()) {
                    long seq=DriveMonitorService.lastSampleSeq();
                    if(seq!=lastSeq && seq>0) {
                        lastSeq=seq;
                        String sample=DriveMonitorService.lastSample();
                        if (!sample.isEmpty()) web.evaluateJavascript("window.SmooDriNative&&window.SmooDriNative.onNativeSample("+sample+");",null);
                    }
                }
                if (sessionRequested && !running && System.currentTimeMillis()-startRequestedAt>4000) {
                    sessionRequested=false;
                    web.evaluateJavascript("window.SmooDriNative&&window.SmooDriNative.onMonitorStopped();",null);
                    if(isInPictureInPictureMode())finish();
                }
                if(pipView!=null && isInPictureInPictureMode()) pipView.invalidate();
            }
            handler.postDelayed(this,150);
        }
    };
    private GeolocationPermissions.Callback locationCallback;
    private String locationOrigin;
    private String pendingConfig;
    private boolean previousRunning,sessionRequested,useBundledFallback;
    private long lastSeq,startRequestedAt;
    private String lastPipRatio = "";
    public static volatile boolean uiForeground;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        root=new FrameLayout(this);
        web=new WebView(this);
        pipView=new NativePipView(this);
        root.addView(web,new FrameLayout.LayoutParams(-1,-1));
        root.addView(pipView,new FrameLayout.LayoutParams(-1,-1));
        pipView.setVisibility(View.GONE);
        setContentView(root);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setDatabaseEnabled(true);
        web.getSettings().setGeolocationEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        // This bridge is exposed only to the trusted first-party GitHub Pages app. Don't embed untrusted iframes.
        web.addJavascriptInterface(new WebBridge(),"SmooDriAndroid");
        web.setWebChromeClient(new WebChromeClient(){
            @Override public void onGeolocationPermissionsShowPrompt(String origin,GeolocationPermissions.Callback cb){
                if(!("https://"+HOST).equals(origin)){cb.invoke(origin,false,false);return;}
                if(hasLocationPermission())cb.invoke(origin,true,false);
                else { locationOrigin=origin; locationCallback=cb;
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},REQ_LOCATION_START); }
            }
        });
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest req){
                android.net.Uri uri=req.getUrl();
                return !"https".equals(uri.getScheme()) || !HOST.equals(uri.getHost()) ||
                        uri.getPath()==null || !uri.getPath().startsWith(WEB_PATH);
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest request){
                if(!"GET".equalsIgnoreCase(request.getMethod())) return null;
                android.net.Uri uri=request.getUrl();
                if(!"https".equals(uri.getScheme()) || !HOST.equals(uri.getHost())) return null;
                String path=uri.getPath();
                if(path==null || !path.startsWith(WEB_PATH))return null;
                if(!useBundledFallback)return null;
                String relative=path.substring(WEB_PATH.length());
                if(relative.isEmpty())relative="index.html";
                if(relative.contains("..") || relative.startsWith("/"))return null;
                return fromAssets(relative);
            }
            @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){
                if(r.isForMainFrame() && !useBundledFallback)showBundledPage();
            }
            @Override public void onReceivedHttpError(WebView v,WebResourceRequest r,WebResourceResponse e){
                if(r.isForMainFrame() && e.getStatusCode()>=400 && !useBundledFallback)showBundledPage();
            }
        });
        updatePipParams();
        // First try the live website or Service Worker offline cache.
        // Do not intercept offline requests before Service Worker can serve a downloaded OTA.
        web.loadUrl(BASE_URL);
        handler.post(updateLoop);
    }
    private boolean hasNetwork(){
        try{
            ConnectivityManager cm=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            if(cm==null)return false;
            Network n=cm.getActiveNetwork();
            if(n==null)return false;
            NetworkCapabilities c=cm.getNetworkCapabilities(n);
            return c!=null && c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }catch(Exception e){return false;}
    }
    private boolean hasLocationPermission(){
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;
    }
    private boolean isLocationEnabled(){
        try{
            LocationManager lm=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
            return lm!=null && (lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
        }catch(Exception ignored){return false;}
    }
    private class WebBridge {
        @JavascriptInterface public void start(String config){
            runOnUiThread(()->requestStart(config));
        }
        @JavascriptInterface public void stop(){ runOnUiThread(()->stopMonitor()); }
        @JavascriptInterface public void updateConfig(String config){
            runOnUiThread(()->{
                if(DriveMonitorService.running())sendService(DriveMonitorService.ACTION_SETTINGS,config);
                updatePipParams();
            });
        }
        @JavascriptInterface public void enterPip(){ runOnUiThread(()->openPip(true)); }
        @JavascriptInterface public boolean supported(){ return Build.VERSION.SDK_INT>=26; }
    }
    private void requestStart(String config){
        if(!hasLocationPermission()){
            pendingConfig=config;
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},REQ_LOCATION_START);
            return;
        }
        if(!isLocationEnabled()){
            signalStarted(false,"Hãy bật Vị trí/GPS trên điện thoại rồi thử lại.");
            return;
        }
        try{
            sendService(DriveMonitorService.ACTION_START,config);
            sessionRequested=true;
            startRequestedAt=System.currentTimeMillis();
            updatePipParams();
            signalStarted(true,"");
            if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIFICATIONS);
            }
        }catch(Exception e){ signalStarted(false,"Android không khởi động được dịch vụ theo dõi."); }
    }
    private void signalStarted(boolean ok,String message){
        if(web!=null)web.evaluateJavascript("window.SmooDriNative&&window.SmooDriNative.onStarted("+ok+","+org.json.JSONObject.quote(message)+");",null);
        if(!ok)Toast.makeText(this,message,Toast.LENGTH_LONG).show();
    }
    private void sendService(String action,String config){
        Intent intent=new Intent(this,DriveMonitorService.class).setAction(action);
        if(config!=null)intent.putExtra(DriveMonitorService.EXTRA_SETTINGS,config);
        if(action.equals(DriveMonitorService.ACTION_START))startForegroundService(intent);
        else startService(intent);
    }
    private void stopMonitor(){
        sessionRequested=false;
        stopService(new Intent(this,DriveMonitorService.class));
        previousRunning=false;
        updatePipParams();
        if(isInPictureInPictureMode())finish();
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){
        super.onRequestPermissionsResult(code,permissions,results);
        if(code!=REQ_LOCATION_START)return;
        boolean granted=hasLocationPermission();
        if(locationCallback!=null){locationCallback.invoke(locationOrigin,granted,false);locationCallback=null;locationOrigin=null;}
        if(pendingConfig!=null){String cfg=pendingConfig;pendingConfig=null;
            if(granted)requestStart(cfg);
            else signalStarted(false,"SmooDri cần quyền Vị trí để theo dõi khi chạy nền.");
        }
    }
    // Dùng cùng một bộ tham số cho cả PiP tự động và PiP mở bằng nút.
    // Activity không có API getPictureInPictureParams().
    private PictureInPictureParams buildPipParams(){
        String ratio=DriveMonitorService.snapshot().pipRatio;
        int a=16,b=9;
        if("4:3".equals(ratio)){a=4;b=3;}
        else if("1:1".equals(ratio)){a=1;b=1;}
        PictureInPictureParams.Builder builder=new PictureInPictureParams.Builder()
                .setAspectRatio(new Rational(a,b));
        if(Build.VERSION.SDK_INT>=31){
            builder.setAutoEnterEnabled(DriveMonitorService.running() || sessionRequested);
        }
        return builder.build();
    }
    private void updatePipParams(){
        if(Build.VERSION.SDK_INT<26)return;
        setPictureInPictureParams(buildPipParams());
    }
    private void openPip(boolean userInitiated){
        if(Build.VERSION.SDK_INT<26){Toast.makeText(this,"Thiết bị không hỗ trợ PiP.",Toast.LENGTH_SHORT).show();return;}
        if(!DriveMonitorService.running()){
            Toast.makeText(this,"Hãy nhấn BẮT ĐẦU trước khi mở PiP.",Toast.LENGTH_SHORT).show();return;
        }
        try{
            AppOpsManager ops=(AppOpsManager)getSystemService(Context.APP_OPS_SERVICE);
            if(ops!=null && ops.checkOpNoThrow(AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                    android.os.Process.myUid(),getPackageName())==AppOpsManager.MODE_IGNORED){
                if(userInitiated){
                    Toast.makeText(this,"Hãy bật quyền Hình trong hình (PiP) cho SmooDri.",Toast.LENGTH_LONG).show();
                    Intent settings=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:"+getPackageName()));
                    startActivity(settings);
                }
                return;
            }
            updatePipParams();
            if(!enterPictureInPictureMode(buildPipParams())){
                Toast.makeText(this,"Android chưa cho phép mở PiP; hãy kiểm tra cài đặt Hình trong hình.",Toast.LENGTH_LONG).show();
            }
        }catch(RuntimeException e){
            Toast.makeText(this,"Không mở được PiP. Kiểm tra quyền Hình trong hình trên điện thoại.",Toast.LENGTH_LONG).show();
        }
    }
    @Override protected void onResume(){
        super.onResume();
        uiForeground=!isInPictureInPictureMode();
        // If we had to load the APK-bundled fallback, retry the real web on the next
        // foreground resume with internet. The Service Worker can then update its cache.
        if(useBundledFallback && hasNetwork() && web!=null && !isInPictureInPictureMode()){
            useBundledFallback=false;
            web.loadUrl(BASE_URL);
        }
    }
    @Override protected void onPause(){ uiForeground=false; super.onPause(); }
    @Override public void onUserLeaveHint(){
        super.onUserLeaveHint();
        if(Build.VERSION.SDK_INT>=26 && Build.VERSION.SDK_INT<31 && DriveMonitorService.running())openPip(false);
    }
    @Override public void onPictureInPictureModeChanged(boolean inPip,Configuration c){
        super.onPictureInPictureModeChanged(inPip,c);
        uiForeground=!inPip;
        pipView.setVisibility(inPip?View.VISIBLE:View.GONE);
        web.setVisibility(inPip?View.INVISIBLE:View.VISIBLE);
        if(inPip)pipView.invalidate();
    }
    private WebResourceResponse fromAssets(String path){
        String mime;
        if(path.endsWith(".html"))mime="text/html";
        else if(path.endsWith(".css"))mime="text/css";
        else if(path.endsWith(".js"))mime="application/javascript";
        else if(path.endsWith(".json"))mime="application/json";
        else if(path.endsWith(".png"))mime="image/png";
        else if(path.endsWith(".svg"))mime="image/svg+xml";
        else return null;
        try{
            InputStream input=getAssets().open("web/"+path);
            return new WebResourceResponse(mime,"UTF-8",input);
        }catch(IOException e){return null;}
    }
    private void showBundledPage(){
        useBundledFallback=true;
        try(InputStream input=getAssets().open("web/index.html")){
            java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
            byte[] bytes=new byte[8192];int n;
            while((n=input.read(bytes))!=-1)out.write(bytes,0,n);
            web.loadDataWithBaseURL(BASE_URL,out.toString("UTF-8"),"text/html","UTF-8",BASE_URL);
        }catch(IOException e){Toast.makeText(this,"Không tìm thấy nội dung offline",Toast.LENGTH_LONG).show();}
    }
    @Override public void onBackPressed(){
        if(web!=null && web.canGoBack() && !isInPictureInPictureMode())web.goBack();
        else super.onBackPressed();
    }
    @Override protected void onDestroy(){
        handler.removeCallbacks(updateLoop);
        if(isFinishing())stopService(new Intent(this,DriveMonitorService.class));
        if(web!=null){root.removeView(web);web.destroy();web=null;}
        super.onDestroy();
    }
}
