package com.goydevv.studrblx.anim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

import com.goydevv.studrblx.ui.Ui;

import java.util.Map;
import java.util.TreeSet;

/** Custom-drawn dopesheet: ruler, playhead, a summary row of all keys and a row for the selected joint. */
public final class TimelineView extends View {

    public interface Listener {
        void onScrub(int tick);

        void onKeyMoved(int fromTick, int toTick, boolean summaryRow);

        void onKeySelected(int tick);

        void onBeginEdit();
    }

    private final AnimSession session;
    private String joint;
    private Listener listener;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path diamond = new Path();
    private final float d;

    /** Pixels per second and left-most visible second. */
    private float pxPerSec;
    private float originSec = -0.1f;

    private int dragKeyTick = -1;
    private boolean dragSummary;
    private boolean scrubbing;
    private boolean panning;
    private float lastX;
    private float downX, downY;
    private boolean moved;
    private boolean dragStarted;
    private int selectedKey = -1;

    public TimelineView(Context ctx, AnimSession s) {
        super(ctx);
        this.session = s;
        d = ctx.getResources().getDisplayMetrics().density;
        pxPerSec = 160 * d;
        text.setTextSize(9 * d);
        text.setColor(Ui.INSTANCE.getDIM());
    }

    public void setListener(Listener l) {
        listener = l;
    }

    public void setJoint(String j) {
        joint = j;
        invalidate();
    }

    public void setSelectedKey(int tick) {
        selectedKey = tick;
        invalidate();
    }

    public void zoom(float factor) {
        float centerSec = originSec + (getWidth() / 2f) / pxPerSec;
        pxPerSec = Math.max(30 * d, Math.min(900 * d, pxPerSec * factor));
        originSec = centerSec - (getWidth() / 2f) / pxPerSec;
        invalidate();
    }

    /** Keep the playhead on screen during playback. */
    public void follow() {
        float t = session.seconds(session.curTick);
        float visible = getWidth() / pxPerSec;
        if (t < originSec || t > originSec + visible - 0.05f) {
            originSec = t - visible * 0.1f;
        }
        invalidate();
    }

    private float xOf(int tick) {
        return (session.seconds(tick) - originSec) * pxPerSec;
    }

    private int tickAt(float x) {
        return Math.max(0, Math.round((x / pxPerSec + originSec) * AnimSession.TICKS));
    }

    private float rulerH() {
        return 16 * d;
    }

    private float rowH() {
        return (getHeight() - rulerH()) / 2f;
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth();
        int h = getHeight();
        float rh = rulerH();
        float row = rowH();
        p.setStyle(Paint.Style.FILL);
        p.setColor(Ui.INSTANCE.getBG());
        c.drawRect(0, 0, w, h, p);
        p.setColor(Ui.INSTANCE.getHEADER());
        c.drawRect(0, 0, w, rh, p);

        // Out-of-range shading after the animation length.
        float endX = xOf(session.lengthTicks);
        p.setColor(0x33000000);
        if (endX < w) c.drawRect(Math.max(0, endX), rh, w, h, p);

        // Ruler: whole seconds with labels, frame ticks when zoomed in far enough.
        int stepTicks = session.tickStep();
        float pxPerFrame = pxPerSec / session.fps;
        p.setStrokeWidth(1f);
        if (pxPerFrame >= 6 * d) {
            p.setColor(0x33FFFFFF);
            int first = (int) Math.floor(originSec * session.fps);
            int last = (int) Math.ceil((originSec + w / pxPerSec) * session.fps);
            for (int f = Math.max(0, first); f <= last; f++) {
                float x = xOf(f * stepTicks);
                c.drawLine(x, rh - 5 * d, x, rh, p);
            }
        }
        float secStep = pxPerSec > 240 * d ? 0.25f : (pxPerSec > 80 * d ? 0.5f : 1f);
        int firstS = (int) Math.floor(originSec / secStep);
        int lastS = (int) Math.ceil((originSec + w / pxPerSec) / secStep);
        for (int i = Math.max(0, firstS); i <= lastS; i++) {
            float sec = i * secStep;
            float x = (sec - originSec) * pxPerSec;
            p.setColor(0x55FFFFFF);
            c.drawLine(x, 0, x, h, p);
            c.drawText(trim(sec) + "s", x + 3 * d, rh - 5 * d, text);
        }
        p.setColor(Ui.INSTANCE.getBORDER());
        c.drawLine(0, rh, w, rh, p);
        c.drawLine(0, rh + row, w, rh + row, p);

        // Row labels
        text.setColor(Ui.INSTANCE.getDIM());
        c.drawText("All", 3 * d, rh + 10 * d, text);
        c.drawText(joint == null ? "-" : joint, 3 * d, rh + row + 10 * d, text);

        TreeSet<Integer> all = session.allTicks();
        for (int t : all) drawKey(c, xOf(t), rh + row * 0.55f, t == selectedKey, 0xFFE0B040);
        if (joint != null) {
            for (Map.Entry<Integer, AnimSession.Key> e : session.track(joint).entrySet()) {
                drawKey(c, xOf(e.getKey()), rh + row * 1.55f, e.getKey() == selectedKey, 0xFF40B0FF);
            }
        }

        // Playhead
        float px = xOf(session.curTick);
        p.setColor(0xFFFF4444);
        p.setStrokeWidth(2 * d);
        c.drawLine(px, 0, px, h, p);
        Path tri = new Path();
        tri.moveTo(px - 6 * d, 0);
        tri.lineTo(px + 6 * d, 0);
        tri.lineTo(px, 9 * d);
        tri.close();
        p.setStyle(Paint.Style.FILL);
        c.drawPath(tri, p);
    }

    private static String trim(float v) {
        return v == (int) v ? Integer.toString((int) v) : String.format(java.util.Locale.US, "%.2f", v);
    }

    private void drawKey(Canvas c, float x, float y, boolean selected, int color) {
        float r = 5 * d;
        diamond.reset();
        diamond.moveTo(x, y - r);
        diamond.lineTo(x + r, y);
        diamond.lineTo(x, y + r);
        diamond.lineTo(x - r, y);
        diamond.close();
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        c.drawPath(diamond, p);
        if (selected) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2 * d);
            p.setColor(0xFFFFFFFF);
            c.drawPath(diamond, p);
            p.setStyle(Paint.Style.FILL);
        }
    }

    private int hitKey(float x, float y) {
        float rh = rulerH();
        float row = rowH();
        boolean summary = y > rh && y <= rh + row;
        boolean jointRow = y > rh + row;
        if (!summary && !jointRow) return -1;
        int best = -1;
        float bestD = 14 * d;
        Iterable<Integer> ticks = summary ? session.allTicks()
                : (joint == null ? new TreeSet<Integer>() : session.track(joint).keySet());
        for (int t : ticks) {
            float dx = Math.abs(xOf(t) - x);
            if (dx < bestD) {
                bestD = dx;
                best = t;
            }
        }
        dragSummary = summary;
        return best;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                downX = lastX = x;
                downY = y;
                moved = false;
                dragStarted = false;
                dragKeyTick = -1;
                scrubbing = false;
                panning = false;
                if (y <= rulerH()) {
                    scrubbing = true;
                    scrub(x);
                } else {
                    int k = hitKey(x, y);
                    if (k >= 0) {
                        dragKeyTick = k;
                        selectedKey = k;
                        if (listener != null) listener.onKeySelected(k);
                        invalidate();
                    } else {
                        panning = true;
                    }
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(x - downX) > 6 * d || Math.abs(y - downY) > 6 * d) moved = true;
                if (scrubbing) {
                    scrub(x);
                } else if (dragKeyTick >= 0 && moved) {
                    if (!dragStarted) {
                        dragStarted = true;
                        if (listener != null) listener.onBeginEdit();
                    }
                    int to = session.snap(tickAt(x));
                    if (to != dragKeyTick) {
                        if (listener != null) listener.onKeyMoved(dragKeyTick, to, dragSummary);
                        dragKeyTick = to;
                        selectedKey = to;
                    }
                    lastX = x;
                    invalidate();
                } else if (panning) {
                    originSec -= (x - lastX) / pxPerSec;
                    lastX = x;
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (panning && !moved) scrub(x);
                // fall through
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    private void scrub(float x) {
        if (listener != null) listener.onScrub(session.snap(tickAt(x)));
        invalidate();
    }
}
