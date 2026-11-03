package org.smoodri.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;
import java.util.Locale;

/**
 * Shared renderer for standard Android PiP and draggable Overlay.
 * Default compact view: SPEED + STATUS + live G-force cup.
 * Technical values can be enabled individually via web SETTINGS.
 */
public final class NativePipView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int selectedWeight = 700;
    private boolean overlayMode = false;

    public NativePipView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }
    public void setOverlayMode(boolean enabled) { overlayMode=enabled; invalidate(); }

    private int parseColor(String str,int fallback){
        try { return Color.parseColor(str); } catch(Exception e){ return fallback; }
    }
    private void textStyle(float size,int color,boolean bold){
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(Math.max(8f,size));
        if(!bold)paint.setTypeface(Typeface.DEFAULT);
        else if(android.os.Build.VERSION.SDK_INT>=28)
            paint.setTypeface(Typeface.create(Typeface.DEFAULT,selectedWeight,false));
        else paint.setTypeface(Typeface.DEFAULT_BOLD);
    }
    private void fitText(Canvas canvas,String value,float x,float y,float requested,float maxWidth,
                         float maxHeight,int color,boolean bold){
        float font=Math.min(Math.max(8f,requested),Math.max(9f,maxHeight));
        textStyle(font,color,bold);
        float measured=paint.measureText(value);
        if(measured>maxWidth && measured>0)paint.setTextSize(Math.max(8f,font*maxWidth/measured));
        canvas.drawText(value,x,y,paint);
    }
    private void rect(Canvas canvas,float l,float t,float r,float b,int color,float rad){
        paint.setColor(color);paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(l,t,r,b),rad,rad,paint);
    }
    private void circle(Canvas canvas,float x,float y,float radius,int color,boolean filled,float thickness){
        paint.setColor(color);
        paint.setStyle(filled?Paint.Style.FILL:Paint.Style.STROKE);
        paint.setStrokeWidth(thickness);
        canvas.drawCircle(x,y,radius,paint);
        paint.setStyle(Paint.Style.FILL);
    }
    private boolean visible(DriveMonitorService.Snapshot s,String key){
        for(String field:s.visible)if(key.equals(field))return true;
        return false;
    }
    private int statusColor(String label){
        if(label.contains("PHANH") || label.contains("MẠNH") || label.contains("XÓC"))return 0xFFFEB86B;
        if(label.contains("MẤT") || label.contains("CÂN CHỈNH"))return 0xFFFFCB78;
        return 0xFF70E3AC;
    }
    private void drawCup(Canvas c,DriveMonitorService.Snapshot s,float x,float y,float width,float height,int fg){
        // s.cupSize is the user-selected native logical diameter, constrained by available layout.
        float diameter=Math.min(Math.max(34,s.cupSize),Math.min(width-8,height-8));
        if(diameter<32)return;
        float radius=diameter/2f;
        float cx=x+width/2f,cy=y+height/2f;
        circle(c,cx,cy,radius,0xFF6D96AC,false,2.5f);
        circle(c,cx,cy,radius*.63f,0xFF36566B,false,1.5f);
        paint.setColor(0xFF48677A);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.3f);
        c.drawLine(cx-radius,cy,cx+radius,cy,paint);
        c.drawLine(cx,cy-radius,cx,cy+radius,paint);
        paint.setStyle(Paint.Style.FILL);
        float boundary=Math.max(.02f,s.cupLimit);
        float gx=Math.max(-1f,Math.min(1f,s.lateralG/boundary));
        float gy=Math.max(-1f,Math.min(1f,s.longitudinalG/boundary));
        // Clamp the ball vector to the circle rather than permitting it to leave the G cup.
        float norm=(float)Math.hypot(gx,gy);
        if(norm>1){gx/=norm;gy/=norm;}
        int color=(s.ga>=100 || s.phanh>=100 || s.cua>=100 || s.xoc>=100)
                    ?0xFFFFB25A:0xFF48E8BE;
        circle(c,cx+gx*radius*.83f,cy-gy*radius*.83f,
               Math.max(5.5f,diameter*.055f),color,true,0);
        String reading=String.format(Locale.US,"%.2f G",Math.hypot(s.lateralG,s.longitudinalG));
        textStyle(Math.min(14f,Math.max(10f,diameter*.11f)),fg,true);
        float measure=paint.measureText(reading);
        c.drawText(reading,cx-measure/2f,cy+radius*.68f,paint);
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        DriveMonitorService.Snapshot s=DriveMonitorService.snapshot();
        int bg=parseColor(s.bg,0xFF0D1A2D),fg=parseColor(s.fg,Color.WHITE);
        selectedWeight=s.fontWeight;
        canvas.drawColor(bg);
        float width=getWidth(),height=getHeight();
        if(width<10||height<10)return;
        final float w=480f;
        final float h=overlayMode?Math.max(270f,Math.min(670f,480f*height/width)):
            ("1:1".equals(s.pipRatio)?480f:("4:3".equals(s.pipRatio)?360f:270f));
        float factor=Math.min(width/w,height/h);
        canvas.save();
        canvas.translate((width-w*factor)/2f,(height-h*factor)/2f);
        canvas.scale(factor,factor);
        paint.setStyle(Paint.Style.STROKE);paint.setColor(0xFF31475F);paint.setStrokeWidth(2f);
        canvas.drawRoundRect(new RectF(7,7,w-7,h-7),18,18,paint);
        paint.setStyle(Paint.Style.FILL);
        fitText(canvas,"SMOODRI",19,34,20,170,22,fg,true);
        fitText(canvas,s.active?(s.calibrated?"ĐANG ĐO":"ĐANG CÂN CHỈNH"):"ĐÃ DỪNG",
                w-184,33,13,166,20,0xFF93BAAC,true);

        float top=48;
        if(visible(s,"status")){
            String label=s.drivingStatus;
            int foreground=statusColor(label);
            rect(canvas,18,top,w-18,top+47,0xFF18384A,10f);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.7f);paint.setColor(foreground);
            canvas.drawRoundRect(new RectF(18,top,w-18,top+47),10f,10f,paint);
            paint.setStyle(Paint.Style.FILL);
            fitText(canvas,label,36,top+32,s.statusFontSize,w-70,34,foreground,true);
            top+=57;
        }
        int metricCount=0;
        String[] keys={"ga","phanh","cua","xoc"};
        for(String key:keys)if(visible(s,key))metricCount++;
        float technicalRows=((metricCount+1)/2)*39f;
        float footer=visible(s,"gps")?35f:12f;
        float mainHeight=Math.max(0,h-top-technicalRows-footer-8f);
        boolean showSpeed=visible(s,"speed"),showCup=visible(s,"cup");
        if(mainHeight>20 && (showSpeed||showCup)){
            if(showSpeed){
                float span=showCup?235f:w-35f;
                fitText(canvas,"TỐC ĐỘ",24,top+18,16,span-22,19,0xFFAFC6D6,true);
                String speed=s.gpsAgeMs<3500 && Float.isFinite(s.speed)
                    ?String.format(Locale.US,"%.0f km/h",s.speed):"-- km/h";
                fitText(canvas,speed,24,top+mainHeight*.66f,s.speedFontSize,span-24,
                        Math.max(18f,mainHeight-20f),fg,true);
            }
            if(showCup){
                float left=showSpeed?248f:30f;
                float boxW=showSpeed?w-left-18f:w-60f;
                drawCup(canvas,s,left,top,boxW,mainHeight,fg);
            }
        }
        float rowStart=h-footer-technicalRows+15;
        int pos=0;
        String[] labels={"GA","PHANH","CUA","Ổ GÀ"};
        int[] values={s.ga,s.phanh,s.cua,s.xoc};
        int[] fonts={s.gaFontSize,s.phanhFontSize,s.cuaFontSize,s.xocFontSize};
        for(int i=0;i<keys.length;i++){
            if(!visible(s,keys[i]))continue;
            float x=20+(pos%2)*230f,y=rowStart+(pos/2)*39f;
            fitText(canvas,labels[i]+" "+values[i]+"%",x,y,fonts[i],212f,31f,fg,true);
            pos++;
        }
        if(visible(s,"gps")){
            String gps=s.gpsAgeMs<3500&&Float.isFinite(s.accuracy)
                ?String.format(Locale.US,"GPS ±%.0fm · %s",s.accuracy,
                    Float.isFinite(s.hz)?String.format(Locale.US,"%.1fHz",s.hz):"--Hz")
                :"GPS MẤT";
            fitText(canvas,gps,22,h-15,s.gpsFontSize,w-44,26f,0xFFADC7D8,false);
        }
        canvas.restore();
    }
}
