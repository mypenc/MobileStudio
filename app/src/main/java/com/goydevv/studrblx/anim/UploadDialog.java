package com.goydevv.studrblx.anim;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

/** Shared "Upload to Roblox" dialog (Open Cloud Assets API) for models and other .rbxm assets. */
public final class UploadDialog {
    private UploadDialog() {
    }

    public static void show(final Activity act, final String assetType, final String name, final byte[] rbxm) {
        if (rbxm == null || rbxm.length == 0) {
            Toast.makeText(act, "Nothing to upload.", Toast.LENGTH_SHORT).show();
            return;
        }
        final float d = act.getResources().getDisplayMetrics().density;
        final AnimUploader.Settings st = AnimUploader.load(act);

        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding((int) (16 * d), (int) (4 * d), (int) (16 * d), 0);

        final EditText title = new EditText(act);
        title.setHint("Name");
        title.setSingleLine(true);
        title.setText(name);
        col.addView(title);
        final EditText key = new EditText(act);
        key.setHint("Open Cloud API key (Assets: write)");
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
        desc.setText("Uploaded from StudioRblx");
        col.addView(desc);
        TextView note = new TextView(act);
        note.setText("Size: " + (rbxm.length / 1024 + 1) + " KB. The key needs the Assets API permission and must belong to the same user/group.");
        note.setTextSize(11f);
        col.addView(note);

        new AlertDialog.Builder(act).setTitle("Upload to Roblox (" + assetType + ")").setView(col)
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
                        String n = title.getText().toString().trim();
                        if (n.length() == 0) n = name;
                        if (st.apiKey.length() == 0 || st.creatorId == 0) {
                            Toast.makeText(act, "API key and creator ID are required.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        AnimUploader.save(act, st);
                        run(act, assetType, n, desc.getText().toString().trim(), rbxm, st);
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private static void run(final Activity act, String assetType, String name, String description, byte[] rbxm, AnimUploader.Settings st) {
        final AlertDialog progress = new AlertDialog.Builder(act).setTitle("Uploading").setMessage("Starting...")
                .setCancelable(false).create();
        progress.show();
        AnimUploader.uploadBytes(act, rbxm, assetType, name, description, st, new AnimUploader.Callback() {
            @Override
            public void onStatus(final String msg) {
                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.setMessage(msg);
                    }
                });
            }

            @Override
            public void onDone(final String assetId) {
                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.dismiss();
                        done(act, assetId);
                    }
                });
            }

            @Override
            public void onError(final String msg) {
                act.runOnUiThread(new Runnable() {
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

    private static void done(final Activity act, final String assetId) {
        new AlertDialog.Builder(act).setTitle("Uploaded to Roblox!")
                .setMessage("Asset ID: " + assetId + "\n\nIt is private to your account/group until you put it on sale or share it in Creator Hub.")
                .setPositiveButton("Copy ID", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ClipboardManager cm = (ClipboardManager) act.getSystemService(Context.CLIPBOARD_SERVICE);
                        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Asset ID", assetId));
                        Toast.makeText(act, "Copied " + assetId, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Open in Creator Hub", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            act.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://create.roblox.com/store/asset/" + assetId)));
                        } catch (Throwable ignored) {
                        }
                    }
                })
                .setNegativeButton("Close", null).show();
    }
}
