package org.smoodri.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;

/** SmooDri audio-only hybrid APK: Web Audio while visible, native alert service in background. */
public final class MainActivity extends Activity {
    private static final String BASE_URL="https://gptlohoi-del.github.io/smoodri/";
    private static final String HOST="gptlohoi-del.github.io",WEB_PATH="/smoodri/";
    private static final int REQ_GPS=1901,REQ_NOTIFICATIONS=1902;
    private WebView web;
    private final NativeAlertPlayer previewPlayer=new NativeAlertPlayer();
    private GeolocationPermissions.Callback gpsCallback;
    private String gpsOrigin;
    private boolean useBundledFallback,sessionRequested,sessionConfirmed;
    private long startRequestedAt,lastSampleSeq;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable updateLoop=new Runnable(){
        @Override public void run(){
            if(web!=null){
                boolean running=DriveMonitorService.running();
                if(sessionRequested&&running&&!sessionConfirmed){
                    sessionConfirmed=true;
                    signalStarted(true,"");
                }
                if(sessionRequested&&!running&&System.currentTimeMillis()-startRequestedAt>5000){
                    sessionRequested=false;
                    String reason=DriveMonitorService.lastStartError();
                    if(reason.isEmpty())reason="Không nhận được cảm biến hoặc dịch vụ Android đã dừng.";
                    if(!sessionConfirmed)signalStarted(false,reason);
                    else web.evaluateJavascript("window.SmooDriNative&&window.SmooDriNative.onMonitorStopped("+
                            JSONObject.quote(reason)+");",null);
                    sessionConfirmed=false;
                }
                if(running&&DriveMonitorService.isUiVisible()){
                    long seq=DriveMonitorService.lastSampleSeq();
                    if(seq>0&&seq!=lastSampleSeq){
                        lastSampleSeq=seq;
                        String sample=DriveMonitorService.lastSample();
                        if(!sample.isEmpty())web.evaluateJavascript(
                                "window.SmooDriNative&&window.SmooDriNative.onNativeSample("+sample+");",null);
                    }
                }
            }
            handler.postDelayed(this,150);
        }
    };

    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);
        DriveMonitorService.setUiVisible(true);
        web=new WebView(this);
        setContentView(web);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setDatabaseEnabled(true);
        web.getSettings().setGeolocationEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        // Only load trusted first-party content. Never embed untrusted iframes.
        web.addJavascriptInterface(new WebBridge(),"SmooDriAndroid");
        web.setWebChromeClient(new WebChromeClient(){
            @Override public void onGeolocationPermissionsShowPrompt(String origin,GeolocationPermissions.Callback cb){
                if(!("https://"+HOST).equals(origin)){cb.invoke(origin,false,false);return;}
                if(hasLocationPermission())cb.invoke(origin,true,false);
                else{
                    gpsCallback=cb;gpsOrigin=origin;
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},REQ_GPS);
                }
            }
        });
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
                android.net.Uri uri=request.getUrl();
                return !"https".equals(uri.getScheme()) || !HOST.equals(uri.getHost()) ||
                        uri.getPath()==null || !uri.getPath().startsWith(WEB_PATH);
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){
                if(!"GET".equalsIgnoreCase(request.getMethod()))return null;
                android.net.Uri uri=request.getUrl();
                if(!"https".equals(uri.getScheme())||!HOST.equals(uri.getHost()))return null;
                String path=uri.getPath();
                if(path==null||!path.startsWith(WEB_PATH)||!useBundledFallback)return null;
                String relative=path.substring(WEB_PATH.length());
                if(relative.isEmpty())relative="index.html";
                if(relative.contains("..")||relative.startsWith("/"))return null;
                return fromAssets(relative);
            }
            @Override public void onReceivedError(WebView view,WebResourceRequest request,WebResourceError error){
                if(request.isForMainFrame()&&!useBundledFallback)showBundledPage();
            }
            @Override public void onReceivedHttpError(WebView view,WebResourceRequest request,WebResourceResponse response){
                if(request.isForMainFrame()&&response.getStatusCode()>=400&&!useBundledFallback)showBundledPage();
            }
        });
        web.loadUrl(BASE_URL);
        handler.post(updateLoop);
    }
    private boolean hasLocationPermission(){
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;
    }
    private boolean hasNetwork(){
        try{
            ConnectivityManager cm=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            if(cm==null)return false;
            Network n=cm.getActiveNetwork();
            if(n==null)return false;
            NetworkCapabilities nc=cm.getNetworkCapabilities(n);
            return nc!=null&&nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }catch(Exception e){return false;}
    }
    private void sendService(String action,String config){
        Intent intent=new Intent(this,DriveMonitorService.class).setAction(action);
        if(config!=null)intent.putExtra(DriveMonitorService.EXTRA_SETTINGS,config);
        if(DriveMonitorService.ACTION_START.equals(action))startForegroundService(intent);
        else startService(intent);
    }
    private void signalStarted(boolean success,String message){
        if(web!=null)web.evaluateJavascript("window.SmooDriNative&&window.SmooDriNative.onStarted("+
                success+","+JSONObject.quote(message)+");",null);
        if(!success)Toast.makeText(this,message,Toast.LENGTH_LONG).show();
    }
    private class WebBridge{
        @JavascriptInterface public void start(String config){runOnUiThread(()->{
            try{
                sendService(DriveMonitorService.ACTION_START,config);
                sessionRequested=true;sessionConfirmed=false;startRequestedAt=System.currentTimeMillis();
                if(Build.VERSION.SDK_INT>=33 &&
                        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIFICATIONS);
                }
            }catch(Exception error){signalStarted(false,"Không bật được dịch vụ cảm biến: "+error.getClass().getSimpleName());}
        });}
        @JavascriptInterface public void stop(){runOnUiThread(()->{
            sessionRequested=false;sessionConfirmed=false;
            stopService(new Intent(MainActivity.this,DriveMonitorService.class));
        });}
        @JavascriptInterface public void updateConfig(String config){runOnUiThread(()->{
            if(DriveMonitorService.running())sendService(DriveMonitorService.ACTION_SETTINGS,config);
        });}
        @JavascriptInterface public void goBackground(){runOnUiThread(()->{
            if(DriveMonitorService.running())moveTaskToBack(true);
            else Toast.makeText(MainActivity.this,"Hãy nhấn BẮT ĐẦU trước.",Toast.LENGTH_SHORT).show();
        });}
        @JavascriptInterface public void testBackgroundSound(String kind,int volume,String wave){
            runOnUiThread(()->previewPlayer.play(kind,wave,volume));
        }
        @JavascriptInterface public boolean monitoring(){return DriveMonitorService.running();}
        @JavascriptInterface public String diagnostics(){
            JSONObject result=new JSONObject();
            try{
                result.put("version","1.0.10");
                result.put("running",DriveMonitorService.running());
                result.put("sensorReady",DriveMonitorService.sensorReady());
                result.put("soundEnabled",DriveMonitorService.soundEnabled());
                result.put("uiVisible",DriveMonitorService.isUiVisible());
                result.put("locationPermission",hasLocationPermission());
                result.put("lastAlert",DriveMonitorService.lastAlert());
                result.put("error",DriveMonitorService.lastStartError());
                result.put("audioError",DriveMonitorService.lastAudioError());
                result.put("previewAudioError",previewPlayer.lastError());
            }catch(Exception ignored){}
            return result.toString();
        }
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){
        super.onRequestPermissionsResult(code,permissions,results);
        if(code==REQ_GPS && gpsCallback!=null){
            gpsCallback.invoke(gpsOrigin,hasLocationPermission(),false);
            gpsCallback=null;gpsOrigin=null;
        }
    }
    @Override protected void onResume(){
        super.onResume();
        DriveMonitorService.setUiVisible(true);
        if(web!=null)web.evaluateJavascript("window.SmooDriNative&&window.SmooDriNative.onAppResumed&&window.SmooDriNative.onAppResumed();",null);
        if(useBundledFallback&&hasNetwork()&&web!=null){useBundledFallback=false;web.loadUrl(BASE_URL);}
    }
    @Override protected void onPause(){
        DriveMonitorService.setUiVisible(false);
        super.onPause();
    }
    private WebResourceResponse fromAssets(String file){
        String mime;
        if(file.endsWith(".html"))mime="text/html";
        else if(file.endsWith(".css"))mime="text/css";
        else if(file.endsWith(".js"))mime="application/javascript";
        else if(file.endsWith(".json"))mime="application/json";
        else if(file.endsWith(".png"))mime="image/png";
        else if(file.endsWith(".svg"))mime="image/svg+xml";
        else return null;
        try{InputStream input=getAssets().open("web/"+file);return new WebResourceResponse(mime,"UTF-8",input);}
        catch(IOException error){return null;}
    }
    private void showBundledPage(){
        useBundledFallback=true;
        try(InputStream input=getAssets().open("web/index.html")){
            java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
            byte[] buf=new byte[8192];int len;
            while((len=input.read(buf))>=0)out.write(buf,0,len);
            web.loadDataWithBaseURL(BASE_URL,out.toString("UTF-8"),"text/html","UTF-8",BASE_URL);
        }catch(IOException ignored){Toast.makeText(this,"Không có bản offline",Toast.LENGTH_LONG).show();}
    }
    @Override public void onBackPressed(){
        if(web!=null&&web.canGoBack())web.goBack();
        else super.onBackPressed();
    }
    @Override protected void onDestroy(){
        handler.removeCallbacks(updateLoop);
        if(isFinishing())stopService(new Intent(this,DriveMonitorService.class));
        previewPlayer.release();
        if(web!=null){web.destroy();web=null;}
        super.onDestroy();
    }
}
