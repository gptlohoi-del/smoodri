package org.smoodri.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayDeque;
import java.util.Locale;

/** Android-owned sampling independent of WebView visibility; limited wake lock for audio-only mode. */
public final class DriveMonitorService extends Service implements SensorEventListener, LocationListener {
    public static final String ACTION_START = "org.smoodri.app.START";
    public static final String ACTION_STOP = "org.smoodri.app.STOP";
    public static final String ACTION_SETTINGS = "org.smoodri.app.SETTINGS";
    public static final String ACTION_OVERLAY_SHOW = "org.smoodri.app.OVERLAY_SHOW";
    public static final String ACTION_OVERLAY_HIDE = "org.smoodri.app.OVERLAY_HIDE";
    public static final String ACTION_TEST_BEEP = "org.smoodri.app.TEST_BEEP";
    public static final String EXTRA_SETTINGS = "settings";
    private static final String CHANNEL_ID = "smoodri_monitor_v1";
    private static final int NOTIFICATION_ID = 2026;
    private static final double GRAVITY = 9.80665;
    private static volatile DriveMonitorService instance;
    private static volatile String startError="";
    private volatile boolean ready;
    public static String lastStartError(){return startError;}
    public static String lastOverlayError(){return overlayError;}
    public static boolean overlayVisible(){
        DriveMonitorService svc=instance;
        return svc!=null && svc.overlay!=null && svc.overlay.isVisible();
    }
    public static String lastAudioError(){
        DriveMonitorService svc=instance;
        return svc==null?"":svc.alarm.lastError();
    }
    private static volatile Snapshot lastSnapshot = new Snapshot();
    private static volatile String lastSample = "";
    private static volatile long lastSampleSeq = 0;

    public static boolean running() { return instance != null && instance.ready; }
    public static Snapshot snapshot() { return lastSnapshot; }
    public static String lastSample() { return lastSample; }
    public static long lastSampleSeq() { return lastSampleSeq; }

    public static final class Snapshot {
        public final boolean active, calibrated;
        public final int ga, phanh, cua, xoc;
        public final float speed, accuracy, hz, lateralG, longitudinalG, cupLimit;
        public final String drivingStatus;
        public final long gpsAgeMs, sensorAgeMs;
        public final String pipRatio, bg, fg;
        public final int speedFontSize, gaFontSize, phanhFontSize, cuaFontSize, xocFontSize, gpsFontSize;
        public final int fontWeight, statusFontSize, cupSize;
        public final String[] visible;
        Snapshot() {
            this(false, false, 0, 0, 0, 0, Float.NaN, Float.NaN, Float.NaN,
                 Long.MAX_VALUE, Long.MAX_VALUE, "16:9", "#0d1a2d", "#ffffff",
                 44, 22, 22, 22, 22, 19, 700, 25, 130,
                 0f, 0f, .20f, "SẴN SÀNG", new String[] {"speed", "status", "cup"});
        }
        Snapshot(boolean active, boolean calibrated, int ga, int phanh, int cua, int xoc,
                 float speed, float accuracy, float hz, long gpsAgeMs, long sensorAgeMs,
                 String pipRatio, String bg, String fg, int speedFontSize, int gaFontSize,
                 int phanhFontSize, int cuaFontSize, int xocFontSize, int gpsFontSize,
                 int fontWeight, int statusFontSize, int cupSize,
                 float lateralG, float longitudinalG, float cupLimit, String drivingStatus, String[] visible) {
            this.active=active; this.calibrated=calibrated;
            this.ga=ga; this.phanh=phanh; this.cua=cua; this.xoc=xoc;
            this.speed=speed; this.accuracy=accuracy; this.hz=hz;
            this.gpsAgeMs=gpsAgeMs; this.sensorAgeMs=sensorAgeMs;
            this.pipRatio=pipRatio; this.bg=bg; this.fg=fg;
            this.speedFontSize=speedFontSize; this.gaFontSize=gaFontSize;
            this.phanhFontSize=phanhFontSize; this.cuaFontSize=cuaFontSize;
            this.xocFontSize=xocFontSize; this.gpsFontSize=gpsFontSize;
            this.fontWeight=fontWeight; this.statusFontSize=statusFontSize;
            this.cupSize=cupSize; this.lateralG=lateralG; this.longitudinalG=longitudinalG;
            this.cupLimit=cupLimit;
            this.drivingStatus=drivingStatus; this.visible=visible;
        }
    }

    private SensorManager sensors;
    private LocationManager locations;
    private double[] base = {0, 0, 1}, ag = new double[3], linear = new double[3];
    private boolean gotLinear, calibrated;
    private long calibrationStart, previousSample, lastSensor, lastGps, prevGps;
    private int calibrationCount;
    private double[] sum = new double[3];
    private final ArrayDeque<double[]> calibrationWindow = new ArrayDeque<>();
    private double smX, smLon, smVert, vertBaseline, slopeBaselineLon, prevX;
    private int ga, phanh, cua, xoc;
    private String drivingStatus = "ÊM ÁI";
    private long statusHoldUntil = 0;
    private int lastStatusLevel = 0;
    private float speed = Float.NaN, accuracy = Float.NaN, hz = Float.NaN;
    private float previousSpeed = Float.NaN;
    private int lastSign;
    private long lastReversal;
    private final ArrayDeque<Long> reversals = new ArrayDeque<>();
    private JSONObject configuration = new JSONObject();
    private long emitAt, lastAlert;
    private final NativeAlertPlayer alarm = new NativeAlertPlayer();
    private static volatile String overlayError = "";
    private PowerManager.WakeLock audioWakeLock;
    private NativeOverlayController overlay;

    /** Keep the CPU sampling while audio-only is selected, even when the screen sleeps.
     * Not an auto-restart mechanism; stops with the session or if system restrictions apply.
     */
    private void updateAudioWakeLock(){
        boolean required=sensors!=null && "audio".equals(configuration.optString("floatingMode","pip"))
                && configuration.optBoolean("soundEnabled",true);
        if(required){
            try {
                if(audioWakeLock==null){
                    PowerManager manager=(PowerManager)getSystemService(Context.POWER_SERVICE);
                    if(manager==null)return;
                    audioWakeLock=manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"SmooDri:AudioOnlyMonitoring");
                    audioWakeLock.setReferenceCounted(false);
                }
                if(!audioWakeLock.isHeld())audioWakeLock.acquire();
            }catch(RuntimeException ignored){}
        }else releaseAudioWakeLock();
    }
    private void releaseAudioWakeLock(){
        if(audioWakeLock!=null){
            try{if(audioWakeLock.isHeld())audioWakeLock.release();}catch(RuntimeException ignored){}
            audioWakeLock=null;
        }
    }

    @Override public void onCreate() {
        super.onCreate(); instance=this;
        overlayError="";
        overlay=new NativeOverlayController(this);
    }

    void stopFromOverlay(){ shutdown(); }
    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? "" : intent.getAction();
        if (ACTION_STOP.equals(action)) { shutdown(); return START_NOT_STICKY; }
        if (ACTION_OVERLAY_HIDE.equals(action)) {
            if(overlay!=null)overlay.hide();
            return START_NOT_STICKY;
        }
        if (ACTION_OVERLAY_SHOW.equals(action)) {
            if(!ready) overlayError="Dịch vụ theo dõi chưa sẵn sàng.";
            else if(!"overlay".equals(configuration.optString("floatingMode","pip")))
                overlayError="Chế độ cửa sổ nổi chưa được chọn trong SETTING.";
            else if(overlay!=null && overlay.show()) overlayError="";
            else overlayError=overlay==null?"Không khởi tạo được cửa sổ nổi.":overlay.lastError();
            return START_NOT_STICKY;
        }
        if (ACTION_TEST_BEEP.equals(action)) {
            if(ready)playNativeAlert(intent!=null && "pothole".equals(intent.getStringExtra("soundKind")));
            return START_NOT_STICKY;
        }
        if (ACTION_SETTINGS.equals(action) && !ready) return START_NOT_STICKY;
        if (intent != null && intent.hasExtra(EXTRA_SETTINGS)) {
            try { configuration = new JSONObject(intent.getStringExtra(EXTRA_SETTINGS)); }
            catch (Exception ignored) { /* use previous defaults */ }
        }
        if(overlay!=null)overlay.configure(configuration);
        if (ACTION_SETTINGS.equals(action) && sensors != null) {
            // Update settings for the single shared native alert player.
            updateAudioWakeLock();
            publish(); return START_NOT_STICKY;
        }
        if (sensors != null) { updateAudioWakeLock();publish(); return START_NOT_STICKY; }
        // For targetSdk 34, a location FGS needs location permission AND location enabled.
        if (!hasLocationPermission() || !locationEnabled()) {
            startError=!hasLocationPermission()?"Android chưa cấp quyền Vị trí cho SmooDri.":"Dịch vụ GPS/Vị trí đang tắt.";
            shutdown(); return START_NOT_STICKY;
        }
        startError="";
        createChannel();
        try {
            Notification notification = createNotification();
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else startForeground(NOTIFICATION_ID, notification);
        } catch (RuntimeException error) {
            startError="Không thể bật dịch vụ nền: "+error.getClass().getSimpleName();
            shutdown(); return START_NOT_STICKY;
        }
        sensors = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        locations = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        calibrationStart = SystemClock.elapsedRealtime();
        boolean accelerometerReady=false;
        if (sensors != null) {
            Sensor accel = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Sensor lin = sensors.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            if (accel != null) accelerometerReady=sensors.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
            if (lin != null) sensors.registerListener(this, lin, SensorManager.SENSOR_DELAY_GAME);
        }
        if(!accelerometerReady){
            startError="Không nhận được cảm biến gia tốc. Kiểm tra thiết bị hoặc chính sách quản trị.";
            shutdown();return START_NOT_STICKY;
        }
        try {
            if (locations != null) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
                        && locations.isProviderEnabled(LocationManager.GPS_PROVIDER))
                    locations.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this, Looper.getMainLooper());
                else if (locations.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                    locations.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, this, Looper.getMainLooper());
            }
        } catch (SecurityException | IllegalArgumentException ignored) {}
        ready=true;
        updateAudioWakeLock();
        publish();
        return START_NOT_STICKY;
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    private boolean locationEnabled() {
        try {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            return lm != null && (lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
        } catch (Exception e) { return false; }
    }
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "SmooDri đang theo dõi", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Cảm biến chuyển động và tốc độ GPS khi ứng dụng chạy nền");
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }
    private Notification createNotification() {
        Intent open = new Intent(this, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int imm = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent openPi = PendingIntent.getActivity(this, 1, open, imm);
        Intent stop = new Intent(this, DriveMonitorService.class).setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(this, 2, stop, imm);
        return new Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("SmooDri đang theo dõi")
            .setContentText("Đo cảm biến và GPS · Chạm để mở ứng dụng")
            .setContentIntent(openPi)
            .setOngoing(true)
            .addAction(new Notification.Action.Builder(null, "Dừng theo dõi", stopPi).build())
            .build();
    }
    private static double clamp(double x, double a, double b) { return Math.max(a, Math.min(b, x)); }
    private static double factor(double dt, double tau) { return 1-Math.exp(-clamp(dt,0,0.3)/tau); }
    private double setting(String name, double fallback) {
        double v = configuration.optDouble(name, fallback);
        return Double.isFinite(v) ? v : fallback;
    }
    /** Giá trị cỡ chữ PiP không bị chặn bởi MAX ẩn trong Android.
     * Chỉ yêu cầu kích thước dương, hữu hạn và biểu diễn được bằng int.
     * Khi chữ vượt chiều rộng PiP, NativePipView tự co để vừa chỗ.
     */
    private int pipTextSize(String name, int fallback) {
        double requested=setting(name,fallback);
        if (!(requested > 0)) return fallback;
        return (int)Math.min(Integer.MAX_VALUE, Math.round(requested));
    }
    private String color(String name, String fallback) {
        String s = configuration.optString(name, fallback);
        return s.matches("#[0-9a-fA-F]{6}") ? s : fallback;
    }
    private String[] visible() {
        JSONArray a = configuration.optJSONArray("pipVisible");
        if (a == null) return new String[] {"speed", "status", "cup"};
        String[] result = new String[a.length()];
        for (int i=0; i<a.length(); i++) result[i] = a.optString(i, "");
        return result;
    }
    @Override public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            gotLinear = true;
            for (int i=0; i<3; i++) linear[i] = event.values[i] / GRAVITY;
            return;
        }
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
        long now = SystemClock.elapsedRealtime();
        for (int i=0; i<3; i++) ag[i] = event.values[i] / GRAVITY;
        lastSensor = now;
        if (!calibrated) {
            calibrationWindow.addLast(ag.clone());
            while (calibrationWindow.size() > 60) calibrationWindow.removeFirst();
            double delay = Math.max(0, setting("startDelay", 1.5)) * 1000;
            if (now-calibrationStart >= delay && calibrationWindow.size() >= 6) {
                double[] avg = new double[3];
                for (double[] xyz : calibrationWindow) for (int i=0; i<3; i++) avg[i]+=xyz[i];
                for (int i=0; i<3; i++) avg[i]/=calibrationWindow.size();
                double variance=0;
                for (double[] xyz : calibrationWindow) for (int i=0; i<3; i++) variance+=Math.pow(xyz[i]-avg[i],2);
                double sd = Math.sqrt(variance/calibrationWindow.size());
                if (sd <= 0.07 && Math.sqrt(avg[0]*avg[0]+avg[1]*avg[1]+avg[2]*avg[2])>=0.55) {
                    base=avg; calibrated=true; previousSample=now;
                }
            }
            sendSample(now);
            publish();
            return;
        }
        double dt = clamp((now-previousSample)/1000.0, 0.012, 0.15);
        previousSample = now;
        double dx=ag[0]-base[0],dy=ag[1]-base[1],dz=ag[2]-base[2];
        double normYZ=Math.hypot(base[1],base[2]);
        double norm3=Math.sqrt(base[0]*base[0]+base[1]*base[1]+base[2]*base[2]);
        if (norm3<0.001) norm3=1;
        double lon=normYZ>0.25?(dy*base[2]-dz*base[1])/normYZ:dy;
        double vert=(dx*base[0]+dy*base[1]+dz*base[2])/norm3;
        vertBaseline+=(vert-vertBaseline)*factor(dt,0.55);
        double dynVert=Math.abs(vert-vertBaseline), dynX=dx, dynLon=lon;
        if (gotLinear) {
            double linLon=normYZ>0.25?(linear[1]*base[2]-linear[2]*base[1])/normYZ:linear[1];
            double linVert=Math.abs((linear[0]*base[0]+linear[1]*base[1]+linear[2]*base[2])/norm3);
            if (Math.sqrt(linear[0]*linear[0]+linear[1]*linear[1]+linear[2]*linear[2])<0.12)
                slopeBaselineLon+=((lon-linLon)-slopeBaselineLon)*factor(dt,5);
            dynX=0.25*dx+0.75*linear[0];
            dynLon=0.25*(lon-slopeBaselineLon)+0.75*linLon;
            dynVert=0.35*dynVert+0.65*linVert;
        }
        if (dynVert<0.015) dynVert=0;
        smX+=(dynX-smX)*factor(dt,0.20);
        smLon+=(dynLon-smLon)*factor(dt,0.25);
        smVert+=(dynVert-smVert)*factor(dt,0.085);
        double maxGa=Math.max(0.05,setting("maxGa",0.20));
        double maxPhanh=Math.max(0.05,setting("maxPhanh",0.224));
        double maxCua=Math.max(0.05,setting("maxCua",0.20));
        double maxXoc=Math.max(0.08,setting("potholeThreshold",0.24));
        ga=(int)Math.round(clamp(Math.max(0,smLon)/maxGa*100,0,150));
        phanh=(int)Math.round(clamp(Math.max(0,-smLon)/maxPhanh*100,0,150));
        cua=(int)Math.round(clamp(Math.abs(smX)/maxCua*100,0,150));
        xoc=(int)Math.round(clamp(smVert/maxXoc*100,0,150));
        double jerk=Math.abs(smX-prevX)/dt; prevX=smX;
        int sign=Math.abs(smX)>=maxCua*0.52?(smX>=0?1:-1):0;
        if (sign!=0 && lastSign!=0 && sign!=lastSign && now-lastReversal>250 &&
                jerk>setting("maxJerk",0.24)*0.25) { lastReversal=now; reversals.addLast(now); }
        if (sign!=0) lastSign=sign;
        while (!reversals.isEmpty() && now-reversals.peekFirst()>3200) reversals.removeFirst();
        // Trạng thái ngắn cho PiP/Overlay; giữ vài giây để không nhấp nháy liên tục.
        // Ưu tiên mức vượt ngưỡng lớn nhất, nếu bằng nhau ưu tiên PHANH.
        int highest=Math.max(Math.max(ga,phanh),Math.max(cua,xoc));
        boolean weaving=reversals.size()>=2;
        int severity=Math.max(highest,weaving?110:0);
        String eventName="ÊM ÁI";
        if(weaving && highest<100)eventName="LẮC NGANG LIÊN TỤC";
        if(highest>=100){
            if(phanh==highest)eventName="PHANH GẤP";
            else if(ga==highest)eventName="TĂNG GA MẠNH";
            else if(cua==highest)eventName="VÀO CUA MẠNH";
            else eventName="ĐƯỜNG XÓC";
        }
        if(severity>=100){
            // Nếu đã có cảnh báo mạnh hơn đang giữ, không thay bằng cảnh báo nhẹ hơn.
            if(now>=statusHoldUntil || severity>=lastStatusLevel){
                drivingStatus=eventName;
                lastStatusLevel=severity;
            }
            statusHoldUntil=now+(long)(Math.max(0,setting("alertHoldTime",3))*1000);
        }else if(now>=statusHoldUntil){ drivingStatus="ÊM ÁI";lastStatusLevel=0; }
        // One audio engine in the APK, whether foreground, PiP, Overlay or audio-only.
        // Browser Web Audio is disabled in native mode to prevent two different alerts.
        if(configuration.optBoolean("soundEnabled",true) && severity>=100 && now-lastAlert>5000) {
            lastAlert=now;
            playNativeAlert(xoc>=100 && xoc>=Math.max(ga,Math.max(phanh,cua)));
        }
        sendSample(now);
        publish();
    }
    private void playNativeAlert(boolean pothole){
        String type=configuration.optString(pothole?"potholeWave":"waveType","sine");
        alarm.play((int)clamp(setting("volumePercent",70),0,100),type);
    }
    private long gpsSequence;
    private void sendSample(long now) {
        // Web UI is updated only when visible; native service keeps running regardless.
        if (now-emitAt<90) return;
        emitAt=now;
        JSONObject sample=new JSONObject();
        try {
            JSONObject gravity = new JSONObject();
            gravity.put("x",ag[0]*GRAVITY); gravity.put("y",ag[1]*GRAVITY); gravity.put("z",ag[2]*GRAVITY);
            sample.put("ag",gravity);
            if (gotLinear) {
                JSONObject lin = new JSONObject();
                lin.put("x",linear[0]*GRAVITY); lin.put("y",linear[1]*GRAVITY); lin.put("z",linear[2]*GRAVITY);
                sample.put("lin",lin);
            }
            // Permission and GPS speed come from native LocationManager, not WebView.
            sample.put("gpsSeq",gpsSequence);
            if(lastGps>0){
                sample.put("gpsAgeMs",Math.max(0,now-lastGps));
                sample.put("gpsSpeed",Float.isFinite(speed)?speed:JSONObject.NULL);
                sample.put("gpsAccuracy",Float.isFinite(accuracy)?accuracy:JSONObject.NULL);
            }
            lastSample=sample.toString();
            lastSampleSeq++;
        } catch (Exception ignored) {}
    }
    private void publish() {
        long now=SystemClock.elapsedRealtime();
        String ratio=configuration.optString("pipRatio","16:9");
        if (!ratio.equals("16:9") && !ratio.equals("4:3") && !ratio.equals("1:1")) ratio="16:9";
        int speedFont=pipTextSize("pipSpeedFontSize",44);
        int gaFont=pipTextSize("pipGaFontSize",22);
        int phanhFont=pipTextSize("pipPhanhFontSize",22);
        int cuaFont=pipTextSize("pipCuaFontSize",22);
        int xocFont=pipTextSize("pipXocFontSize",22);
        int gpsFont=pipTextSize("pipGpsFontSize",19);
        int statusFont=pipTextSize("pipStatusFontSize",25);
        int cupSize=pipTextSize("pipCupSize",130);
        String status=!calibrated?"ĐANG CÂN CHỈNH":(now-lastSensor>4000?"MẤT CẢM BIẾN":drivingStatus);
        int weight=(int)clamp(setting("pipFontWeight",700),500,800);
        lastSnapshot=new Snapshot(true,calibrated,ga,phanh,cua,xoc,speed,accuracy,hz,
          lastGps==0?Long.MAX_VALUE:now-lastGps, lastSensor==0?Long.MAX_VALUE:now-lastSensor,
          ratio,color("pipBgColor","#0d1a2d"),color("pipTextColor","#ffffff"),
          speedFont,gaFont,phanhFont,cuaFont,xocFont,gpsFont,weight,statusFont,cupSize,
          (float)smX,(float)smLon,(float)Math.max(0.05,Math.max(setting("maxGa",0.2),Math.max(setting("maxPhanh",0.224),setting("maxCua",0.2)))),status,visible());
    }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    @Override public void onLocationChanged(Location loc) {
        long now=SystemClock.elapsedRealtime();
        if (lastGps>0) { long dt=now-lastGps; if(dt>0) hz=(float)(1000.0/dt); }
        lastGps=now;
        gpsSequence++;
        accuracy=loc.hasAccuracy()?loc.getAccuracy():Float.NaN;
        if (loc.hasSpeed() && loc.getSpeed()>=0) speed=loc.getSpeed()*3.6f;
        else speed=Float.NaN; // Never retain stale speed or infer speed from one location.
        publish();
    }
    @Override public void onProviderDisabled(String provider) { speed=Float.NaN; publish(); }
    @Override public void onTaskRemoved(Intent rootIntent) { shutdown(); super.onTaskRemoved(rootIntent); }
    private void releaseResources() {
        ready=false;
        releaseAudioWakeLock();
        if(overlay!=null)overlay.hide();
        alarm.release();
        if(sensors!=null){ sensors.unregisterListener(this); sensors=null; }
        if(locations!=null){
            try{ locations.removeUpdates(this); }catch(SecurityException ignored){}
            locations=null;
        }
        instance=null;
        lastSnapshot=new Snapshot();
        lastSample="";
        lastSampleSeq=0;
    }
    private void shutdown() {
        releaseResources();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }
    @Override public void onDestroy() {
        releaseResources();
        super.onDestroy();
    }
}
