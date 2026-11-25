package com.goydevv.studrblx.anim;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.goydevv.studrblx.MainActivity;
import com.goydevv.studrblx.core.Studio;
import com.goydevv.studrblx.core.Tool;
import com.goydevv.studrblx.engine.AnimClip;
import com.goydevv.studrblx.engine.AnimLibrary;
import com.goydevv.studrblx.model.CFrame;
import com.goydevv.studrblx.model.Instance;
import com.goydevv.studrblx.model.InstanceFactory;
import com.goydevv.studrblx.model.Vector3;
import com.goydevv.studrblx.ui.Ui;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Compact full-screen animation workspace for phones. The normal 3D viewport keeps rendering underneath;
 * only touches that land on our small panels are consumed. Joints are posed with the regular Move / Rotate gizmos.
 */
public final class AnimationModeView extends FrameLayout implements Studio.Listener {

    private static final int TOP_H = 28;
    private static final int BOTTOM_H = 92;
    private static final int LEFT_W = 84;
    private static final int RIGHT_W = 158;

    private final Activity act;
    private final Studio studio;
    private final AnimSession s;
    private final RigInfo rig;
    private final Runnable onExit;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final float d;

    private LinearLayout topBar;
    private ScrollView leftScroll;
    private ScrollView rightScroll;
    private LinearLayout bottom;
    private FrameLayout.LayoutParams lpTop, lpLeft, lpRight, lpBottom;
    private int cutL, cutT, cutR;

    private TextView timeTv, playBtn, undoBtn, redoBtn, nameBtn, selBtn, moveBtn, rotBtn, propsBtn;
    private final List<TextView> jointRows = new ArrayList<TextView>();
    private TimelineView timeline;
    private TextView jointTitle, styleBtn, dirBtn;
    private final SeekBar[] bars = new SeekBar[6];
    private final TextView[] vals = new TextView[6];

    private int selected = 0;
    private boolean suppress = false;
    private boolean pushNext = true;
    private boolean ignoreSel = false;
    private boolean playing = false;
    private boolean propsVisible = false;
    private float playT = 0f;
    private long lastFrame = 0L;
    private long lastAbsorb = 0L;
    private boolean closed = false;
    private boolean running = false;

    private Tool prevTool;
    private boolean prevInside, prevMoveSnap, prevRotSnap;

    public AnimationModeView(Activity act, Studio studio, RigInfo rig, Runnable onExit) {
        super(act);
        this.act = act;
        this.studio = studio;
        this.rig = rig;
        this.s = new AnimSession(rig);
        this.onExit = onExit;
        this.d = act.getResources().getDisplayMetrics().density;
        s.name = rig.displayName() + "_Anim";
        build();
    }

    // ------------------------------------------------------------------ lifecycle
    public void start() {
        studio.getListeners().add(this);
        prevTool = studio.getTool();
        prevInside = studio.getSelectInsideModels();
        prevMoveSnap = studio.getSnapEnabled();
        prevRotSnap = studio.getRotSnapEnabled();
        studio.setSelectInsideModels(true);
        studio.setSnapEnabled(false);
        studio.setRotSnapEnabled(false);
        ignoreSel = true;
        studio.select(Collections.singletonList(rig.model));
        studio.focusSelection();
        ignoreSel = false;
        s.applyPose();
        setToolMode(Tool.ROTATE);
        selectJoint(0, true);
        refreshAll();
        running = true;
        postOnAnimation(loop);
    }

    public boolean isDirty() {
        return s.dirty;
    }

    public void requestExit() {
        if (closed) return;
        if (!s.dirty) {
            finishExit();
            return;
        }
        new AlertDialog.Builder(act)
                .setTitle("Leave Animation Editor?")
                .setMessage("Unsaved changes to \"" + s.name + "\".")
                .setPositiveButton("Save & exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        save();
                        finishExit();
                    }
                })
                .setNegativeButton("Discard", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finishExit();
                    }
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void finishExit() {
        if (closed) return;
        closed = true;
        running = false;
        playing = false;
        removeCallbacks(loop);
        studio.getListeners().remove(this);
        s.restoreRig();
        if (prevTool != null) studio.setTool(prevTool);
        studio.setSelectInsideModels(prevInside);
        studio.setSnapEnabled(prevMoveSnap);
        studio.setRotSnapEnabled(prevRotSnap);
        onExit.run();
    }

    public void setCutout(int l, int t, int r) {
        cutL = l;
        cutT = t;
        cutR = r;
        applyLayout();
    }

    // ------------------------------------------------------------------ ui helpers
    private int dp(float v) {
        return Math.round(v * d);
    }

    private GradientDrawable rounded(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius * d);
        return g;
    }

    private TextView btn(String label, int bg, final Runnable r) {
        TextView t = new TextView(act);
        t.setText(label);
        t.setTextSize(10.5f);
        t.setTextColor(Color.WHITE);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        t.setIncludeFontPadding(false);
        t.setPadding(dp(6), dp(3), dp(6), dp(3));
        t.setBackground(rounded(bg, 4));
        t.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                r.run();
            }
        });
        return t;
    }

    private TextView label(String s0, float size, int color, boolean bold) {
        TextView t = new TextView(act);
        t.setText(s0);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private LinearLayout.LayoutParams wrap(int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(l), dp(t), dp(r), dp(b));
        return lp;
    }

    private int accent() {
        return Ui.INSTANCE.getACCENT();
    }

    private int soft() {
        return 0xFF3C3C3C;
    }

    private static int withAlpha(int c, int a) {
        return (c & 0x00FFFFFF) | (a << 24);
    }

    private void toast(String m) {
        Toast.makeText(act, m, Toast.LENGTH_SHORT).show();
    }

    // ------------------------------------------------------------------ construction
    private void build() {
        // ---- top bar (scrollable so nothing is ever cut off on narrow phones)
        topBar = new LinearLayout(act);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.addView(btn("\u2715", 0xFF8A3030, new Runnable() {
            @Override
            public void run() {
                requestExit();
            }
        }), wrap(2, 0, 2, 0));
        nameBtn = btn(s.name, soft(), new Runnable() {
            @Override
            public void run() {
                renameDialog();
            }
        });
        nameBtn.setSingleLine(true);
        nameBtn.setMaxWidth(dp(110));
        nameBtn.setEllipsize(android.text.TextUtils.TruncateAt.END);
        topBar.addView(nameBtn, wrap(2, 0, 6, 0));
        selBtn = btn("Sel", soft(), new Runnable() {
            @Override
            public void run() {
                setToolMode(Tool.SELECT);
            }
        });
        moveBtn = btn("Move", soft(), new Runnable() {
            @Override
            public void run() {
                setToolMode(Tool.MOVE);
            }
        });
        rotBtn = btn("Rotate", soft(), new Runnable() {
            @Override
            public void run() {
                setToolMode(Tool.ROTATE);
            }
        });
        topBar.addView(selBtn, wrap(1, 0, 1, 0));
        topBar.addView(moveBtn, wrap(1, 0, 1, 0));
        topBar.addView(rotBtn, wrap(1, 0, 8, 0));
        undoBtn = btn("Undo", soft(), new Runnable() {
            @Override
            public void run() {
                s.undo();
                afterEdit();
            }
        });
        redoBtn = btn("Redo", soft(), new Runnable() {
            @Override
            public void run() {
                s.redo();
                afterEdit();
            }
        });
        topBar.addView(undoBtn, wrap(1, 0, 1, 0));
        topBar.addView(redoBtn, wrap(1, 0, 8, 0));
        topBar.addView(btn("Import", soft(), new Runnable() {
            @Override
            public void run() {
                importMenu();
            }
        }), wrap(1, 0, 1, 0));
        topBar.addView(btn("Save", accent(), new Runnable() {
            @Override
            public void run() {
                save();
            }
        }), wrap(1, 0, 1, 0));
        topBar.addView(btn("Upload", 0xFF2E8B57, new Runnable() {
            @Override
            public void run() {
                uploadDialog();
            }
        }), wrap(1, 0, 8, 0));
        topBar.addView(btn("Settings", soft(), new Runnable() {
            @Override
            public void run() {
                settingsDialog();
            }
        }), wrap(1, 0, 1, 0));
        propsBtn = btn("Pose", soft(), new Runnable() {
            @Override
            public void run() {
                propsVisible = !propsVisible;
                rightScroll.setVisibility(propsVisible ? VISIBLE : GONE);
                paintTools();
                if (propsVisible) refreshAll();
            }
        });
        topBar.addView(propsBtn, wrap(1, 0, 4, 0));
        HorizontalScrollView topScroll = new HorizontalScrollView(act);
        topScroll.setHorizontalScrollBarEnabled(false);
        topScroll.setBackgroundColor(withAlpha(Ui.INSTANCE.getTOPBAR(), 0xE6));
        topScroll.addView(topBar);
        topBar = null; // scroll is the real child; keep padding on it
        topScrollRef = topScroll;
        lpTop = new LayoutParams(LayoutParams.MATCH_PARENT, dp(TOP_H), Gravity.TOP);
        addView(topScroll, lpTop);

        // ---- left: joints (narrow, wraps its content so the viewport stays visible)
        LinearLayout jointCol = new LinearLayout(act);
        jointCol.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < rig.joints.size(); i++) {
            final int idx = i;
            RigInfo.Joint j = rig.joints.get(i);
            TextView row = label(j.name, 11f, Ui.INSTANCE.getTEXT(), false);
            row.setSingleLine(true);
            row.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.setPadding(dp(3 + Math.min(j.depth, 5) * 3), dp(4), dp(2), dp(4));
            row.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectJoint(idx, true);
                }
            });
            jointRows.add(row);
            jointCol.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        leftScroll = new ScrollView(act);
        leftScroll.setBackgroundColor(withAlpha(Ui.INSTANCE.getPANEL(), 0xB8));
        leftScroll.addView(jointCol);
        lpLeft = new LayoutParams(dp(LEFT_W), LayoutParams.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP);
        addView(leftScroll, lpLeft);

        // ---- right: pose panel (hidden until toggled, gizmos are the primary way to pose)
        rightScroll = new ScrollView(act);
        rightScroll.setBackgroundColor(withAlpha(Ui.INSTANCE.getPANEL(), 0xC8));
        rightScroll.addView(buildInspector());
        rightScroll.setVisibility(GONE);
        lpRight = new LayoutParams(dp(RIGHT_W), LayoutParams.WRAP_CONTENT, Gravity.RIGHT | Gravity.TOP);
        addView(rightScroll, lpRight);

        // ---- bottom: transport + timeline
        bottom = new LinearLayout(act);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setBackgroundColor(withAlpha(Ui.INSTANCE.getPANEL(), 0xE6));
        bottom.addView(buildTransport(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(24)));
        timeline = new TimelineView(act, s);
        timeline.setListener(new TimelineView.Listener() {
            @Override
            public void onScrub(int tick) {
                stopPlayback();
                seek(tick);
            }

            @Override
            public void onKeyMoved(int from, int to, boolean summary) {
                s.moveKeys(from, to, summary ? null : currentJoint());
                afterEdit();
            }

            @Override
            public void onKeySelected(int tick) {
                stopPlayback();
                seek(tick);
            }

            @Override
            public void onBeginEdit() {
                s.pushUndo();
            }
        });
        bottom.addView(timeline, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        lpBottom = new LayoutParams(LayoutParams.MATCH_PARENT, dp(BOTTOM_H), Gravity.BOTTOM);
        addView(bottom, lpBottom);

        applyLayout();
    }

    private HorizontalScrollView topScrollRef;

    private View buildTransport() {
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        row.addView(btn("|<", soft(), new Runnable() {
            @Override
            public void run() {
                stopPlayback();
                seek(0);
            }
        }), wrap(2, 0, 1, 0));
        row.addView(btn("<", soft(), new Runnable() {
            @Override
            public void run() {
                stopPlayback();
                Integer k = s.allTicks().lower(s.curTick);
                seek(k != null ? k : Math.max(0, s.curTick - s.tickStep()));
            }
        }), wrap(1, 0, 1, 0));
        playBtn = btn("Play", 0xFF2E8B57, new Runnable() {
            @Override
            public void run() {
                if (playing) stopPlayback();
                else startPlayback();
            }
        });
        playBtn.setMinWidth(dp(40));
        row.addView(playBtn, wrap(1, 0, 1, 0));
        row.addView(btn(">", soft(), new Runnable() {
            @Override
            public void run() {
                stopPlayback();
                Integer k = s.allTicks().higher(s.curTick);
                seek(k != null ? k : s.curTick + s.tickStep());
            }
        }), wrap(1, 0, 1, 0));
        row.addView(btn(">|", soft(), new Runnable() {
            @Override
            public void run() {
                stopPlayback();
                seek(s.lengthTicks);
            }
        }), wrap(1, 0, 6, 0));

        timeTv = label("0.00s f0", 11f, Ui.INSTANCE.getTEXT(), true);
        timeTv.setMinWidth(dp(66));
        row.addView(timeTv, wrap(0, 0, 6, 0));

        row.addView(btn("+Key", accent(), new Runnable() {
            @Override
            public void run() {
                s.keyJoint(currentJoint());
                afterEdit();
            }
        }), wrap(1, 0, 1, 0));
        row.addView(btn("+All", accent(), new Runnable() {
            @Override
            public void run() {
                s.keyAll();
                afterEdit();
            }
        }), wrap(1, 0, 1, 0));
        row.addView(btn("Del", 0xFF8A3030, new Runnable() {
            @Override
            public void run() {
                if (s.hasKey(currentJoint(), s.curTick)) s.deleteKeyAt(s.curTick, currentJoint());
                else s.deleteKeyAt(s.curTick, null);
                afterEdit();
            }
        }), wrap(1, 0, 6, 0));
        row.addView(btn("\u2212", soft(), new Runnable() {
            @Override
            public void run() {
                timeline.zoom(0.7f);
            }
        }), wrap(1, 0, 1, 0));
        row.addView(btn("+", soft(), new Runnable() {
            @Override
            public void run() {
                timeline.zoom(1.4f);
            }
        }), wrap(1, 0, 2, 0));

        HorizontalScrollView hs = new HorizontalScrollView(act);
        hs.setHorizontalScrollBarEnabled(false);
        hs.addView(row);
        hs.setBackgroundColor(Ui.INSTANCE.getHEADER());
        return hs;
    }

    private View buildInspector() {
        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(5), dp(3), dp(5), dp(5));

        jointTitle = label("Joint", 11.5f, Ui.INSTANCE.getTEXT(), true);
        col.addView(jointTitle, wrap(0, 0, 0, 2));
        String[] ax = {"RX", "RY", "RZ", "X", "Y", "Z"};
        for (int i = 0; i < 6; i++) {
            if (i < 3) col.addView(sliderRow(i, ax[i], -180, 180, 1f), wrap(0, 0, 0, 0));
            else col.addView(sliderRow(i, ax[i], -500, 500, 0.02f), wrap(0, 0, 0, 0));
        }

        LinearLayout r1 = new LinearLayout(act);
        r1.addView(btn("Reset", soft(), new Runnable() {
            @Override
            public void run() {
                s.resetJoint(currentJoint());
                afterEdit();
            }
        }), wrap(0, 4, 2, 0));
        r1.addView(btn("Copy", soft(), new Runnable() {
            @Override
            public void run() {
                s.copyPose();
                toast("Pose copied");
            }
        }), wrap(0, 4, 2, 0));
        r1.addView(btn("Paste", soft(), new Runnable() {
            @Override
            public void run() {
                if (!s.hasClipboard()) {
                    toast("Copy a pose first");
                    return;
                }
                s.pastePose();
                afterEdit();
            }
        }), wrap(0, 4, 0, 0));
        col.addView(r1);

        LinearLayout r2 = new LinearLayout(act);
        styleBtn = btn("Style", soft(), new Runnable() {
            @Override
            public void run() {
                AnimSession.Key k = s.track(currentJoint()).get(s.curTick);
                if (k == null) {
                    toast("No key on this joint at the playhead");
                    return;
                }
                s.setEasing(currentJoint(), s.curTick, (k.style + 1) % AnimSession.STYLES.length, k.dir);
                afterEdit();
            }
        });
        dirBtn = btn("Dir", soft(), new Runnable() {
            @Override
            public void run() {
                AnimSession.Key k = s.track(currentJoint()).get(s.curTick);
                if (k == null) {
                    toast("No key on this joint at the playhead");
                    return;
                }
                s.setEasing(currentJoint(), s.curTick, k.style, (k.dir + 1) % AnimSession.DIRS.length);
                afterEdit();
            }
        });
        r2.addView(styleBtn, wrap(0, 4, 2, 0));
        r2.addView(dirBtn, wrap(0, 4, 0, 0));
        col.addView(r2);
        return col;
    }

    private View sliderRow(final int idx, String axis, int min, int max, final float scale) {
        LinearLayout row = new LinearLayout(act);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView a = label(axis, 10f, Ui.INSTANCE.getDIM(), true);
        a.setMinWidth(dp(16));
        row.addView(a);
        SeekBar sb = new SeekBar(act);
        sb.setMax(max - min);
        sb.setTag(new float[]{min, scale});
        sb.setPadding(dp(6), 0, dp(6), 0);
        bars[idx] = sb;
        row.addView(sb, new LinearLayout.LayoutParams(0, dp(22), 1f));
        TextView v = label("0", 10f, Ui.INSTANCE.getTEXT(), false);
        v.setMinWidth(dp(28));
        v.setGravity(Gravity.RIGHT);
        vals[idx] = v;
        row.addView(v);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) onSliderEdited();
                else updateValueText(idx);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopPlayback();
                pushNext = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                pushNext = true;
            }
        });
        return row;
    }

    private float sliderValue(int idx) {
        float[] meta = (float[]) bars[idx].getTag();
        return (bars[idx].getProgress() + meta[0]) * meta[1];
    }

    private void updateValueText(int idx) {
        float v = sliderValue(idx);
        vals[idx].setText(idx < 3 ? String.format(Locale.US, "%d\u00B0", Math.round(v)) : String.format(Locale.US, "%.2f", v));
    }

    private void onSliderEdited() {
        if (suppress) return;
        for (int i = 0; i < 6; i++) updateValueText(i);
        double r = Math.PI / 180.0;
        CFrame cf = CFrame.INSTANCE.at(new Vector3(sliderValue(3), sliderValue(4), sliderValue(5)))
                .times(CFrame.INSTANCE.angles((float) (sliderValue(0) * r), (float) (sliderValue(1) * r), (float) (sliderValue(2) * r)));
        s.setPose(currentJoint(), cf, pushNext);
        pushNext = false;
        s.applyPose();
        refreshLight();
    }

    // ------------------------------------------------------------------ state
    private String currentJoint() {
        return rig.joints.get(Math.max(0, Math.min(selected, rig.joints.size() - 1))).name;
    }

    private void setToolMode(Tool t) {
        studio.setTool(t);
        paintTools();
    }

    private void paintTools() {
        Tool t = studio.getTool();
        selBtn.setBackground(rounded(t == Tool.SELECT ? accent() : soft(), 4));
        moveBtn.setBackground(rounded(t == Tool.MOVE ? accent() : soft(), 4));
        rotBtn.setBackground(rounded(t == Tool.ROTATE ? accent() : soft(), 4));
        propsBtn.setBackground(rounded(propsVisible ? accent() : soft(), 4));
    }

    private void selectJoint(int idx, boolean updateStudio) {
        if (idx < 0 || idx >= rig.joints.size()) return;
        selected = idx;
        if (updateStudio) {
            ignoreSel = true;
            studio.select(Collections.singletonList(rig.joints.get(idx).p1));
            ignoreSel = false;
        }
        timeline.setJoint(currentJoint());
        refreshAll();
    }

    private void seek(int tick) {
        s.setTime(tick);
        s.applyPose();
        timeline.setSelectedKey(s.allTicks().contains(s.curTick) ? s.curTick : -1);
        timeline.follow();
        refreshAll();
    }

    private void afterEdit() {
        s.applyPose();
        refreshAll();
    }

    private void refreshLight() {
        timeTv.setText(String.format(Locale.US, "%.2fs f%d", s.seconds(s.curTick), s.frameOf(s.curTick)));
        for (int i = 0; i < jointRows.size(); i++) paintRow(i);
        timeline.invalidate();
    }

    private void paintRow(int i) {
        TextView row = jointRows.get(i);
        boolean sel = i == selected;
        boolean key = s.hasKey(rig.joints.get(i).name, s.curTick);
        row.setText((key ? "\u25C6" : "") + rig.joints.get(i).name);
        row.setTextColor(sel ? Color.WHITE : Ui.INSTANCE.getTEXT());
        row.setBackgroundColor(sel ? accent() : 0);
    }

    private void refreshAll() {
        nameBtn.setText(s.name);
        undoBtn.setAlpha(s.canUndo() ? 1f : 0.4f);
        redoBtn.setAlpha(s.canRedo() ? 1f : 0.4f);
        playBtn.setText(playing ? "Pause" : "Play");
        paintTools();
        if (propsVisible) {
            jointTitle.setText(currentJoint());
            AnimSession.Key k = s.track(currentJoint()).get(s.curTick);
            styleBtn.setText(k == null ? "Ease: -" : AnimSession.STYLES[Math.min(k.style, AnimSession.STYLES.length - 1)]);
            dirBtn.setText(k == null ? "-" : AnimSession.DIRS[Math.min(k.dir, AnimSession.DIRS.length - 1)]);
            CFrame cf = s.current(currentJoint());
            Vector3 e = cf.toEulerXYZ();
            Vector3 p = cf.getPosition();
            float[] vs = {
                    (float) Math.toDegrees(e.getX()), (float) Math.toDegrees(e.getY()), (float) Math.toDegrees(e.getZ()),
                    p.getX(), p.getY(), p.getZ()
            };
            suppress = true;
            for (int i = 0; i < 6; i++) {
                float[] meta = (float[]) bars[i].getTag();
                int prog = Math.round(vs[i] / meta[1] - meta[0]);
                bars[i].setProgress(Math.max(0, Math.min(bars[i].getMax(), prog)));
                updateValueText(i);
            }
            suppress = false;
        }
        refreshLight();
    }

    // ------------------------------------------------------------------ frame loop: playback + gizmo capture
    private final Runnable loop = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            if (playing) stepPlayback();
            else gizmoPoll();
            postOnAnimation(this);
        }
    };

    private void stepPlayback() {
        long now = SystemClock.uptimeMillis();
        float dt = (now - lastFrame) / 1000f * AnimSession.TICKS;
        lastFrame = now;
        playT += dt;
        int len = Math.max(1, s.lengthTicks);
        if (playT >= len) {
            if (s.loop) {
                playT = playT % len;
            } else {
                s.setTime(len);
                s.applyPose();
                stopPlayback();
                return;
            }
        }
        s.setTime((int) playT);
        s.applyPose();
        timeline.follow();
        refreshLight();
    }

    /** The user dragged the selected part with the Move/Rotate gizmo: turn it into a joint pose. */
    private void gizmoPoll() {
        Tool t = studio.getTool();
        boolean rot = t == Tool.ROTATE;
        boolean mov = t == Tool.MOVE;
        if (!rot && !mov) return;
        long now = SystemClock.uptimeMillis();
        boolean newDrag = now - lastAbsorb > 350;
        if (s.absorbExternalEdit(selected, rot, mov, newDrag)) {
            lastAbsorb = now;
            s.applyPose();
            if (propsVisible) refreshAll();
            else refreshLight();
        }
    }

    private void startPlayback() {
        if (playing) return;
        playing = true;
        playT = s.curTick >= s.lengthTicks ? 0f : s.curTick;
        lastFrame = SystemClock.uptimeMillis();
        playBtn.setText("Pause");
    }

    private void stopPlayback() {
        if (!playing) return;
        playing = false;
        playBtn.setText("Play");
        refreshAll();
    }

    // ------------------------------------------------------------------ dialogs
    private void renameDialog() {
        final EditText et = new EditText(act);
        et.setText(s.name);
        et.setSingleLine(true);
        new AlertDialog.Builder(act).setTitle("Animation name").setView(et)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String n = et.getText().toString().trim();
                        if (n.length() > 0) {
                            s.name = n;
                            s.dirty = true;
                            refreshAll();
                        }
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private void settingsDialog() {
        stopPlayback();
        final String[] items = {
                String.format(Locale.US, "Length: %.2fs  (tap = +0.25s)", s.seconds(s.lengthTicks)),
                "Length -0.25s",
                "Length = last keyframe",
                "FPS: " + s.fps + "  (tap to change)",
                "Loop: " + (s.loop ? "on" : "off"),
                "Priority: " + AnimSession.PRIORITY_NAMES[s.priorityIdx],
                "Auto-key: " + (s.autoKey ? "on" : "off"),
                "Zero rig rotation (0,0,0)",
                "Clear all keyframes"
        };
        new AlertDialog.Builder(act).setTitle("Animation settings").setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        s.lengthTicks += AnimSession.TICKS / 4;
                        break;
                    case 1:
                        s.lengthTicks = Math.max(Math.max(s.lastKeyTick(), AnimSession.TICKS / 4), s.lengthTicks - AnimSession.TICKS / 4);
                        break;
                    case 2:
                        s.lengthTicks = Math.max(AnimSession.TICKS / 4, s.lastKeyTick());
                        break;
                    case 3:
                        s.fps = s.fps == 30 ? 60 : (s.fps == 60 ? 24 : 30);
                        break;
                    case 4:
                        s.loop = !s.loop;
                        break;
                    case 5:
                        s.priorityIdx = (s.priorityIdx + 1) % AnimSession.PRIORITY_NAMES.length;
                        break;
                    case 6:
                        s.autoKey = !s.autoKey;
                        break;
                    case 7:
                        s.zeroRigRotation();
                        toast("Rig rotation zeroed");
                        refreshAll();
                        return;
                    case 8:
                        s.clearAll();
                        afterEdit();
                        return;
                    default:
                        break;
                }
                s.dirty = true;
                afterEdit();
                if (which <= 6) settingsDialog();
            }
        }).setNegativeButton("Close", null).show();
    }

    private void save() {
        Instance ks = s.saveToRig();
        studio.markDirty();
        toast("Saved KeyframeSequence \"" + ks.getName() + "\" in " + rig.displayName());
        refreshAll();
    }

    private void importMenu() {
        final String[] items = {"CFrame table (paste text)", "CFrame table (.txt / .lua file)", "KeyframeSequence in this place", "Animation asset ID"};
        new AlertDialog.Builder(act).setTitle("Import animation").setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) importPasteDialog();
                else if (which == 1) {
                    if (act instanceof MainActivity) ((MainActivity) act).pickAnimTextFile();
                } else if (which == 2) importFromPlace();
                else importFromId();
            }
        }).show();
    }

    private void importPasteDialog() {
        final EditText et = new EditText(act);
        et.setHint("Paste the Lua table here");
        et.setMinLines(4);
        et.setMaxLines(8);
        et.setTextSize(11f);
        et.setGravity(Gravity.TOP);
        try {
            ClipboardManager cm = (ClipboardManager) act.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                CharSequence c = cm.getPrimaryClip().getItemAt(0).coerceToText(act);
                if (c != null && c.toString().contains("CFrame")) et.setText(c);
            }
        } catch (Throwable ignored) {
        }
        new AlertDialog.Builder(act).setTitle("Import CFrame table").setView(et)
                .setPositiveButton("Import", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        importCFrameText(et.getText().toString(), "Imported");
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    /** Called by MainActivity after the user picked a text/Lua file. */
    public void onTextFile(final Uri uri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream in = act.getContentResolver().openInputStream(uri);
                    ByteArrayOutputStream bo = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while (in != null && (n = in.read(buf)) > 0) {
                        bo.write(buf, 0, n);
                        if (bo.size() > 16 * 1024 * 1024) throw new Exception("File is too large.");
                    }
                    if (in != null) in.close();
                    final String text = bo.toString("UTF-8");
                    String last = uri.getLastPathSegment() == null ? "Imported" : uri.getLastPathSegment();
                    int slash = Math.max(last.lastIndexOf('/'), last.lastIndexOf(':'));
                    if (slash >= 0) last = last.substring(slash + 1);
                    int dot = last.lastIndexOf('.');
                    if (dot > 0) last = last.substring(0, dot);
                    final String label = last;
                    post(new Runnable() {
                        @Override
                        public void run() {
                            importCFrameText(text, label);
                        }
                    });
                } catch (final Throwable t) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            toast("Could not read file: " + t.getMessage());
                        }
                    });
                }
            }
        }, "anim-read").start();
    }

    private void importCFrameText(String text, String label) {
        try {
            List<CFrameTable.Frame> frames = CFrameTable.parse(text);
            stopPlayback();
            String report = s.importFrames(frames, label);
            s.applyPose();
            timeline.setJoint(currentJoint());
            seek(0);
            new AlertDialog.Builder(act).setTitle("Imported \"" + label + "\"").setMessage(report)
                    .setPositiveButton("Play", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            seek(0);
                            startPlayback();
                        }
                    }).setNegativeButton("Edit", null).show();
        } catch (Throwable t) {
            new AlertDialog.Builder(act).setTitle("Import failed")
                    .setMessage(t.getMessage() == null ? t.toString() : t.getMessage())
                    .setPositiveButton("OK", null).show();
        }
    }

    private void importFromPlace() {
        final List<Instance> found = new ArrayList<Instance>();
        for (Instance i : studio.getEditModel().descendants(new ArrayList<Instance>())) {
            if ("KeyframeSequence".equals(i.getClassName())) found.add(i);
        }
        if (found.isEmpty()) {
            toast("No KeyframeSequence in this place.");
            return;
        }
        String[] names = new String[found.size()];
        for (int i = 0; i < names.length; i++) names[i] = found.get(i).fullName();
        new AlertDialog.Builder(act).setTitle("Pick a KeyframeSequence").setItems(names, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                stopPlayback();
                s.importSequence(found.get(which));
                seek(0);
                toast("Imported " + found.get(which).getName());
            }
        }).setNegativeButton("Cancel", null).show();
    }

    private void importFromId() {
        final EditText et = new EditText(act);
        et.setHint("Animation asset ID");
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(act).setTitle("Import by asset ID").setView(et)
                .setPositiveButton("Import", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        long id;
                        try {
                            id = Long.parseLong(et.getText().toString().trim());
                        } catch (Exception e) {
                            toast("Not a valid asset ID.");
                            return;
                        }
                        pollClip(id, 0);
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private void pollClip(final long id, final int attempt) {
        AnimClip clip = AnimLibrary.INSTANCE.get(id);
        if (clip != null) {
            stopPlayback();
            s.importClip(clip, "Imported_" + id);
            seek(0);
            toast("Imported animation " + id);
            return;
        }
        if (AnimLibrary.INSTANCE.failed(id) || attempt > 40) {
            toast("Could not download/parse animation " + id + ".");
            return;
        }
        if (attempt == 0) toast("Downloading animation...");
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!closed) pollClip(id, attempt + 1);
            }
        }, 400);
    }

    private void uploadDialog() {
        stopPlayback();
        if (s.allTicks().isEmpty()) {
            toast("Add some keyframes before uploading.");
            return;
        }
        final AnimUploader.Settings st = AnimUploader.load(act);

        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), dp(4), dp(16), 0);
        final EditText key = new EditText(act);
        key.setHint("Open Cloud API key (Assets write)");
        key.setSingleLine(true);
        key.setText(st.apiKey);
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        col.addView(key);
        final RadioGroup rg = new RadioGroup(act);
        rg.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton rbU = new RadioButton(act);
        rbU.setText("User");
        rbU.setId(1);
        RadioButton rbG = new RadioButton(act);
        rbG.setText("Group");
        rbG.setId(2);
        rg.addView(rbU);
        rg.addView(rbG);
        rg.check(st.group ? 2 : 1);
        col.addView(rg);
        final EditText cid = new EditText(act);
        cid.setHint("Creator (user/group) ID");
        cid.setSingleLine(true);
        cid.setInputType(InputType.TYPE_CLASS_NUMBER);
        if (st.creatorId != 0) cid.setText(Long.toString(st.creatorId));
        col.addView(cid);
        final EditText desc = new EditText(act);
        desc.setHint("Description (optional)");
        desc.setSingleLine(true);
        desc.setText("Made with StudioRblx Animation Editor");
        col.addView(desc);

        new AlertDialog.Builder(act).setTitle("Upload \"" + s.name + "\"").setView(col)
                .setPositiveButton("Upload", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        st.apiKey = key.getText().toString().trim();
                        st.group = rg.getCheckedRadioButtonId() == 2;
                        try {
                            st.creatorId = Long.parseLong(cid.getText().toString().trim());
                        } catch (Exception e) {
                            st.creatorId = 0;
                        }
                        if (st.apiKey.length() == 0 || st.creatorId == 0) {
                            toast("API key and creator ID are required.");
                            return;
                        }
                        AnimUploader.save(act, st);
                        runUpload(st, desc.getText().toString().trim());
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private void runUpload(AnimUploader.Settings st, String description) {
        Instance ks = s.buildSequence();
        final AlertDialog progress = new AlertDialog.Builder(act).setTitle("Uploading animation")
                .setMessage("Starting...").setCancelable(false).create();
        progress.show();
        AnimUploader.upload(act, ks, s.name, description, st, new AnimUploader.Callback() {
            @Override
            public void onStatus(final String msg) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        progress.setMessage(msg);
                    }
                });
            }

            @Override
            public void onDone(final String assetId) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        progress.dismiss();
                        uploadDone(assetId);
                    }
                });
            }

            @Override
            public void onError(final String msg) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        progress.dismiss();
                        new AlertDialog.Builder(act).setTitle("Upload failed").setMessage(msg)
                                .setPositiveButton("OK", null).show();
                    }
                });
            }
        });
    }

    private void uploadDone(final String assetId) {
        final String url = "rbxassetid://" + assetId;
        new AlertDialog.Builder(act).setTitle("Uploaded!")
                .setMessage("Animation ID: " + assetId + "\n" + url)
                .setPositiveButton("Copy ID", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ClipboardManager cm = (ClipboardManager) act.getSystemService(Context.CLIPBOARD_SERVICE);
                        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Animation ID", url));
                        toast("Copied " + url);
                    }
                })
                .setNeutralButton("Add Animation", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Instance a = InstanceFactory.INSTANCE.create("Animation");
                        a.setName(s.name);
                        a.set("AnimationId", url, false);
                        a.setParent(rig.model, -1);
                        studio.markDirty();
                        toast("Added Animation \"" + s.name + "\"");
                    }
                })
                .setNegativeButton("Close", null).show();
    }

    // ------------------------------------------------------------------ studio selection -> joint
    @Override
    public void onSelectionChanged() {
        post(new Runnable() {
            @Override
            public void run() {
                if (ignoreSel || closed) return;
                for (Instance i : new ArrayList<Instance>(studio.getSelection())) {
                    int idx = rig.indexOfPart(i);
                    if (idx >= 0) {
                        selectJoint(idx, false);
                        return;
                    }
                }
            }
        });
    }

    // ------------------------------------------------------------------ layout + touch passthrough
    private void applyLayout() {
        if (lpTop == null) return;
        topScrollRef.setPadding(cutL, cutT, cutR, 0);
        lpTop.height = dp(TOP_H) + cutT;
        lpLeft.width = dp(LEFT_W) + cutL;
        lpLeft.topMargin = lpTop.height;
        lpLeft.bottomMargin = dp(BOTTOM_H);
        leftScroll.setPadding(cutL, 0, 0, 0);
        lpRight.width = dp(RIGHT_W) + cutR;
        lpRight.topMargin = lpTop.height;
        lpRight.bottomMargin = dp(BOTTOM_H);
        rightScroll.setPadding(0, 0, cutR, 0);
        bottom.setPadding(cutL, 0, cutR, 0);
        requestLayout();
    }

    private boolean passthrough = false;

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            passthrough = !hitsPanel(e.getX(), e.getY());
        }
        if (passthrough) return false; // the 3D viewport + gizmos underneath handle it
        return super.dispatchTouchEvent(e);
    }

    private boolean hitsPanel(float x, float y) {
        for (int i = 0; i < getChildCount(); i++) {
            View c = getChildAt(i);
            if (c.getVisibility() != VISIBLE) continue;
            if (x >= c.getLeft() && x < c.getRight() && y >= c.getTop() && y < c.getBottom()) return true;
        }
        return false;
    }
}
