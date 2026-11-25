package com.goydevv.studrblx.anim;

import com.goydevv.studrblx.model.CFrame;
import com.goydevv.studrblx.model.DataModel;
import com.goydevv.studrblx.model.Instance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * A "valid rig" for the animation editor: a Model whose parts are connected by Motor6D joints.
 * Poses in a KeyframeSequence are named after the joint's Part1 (this matches Roblox's format).
 */
public final class RigInfo {

    public static final class Joint {
        public final Instance motor;
        public final Instance p0;
        public final Instance p1;
        public final CFrame c0;
        public final CFrame c1;
        public final CFrame c1Inv;
        /** Pose name == Part1 name. */
        public final String name;
        public final int parent;
        public final int depth;

        Joint(Instance motor, Instance p0, Instance p1, CFrame c0, CFrame c1, int parent, int depth) {
            this.motor = motor;
            this.p0 = p0;
            this.p1 = p1;
            this.c0 = c0;
            this.c1 = c1;
            this.c1Inv = c1.inverse();
            this.name = p1.getName();
            this.parent = parent;
            this.depth = depth;
        }
    }

    public final Instance model;
    public final Instance rootPart;
    public final List<Joint> joints;

    private RigInfo(Instance model, Instance rootPart, List<Joint> joints) {
        this.model = model;
        this.rootPart = rootPart;
        this.joints = joints;
    }

    public String displayName() {
        return model.getName();
    }

    public int indexOf(String jointName) {
        for (int i = 0; i < joints.size(); i++) {
            if (joints.get(i).name.equals(jointName)) return i;
        }
        return -1;
    }

    public static String norm(String n) {
        return n == null ? "" : n.toLowerCase(java.util.Locale.US).replaceAll("[^a-z0-9]", "");
    }

    /** Match an animation's bone name to a joint: Part1 name first, then Motor6D name, ignoring case/spaces. */
    public int match(String key) {
        int i = indexOf(key);
        if (i >= 0) return i;
        String k = norm(key);
        for (int a = 0; a < joints.size(); a++) if (norm(joints.get(a).name).equals(k)) return a;
        for (int a = 0; a < joints.size(); a++) if (norm(joints.get(a).motor.getName()).equals(k)) return a;
        return -1;
    }

    public int indexOfPart(Instance part) {
        for (int i = 0; i < joints.size(); i++) {
            if (joints.get(i).p1 == part) return i;
        }
        return -1;
    }

    private static int countMotors(Instance m) {
        int n = 0;
        for (Instance d : m.descendants(new ArrayList<Instance>())) {
            if ("Motor6D".equals(d.getClassName())) n++;
        }
        return n;
    }

    /** Finds the rig the given instance belongs to (or null when it isn't part of a valid rig). */
    public static RigInfo detect(Instance sel) {
        if (sel == null) return null;
        Instance best = null;
        Instance fallback = null;
        for (Instance i = sel; i != null && !(i instanceof DataModel); i = i.getParent()) {
            if (i.getIsService() || !i.isA("Model")) continue;
            int motors = countMotors(i);
            if (motors < 1) continue;
            boolean animatable = i.findFirstChildOfClass("Humanoid") != null
                    || i.findFirstChildOfClass("AnimationController") != null;
            if (animatable) best = i; // keep climbing: the outermost animatable model wins
            if (fallback == null && motors >= 2) fallback = i;
        }
        Instance model = best != null ? best : fallback;
        return model == null ? null : build(model);
    }

    private static boolean isPart(Instance i) {
        return i != null && i.isA("BasePart");
    }

    private static CFrame cf(Instance i, String prop) {
        Object o = i.get(prop);
        return o instanceof CFrame ? (CFrame) o : CFrame.INSTANCE.getIDENTITY();
    }

    static RigInfo build(Instance model) {
        List<Instance> motors = new ArrayList<Instance>();
        for (Instance d : model.descendants(new ArrayList<Instance>())) {
            if (!"Motor6D".equals(d.getClassName())) continue;
            Object a = d.get("Part0");
            Object b = d.get("Part1");
            if (a instanceof Instance && b instanceof Instance && isPart((Instance) a) && isPart((Instance) b)) {
                Instance p0 = (Instance) a;
                Instance p1 = (Instance) b;
                if (p0.isDescendantOf(model) && p1.isDescendantOf(model)) motors.add(d);
            }
        }
        if (motors.isEmpty()) return null;

        Set<Instance> p1s = java.util.Collections.newSetFromMap(new IdentityHashMap<Instance, Boolean>());
        for (Instance m : motors) p1s.add((Instance) m.get("Part1"));

        Instance root = null;
        for (Instance m : motors) {
            Instance p0 = (Instance) m.get("Part0");
            if (p1s.contains(p0)) continue;
            if (root == null || "HumanoidRootPart".equals(p0.getName())) root = p0;
        }
        if (root == null) return null; // cyclic joints: not a rig we can solve

        List<Joint> out = new ArrayList<Joint>();
        Set<String> names = new HashSet<String>();
        IdentityHashMap<Instance, Integer> jointOfPart = new IdentityHashMap<Instance, Integer>();
        List<Instance> queue = new ArrayList<Instance>();
        queue.add(root);
        Set<Instance> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<Instance, Boolean>());
        seen.add(root);
        for (int qi = 0; qi < queue.size(); qi++) {
            Instance part = queue.get(qi);
            Integer pj = jointOfPart.get(part);
            int parentIdx = pj == null ? -1 : pj;
            int depth = pj == null ? 0 : out.get(pj).depth + 1;
            for (Instance m : motors) {
                if (m.get("Part0") != part) continue;
                Instance p1 = (Instance) m.get("Part1");
                if (seen.contains(p1) || !names.add(p1.getName())) continue;
                seen.add(p1);
                Joint j = new Joint(m, part, p1, cf(m, "C0"), cf(m, "C1"), parentIdx, depth);
                jointOfPart.put(p1, out.size());
                out.add(j);
                queue.add(p1);
            }
        }
        return out.isEmpty() ? null : new RigInfo(model, root, out);
    }
}
