package org.smoodri.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** SmooDri PWA shell: online OTA via Pages, bundled offline fallback. */
public final class MainActivity extends Activity {
    private static final String BASE_URL = "https://gptlohoi-del.github.io/smoodri/";
    private static final String HOST = "gptlohoi-del.github.io";
    private static final String WEB_PATH = "/smoodri/";
    private static final int REQ_LOCATION = 1801;
    private WebView web;
    private GeolocationPermissions.Callback locationCallback;
    private String locationOrigin;
    private boolean useBundledFallback;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        setContentView(web);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setDatabaseEnabled(true);
        web.getSettings().setGeolocationEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (origin == null || !origin.equals("https://" + HOST)) {
                    callback.invoke(origin, false, false);
                    return;
                }
                if (hasLocationPermission()) {
                    callback.invoke(origin, true, false);
                } else {
                    locationOrigin = origin;
                    locationCallback = callback;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[] {
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        }, REQ_LOCATION);
                    } else {
                        callback.invoke(origin, false, false);
                    }
                }
            }
        });
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (!"GET".equalsIgnoreCase(request.getMethod())) return null;
                android.net.Uri uri = request.getUrl();
                if (!"https".equals(uri.getScheme()) || !HOST.equals(uri.getHost())) return null;
                String path = uri.getPath();
                if (path == null || !path.startsWith(WEB_PATH)) return null;
                // Khi chưa vào chế độ dự phòng, cho Service Worker xử lý trước.
                // Nhờ vậy WebView offline ưu tiên bản OTA đã lưu, không bị ép về
                // tài nguyên cũ đóng gói trong APK. Chỉ dùng asset khi thật sự
                // không thể mở trang và đã kích hoạt showBundledPage().
                if (!useBundledFallback) return null;
                String relative = path.substring(WEB_PATH.length());
                if (relative.isEmpty()) relative = "index.html";
                if (relative.contains("..") || relative.startsWith("/")) return null;
                return fromAssets(relative);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame() && !useBundledFallback) showBundledPage();
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                if (request.isForMainFrame() && errorResponse.getStatusCode() >= 400 && !useBundledFallback) {
                    showBundledPage();
                }
            }
        });
        web.loadUrl(BASE_URL);
    }

    private boolean hasNetwork() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            Network net = cm.getActiveNetwork();
            if (net == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(net);
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean hasLocationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_LOCATION) return;
        boolean granted = hasLocationPermission();
        if (locationCallback != null) locationCallback.invoke(locationOrigin, granted, false);
        locationCallback = null;
        locationOrigin = null;
    }

    private WebResourceResponse fromAssets(String relative) {
        String mime;
        if (relative.endsWith(".html")) mime = "text/html";
        else if (relative.endsWith(".css")) mime = "text/css";
        else if (relative.endsWith(".js")) mime = "application/javascript";
        else if (relative.endsWith(".json")) mime = "application/json";
        else if (relative.endsWith(".png")) mime = "image/png";
        else if (relative.endsWith(".svg")) mime = "image/svg+xml";
        else return null;
        try {
            InputStream input = getAssets().open("web/" + relative);
            return new WebResourceResponse(mime, "UTF-8", input);
        } catch (IOException ignored) {
            return null;
        }
    }

    private void showBundledPage() {
        useBundledFallback = true;
        try (InputStream input = getAssets().open("web/index.html")) {
            byte[] bytes = new byte[input.available()];
            int read = 0;
            while (read < bytes.length) {
                int n = input.read(bytes, read, bytes.length - read);
                if (n < 0) break;
                read += n;
            }
            String html = new String(bytes, 0, read, StandardCharsets.UTF_8);
            web.loadDataWithBaseURL(BASE_URL, html, "text/html", "UTF-8", BASE_URL);
        } catch (IOException ignored) {
            // The bundled assets are verified by the Gradle sync step.
        }
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
