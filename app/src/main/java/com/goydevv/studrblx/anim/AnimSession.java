package com.goydevv.studrblx.anim;

import com.goydevv.studrblx.engine.AnimClip;
import com.goydevv.studrblx.engine.Quat;
import com.goydevv.studrblx.model.CFrame;
import com.goydevv.studrblx.model.EnumValue;
import com.goydevv.studrblx.model.Instance;
import com.goydevv.studrblx.model.InstanceFactory;
import com.goydevv.studrblx.model.PartData;
import com.goydevv.studrblx.model.Vector3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The editable animation: per-joint keyframe tracks + pose evaluation onto the rig.
 * Time is stored in "ticks" (1/240 s) so 24, 30 and 60 fps all land on whole numbers.
 */
public final class AnimSession {
    public static final int TICKS = 240;

    public static final String[] STYLES = {"Linear", "Constant", "Elastic", "Cubic", "Bounce"};
    public static final String[] DIRS = {"In", "Out", "InOut"};
    public static final String[] PRIORITY_NAMES = {"Core", "Idle", "Movement", "Action", "Action2", "Action3", "Action4"};
    /** AnimationPriority enum values as Roblox serialises them. */
    public static final int[] PRIORITY_VALUES = {1000, 0, 1, 2, 3, 4, 5};

    public static final class Key {
        public CFrame cf;
        public int style;
        public int dir;

        public Key(CFrame cf, int style, int dir) {
            this.cf = cf;
            this.style = style;
            this.dir = dir;
        }

        Key copy() {
            return new Key(cf, style, dir);
        }
    }

    public final RigInfo rig;
    public String name = "NewAnimation";
    public int fps = 30;
    public int lengthTicks = TICKS * 2;
    public boolean loop = true;
    public int priorityIdx = 3; // Action
    public boolean autoKey = true;
    public int curTick = 0;
    public boolean dirty = false;

    private Map<String, TreeMap<Integer, Key>> tracks = new LinkedHashMap<String, TreeMap<Integer, Key>>();
    private final Map<String, CFrame> live = new HashMap<String, CFrame>();
    private final IdentityHashMap<Instance, CFrame> originals = new IdentityHashMap<Instance, CFrame>();
    private final ArrayList<Map<String, TreeMap<Integer, Key>>> undo = new ArrayList<Map<String, TreeMap<Integer, Key>>>();
    private final ArrayList<Map<String, TreeMap<Integer, Key>>> redo = new ArrayList<Map<String, TreeMap<Integer, Key>>>();
    private Map<String, CFrame> clipboard = null;

    public AnimSession(RigInfo rig) {
        this.rig = rig;
        for (RigInfo.Joint j : rig.joints) tracks.put(j.name, new TreeMap<Integer, Key>());
        captureOriginals();
    }

    // ---------------------------------------------------------------- time helpers
    public int tickStep() {
        return TICKS / fps;
    }

    public float seconds(int tick) {
        return tick / (float) TICKS;
    }

    public int snap(int tick) {
        int s = tickStep();
        return Math.max(0, Math.round(tick / (float) s) * s);
    }

    public int frameOf(int tick) {
        return Math.round(tick / (float) tickStep());
    }

    // ---------------------------------------------------------------- tracks
    public TreeMap<Integer, Key> track(String joint) {
        TreeMap<Integer, Key> t = tracks.get(joint);
        if (t == null) {
            t = new TreeMap<Integer, Key>();
            tracks.put(joint, t);
        }
        return t;
    }

    public boolean hasKey(String joint, int tick) {
        return track(joint).containsKey(tick);
    }

    public TreeSet<Integer> allTicks() {
        TreeSet<Integer> s = new TreeSet<Integer>();
        for (TreeMap<Integer, Key> t : tracks.values()) s.addAll(t.keySet());
        return s;
    }

    public int lastKeyTick() {
        int m = 0;
        for (TreeMap<Integer, Key> t : tracks.values()) if (!t.isEmpty()) m = Math.max(m, t.lastKey());
        return m;
    }

    public CFrame valueAt(String joint, int tick) {
        TreeMap<Integer, Key> t = tracks.get(joint);
        if (t == null || t.isEmpty()) return CFrame.INSTANCE.getIDENTITY();
        Map.Entry<Integer, Key> lo = t.floorEntry(tick);
        Map.Entry<Integer, Key> hi = t.ceilingEntry(tick);
        if (lo == null) return hi.getValue().cf;
        if (hi == null || lo.getKey().equals(hi.getKey())) return lo.getValue().cf;
        float span = hi.getKey() - lo.getKey();
        float x = (tick - lo.getKey()) / span;
        Key a = lo.getValue();
        return Quat.INSTANCE.lerp(a.cf, hi.getValue().cf, AnimClip.INSTANCE.ease(x, a.style, a.dir));
    }

    /** Pose shown right now: live (unkeyed) edits override the evaluated animation. */
    public CFrame current(String joint) {
        CFrame l = live.get(joint);
        return l != null ? l : valueAt(joint, curTick);
    }

    public void setTime(int tick) {
        curTick = Math.max(0, tick);
        live.clear();
    }

    // ---------------------------------------------------------------- editing
    private static Map<String, TreeMap<Integer, Key>> deepCopy(Map<String, TreeMap<Integer, Key>> src) {
        Map<String, TreeMap<Integer, Key>> out = new LinkedHashMap<String, TreeMap<Integer, Key>>();
        for (Map.Entry<String, TreeMap<Integer, Key>> e : src.entrySet()) {
            TreeMap<Integer, Key> t = new TreeMap<Integer, Key>();
            for (Map.Entry<Integer, Key> k : e.getValue().entrySet()) t.put(k.getKey(), k.getValue().copy());
            out.put(e.getKey(), t);
        }
        return out;
    }

    public void pushUndo() {
        undo.add(deepCopy(tracks));
        if (undo.size() > 80) undo.remove(0);
        redo.clear();
        dirty = true;
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public boolean canRedo() {
        return !redo.isEmpty();
    }

    public void undo() {
        if (undo.isEmpty()) return;
        redo.add(deepCopy(tracks));
        tracks = undo.remove(undo.size() - 1);
        live.clear();
        dirty = true;
    }

    public void redo() {
        if (redo.isEmpty()) return;
        undo.add(deepCopy(tracks));
        tracks = redo.remove(redo.size() - 1);
        live.clear();
        dirty = true;
    }

    /** Set a joint's pose at the playhead. With auto-key off the edit is only a live preview. */
    public void setPose(String joint, CFrame cf, boolean commitUndo) {
        if (!autoKey && !hasKey(joint, curTick)) {
            live.put(joint, cf);
            return;
        }
        if (commitUndo) pushUndo();
        putKey(joint, curTick, cf);
        live.remove(joint);
    }

    private void putKey(String joint, int tick, CFrame cf) {
        TreeMap<Integer, Key> t = track(joint);
        Key old = t.get(tick);
        t.put(tick, new Key(cf, old != null ? old.style : 0, old != null ? old.dir : 1));
        if (tick > lengthTicks) lengthTicks = tick;
        dirty = true;
    }

    /** Key every joint (or just the given one) at the playhead with its current pose. */
    public void keyAll() {
        pushUndo();
        for (RigInfo.Joint j : rig.joints) putKey(j.name, curTick, current(j.name));
        live.clear();
    }

    public void keyJoint(String joint) {
        pushUndo();
        putKey(joint, curTick, current(joint));
        live.remove(joint);
    }

    public void deleteKeyAt(int tick, String jointOrNull) {
        pushUndo();
        if (jointOrNull == null) {
            for (TreeMap<Integer, Key> t : tracks.values()) t.remove(tick);
        } else {
            track(jointOrNull).remove(tick);
        }
    }

    public void moveKeys(int from, int to, String jointOrNull) {
        to = Math.max(0, to);
        if (from == to) return;
        for (Map.Entry<String, TreeMap<Integer, Key>> e : tracks.entrySet()) {
            if (jointOrNull != null && !jointOrNull.equals(e.getKey())) continue;
            Key k = e.getValue().remove(from);
            if (k != null) e.getValue().put(to, k);
        }
        if (to > lengthTicks) lengthTicks = to;
        dirty = true;
    }

    public void setEasing(String joint, int tick, int style, int dir) {
        Key k = track(joint).get(tick);
        if (k == null) return;
        pushUndo();
        k.style = style;
        k.dir = dir;
    }

    public void resetJoint(String joint) {
        setPose(joint, CFrame.INSTANCE.getIDENTITY(), true);
    }

    public void copyPose() {
        clipboard = new HashMap<String, CFrame>();
        for (RigInfo.Joint j : rig.joints) clipboard.put(j.name, current(j.name));
    }

    public boolean hasClipboard() {
        return clipboard != null;
    }

    public void pastePose() {
        if (clipboard == null) return;
        pushUndo();
        for (Map.Entry<String, CFrame> e : clipboard.entrySet()) {
            if (rig.indexOf(e.getKey()) >= 0) putKey(e.getKey(), curTick, e.getValue());
        }
        live.clear();
    }

    public void clearAll() {
        pushUndo();
        for (TreeMap<Integer, Key> t : tracks.values()) t.clear();
        live.clear();
    }

    // ---------------------------------------------------------------- applying to the rig
    private void captureOriginals() {
        originals.clear();
        originals.put(rig.rootPart, PartData.INSTANCE.cframe(rig.rootPart));
        for (RigInfo.Joint j : rig.joints) originals.put(j.p1, PartData.INSTANCE.cframe(j.p1));
    }

    /** Solve every Motor6D from the (fixed) root outwards: P1 = P0 * C0 * Transform * C1^-1. */
    public void applyPose() {
        for (RigInfo.Joint j : rig.joints) {
            CFrame p0 = PartData.INSTANCE.cframe(j.p0);
            CFrame cf = p0.times(j.c0).times(current(j.name)).times(j.c1Inv);
            j.p1.set("CFrame", cf, true);
        }
    }

    /** Put every part back exactly where it was before the editor opened. */
    public void restoreRig() {
        for (Map.Entry<Instance, CFrame> e : originals.entrySet()) {
            e.getKey().set("CFrame", e.getValue(), true);
        }
    }

    // ---------------------------------------------------------------- KeyframeSequence in/out
    private static EnumValue ev(int v) {
        return new EnumValue(v);
    }

    /** Builds a detached KeyframeSequence (not parented to anything) from the current tracks. */
    public Instance buildSequence() {
        Instance ks = InstanceFactory.INSTANCE.create("KeyframeSequence");
        ks.setName(name);
        ks.set("Loop", loop, false);
        ks.set("Priority", ev(PRIORITY_VALUES[priorityIdx]), false);
        ks.set("AuthoredHipHeight", Float.valueOf(2.0f), false);

        TreeSet<Integer> ticks = allTicks();
        ticks.add(0);
        int end = Math.max(lengthTicks, lastKeyTick());
        ticks.add(end);

        for (int tick : ticks) {
            Instance kf = InstanceFactory.INSTANCE.create("Keyframe");
            kf.setName("Keyframe");
            kf.set("Time", Float.valueOf(tick / (float) TICKS), false);
            // Root pose (named after the root part) carries the hierarchy; Roblox keeps it at weight 0.
            Instance rootPose = makePose(rig.rootPart.getName(), CFrame.INSTANCE.getIDENTITY(), 0.0f, 0, 0);
            Instance[] byJoint = new Instance[rig.joints.size()];
            for (int i = 0; i < rig.joints.size(); i++) {
                RigInfo.Joint j = rig.joints.get(i);
                Key k = track(j.name).get(tick);
                Instance p = k != null
                        ? makePose(j.name, k.cf, 1.0f, k.style, k.dir)
                        : makePose(j.name, CFrame.INSTANCE.getIDENTITY(), 0.0f, 0, 0);
                byJoint[i] = p;
                p.attachRaw(j.parent < 0 ? rootPose : byJoint[j.parent]);
            }
            rootPose.attachRaw(kf);
            kf.attachRaw(ks);
        }
        return ks;
    }

    private static Instance makePose(String n, CFrame cf, float weight, int style, int dir) {
        Instance p = InstanceFactory.INSTANCE.create("Pose");
        p.setName(n);
        p.set("CFrame", cf, false);
        p.set("Weight", Float.valueOf(weight), false);
        p.set("MaskWeight", Float.valueOf(0.0f), false);
        p.set("EasingStyle", ev(style), false);
        p.set("EasingDirection", ev(dir), false);
        return p;
    }

    /** Save into the rig as a real KeyframeSequence child (replacing one with the same name). */
    public Instance saveToRig() {
        for (Instance c : new ArrayList<Instance>(rig.model.getChildren())) {
            if ("KeyframeSequence".equals(c.getClassName()) && name.equals(c.getName())) c.destroy();
        }
        Instance ks = buildSequence();
        ks.setParent(rig.model, -1);
        dirty = false;
        return ks;
    }

    private static int enumVal(Object o) {
        return o instanceof EnumValue ? ((EnumValue) o).getValue() : 0;
    }

    private static float num(Object o, float def) {
        return o instanceof Number ? ((Number) o).floatValue() : def;
    }

    private void walkPoses(Instance parent, int tick) {
        for (Instance c : parent.getChildren()) {
            if (!"Pose".equals(c.getClassName())) continue;
            if (num(c.get("Weight"), 1.0f) > 0.0f && rig.indexOf(c.getName()) >= 0) {
                Object cf = c.get("CFrame");
                track(c.getName()).put(tick, new Key(cf instanceof CFrame ? (CFrame) cf : CFrame.INSTANCE.getIDENTITY(),
                        enumVal(c.get("EasingStyle")), enumVal(c.get("EasingDirection"))));
            }
            walkPoses(c, tick);
        }
    }

    public void importSequence(Instance ks) {
        pushUndo();
        for (TreeMap<Integer, Key> t : tracks.values()) t.clear();
        name = ks.getName();
        loop = !Boolean.FALSE.equals(ks.get("Loop"));
        int pv = enumVal(ks.get("Priority"));
        for (int i = 0; i < PRIORITY_VALUES.length; i++) if (PRIORITY_VALUES[i] == pv) priorityIdx = i;
        int maxTick = 0;
        for (Instance kf : ks.getChildren()) {
            if (!"Keyframe".equals(kf.getClassName())) continue;
            int tick = snapExact(num(kf.get("Time"), 0.0f));
            maxTick = Math.max(maxTick, tick);
            walkPoses(kf, tick);
        }
        lengthTicks = Math.max(maxTick, TICKS / 4);
        curTick = 0;
        live.clear();
        dirty = false;
    }

    private static int snapExact(float seconds) {
        return Math.max(0, Math.round(seconds * TICKS));
    }

    /** Import a downloaded animation (by asset id): sample at every source key time. */
    public void importClip(AnimClip clip, String label) {
        pushUndo();
        for (TreeMap<Integer, Key> t : tracks.values()) t.clear();
        name = label;
        loop = clip.getLooped();
        int maxTick = 0;
        for (String jn : clip.getJointNames()) {
            if (rig.indexOf(jn) < 0) continue;
            AnimClip.Key[] keys = clip.keysOf(jn);
            if (keys == null) continue;
            for (AnimClip.Key k : keys) {
                int tick = snapExact(k.getT());
                maxTick = Math.max(maxTick, tick);
                track(jn).put(tick, new Key(k.getCf(), k.getStyle(), k.getDir()));
            }
        }
        lengthTicks = Math.max(maxTick, snapExact(clip.getLength()));
        curTick = 0;
        live.clear();
        dirty = false;
    }

    public static Vector3 v3(float x, float y, float z) {
        return new Vector3(x, y, z);
    }
}
