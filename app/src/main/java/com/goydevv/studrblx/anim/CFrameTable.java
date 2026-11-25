package com.goydevv.studrblx.anim;

import com.goydevv.studrblx.model.CFrame;
import com.goydevv.studrblx.model.Vector3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Lua-style CFrame animation tables, e.g.
 * return { Animation = { { Time = 0, Data = { ["Torso"] = CFrame.new(...), ... } }, ... } }
 * Supports CFrame.new (3 / 7 / 12 numbers), CFrame.Angles, products with '*', math.rad / math.pi.
 */
public final class CFrameTable {

    public static final class Frame {
        public float time;
        public final LinkedHashMap<String, CFrame> data = new LinkedHashMap<String, CFrame>();
    }

    private static final Pattern TIME = Pattern.compile("(?i)(?:\\[\\s*[\"']time[\"']\\s*\\]|\\btime\\b)\\s*=\\s*(-?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?)");
    private static final Pattern KEY = Pattern.compile("(?:\\[\\s*\"([^\"]+)\"\\s*\\]|\\[\\s*'([^']+)'\\s*\\]|\\b([A-Za-z_][A-Za-z0-9_]*))\\s*=\\s*(?=CFrame\\b)");

    private CFrameTable() {
    }

    public static List<Frame> parse(String src) {
        String text = src.replaceAll("--[^\\n]*", "");
        List<int[]> marks = new ArrayList<int[]>(); // start, end of each Time marker
        List<Float> times = new ArrayList<Float>();
        Matcher tm = TIME.matcher(text);
        while (tm.find()) {
            marks.add(new int[]{tm.start(), tm.end()});
            times.add(Float.parseFloat(tm.group(1)));
        }
        if (marks.isEmpty()) throw new IllegalArgumentException("No \"Time = ...\" entries found. Expected a table of frames with Time and CFrame data.");
        List<Frame> out = new ArrayList<Frame>();
        for (int i = 0; i < marks.size(); i++) {
            int from = marks.get(i)[1];
            int to = i + 1 < marks.size() ? marks.get(i + 1)[0] : text.length();
            Frame f = new Frame();
            f.time = times.get(i);
            Matcher km = KEY.matcher(text.substring(from, to));
            String seg = text.substring(from, to);
            while (km.find()) {
                String name = km.group(1) != null ? km.group(1) : (km.group(2) != null ? km.group(2) : km.group(3));
                if (name.equalsIgnoreCase("Time") || name.equalsIgnoreCase("Data")) continue;
                int start = km.end();
                int depth = 0;
                int e = start;
                for (; e < seg.length(); e++) {
                    char c = seg.charAt(e);
                    if (c == '(') depth++;
                    else if (c == ')') depth--;
                    else if (depth == 0 && (c == ',' || c == '}' || c == '\n')) break;
                }
                try {
                    f.data.put(name, evalCFrame(seg.substring(start, e)));
                } catch (RuntimeException ex) {
                    // skip entries we cannot evaluate
                }
                km.region(Math.min(e, seg.length()), seg.length());
            }
            if (!f.data.isEmpty()) out.add(f);
        }
        if (out.isEmpty()) throw new IllegalArgumentException("Found Time entries but no readable CFrame values.");
        Collections.sort(out, new Comparator<Frame>() {
            @Override
            public int compare(Frame a, Frame b) {
                return Float.compare(a.time, b.time);
            }
        });
        return out;
    }

    // ---------------------------------------------------------------- expression evaluation
    private static List<String> splitTop(String s, char sep) {
        List<String> parts = new ArrayList<String>();
        int depth = 0;
        int last = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == sep && depth == 0) {
                parts.add(s.substring(last, i));
                last = i + 1;
            }
        }
        parts.add(s.substring(last));
        return parts;
    }

    static CFrame evalCFrame(String expr) {
        CFrame acc = null;
        for (String term : splitTop(expr, '*')) {
            CFrame c = evalTerm(term.trim());
            acc = acc == null ? c : acc.times(c);
        }
        if (acc == null) throw new IllegalArgumentException("empty");
        return acc;
    }

    private static CFrame evalTerm(String t) {
        if (t.equals("CFrame.identity")) return CFrame.INSTANCE.getIDENTITY();
        int open = t.indexOf('(');
        int close = t.lastIndexOf(')');
        if (!t.startsWith("CFrame.") || open < 0 || close < open) throw new IllegalArgumentException(t);
        String fn = t.substring("CFrame.".length(), open).trim();
        List<String> raw = splitTop(t.substring(open + 1, close), ',');
        List<Float> a = new ArrayList<Float>();
        for (String r : raw) {
            if (r.trim().length() > 0) a.add(Num.eval(r));
        }
        if (fn.equals("new")) {
            if (a.size() == 3) return CFrame.INSTANCE.at(new Vector3(a.get(0), a.get(1), a.get(2)));
            if (a.size() == 12) {
                return new CFrame(a.get(0), a.get(1), a.get(2), a.get(3), a.get(4), a.get(5), a.get(6), a.get(7), a.get(8), a.get(9), a.get(10), a.get(11));
            }
            if (a.size() == 7) return fromQuat(a.get(0), a.get(1), a.get(2), a.get(3), a.get(4), a.get(5), a.get(6));
            throw new IllegalArgumentException("CFrame.new args: " + a.size());
        }
        if ((fn.equals("Angles") || fn.equals("fromEulerAnglesXYZ")) && a.size() == 3) return CFrame.INSTANCE.angles(a.get(0), a.get(1), a.get(2));
        throw new IllegalArgumentException(fn);
    }

    private static CFrame fromQuat(float x, float y, float z, float qx, float qy, float qz, float qw) {
        float n = (float) Math.sqrt(qx * qx + qy * qy + qz * qz + qw * qw);
        if (n == 0) return CFrame.INSTANCE.at(new Vector3(x, y, z));
        qx /= n;
        qy /= n;
        qz /= n;
        qw /= n;
        return new CFrame(x, y, z,
                1 - 2 * (qy * qy + qz * qz), 2 * (qx * qy - qz * qw), 2 * (qx * qz + qy * qw),
                2 * (qx * qy + qz * qw), 1 - 2 * (qx * qx + qz * qz), 2 * (qy * qz - qx * qw),
                2 * (qx * qz - qy * qw), 2 * (qy * qz + qx * qw), 1 - 2 * (qx * qx + qy * qy));
    }

    /** Tiny arithmetic evaluator: numbers, + - * /, parentheses, math.pi, math.rad(x). */
    private static final class Num {
        private final String s;
        private int i = 0;

        private Num(String s) {
            this.s = s.replace(" ", "").replace("\t", "").replace("\r", "");
        }

        static float eval(String s) {
            Num n = new Num(s);
            float v = n.expr();
            if (n.i != n.s.length()) throw new IllegalArgumentException(s);
            return v;
        }

        private float expr() {
            float v = term();
            while (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
                char o = s.charAt(i++);
                float r = term();
                v = o == '+' ? v + r : v - r;
            }
            return v;
        }

        private float term() {
            float v = unary();
            while (i < s.length() && (s.charAt(i) == '*' || s.charAt(i) == '/')) {
                char o = s.charAt(i++);
                float r = unary();
                v = o == '*' ? v * r : v / r;
            }
            return v;
        }

        private float unary() {
            if (i < s.length() && s.charAt(i) == '-') {
                i++;
                return -unary();
            }
            if (i < s.length() && s.charAt(i) == '+') {
                i++;
                return unary();
            }
            return atom();
        }

        private float atom() {
            if (s.startsWith("(", i)) {
                i++;
                float v = expr();
                if (!s.startsWith(")", i)) throw new IllegalArgumentException(s);
                i++;
                return v;
            }
            if (s.startsWith("math.pi", i)) {
                i += 7;
                return (float) Math.PI;
            }
            if (s.startsWith("math.rad(", i)) {
                i += 9;
                float v = expr();
                if (!s.startsWith(")", i)) throw new IllegalArgumentException(s);
                i++;
                return (float) Math.toRadians(v);
            }
            int st = i;
            while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.'
                    || ((s.charAt(i) == 'e' || s.charAt(i) == 'E') && i > st)
                    || ((s.charAt(i) == '-' || s.charAt(i) == '+') && i > st && (s.charAt(i - 1) == 'e' || s.charAt(i - 1) == 'E')))) i++;
            if (st == i) throw new IllegalArgumentException(s);
            return Float.parseFloat(s.substring(st, i));
        }
    }
}
