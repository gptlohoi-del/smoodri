package org.smoodri.app;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

/** One short, four-beep alert shared by PiP, Overlay and audio-only Android modes.
 *  Unlike ToneGenerator.TONE_PROP_BEEP (a fixed 35-ms dual tone), these samples
 *  use a fade-in/out envelope and the same timing in every native mode.
 */
final class NativeAlertPlayer {
    private static final String TAG = "SmooDriAudio";
    private static final int SAMPLE_RATE = 44100;
    private static final int BEEP_MS = 90;
    private static final int GAP_MS = 105;
    private static final int BEEP_COUNT = 4;
    private static final int TAIL_MS = 45;
    private AudioTrack track;
    private String loadedStyle = "";
    private String lastError = "";

    String lastError(){ return lastError; }

    private static short[] makePcm(String style) {
        int beepSamples = SAMPLE_RATE * BEEP_MS / 1000;
        int gapSamples = SAMPLE_RATE * GAP_MS / 1000;
        int total = BEEP_COUNT * beepSamples + (BEEP_COUNT - 1) * gapSamples +
                    SAMPLE_RATE * TAIL_MS / 1000;
        short[] pcm = new short[total];
        int fadeSamples = Math.max(1, SAMPLE_RATE * 8 / 1000);
        for(int burst=0; burst<BEEP_COUNT; burst++) {
            int offset = burst * (beepSamples + gapSamples);
            for(int i=0; i<beepSamples; i++) {
                double phase = 2.0 * Math.PI * 880.0 * i / SAMPLE_RATE;
                double fundamental = Math.sin(phase);
                double wave;
                // Softened harmonics preserve wave choices without harsh clicks.
                if("triangle".equals(style)) wave = (fundamental - Math.sin(3*phase)/9.0) / (1.0+1.0/9.0);
                else if("square".equals(style)) wave = (fundamental + Math.sin(3*phase)/6.0) / (1.0+1.0/6.0);
                else if("sawtooth".equals(style)) wave = (fundamental + Math.sin(2*phase)/5.0) / 1.2;
                else wave = fundamental; // sine/default
                double fade = Math.min(1.0, Math.min(i, beepSamples - 1 - i) / (double)fadeSamples);
                double sample = wave * Math.max(0.0,fade) * 0.68;
                pcm[offset + i] = (short)Math.round(Math.max(-1,Math.min(1,sample)) * Short.MAX_VALUE);
            }
        }
        return pcm;
    }

    private void prepare(String style) {
        release();
        short[] pcm = makePcm(style);
        AudioTrack candidate = new AudioTrack.Builder()
            .setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.length * 2)
            .build();
        if(candidate.getState()!=AudioTrack.STATE_INITIALIZED){
            candidate.release();
            throw new IllegalStateException("Không thể khởi tạo thiết bị phát âm thanh");
        }
        int written=candidate.write(pcm,0,pcm.length,AudioTrack.WRITE_BLOCKING);
        if(written!=pcm.length){
            candidate.release();
            throw new IllegalStateException("Không nạp đủ mẫu âm thanh: "+written+"/"+pcm.length);
        }
        track=candidate;
        loadedStyle=style;
    }

    boolean play(int volumePercent, String requestedStyle) {
        try {
            String style = requestedStyle==null?"sine":requestedStyle;
            if(!style.equals("square") && !style.equals("triangle") &&
               !style.equals("sawtooth") && !style.equals("sine"))style="sine";
            if(track==null || !style.equals(loadedStyle)) prepare(style);
            if(track.getPlayState()==AudioTrack.PLAYSTATE_PLAYING)track.stop();
            track.reloadStaticData();
            track.setVolume(Math.max(0f,Math.min(1f,volumePercent/100f)));
            track.play();
            lastError="";
            return true;
        }catch(RuntimeException error){
            lastError=error.getClass().getSimpleName()+": "+error.getMessage();
            Log.e(TAG,"Không phát được âm cảnh báo",error);
            release();
            return false;
        }
    }

    void release(){
        if(track!=null){
            try{track.stop();}catch(IllegalStateException ignored){}
            track.release();
            track=null;
        }
        loadedStyle="";
    }
}
