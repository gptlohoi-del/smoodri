package org.smoodri.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import java.util.ArrayList;
import java.util.Locale;

/** True Android PiP content; uses the last native foreground-service snapshot. */
public final class NativePipView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int selectedWeight = 700;
    public NativePipView(Context context) { super(context); setLayerType(View.LAYER_TYPE_SOFTWARE,null); }
    private int parseColor(String value, int fallback) { try { return Color.parseColor(value); } catch (Exception e) { return fallback; } }
    private void text(Canvas c,String s,float x,float y,float sz,int color,boolean bold){
        paint.setColor(color); paint.setTextSize(sz); paint.setTypeface(!bold?android.graphics.Typeface.DEFAULT:
            (android.os.Build.VERSION.SDK_INT>=28 ? android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT,selectedWeight,false)
                : android.graphics.Typeface.DEFAULT_BOLD));
        paint.setStyle(Paint.Style.FILL); c.drawText(s,x,y,paint);
    }
    private boolean visible(DriveMonitorService.Snapshot s,String field) {
        for(String v:s.visible) if (field.equals(v)) return true;
        return false;
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        DriveMonitorService.Snapshot s=DriveMonitorService.snapshot();
        int bg=parseColor(s.bg,Color.rgb(13,26,45)), fg=parseColor(s.fg,Color.WHITE);
        selectedWeight=s.fontWeight;
        canvas.drawColor(bg);
        float w=getWidth(),h=getHeight();
        if(w<10 || h<10)return;
        float sw=480, sh=s.pipRatio.equals("1:1")?480:(s.pipRatio.equals("4:3")?360:270);
        float scale=Math.min(w/sw,h/sh);
        float offX=(w-sw*scale)/2,offY=(h-sh*scale)/2;
        canvas.save();canvas.translate(offX,offY);canvas.scale(scale,scale);
        paint.setColor(0xFF27374D);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);
        canvas.drawRoundRect(new RectF(8,8,sw-8,sh-8),20,20,paint);paint.setStyle(Paint.Style.FILL);
        text(canvas,"SMOODRI",22,40,22,fg,true);
        text(canvas,s.active?(s.calibrated?"ĐANG ĐO":"ĐANG CÂN CHỈNH"):"ĐÃ DỪNG",sw-210,40,14,0xFF81D6BB,true);
        float y=70;
        int big=(int)Math.max(20, Math.min(46,s.fontSize*1.25));
        if(visible(s,"score")) { text(canvas,"ÊM ÁI",24,y,18,0xFFAFBED0,true); text(canvas,String.valueOf(s.score),180,y+4,big,0xFF7EE2B1,true); y+=52; }
        if(visible(s,"speed")) {
            boolean valid=s.gpsAgeMs<3500 && Float.isFinite(s.speed);
            text(canvas,"TỐC ĐỘ",24,y,18,0xFFAFBED0,true);
            text(canvas,valid?String.format(Locale.US,"%.0f km/h",s.speed):"-- km/h",180,y+4,big,fg,true);
            y+=50;
        }
        ArrayList<String> metrics=new ArrayList<>();
        if(visible(s,"ga"))metrics.add("GA "+s.ga+"%");
        if(visible(s,"phanh"))metrics.add("PHANH "+s.phanh+"%");
        if(visible(s,"cua"))metrics.add("CUA "+s.cua+"%");
        if(visible(s,"xoc"))metrics.add("XÓC "+s.xoc+"%");
        float metricsY=y+3;
        float colWidth=(sw-44)/2;
        for(int i=0;i<metrics.size();i++){
            float mx=24+(i%2)*colWidth;
            float my=metricsY+(i/2)*29;
            if(my+14>sh-15)break;
            text(canvas,metrics.get(i),mx,my,19,fg,true);
        }
        float gpsY=sh-26;
        if(visible(s,"gps") && gpsY>metricsY+40) {
            String gps=s.gpsAgeMs<3500 && Float.isFinite(s.accuracy)?
                String.format(Locale.US,"GPS ±%.0fm · %s",s.accuracy,Float.isFinite(s.hz)?String.format(Locale.US,"%.1fHz",s.hz):"--Hz"):
                "GPS MẤT";
            text(canvas,gps,24,gpsY,17,0xFFB0C4D8,false);
        }
        if(s.sensorAgeMs>4000 && s.active) text(canvas,"MẤT CẢM BIẾN",sw-190,sh-16,14,0xFFFF9D9D,true);
        canvas.restore();
    }
}
