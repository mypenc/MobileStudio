package com.goydevv.studrblx.engine;

import com.goydevv.studrblx.model.CFrame;
import com.goydevv.studrblx.model.EnumValue;
import com.goydevv.studrblx.model.Instance;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import kotlin.collections.CollectionsKt;
import kotlin.collections.MapsKt;
import kotlin.comparisons.ComparisonsKt;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: KeyframeAnim.kt */
/* JADX INFO: loaded from: classes7.dex */
public final class AnimClip {

    /* JADX INFO: renamed from: Companion, reason: from kotlin metadata */
    public static final Companion INSTANCE = new Companion(null);
    private final Map<String, Key[]> joints;
    private final float length;
    private final boolean looped;
    private final int priority;

    /* JADX INFO: compiled from: KeyframeAnim.kt */
    public static final class Key {
        private final CFrame cf;
        private final int dir;
        private final int style;
        private final float t;

        public Key(float t, CFrame cf, int style, int dir) {
            Intrinsics.checkNotNullParameter(cf, "cf");
            this.t = t;
            this.cf = cf;
            this.style = style;
            this.dir = dir;
        }

        public final CFrame getCf() {
            return this.cf;
        }

        public final int getDir() {
            return this.dir;
        }

        public final int getStyle() {
            return this.style;
        }

        public final float getT() {
            return this.t;
        }
    }

    public AnimClip(float length, boolean looped, int priority, Map<String, Key[]> joints) {
        Intrinsics.checkNotNullParameter(joints, "joints");
        this.length = length;
        this.looped = looped;
        this.priority = priority;
        this.joints = joints;
    }

    public final float getLength() {
        return this.length;
    }

    public final boolean getLooped() {
        return this.looped;
    }

    public final int getPriority() {
        return this.priority;
    }

    public final Key[] keysOf(String joint) {
        return this.joints.get(joint);
    }

    public final Set<String> getJointNames() {
        return this.joints.keySet();
    }

    public final CFrame sample(String joint, float t) {
        Intrinsics.checkNotNullParameter(joint, "joint");
        Key[] keys = this.joints.get(joint);
        if (keys == null) {
            return null;
        }
        if (keys.length == 1 || t <= keys[0].getT()) {
            return keys[0].getCf();
        }
        Key last = keys[keys.length - 1];
        if (t >= last.getT()) {
            return last.getCf();
        }
        int i = 0;
        while (i < keys.length - 2 && keys[i + 1].getT() <= t) {
            i++;
        }
        Key a = keys[i];
        Key b = keys[i + 1];
        float span = b.getT() - a.getT();
        return span <= 1.0E-5f ? b.getCf() : Quat.INSTANCE.lerp(a.getCf(), b.getCf(), INSTANCE.ease((t - a.getT()) / span, a.getStyle(), a.getDir()));
    }

    /* JADX INFO: compiled from: KeyframeAnim.kt */
    public static final class Companion {
        public /* synthetic */ Companion(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }

        private Companion() {
        }

        public final float ease(float x, int style, int dir0) {
            if (style == 0) {
                return x;
            }
            if (style == 1) {
                return 0.0f;
            }
            int dir = (style == 5 || dir0 == 2) ? dir0 : 1 - dir0;
            switch (dir) {
                case 0:
                    return ease$inF(style, x);
                case 1:
                    return 1.0f - ease$inF(style, 1.0f - x);
                default:
                    return x < 0.5f ? ease$inF(style, x * 2.0f) / 2.0f : 1.0f - (ease$inF(style, (1.0f - x) * 2.0f) / 2.0f);
            }
        }

        private static final float ease$inF(int $style, float t) {
            switch ($style) {
                case 2:
                    return (t <= 0.0f || t >= 1.0f) ? t : (-((float) Math.pow(2.0d, (t - 1.0f) * 10.0f))) * ((float) Math.sin(((t - 1.075f) * 6.2831855f) / 0.3f));
                case 3:
                default:
                    return t * t * t;
                case 4:
                    return 1.0f - AnimClip.INSTANCE.bounceOut(1.0f - t);
            }
        }

        private final float bounceOut(float t) {
            if (t < 0.36363637f) {
                return 7.5625f * t * t;
            }
            if (t < 0.72727275f) {
                float u = t - 0.54545456f;
                return (7.5625f * u * u) + 0.75f;
            }
            if (t < 0.90909094f) {
                float u2 = t - 0.8181818f;
                return (7.5625f * u2 * u2) + 0.9375f;
            }
            float u3 = t - 0.95454544f;
            return (7.5625f * u3 * u3) + 0.984375f;
        }

        public final AnimClip from(Instance seq) {
            Intrinsics.checkNotNullParameter(seq, "seq");
            if (!Intrinsics.areEqual(seq.getClassName(), "KeyframeSequence")) {
                return null;
            }
            HashMap lists = new HashMap();
            float length = 0.0f;
            Iterator<Instance> it = seq.getChildren().iterator();
            Intrinsics.checkNotNullExpressionValue(it, "iterator(...)");
            while (it.hasNext()) {
                Instance next = it.next();
                Intrinsics.checkNotNullExpressionValue(next, "next(...)");
                Instance kf = next;
                if (Intrinsics.areEqual(kf.getClassName(), "Keyframe")) {
                    Object obj = kf.get("Time");
                    Number number = obj instanceof Number ? (Number) obj : null;
                    float t = number != null ? number.floatValue() : 0.0f;
                    if (t > length) {
                        length = t;
                    }
                    from$walk(lists, t, kf);
                }
            }
            if (lists.isEmpty()) {
                return null;
            }
            HashMap map = lists;
            Map linkedHashMap = new LinkedHashMap(MapsKt.mapCapacity(map.size()));
            for (Object obj2 : map.entrySet()) {
                linkedHashMap.put(((Map.Entry) obj2).getKey(), (Key[]) CollectionsKt.sortedWith((ArrayList) ((Map.Entry) obj2).getValue(), new Comparator() { // from class: com.goydevv.studrblx.engine.AnimClip$Companion$from$lambda$2$$inlined$sortedBy$1
                    /* JADX WARN: Multi-variable type inference failed */
                    @Override // java.util.Comparator
                    public final int compare(Object t2, Object t3) {
                        return ComparisonsKt.compareValues(Float.valueOf(((AnimClip.Key) t2).getT()), Float.valueOf(((AnimClip.Key) t3).getT()));
                    }
                }).toArray(new Key[0]));
            }
            boolean z = !Intrinsics.areEqual(seq.get("Loop"), (Object) false);
            Object obj3 = seq.get("Priority");
            EnumValue enumValue = obj3 instanceof EnumValue ? (EnumValue) obj3 : null;
            return new AnimClip(length, z, enumValue != null ? enumValue.getValue() : 0, linkedHashMap);
        }

        private static final void from$walk(HashMap<String, ArrayList<Key>> map, float t, Instance p) {
            ArrayList<Key> arrayList;
            Iterator<Instance> it = p.getChildren().iterator();
            Intrinsics.checkNotNullExpressionValue(it, "iterator(...)");
            while (it.hasNext()) {
                Instance next = it.next();
                Intrinsics.checkNotNullExpressionValue(next, "next(...)");
                Instance c = next;
                if (Intrinsics.areEqual(c.getClassName(), "Pose")) {
                    Object obj = c.get("Weight");
                    Number number = obj instanceof Number ? (Number) obj : null;
                    float w = number != null ? number.floatValue() : 1.0f;
                    if (w > 0.0f) {
                        Object obj2 = c.get("CFrame");
                        CFrame cf = obj2 instanceof CFrame ? (CFrame) obj2 : null;
                        if (cf == null) {
                            cf = CFrame.INSTANCE.getIDENTITY();
                        }
                        Object obj3 = c.get("EasingStyle");
                        EnumValue enumValue = obj3 instanceof EnumValue ? (EnumValue) obj3 : null;
                        int st = enumValue != null ? enumValue.getValue() : 0;
                        Object obj4 = c.get("EasingDirection");
                        EnumValue enumValue2 = obj4 instanceof EnumValue ? (EnumValue) obj4 : null;
                        int dr = enumValue2 != null ? enumValue2.getValue() : 0;
                        HashMap<String, ArrayList<Key>> map2 = map;
                        String name = c.getName();
                        ArrayList<Key> arrayList2 = map2.get(name);
                        if (arrayList2 == null) {
                            arrayList = new ArrayList<>();
                            map2.put(name, arrayList);
                        } else {
                            arrayList = arrayList2;
                        }
                        arrayList.add(new Key(t, cf, st, dr));
                    }
                    from$walk(map, t, c);
                }
            }
        }
    }
}
