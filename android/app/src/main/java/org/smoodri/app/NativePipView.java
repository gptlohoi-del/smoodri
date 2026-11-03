package org.smoodri.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;
import java.util.Locale;

/** Android PiP content, rendered from the native foreground-service snapshot.
 * Each metric has an independent logical font size (480-unit design canvas).
 * Android controls the PiP window dimensions; font sizes only change drawing.
 */
public final class NativePipView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int selectedWeight = 700;
    private boolean overlayMode = false;

    /** In Overlay the logical canvas follows the user-selected window aspect. */
    public void setOverlayMode(boolean enabled) { overlayMode = enabled; invalidate(); }

    public NativePipView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    private int parseColor(String value, int fallback) {
        try { return Color.parseColor(value); }
        catch (Exception ignored) { return fallback; }
    }

    private void configureText(float size, int color, boolean bold) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(size);
        if (!bold) paint.setTypeface(Typeface.DEFAULT);
        else if (android.os.Build.VERSION.SDK_INT >= 28)
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, selectedWeight, false));
        else paint.setTypeface(Typeface.DEFAULT_BOLD);
    }

    private void drawText(Canvas c, String value, float x, float y, float size, int color, boolean bold) {
        configureText(size, color, bold);
        c.drawText(value, x, y, paint);
    }

    /** Constrain long values to their column, without affecting other metrics' font settings. */
    private void drawFitText(Canvas c, String value, float x, float baseline,
                             float fontSize, float maxWidth, int color, boolean bold) {
        configureText(fontSize, color, bold);
        float measured = paint.measureText(value);
        if (measured > maxWidth && measured > 0) {
            // Only narrow text that cannot fit. Normal values retain the selected size.
            paint.setTextSize(Math.max(10f, fontSize * (maxWidth / measured)));
        }
        c.drawText(value, x, baseline, paint);
    }

    private boolean visible(DriveMonitorService.Snapshot snapshot, String field) {
        for (String value : snapshot.visible) if (field.equals(value)) return true;
        return false;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        DriveMonitorService.Snapshot s = DriveMonitorService.snapshot();
        int background = parseColor(s.bg, Color.rgb(13, 26, 45));
        int foreground = parseColor(s.fg, Color.WHITE);
        selectedWeight = s.fontWeight;
        canvas.drawColor(background);
        float width = getWidth(), height = getHeight();
        if (width < 10 || height < 10) return;

        // Fixed logical canvas keeps placement consistent across Android PiP sizes.
        float logicalWidth = 480f;
        float logicalHeight = overlayMode
                ? Math.max(270f, Math.min(640f, 480f * height / width))
                : ("1:1".equals(s.pipRatio) ? 480f : ("4:3".equals(s.pipRatio) ? 360f : 270f));
        float scale = Math.min(width / logicalWidth, height / logicalHeight);
        canvas.save();
        canvas.translate((width - logicalWidth * scale) / 2f, (height - logicalHeight * scale) / 2f);
        canvas.scale(scale, scale);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(0xFF27374D);
        canvas.drawRoundRect(new RectF(8, 8, logicalWidth - 8, logicalHeight - 8), 20, 20, paint);
        paint.setStyle(Paint.Style.FILL);
        drawText(canvas, "SMOODRI", 22, 39, 22, foreground, true);
        drawText(canvas, s.active ? (s.calibrated ? "ĐANG ĐO" : "ĐANG CÂN CHỈNH") : "ĐÃ DỪNG",
                logicalWidth - 210, 39, 14, 0xFF81D6BB, true);

        // The old cumulative trip score has been removed. Speed is now the primary figure.
        float firstMetricY;
        if (visible(s, "speed")) {
            drawText(canvas, "TỐC ĐỘ", 24, 100, 18, 0xFFAFBED0, true);
            boolean valid = s.gpsAgeMs < 3500 && Float.isFinite(s.speed);
            String speedText = valid ? String.format(Locale.US, "%.0f km/h", s.speed) : "-- km/h";
            drawFitText(canvas, speedText, 151, 105, s.speedFontSize, logicalWidth - 173,
                    foreground, true);
            firstMetricY = 151;
        } else {
            firstMetricY = 100;
        }

        String[] keys = {"ga", "phanh", "cua", "xoc"};
        String[] captions = {"GA", "PHANH", "CUA", "Ổ GÀ"};
        int[] values = {s.ga, s.phanh, s.cua, s.xoc};
        int[] sizes = {s.gaFontSize, s.phanhFontSize, s.cuaFontSize, s.xocFontSize};
        int position = 0;
        final float colWidth = (logicalWidth - 44) / 2f;
        for (int i = 0; i < keys.length; i++) {
            if (!visible(s, keys[i])) continue;
            float x = 24 + (position % 2) * colWidth;
            float y = firstMetricY + (position / 2) * 49;
            // The reserved GPS row prevents a large label from being clipped.
            float bottomLimit = visible(s, "gps") ? logicalHeight - 54 : logicalHeight - 18;
            if (y > bottomLimit) break;
            drawFitText(canvas, captions[i] + " " + values[i] + "%", x, y,
                    sizes[i], colWidth - 13, foreground, true);
            position++;
        }

        if (visible(s, "gps")) {
            String gps;
            if (s.gpsAgeMs < 3500 && Float.isFinite(s.accuracy)) {
                String hz = Float.isFinite(s.hz) ? String.format(Locale.US, "%.1fHz", s.hz) : "--Hz";
                gps = String.format(Locale.US, "GPS ±%.0fm · %s", s.accuracy, hz);
            } else gps = "GPS MẤT";
            drawFitText(canvas, gps, 24, logicalHeight - 23, s.gpsFontSize,
                    logicalWidth - 50, 0xFFB0C4D8, false);
        }
        if (s.sensorAgeMs > 4000 && s.active) {
            drawText(canvas, "MẤT CẢM BIẾN", logicalWidth - 190,
                    logicalHeight - 9, 12, 0xFFFF9D9D, true);
        }
        canvas.restore();
    }
}
