package org.smoodri.app;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

/** Equivalent synthesis to Web Audio tone()/burst()/potholeBurst() in app.js.
 * Foreground WebView plays Web Audio. Android plays this PCM only while the UI is hidden.
 */
final class NativeAlertPlayer {
    private static final String TAG="SmooDriAudio";
    private static final int RATE=44100;
    private AudioTrack track;
    private String loadedKey="",lastError="";
    String lastError(){return lastError;}

    private static double oscillator(String style,double phase) {
        double theta=phase%(Math.PI*2);
        if("square".equals(style))return Math.sin(theta)>=0?1:-1;
        if("triangle".equals(style))return (2.0/Math.PI)*Math.asin(Math.sin(theta));
        if("sawtooth".equals(style))return 2*(theta/(Math.PI*2))-1;
        return Math.sin(theta);
    }
    private static short[] render(String kind,String style){
        boolean pothole="pothole".equals(kind);
        int count=pothole?2:4;
        int beepMs=pothole?95:60;
        int gapStart=pothole?135:105; // JS schedules at 0,105,210,315 or 0,135 ms
        int totalMs=gapStart*(count-1)+beepMs+35;
        short[] pcm=new short[(int)((long)RATE*totalMs/1000)];
        double factor=0.6; // Web Audio: amplitude = volume * 0.6
        for(int note=0;note<count;note++){
            final double freq=pothole?(note==0?850:600):("weave".equals(kind)?2000:1550+(note%2)*350);
            int start=(int)((long)RATE*gapStart*note/1000);
            int samples=(int)((long)RATE*beepMs/1000);
            for(int n=0;n<samples;n++){
                double t=n/(double)RATE;
                // Identical Web Audio ADSR: 8ms linear attack, then exponential decay to .0001
                double gain=t<.008?Math.max(.0001, factor*t/.008)
                        :factor*Math.pow(.0001/factor,(t-.008)/Math.max(.001,beepMs/1000.0-.008));
                double sample=oscillator(style,2*Math.PI*freq*t)*gain;
                pcm[start+n]=(short)Math.round(Math.max(-1,Math.min(1,sample))*32767.0);
            }
        }
        return pcm;
    }
    private void prepare(String kind,String style){
        release();
        short[] pcm=render(kind,style);
        AudioTrack result=new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(pcm.length*2).build();
        if(result.getState()!=AudioTrack.STATE_INITIALIZED){result.release();throw new IllegalStateException("AudioTrack chưa khởi tạo");}
        int written=result.write(pcm,0,pcm.length,AudioTrack.WRITE_BLOCKING);
        if(written!=pcm.length){result.release();throw new IllegalStateException("Thiếu dữ liệu PCM");}
        track=result; loadedKey=kind+"/"+style;
    }
    synchronized boolean play(String kind,String style,int volume){
        try{
            if(!"weave".equals(kind)&&!"pothole".equals(kind))kind="normal";
            if(!"square".equals(style)&&!"sine".equals(style)&&!"triangle".equals(style)&&!"sawtooth".equals(style))style="square";
            String key=kind+"/"+style;
            if(track==null||!loadedKey.equals(key))prepare(kind,style);
            if(track.getPlayState()==AudioTrack.PLAYSTATE_PLAYING)track.stop();
            track.reloadStaticData();
            track.setVolume(Math.max(0,Math.min(100,volume))/100f);
            track.play();
            lastError="";
            return true;
        }catch(Exception error){
            lastError=error.getClass().getSimpleName()+": "+error.getMessage();
            Log.e(TAG,"Không phát được cảnh báo âm thanh",error);
            release(); return false;
        }
    }
    synchronized void release(){
        if(track!=null){
            try{track.stop();}catch(IllegalStateException ignored){}
            track.release();track=null;
        }
        loadedKey="";
    }
}
