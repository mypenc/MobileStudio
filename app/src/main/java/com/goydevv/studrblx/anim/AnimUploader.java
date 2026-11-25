package com.goydevv.studrblx.anim;

import android.content.Context;
import android.content.SharedPreferences;

import com.goydevv.studrblx.format.RbxBinaryWriter;
import com.goydevv.studrblx.model.Instance;
import com.goydevv.studrblx.net.Http;
import com.goydevv.studrblx.net.RobloxApi;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Uploads a KeyframeSequence as an Animation asset through the Roblox Open Cloud Assets API
 * (POST https://apis.roblox.com/assets/v1/assets, then poll operations/{id}).
 */
public final class AnimUploader {
    private static final String PREFS = "anim_upload";
    private static final Charset UTF8 = Charset.forName("UTF-8");

    public static final class Settings {
        public String apiKey = "";
        public boolean group = false;
        public long creatorId = 0;
    }

    public interface Callback {
        void onStatus(String msg);

        void onDone(String assetId);

        void onError(String msg);
    }

    private AnimUploader() {
    }

    public static Settings load(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Settings s = new Settings();
        s.apiKey = sp.getString("key", "");
        if (s.apiKey.length() == 0) s.apiKey = RobloxApi.INSTANCE.apiKey(ctx); // reuse the publish key if one is set
        s.group = sp.getBoolean("group", false);
        s.creatorId = sp.getLong("creator", 0);
        if (s.creatorId == 0 && !s.group) s.creatorId = RobloxApi.INSTANCE.userId(ctx);
        return s;
    }

    public static void save(Context ctx, Settings s) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("key", s.apiKey).putBoolean("group", s.group).putLong("creator", s.creatorId).apply();
    }

    public static void upload(final Context ctx, final Instance sequence, final String displayName,
                              final String description, final Settings st, final Callback cb) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    cb.onStatus("Serialising animation...");
                    byte[] rbxm = RbxBinaryWriter.INSTANCE.write(Collections.singletonList(sequence),
                            new LinkedHashMap<String, String>());
                    if (rbxm == null || rbxm.length == 0) throw new Exception("Could not serialise the KeyframeSequence.");

                    String boundary = "----StudioRblx" + System.nanoTime();
                    JSONObject creator = new JSONObject().put(st.group ? "groupId" : "userId", Long.toString(st.creatorId));
                    JSONObject req = new JSONObject()
                            .put("assetType", "Animation")
                            .put("displayName", displayName)
                            .put("description", description)
                            .put("creationContext", new JSONObject().put("creator", creator));

                    ByteArrayOutputStream body = new ByteArrayOutputStream();
                    String nl = "\r\n";
                    body.write(("--" + boundary + nl + "Content-Disposition: form-data; name=\"request\"" + nl
                            + "Content-Type: application/json" + nl + nl + req.toString() + nl).getBytes(UTF8));
                    body.write(("--" + boundary + nl + "Content-Disposition: form-data; name=\"fileContent\"; filename=\""
                            + safe(displayName) + ".rbxm\"" + nl + "Content-Type: model/x-rbxm" + nl + nl).getBytes(UTF8));
                    body.write(rbxm);
                    body.write((nl + "--" + boundary + "--" + nl).getBytes(UTF8));

                    Map<String, String> headers = new HashMap<String, String>();
                    headers.put("x-api-key", st.apiKey);
                    cb.onStatus("Uploading " + (rbxm.length / 1024 + 1) + " KB...");
                    Http.Response r = Http.INSTANCE.call("https://apis.roblox.com/assets/v1/assets", "POST", headers,
                            body.toByteArray(), "multipart/form-data; boundary=" + boundary, 4 * 1024 * 1024);
                    if (!r.getOk()) throw new Exception(explain(r.getCode(), r.getText()));

                    JSONObject op = new JSONObject(r.getText());
                    for (int i = 0; i < 40; i++) {
                        if (op.optBoolean("done", false)) break;
                        String path = op.optString("path", "");
                        if (path.length() == 0) break;
                        cb.onStatus("Waiting for Roblox to process... (" + (i + 1) + ")");
                        Thread.sleep(1500);
                        Http.Response pr = Http.INSTANCE.call("https://apis.roblox.com/assets/v1/" + path, "GET", headers,
                                null, null, 1024 * 1024);
                        if (!pr.getOk()) throw new Exception(explain(pr.getCode(), pr.getText()));
                        op = new JSONObject(pr.getText());
                    }
                    if (!op.optBoolean("done", false)) {
                        throw new Exception("Roblox is still processing the upload. Check your Creator Hub inventory in a minute.");
                    }
                    if (op.has("error")) {
                        throw new Exception(op.getJSONObject("error").optString("message", "Upload rejected."));
                    }
                    JSONObject resp = op.optJSONObject("response");
                    String id = resp == null ? "" : resp.optString("assetId", "");
                    if (id.length() == 0) throw new Exception("Upload finished but no assetId was returned.");
                    cb.onDone(id);
                } catch (Throwable t) {
                    cb.onError(t.getMessage() == null ? t.toString() : t.getMessage());
                }
            }
        }, "anim-upload").start();
    }

    private static String safe(String s) {
        String o = s.replaceAll("[^A-Za-z0-9_\\- ]", "_");
        return o.length() == 0 ? "Animation" : o;
    }

    private static String explain(int code, String bodyText) {
        String detail = bodyText == null ? "" : bodyText;
        try {
            JSONObject j = new JSONObject(detail);
            detail = j.optString("message", detail);
        } catch (Throwable ignored) {
        }
        if (detail.length() > 300) detail = detail.substring(0, 300);
        switch (code) {
            case 401:
            case 403:
                return "HTTP " + code + ": the API key was rejected. It needs the 'assets' write permission and must be owned by the same user/group you upload for. " + detail;
            case 400:
                return "HTTP 400: Roblox refused the animation. " + detail;
            case 429:
                return "HTTP 429: rate limited, wait a bit and try again.";
            default:
                return "HTTP " + code + ": " + detail;
        }
    }
}
