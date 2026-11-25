package com.goydevv.studrblx.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.goydevv.studrblx.core.OutputLog;
import com.goydevv.studrblx.core.Studio;
import com.goydevv.studrblx.engine.Avatar;
import com.goydevv.studrblx.engine.Geometry;
import com.goydevv.studrblx.engine.PlaySession;
import com.goydevv.studrblx.model.CFrame;
import com.goydevv.studrblx.model.DataTypesKt;
import com.goydevv.studrblx.model.EnumValue;
import com.goydevv.studrblx.model.Instance;
import com.goydevv.studrblx.model.NumberRange;
import com.goydevv.studrblx.model.PartData;
import com.goydevv.studrblx.model.Vector3;
import com.goydevv.studrblx.net.AuthRequiredException;
import com.goydevv.studrblx.net.CloudPlace;
import com.goydevv.studrblx.net.HttpException;
import com.goydevv.studrblx.net.RobloxApi;
import com.goydevv.studrblx.net.RobloxCloud;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import kotlin.Pair;
import kotlin.TuplesKt;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.collections.MapsKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Ref;
import kotlin.ranges.RangesKt;
import kotlin.text.StringsKt;
import org.json.JSONException;

/* JADX INFO: compiled from: AvatarUi.kt */
/* JADX INFO: loaded from: classes5.dex */
public final class AvatarUi {
    public static final AvatarUi INSTANCE = new AvatarUi();
    private static final ExecutorService net = Executors.newSingleThreadExecutor();
    private static final List<Pair<String, String>> scaleKeys = CollectionsKt.listOf(new Pair[]{TuplesKt.to("Height", "height"), TuplesKt.to("Width", "width"), TuplesKt.to("Head", "head"), TuplesKt.to("BodyType", "bodyType"), TuplesKt.to("Proportion", "proportion")});
    private static final Map<String, String> scaleLabels = MapsKt.mapOf(TuplesKt.to("Height", "Height"), TuplesKt.to("Width", "Width"), TuplesKt.to("Head", "Head"), TuplesKt.to("BodyType", "Body type"), TuplesKt.to("Proportion", "Proportions"));
    private static final Map<String, NumberRange> scaleDefaults = MapsKt.mapOf(TuplesKt.to("Height", new NumberRange(0.9f, 1.05f)), TuplesKt.to("Width", new NumberRange(0.7f, 1.0f)), TuplesKt.to("Head", new NumberRange(0.95f, 1.0f)), TuplesKt.to("BodyType", new NumberRange(0.0f, 1.0f)), TuplesKt.to("Proportion", new NumberRange(0.0f, 1.0f)));

    private AvatarUi() {
    }

    private final void toast(Activity act, String s) {
        Toast.makeText(act, s, 0).show();
    }

    private final int dp(Activity act, float v) {
        return Ui.INSTANCE.dp(act, v);
    }

    private final View optionRow(Activity act, String icon, String title, String sub, boolean current, final Function0<Unit> onClick) {
        int card;
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(0);
        row.setGravity(16);
        row.setPadding(INSTANCE.dp(act, 10.0f), INSTANCE.dp(act, 9.0f), INSTANCE.dp(act, 12.0f), INSTANCE.dp(act, 9.0f));
        Ui ui = Ui.INSTANCE;
        Activity activity = act;
        Ui ui2 = Ui.INSTANCE;
        if (current) {
            card = ui2.blend(Ui.INSTANCE.getPANEL(), Ui.INSTANCE.getACCENT(), Ui.INSTANCE.getLIGHT() ? 0.14f : 0.28f);
        } else {
            card = ui2.getCARD();
        }
        row.setBackground(ui.rippleBg(activity, card, 10.0f));
        row.setClickable(true);
        row.setOnClickListener(new View.OnClickListener() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda21
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                onClick.invoke();
            }
        });
        ImageView imageView = new ImageView(act);
        imageView.setImageDrawable(Icons.general$default(Icons.INSTANCE, act, icon, null, 4, null));
        LinearLayout.LayoutParams layoutParamsLp$default = Ui.lp$default(Ui.INSTANCE, dp(act, 28.0f), dp(act, 28.0f), 0.0f, 4, null);
        layoutParamsLp$default.rightMargin = INSTANCE.dp(act, 12.0f);
        Unit unit = Unit.INSTANCE;
        row.addView(imageView, layoutParamsLp$default);
        LinearLayout t = new LinearLayout(act);
        t.setOrientation(1);
        t.addView(Ui.INSTANCE.text(act, title, 14.0f, Ui.INSTANCE.getTEXT(), true));
        TextView textViewText$default = Ui.text$default(Ui.INSTANCE, act, sub, 11.5f, Ui.INSTANCE.getDIM(), false, 16, null);
        textViewText$default.setSingleLine(false);
        t.addView(textViewText$default);
        row.addView(t, Ui.INSTANCE.lp(0, -2, 1.0f));
        if (current) {
            row.addView(Ui.INSTANCE.text(act, "✓", 17.0f, Ui.INSTANCE.getLIGHT() ? Ui.INSTANCE.getACCENT() : Ui.INSTANCE.blend(Ui.INSTANCE.getACCENT(), -1, 0.4f), true));
        }
        return row;
    }

    /* JADX WARN: Type inference failed for: r4v31, types: [T, android.app.AlertDialog] */
    public final void playtestAvatar(final Activity act, final Studio studio) {
        String str;
        String str2;
        Intrinsics.checkNotNullParameter(act, "act");
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(1);
        box.setPadding(INSTANCE.dp(act, 16.0f), INSTANCE.dp(act, 8.0f), INSTANCE.dp(act, 16.0f), INSTANCE.dp(act, 4.0f));
        Ref.ObjectRef dlg = new Ref.ObjectRef();
        Avatar.Mode mode = Avatar.INSTANCE.mode(act);
        final boolean guest = RobloxApi.INSTANCE.isGuest(act);
        if (guest) {
            str = "Sign in first to wear your avatar";
        } else {
            str = "@" + RobloxApi.INSTANCE.userName(act) + ", as it looks on Roblox";
        }
        playtestAvatar$add(box, act, dlg, "Account", "My Roblox avatar", str, mode == Avatar.Mode.ACCOUNT, new Function0() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda15
            @Override // kotlin.jvm.functions.Function0
            public final Object invoke() {
                return AvatarUi.playtestAvatar$lambda$10(guest, act);
            }
        });
        final String other = Avatar.INSTANCE.otherUser(act).getSecond();
        playtestAvatar$add(box, act, dlg, "Shared", "Another user's avatar…", other.length() > 0 ? "Now: @" + other : "Wear any Roblox user's avatar", mode == Avatar.Mode.USER, new Function0() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda16
            @Override // kotlin.jvm.functions.Function0
            public final Object invoke() {
                return AvatarUi.playtestAvatar$lambda$14(act, other);
            }
        });
        playtestAvatar$add(box, act, dlg, "Color", "Create my own…", "Body colors, clothing, face and accessories", mode == Avatar.Mode.CUSTOM, new Function0() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda17
            @Override // kotlin.jvm.functions.Function0
            public final Object invoke() {
                return AvatarUi.playtestAvatar$lambda$15(act);
            }
        });
        playtestAvatar$add(box, act, dlg, "Part_Block", "Classic", "The yellow, blue and green blocky avatar", mode == Avatar.Mode.CLASSIC, new Function0() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda18
            @Override // kotlin.jvm.functions.Function0
            public final Object invoke() {
                return AvatarUi.playtestAvatar$lambda$16(act);
            }
        });
        String charName = Avatar.INSTANCE.characterName(act);
        if (Avatar.INSTANCE.hasCharacter(act)) {
            String str3 = charName;
            if (str3.length() == 0) {
                str3 = "Saved model";
            }
            str2 = "“" + ((Object) str3) + "” — you spawn as it in every place";
        } else {
            str2 = "Play as any model or part, in every place you edit";
        }
        playtestAvatar$add(box, act, dlg, "Model", "Custom character (model or part)…", str2, mode == Avatar.Mode.CHARACTER, new Function0() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda19
            @Override // kotlin.jvm.functions.Function0
            public final Object invoke() {
                return AvatarUi.playtestAvatar$lambda$18(act, studio);
            }
        });
        TextView textViewText$default = Ui.text$default(Ui.INSTANCE, act, "Whether your avatar is R6 or R15 in a playtest follows each place's Game Settings → Avatar, like on Roblox.", 11.5f, Ui.INSTANCE.getDIM(), false, 16, null);
        textViewText$default.setSingleLine(false);
        textViewText$default.setPadding(INSTANCE.dp(act, 2.0f), INSTANCE.dp(act, 6.0f), 0, INSTANCE.dp(act, 4.0f));
        box.addView(textViewText$default);
        AlertDialog.Builder title = Dialogs.INSTANCE.themed(act).setTitle("Playtest Avatar");
        ScrollView scrollView = new ScrollView(act);
        scrollView.addView(box);
        AlertDialog.Builder b = title.setView(scrollView).setNegativeButton("Close", (DialogInterface.OnClickListener) null);
        if (studio != null) {
            b.setNeutralButton("Game Settings…", new DialogInterface.OnClickListener() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda20
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    try {
                        AvatarUi.INSTANCE.gameSettings(act, studio);
                    } catch (Throwable _t) {
                        if (_t instanceof RuntimeException) throw (RuntimeException) _t;
                        if (_t instanceof Error) throw (Error) _t;
                        throw new RuntimeException(_t);
                    }
                }
            });
        }
        dlg.element = b.show();
    }

    private static final void playtestAvatar$add(LinearLayout box, Activity $act, final Ref.ObjectRef<AlertDialog> objectRef, String icon, String title, String sub, boolean current, final Function0<Unit> function0) {
        View viewOptionRow = INSTANCE.optionRow($act, icon, title, sub, current, new Function0() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda12
            @Override // kotlin.jvm.functions.Function0
            public final Object invoke() {
                return AvatarUi.playtestAvatar$add$lambda$7(objectRef, function0);
            }
        });
        LinearLayout.LayoutParams layoutParamsLp$default = Ui.lp$default(Ui.INSTANCE, -1, -2, 0.0f, 4, null);
        layoutParamsLp$default.bottomMargin = INSTANCE.dp($act, 6.0f);
        Unit unit = Unit.INSTANCE;
        box.addView(viewOptionRow, layoutParamsLp$default);
    }

    /* JADX WARN: Multi-variable type inference failed */
    static final Unit playtestAvatar$add$lambda$7(Ref.ObjectRef $dlg, Function0 $fn) {
        AlertDialog alertDialog = (AlertDialog) $dlg.element;
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
        $fn.invoke();
        return Unit.INSTANCE;
    }

    static final Unit playtestAvatar$lambda$10(boolean $guest, final Activity $act) {
        if ($guest) {
            AccountUi.INSTANCE.signIn($act, new Function0() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda5
                @Override // kotlin.jvm.functions.Function0
                public final Object invoke() {
                    return AvatarUi.playtestAvatar$lambda$10$lambda$9($act);
                }
            });
        } else {
            Avatar.setMode$default(Avatar.INSTANCE, $act, Avatar.Mode.ACCOUNT, 0L, null, 12, null);
            INSTANCE.toast($act, "Using your avatar");
        }
        return Unit.INSTANCE;
    }

    static final Unit playtestAvatar$lambda$10$lambda$9(Activity $act) {
        Avatar.setMode$default(Avatar.INSTANCE, $act, Avatar.Mode.ACCOUNT, 0L, null, 12, null);
        return Unit.INSTANCE;
    }

    static final Unit playtestAvatar$lambda$14(final Activity $act, String $other) {
        Dialogs.INSTANCE.prompt($act, "Roblox username", $other, new Function1() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda22
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                return AvatarUi.playtestAvatar$lambda$14$lambda$13($act, (String) obj);
            }
        });
        return Unit.INSTANCE;
    }

    static final Unit playtestAvatar$lambda$14$lambda$13(final Activity $act, final String name) {
        Intrinsics.checkNotNullParameter(name, "name");
        AccountUi.INSTANCE.background($act, "Looking up @" + name + "…", new Function0() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda7
            @Override // kotlin.jvm.functions.Function0
            public final Object invoke() {
                return AvatarUi.playtestAvatar$lambda$14$lambda$13$lambda$11(name);
            }
        }, new Function1() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda8
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                return AvatarUi.playtestAvatar$lambda$14$lambda$13$lambda$12($act, (RobloxCloud.User) obj);
            }
        });
        return Unit.INSTANCE;
    }

    static final RobloxCloud.User playtestAvatar$lambda$14$lambda$13$lambda$11(String $name) {
        try {
        RobloxCloud.User userUserByName = RobloxCloud.INSTANCE.userByName($name);
        if (userUserByName != null) {
            return userUserByName;
        }
        throw new Exception("No Roblox user named \"" + $name + "\".");
    }
        catch (Throwable _ktEx) {
            throw com.goydevv.studrblx.util.KtCompat.sneakyThrow(_ktEx);
        }
    }

    static final Unit playtestAvatar$lambda$14$lambda$13$lambda$12(Activity $act, RobloxCloud.User u) {
        Intrinsics.checkNotNullParameter(u, "u");
        Avatar.INSTANCE.setMode($act, Avatar.Mode.USER, u.getId(), u.getName());
        INSTANCE.toast($act, "Wearing @" + u.getName() + "'s avatar");
        return Unit.INSTANCE;
    }

    static final Unit playtestAvatar$lambda$15(Activity $act) {
        AccountUi.INSTANCE.customAvatar($act);
        return Unit.INSTANCE;
    }

    static final Unit playtestAvatar$lambda$16(Activity $act) {
        Avatar.setMode$default(Avatar.INSTANCE, $act, Avatar.Mode.CLASSIC, 0L, null, 12, null);
        INSTANCE.toast($act, "Using the classic avatar");
        return Unit.INSTANCE;
    }

    static final Unit playtestAvatar$lambda$18(Activity $act, Studio $studio) {
        INSTANCE.characterChooser($act, $studio);
        return Unit.INSTANCE;
    }

    public final void characterChooser(final Activity act, final Studio studio) {
        final Instance selected;
        Object next;
        Instance instance;
        Intrinsics.checkNotNullParameter(act, "act");
        final ArrayList items = new ArrayList();
        if (studio != null) {
            synchronized (studio.getLock()) {
                Iterator it = studio.getSelection().iterator();
                do {
                    if (!it.hasNext()) {
                        next = null;
                        break;
                    } else {
                        next = it.next();
                        instance = (Instance) next;
                    }
                } while (!((instance.isA("Model") || instance.isA("BasePart")) && !instance.getIsService()));
                selected = (Instance) next;
            }
        } else {
            selected = null;
        }
        if (studio != null && selected != null) {
            items.add(TuplesKt.to("Use the selection (" + selected.getName() + ")", new Function0() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda28
                @Override // kotlin.jvm.functions.Function0
                public final Object invoke() {
                    return AvatarUi.characterChooser$lambda$25(act, studio, selected);
                }
            }));
        }
        if (studio != null) {
            items.add(TuplesKt.to("Pick from Workspace…", new Function0() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda29
                @Override // kotlin.jvm.functions.Function0
                public final Object invoke() {
                    return AvatarUi.characterChooser$lambda$26(act, studio);
                }
            }));
        }
        if (Avatar.INSTANCE.hasCharacter(act)) {
            if (Avatar.INSTANCE.mode(act) != Avatar.Mode.CHARACTER) {
                String strCharacterName = Avatar.INSTANCE.characterName(act);
                if (strCharacterName.length() == 0) {
                    strCharacterName = "saved model";
                }
                items.add(TuplesKt.to("Use “" + ((Object) strCharacterName) + "” again", new Function0() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda30
                    @Override // kotlin.jvm.functions.Function0
                    public final Object invoke() {
                        return AvatarUi.characterChooser$lambda$28(act);
                    }
                }));
            }
            String strCharacterName2 = Avatar.INSTANCE.characterName(act);
            if (strCharacterName2.length() == 0) {
                strCharacterName2 = "saved model";
            }
            items.add(TuplesKt.to("Stop using “" + ((Object) strCharacterName2) + "”", new Function0() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda31
                @Override // kotlin.jvm.functions.Function0
                public final Object invoke() {
                    return AvatarUi.characterChooser$lambda$30(act);
                }
            }));
        }
        if (items.isEmpty()) {
            toast(act, "Open a place and select a model or part first");
            return;
        }
        AlertDialog.Builder title = Dialogs.INSTANCE.themed(act).setTitle("Custom character");
        ArrayList arrayList = items;
        Collection arrayList2 = new ArrayList(CollectionsKt.collectionSizeOrDefault(arrayList, 10));
        Iterator it2 = arrayList.iterator();
        while (it2.hasNext()) {
            arrayList2.add((String) ((Pair) it2.next()).getFirst());
        }
        title.setItems((CharSequence[]) ((List) arrayList2).toArray(new String[0]), new DialogInterface.OnClickListener() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda32
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                ((Function0) ((Pair) items.get(i)).getSecond()).invoke();
            }
        }).setNegativeButton("Cancel", (DialogInterface.OnClickListener) null).show();
    }

    static final Unit characterChooser$lambda$25(Activity $act, Studio $studio, Instance $selected) {
        INSTANCE.saveCharacter($act, $studio, $selected);
        return Unit.INSTANCE;
    }

    static final Unit characterChooser$lambda$26(Activity $act, Studio $studio) {
        INSTANCE.pickFromWorkspace($act, $studio);
        return Unit.INSTANCE;
    }

    static final Unit characterChooser$lambda$28(Activity $act) {
        Avatar.setMode$default(Avatar.INSTANCE, $act, Avatar.Mode.CHARACTER, 0L, null, 12, null);
        INSTANCE.toast($act, "Playing as " + Avatar.INSTANCE.characterName($act));
        return Unit.INSTANCE;
    }

    static final Unit characterChooser$lambda$30(Activity $act) {
        Avatar.INSTANCE.clearCharacter($act);
        INSTANCE.toast($act, "Back to your avatar");
        return Unit.INSTANCE;
    }

    private final void pickFromWorkspace(final Activity act, final Studio studio) {
        final ArrayList arrayList;
        synchronized (studio.getLock()) {
            Instance workspace = studio.getModel().getWorkspace();
            arrayList = new ArrayList();
            pickFromWorkspace$lambda$33$visit(arrayList, workspace, 0);
        }
        if (arrayList.isEmpty()) {
            toast(act, "Workspace has no models or parts");
            return;
        }
        AlertDialog.Builder title = Dialogs.INSTANCE.themed(act).setTitle("Play as…");
        ArrayList<Instance> arrayList2 = arrayList;
        Collection arrayList3 = new ArrayList(CollectionsKt.collectionSizeOrDefault(arrayList2, 10));
        for (Instance instance : arrayList2) {
            arrayList3.add((instance.isA("BasePart") ? "▫ " : "▣ ") + instance.getName());
        }
        title.setItems((CharSequence[]) ((List) arrayList3).toArray(new String[0]), new DialogInterface.OnClickListener() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda14
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                AvatarUi.pickFromWorkspace$lambda$35(act, studio, arrayList, dialogInterface, i);
            }
        }).setNegativeButton("Cancel", (DialogInterface.OnClickListener) null).show();
    }

    private static final void pickFromWorkspace$lambda$33$visit(ArrayList<Instance> arrayList, Instance parent, int depth) {
        Iterator<Instance> it = parent.getChildren().iterator();
        Intrinsics.checkNotNullExpressionValue(it, "iterator(...)");
        while (it.hasNext()) {
            Instance c = it.next();
            if (arrayList.size() >= 200) {
                return;
            }
            if (!Intrinsics.areEqual(c.getClassName(), "Terrain") && !Intrinsics.areEqual(c.getClassName(), "Camera")) {
                if (c.isA("Model") || c.isA("BasePart")) {
                    arrayList.add(c);
                } else if (Intrinsics.areEqual(c.getClassName(), "Folder") && depth < 2) {
                    pickFromWorkspace$lambda$33$visit(arrayList, c, depth + 1);
                }
            }
        }
    }

    static final void pickFromWorkspace$lambda$35(Activity $act, Studio $studio, ArrayList $list, DialogInterface dialogInterface, int i) {
        AvatarUi avatarUi = INSTANCE;
        Object obj = $list.get(i);
        Intrinsics.checkNotNullExpressionValue(obj, "get(...)");
        avatarUi.saveCharacter($act, $studio, (Instance) obj);
    }

    private final void saveCharacter(Activity act, Studio studio, Instance inst) {
        boolean hasParts;
        boolean z;
        synchronized (studio.getLock()) {
            hasParts = true;
            if (!inst.isA("BasePart")) {
                Iterable iterableDescendants$default = Instance.descendants$default(inst, null, 1, null);
                if (!(iterableDescendants$default instanceof Collection) || !((Collection) iterableDescendants$default).isEmpty()) {
                    Iterator it = iterableDescendants$default.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            z = false;
                            break;
                        } else if (((Instance) it.next()).isA("BasePart")) {
                            z = true;
                            break;
                        }
                    }
                } else {
                    z = false;
                }
                if (!z) {
                    hasParts = false;
                }
            }
        }
        if (!hasParts) {
            toast(act, inst.getName() + " has no parts to play as");
            return;
        }
        byte[] bytes = studio.serializeInstances(CollectionsKt.listOf(inst));
        if (bytes == null) {
            toast(act, "Couldn't save " + inst.getName());
            return;
        }
        Avatar.INSTANCE.setCharacter(act, bytes, inst.getName());
        OutputLog.INSTANCE.info("Playtest avatar: you'll spawn as " + inst.getName() + " in every place (File → Playtest Avatar to change)");
        toast(act, "You'll play as " + inst.getName() + " in every place");
    }

    /* JADX WARN: Multi-variable type inference failed */
    static /* synthetic */ Function0 segmented$default(AvatarUi avatarUi, Activity activity, LinearLayout linearLayout, List list, int i, Function1 function1, int i2, Object obj) {
        if ((i2 & 16) != 0) {
            function1 = new Function1() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda6
                @Override // kotlin.jvm.functions.Function1
                public final Object invoke(Object obj2) {
                    ((Integer) obj2).intValue();
                    return Unit.INSTANCE;
                }
            };
        }
        return avatarUi.segmented(activity, linearLayout, list, i, function1);
    }

    private final Function0<Integer> segmented(final Activity act, LinearLayout parent, List<String> options, int initial, final Function1<? super Integer, Unit> onChange) {
        final Ref.IntRef sel = new Ref.IntRef();
        sel.element = initial;
        LinearLayout linearLayout = new LinearLayout(act);
        linearLayout.setOrientation(0);
        final ArrayList views = new ArrayList();
        int i = 0;
        for (Object obj : options) {
            int i2 = i + 1;
            if (i < 0) {
                CollectionsKt.throwIndexOverflow();
            }
            final int i3 = i;
            TextView textViewText = Ui.INSTANCE.text(act, (String) obj, 13.0f, Ui.INSTANCE.getTEXT(), true);
            textViewText.setGravity(17);
            textViewText.setPadding(INSTANCE.dp(act, 12.0f), INSTANCE.dp(act, 8.0f), INSTANCE.dp(act, 12.0f), INSTANCE.dp(act, 8.0f));
            textViewText.setOnClickListener(new View.OnClickListener() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    AvatarUi.segmented$lambda$45$lambda$43$lambda$42(sel, i3, onChange, views, act, view);
                }
            });
            views.add(textViewText);
            TextView textView = textViewText;
            LinearLayout.LayoutParams layoutParamsLp = Ui.INSTANCE.lp(0, -2, 1.0f);
            layoutParamsLp.rightMargin = i3 < options.size() + (-1) ? INSTANCE.dp(act, 6.0f) : 0;
            Unit unit = Unit.INSTANCE;
            linearLayout.addView(textView, layoutParamsLp);
            i = i2;
        }
        segmented$paint(views, sel, act);
        parent.addView(linearLayout, Ui.lp$default(Ui.INSTANCE, -1, -2, 0.0f, 4, null));
        return new Function0() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda1
            @Override // kotlin.jvm.functions.Function0
            public final Object invoke() {
                return Integer.valueOf(sel.element);
            }
        };
    }

    private static final void segmented$paint(ArrayList<TextView> arrayList, Ref.IntRef sel, Activity $act) {
        int i = 0;
        for (Object obj : arrayList) {
            int i2 = i + 1;
            if (i < 0) {
                CollectionsKt.throwIndexOverflow();
            }
            TextView textView = (TextView) obj;
            boolean z = i == sel.element;
            textView.setTextColor(z ? -1 : Ui.INSTANCE.getTEXT());
            Ui ui = Ui.INSTANCE;
            Activity activity = $act;
            Ui ui2 = Ui.INSTANCE;
            textView.setBackground(ui.rippleBg(activity, z ? ui2.getACCENT() : ui2.getCARD(), 16.0f));
            i = i2;
        }
    }

    static final void segmented$lambda$45$lambda$43$lambda$42(Ref.IntRef $sel, int $i, Function1 $onChange, ArrayList $views, Activity $act, View it) {
        $sel.element = $i;
        segmented$paint($views, $sel, $act);
        $onChange.invoke(Integer.valueOf($i));
    }

    public final void gameSettings(final Activity act, final Studio studio) {
        try {
        Instance service;
        EnumValue enumValue;
        EnumValue enumValue2;
        boolean curLoad;
        LinearLayout loadRow = null;
        NumberRange r = null;
        Intrinsics.checkNotNullParameter(act, "act");
        Studio studio2 = studio;
        Intrinsics.checkNotNullParameter(studio2, "studio");
        if (studio2.isPlaying()) {
            toast(act, "Stop the playtest to change Game Settings");
            return;
        }
        synchronized (studio2.getLock()) {
            service = studio2.getModel().getService("StarterPlayer");
        }
        Instance sp = service;
        synchronized (studio2.getLock()) {
            try {
                Object obj = sp.get("GameSettingsAvatar");
                if (obj instanceof EnumValue) {
                    try {
                        enumValue = (EnumValue) obj;
                    } catch (Throwable th) {
                        throw th;
                    }
                } else {
                    enumValue = null;
                }
                int cur = enumValue != null ? enumValue.getValue() : 2;
                synchronized (studio2.getLock()) {
                    try {
                        Object obj2 = sp.get("GameSettingsR15Collision");
                        if (obj2 instanceof EnumValue) {
                            try {
                                enumValue2 = (EnumValue) obj2;
                            } catch (Throwable th2) {
                                Throwable th = th2;
                                throw th;
                            }
                        } else {
                            enumValue2 = null;
                        }
                        int curCollision = enumValue2 != null ? enumValue2.getValue() : 0;
                        synchronized (studio2.getLock()) {
                            try {
                                curLoad = !Intrinsics.areEqual(sp.get("LoadCharacterAppearance"), (Object) false);
                            } catch (Throwable th3) {
                                throw th3;
                            }
                        }
                        LinearLayout box = new LinearLayout(act);
                        box.setOrientation(1);
                        box.setPadding(INSTANCE.dp(act, 20.0f), INSTANCE.dp(act, 8.0f), INSTANCE.dp(act, 20.0f), INSTANCE.dp(act, 4.0f));
                        gameSettings$section(box, act, "Avatar Type", "Which body players get in this place. Player Choice uses each player's own setting.");
                        final LinearLayout linearLayout = new LinearLayout(act);
                        linearLayout.setOrientation(1);
                        final Function0<Integer> function0Segmented = segmented(act, box, CollectionsKt.listOf(new String[]{"R6", "R15", "Player Choice"}), cur, new Function1() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda2
                            @Override // kotlin.jvm.functions.Function1
                            public final Object invoke(Object obj3) {
                                return AvatarUi.gameSettings$lambda$55(linearLayout, ((Integer) obj3).intValue());
                            }
                        });
                        int cur2 = cur;
                        box.addView(linearLayout);
                        TextView textViewText = Ui.INSTANCE.text(act, "Collision", 14.0f, Ui.INSTANCE.getTEXT(), true);
                        textViewText.setPadding(0, INSTANCE.dp(act, 14.0f), 0, INSTANCE.dp(act, 8.0f));
                        linearLayout.addView(textViewText);
                        Function0 collisionOf = segmented$default(this, act, linearLayout, CollectionsKt.listOf(new String[]{"Outer Box", "Inner Box"}), curCollision, null, 16, null);
                        linearLayout.setVisibility(cur2 == 0 ? 8 : 0);
                        gameSettings$section$default(box, act, "Player appearance", null, 8, null);
                        LinearLayout loadRow2 = new LinearLayout(act);
                        loadRow2.setOrientation(0);
                        loadRow2.setGravity(16);
                        TextView textViewText$default = Ui.text$default(Ui.INSTANCE, act, "Load each player's avatar (clothing, accessories, body)", 13.0f, 0, false, 24, null);
                        textViewText$default.setSingleLine(false);
                        loadRow2.addView(textViewText$default, Ui.INSTANCE.lp(0, -2, 1.0f));
                        Switch loadSwitch = new Switch(act);
                        loadSwitch.setChecked(curLoad);
                        loadRow2.addView(loadSwitch);
                        box.addView(loadRow2);
                        gameSettings$section(box, act, "Body scales (R15)", "Players' body scales are kept between these values.");
                        HashMap fields = new HashMap();
                        Iterator<Pair<String, String>> it = scaleKeys.iterator();
                        while (it.hasNext()) {
                            String key = it.next().component1();
                            synchronized (studio2.getLock()) {
                                try {
                                    loadRow = loadRow2;
                                    try {
                                        Object obj3 = sp.get("GameSettingsScaleRange" + key);
                                        if (obj3 instanceof NumberRange) {
                                            try {
                                                r = (NumberRange) obj3;
                                            } catch (Throwable th4) {
                                                Throwable th = th4;
                                                throw th;
                                            }
                                        } else {
                                            r = null;
                                        }
                                    } catch (Throwable th5) {
                                        Throwable th = th5;
                                    }
                                } catch (Throwable th6) {
                                    Throwable th = th6;
                                }
                            }
                            if (r == null) {
                                NumberRange numberRange = scaleDefaults.get(key);
                                Intrinsics.checkNotNull(numberRange);
                                r = numberRange;
                            }
                            NumberRange r2 = r;
                            LinearLayout linearLayout2 = new LinearLayout(act);
                            linearLayout2.setOrientation(0);
                            linearLayout2.setGravity(16);
                            Switch loadSwitch2 = loadSwitch;
                            HashMap fields2 = fields;
                            linearLayout2.setPadding(0, INSTANCE.dp(act, 2.0f), 0, INSTANCE.dp(act, 2.0f));
                            String str = scaleLabels.get(key);
                            Intrinsics.checkNotNull(str);
                            Function0 collisionOf2 = collisionOf;
                            linearLayout2.addView(Ui.text$default(Ui.INSTANCE, act, str, 13.0f, 0, false, 24, null), Ui.INSTANCE.lp(0, -2, 1.0f));
                            EditText lo = Ui.input$default(Ui.INSTANCE, act, DataTypesKt.fmtNum(r2.getMin()), "min", true, false, 16, null);
                            EditText hi = Ui.input$default(Ui.INSTANCE, act, DataTypesKt.fmtNum(r2.getMax()), "max", true, false, 16, null);
                            linearLayout2.addView(lo, Ui.lp$default(Ui.INSTANCE, dp(act, 72.0f), -2, 0.0f, 4, null));
                            TextView textViewText$default2 = Ui.text$default(Ui.INSTANCE, act, "–", 13.0f, Ui.INSTANCE.getDIM(), false, 16, null);
                            textViewText$default2.setPadding(INSTANCE.dp(act, 6.0f), 0, INSTANCE.dp(act, 6.0f), 0);
                            linearLayout2.addView(textViewText$default2);
                            linearLayout2.addView(hi, Ui.lp$default(Ui.INSTANCE, dp(act, 72.0f), -2, 0.0f, 4, null));
                            box.addView(linearLayout2);
                            fields2.put(key, TuplesKt.to(lo, hi));
                            studio2 = studio;
                            collisionOf = collisionOf2;
                            loadRow2 = loadRow;
                            loadSwitch = loadSwitch2;
                            fields = fields2;
                        }
                        final Switch loadSwitch3 = loadSwitch;
                        final HashMap fields3 = fields;
                        final Function0 collisionOf3 = collisionOf;
                        final Instance sp2 = sp;
                        final CloudPlace cloud = studio.getCloud();
                        final CheckBox checkBox = new CheckBox(act);
                        checkBox.setText("Also save to “" + (cloud != null ? cloud.getName() : null) + "” on Roblox");
                        checkBox.setTextColor(Ui.INSTANCE.getTEXT());
                        checkBox.setChecked(true);
                        checkBox.setVisibility((cloud == null || RobloxApi.INSTANCE.isGuest(act)) ? 8 : 0);
                        LinearLayout.LayoutParams layoutParamsLp$default = Ui.lp$default(Ui.INSTANCE, -1, -2, 0.0f, 4, null);
                        layoutParamsLp$default.topMargin = INSTANCE.dp(act, 12.0f);
                        Unit unit = Unit.INSTANCE;
                        box.addView(checkBox, layoutParamsLp$default);
                        TextView textViewText$default3 = Ui.text$default(Ui.INSTANCE, act, "Saved in the place (StarterPlayer), so playtests here use it." + (cloud != null ? " Roblox games keep these settings on the game itself, so they're synced when you open it." : ""), 11.5f, Ui.INSTANCE.getDIM(), false, 16, null);
                        textViewText$default3.setSingleLine(false);
                        textViewText$default3.setPadding(0, INSTANCE.dp(act, 6.0f), 0, INSTANCE.dp(act, 4.0f));
                        box.addView(textViewText$default3);
                        AlertDialog.Builder title = Dialogs.INSTANCE.themed(act).setTitle("Game Settings — Avatar");
                        ScrollView scrollView = new ScrollView(act);
                        scrollView.addView(box);
                        title.setView(scrollView).setPositiveButton("Save", new DialogInterface.OnClickListener() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda3
                            @Override // android.content.DialogInterface.OnClickListener
                            public final void onClick(DialogInterface dialogInterface, int i) {
                                AvatarUi.gameSettings$lambda$77(function0Segmented, collisionOf3, fields3, studio, cloud, checkBox, act, sp2, loadSwitch3, dialogInterface, i);
                            }
                        }).setNegativeButton("Cancel", (DialogInterface.OnClickListener) null).show();
                    } catch (Throwable th7) {
                        Throwable th = th7;
                    }
                }
            } catch (Throwable th8) {
                Throwable th = th8;
            }
        }
    }
        catch (Throwable _ktEx) {
            throw com.goydevv.studrblx.util.KtCompat.sneakyThrow(_ktEx);
        }
    }

    static /* synthetic */ void gameSettings$section$default(LinearLayout linearLayout, Activity activity, String str, String str2, int i, Object obj) {
        if ((i & 8) != 0) {
            str2 = null;
        }
        gameSettings$section(linearLayout, activity, str, str2);
    }

    private static final void gameSettings$section(LinearLayout box, Activity $act, String t, String sub) {
        TextView textViewText = Ui.INSTANCE.text($act, t, 14.0f, Ui.INSTANCE.getTEXT(), true);
        textViewText.setPadding(0, INSTANCE.dp($act, 14.0f), 0, INSTANCE.dp($act, sub != null ? 2.0f : 8.0f));
        box.addView(textViewText);
        if (sub != null) {
            TextView textViewText$default = Ui.text$default(Ui.INSTANCE, $act, sub, 11.5f, Ui.INSTANCE.getDIM(), false, 16, null);
            textViewText$default.setSingleLine(false);
            textViewText$default.setPadding(0, 0, 0, INSTANCE.dp($act, 8.0f));
            box.addView(textViewText$default);
        }
    }

    static final Unit gameSettings$lambda$55(LinearLayout $collisionBox, int it) {
        $collisionBox.setVisibility(it == 0 ? 8 : 0);
        return Unit.INSTANCE;
    }

    static final void gameSettings$lambda$77(Function0 $typeOf, Function0 $collisionOf, HashMap $fields, Studio $studio, final CloudPlace $cloud, CheckBox $pushBox, final Activity $act, Instance $sp, Switch $loadSwitch, DialogInterface dialogInterface, int i) {
        Object next;
        Map map;
        int i2;
        Object next2;
        Map map2;
        int type = ((Number) $typeOf.invoke()).intValue();
        int collision = ((Number) $collisionOf.invoke()).intValue();
        LinkedHashMap ranges = new LinkedHashMap();
        Iterator<Pair<String, String>> it = scaleKeys.iterator();
        while (it.hasNext()) {
            String key = it.next().component1();
            Object obj = $fields.get(key);
            Intrinsics.checkNotNull(obj);
            Pair pair = (Pair) obj;
            EditText lo = (EditText) pair.component1();
            EditText hi = (EditText) pair.component2();
            NumberRange numberRange = scaleDefaults.get(key);
            Intrinsics.checkNotNull(numberRange);
            NumberRange d = numberRange;
            Float floatOrNull = StringsKt.toFloatOrNull(StringsKt.replace(lo.getText().toString(), ',', '.', false));
            float a = floatOrNull != null ? floatOrNull.floatValue() : d.getMin();
            Float floatOrNull2 = StringsKt.toFloatOrNull(StringsKt.replace(hi.getText().toString(), ',', '.', false));
            float b = floatOrNull2 != null ? floatOrNull2.floatValue() : d.getMax();
            ranges.put(key, new NumberRange(RangesKt.coerceIn(Math.min(a, b), 0.0f, 5.0f), RangesKt.coerceIn(Math.max(a, b), 0.0f, 5.0f)));
        }
        LinkedHashMap ranges2 = ranges;
        synchronized ($studio.getLock()) {
            $studio.getUndo().begin("Game Settings: Avatar");
            $studio.setProp($sp, "GameSettingsAvatar", new EnumValue(type));
            $studio.setProp($sp, "GameSettingsR15Collision", new EnumValue(collision));
            $studio.setProp($sp, "LoadCharacterAppearance", Boolean.valueOf($loadSwitch.isChecked()));
            for (Map.Entry entry : ((Map<?, ?>) ranges2).entrySet()) {
                $studio.setProp($sp, "GameSettingsScaleRange" + ((String) entry.getKey()), (NumberRange) entry.getValue());
            }
            $studio.getUndo().commit();
            Unit unit = Unit.INSTANCE;
        }
        $studio.markDirty();
        final String label = (String) CollectionsKt.listOf(new String[]{"R6", "R15", "Player Choice"}).get(type);
        OutputLog.INSTANCE.info("Game Settings: avatar type " + label);
        if ($cloud == null || !$pushBox.isChecked() || $pushBox.getVisibility() != 0) {
            INSTANCE.toast($act, "Avatar type: " + label);
            return;
        }
        Integer numValueOf = Integer.valueOf(collision);
        Map linkedHashMap = ranges2;
        Map linkedHashMap2 = new LinkedHashMap(MapsKt.mapCapacity(linkedHashMap.size()));
        for (Object obj2 : linkedHashMap.entrySet()) {
            Map.Entry entry2 = (Map.Entry) obj2;
            Iterator it2 = scaleKeys.iterator();
            while (true) {
                if (!it2.hasNext()) {
                    throw new NoSuchElementException("Collection contains no element matching the predicate.");
                }
                next2 = it2.next();
                map2 = linkedHashMap;
                if (Intrinsics.areEqual(((Pair) next2).getFirst(), entry2.getKey())) {
                    break;
                } else {
                    linkedHashMap = map2;
                }
            }
            linkedHashMap2.put((String) ((Pair) next2).getSecond(), ((Map.Entry) obj2).getValue());
            linkedHashMap = map2;
        }
        int i3 = 0;
        Map linkedHashMap3 = new LinkedHashMap(MapsKt.mapCapacity(linkedHashMap2.size()));
        Map map3 = linkedHashMap2;
        for (Object obj3 : map3.entrySet()) {
            linkedHashMap3.put(((Map.Entry) obj3).getKey(), Float.valueOf(((NumberRange) ((Map.Entry) obj3).getValue()).getMin()));
        }
        Map linkedHashMap4 = ranges2;
        int i4 = 0;
        Map linkedHashMap5 = new LinkedHashMap(MapsKt.mapCapacity(linkedHashMap4.size()));
        for (Object obj4 : linkedHashMap4.entrySet()) {
            Map.Entry entry3 = (Map.Entry) obj4;
            Iterator it3 = scaleKeys.iterator();
            while (true) {
                if (!it3.hasNext()) {
                    throw new NoSuchElementException("Collection contains no element matching the predicate.");
                }
                next = it3.next();
                map = linkedHashMap4;
                i2 = i4;
                if (Intrinsics.areEqual(((Pair) next).getFirst(), entry3.getKey())) {
                    break;
                }
                linkedHashMap4 = map;
                i4 = i2;
            }
            linkedHashMap5.put((String) ((Pair) next).getSecond(), ((Map.Entry) obj4).getValue());
            linkedHashMap4 = map;
            i4 = i2;
        }
        int i5 = 0;
        Map linkedHashMap6 = new LinkedHashMap(MapsKt.mapCapacity(linkedHashMap5.size()));
        for (Object obj5 : linkedHashMap5.entrySet()) {
            linkedHashMap6.put(((Map.Entry) obj5).getKey(), Float.valueOf(((NumberRange) ((Map.Entry) obj5).getValue()).getMax()));
        }
        final RobloxCloud.AvatarSettings s = new RobloxCloud.AvatarSettings(type, numValueOf, linkedHashMap3, linkedHashMap6);
        AccountUi.INSTANCE.background($act, "Saving to Roblox…", new Function0() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda9
            @Override // kotlin.jvm.functions.Function0
            public final Object invoke() {
                return AvatarUi.gameSettings$lambda$77$lambda$74($act, $cloud, s);
            }
        }, new Function1() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda10
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj6) {
                return AvatarUi.gameSettings$lambda$77$lambda$75($act, label, (Unit) obj6);
            }
        }, new Function1() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda11
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj6) {
                return Boolean.valueOf(AvatarUi.gameSettings$lambda$77$lambda$76($act, label, (Throwable) obj6));
            }
        });
    }

    static final Unit gameSettings$lambda$77$lambda$74(Activity $act, CloudPlace $cloud, RobloxCloud.AvatarSettings $s) {
        try {
        RobloxCloud.INSTANCE.setAvatarSettings($act, $cloud.getUniverseId(), $s);
        return Unit.INSTANCE;
    }
        catch (Throwable _ktEx) {
            throw com.goydevv.studrblx.util.KtCompat.sneakyThrow(_ktEx);
        }
    }

    static final Unit gameSettings$lambda$77$lambda$75(Activity $act, String $label, Unit it) {
        Intrinsics.checkNotNullParameter(it, "it");
        INSTANCE.toast($act, "Avatar settings saved (" + $label + ")");
        return Unit.INSTANCE;
    }

    static final boolean gameSettings$lambda$77$lambda$76(Activity $act, String $label, Throwable e) {
        Intrinsics.checkNotNullParameter(e, "e");
        OutputLog.INSTANCE.warn("Couldn't save avatar settings to Roblox: " + e.getMessage());
        Dialogs.INSTANCE.themed($act).setTitle("Saved in the place only").setMessage("The place now uses " + $label + ", but Roblox didn't accept the change for the game:\n\n" + e.getMessage()).setPositiveButton("OK", (DialogInterface.OnClickListener) null).show();
        return true;
    }

    public final void syncFromCloud(Activity act, final Studio studio, final CloudPlace cloud) {
        Intrinsics.checkNotNullParameter(act, "act");
        Intrinsics.checkNotNullParameter(studio, "studio");
        Intrinsics.checkNotNullParameter(cloud, "cloud");
        if (cloud.getUniverseId() == 0 || RobloxApi.INSTANCE.isGuest(act)) {
            return;
        }
        final Context app = act.getApplicationContext();
        net.execute(new Runnable() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                AvatarUi.syncFromCloud$lambda$81(app, cloud, studio);
            }
        });
    }

    static final void syncFromCloud$lambda$81(Context $app, final CloudPlace $cloud, final Studio $studio) {
        RobloxCloud.AvatarSettings _s;
        try {
            RobloxCloud robloxCloud = RobloxCloud.INSTANCE;
            Intrinsics.checkNotNull($app);
            _s = robloxCloud.avatarSettings($app, $cloud.getUniverseId());
        } catch (Exception e) {
            _s = null;
        }
        final RobloxCloud.AvatarSettings s = _s;
        if (s == null) {
            return;
        }
        $studio.getMain().post(new Runnable() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda13
            @Override // java.lang.Runnable
            public final void run() {
                AvatarUi.syncFromCloud$lambda$81$lambda$80($studio, $cloud, s);
            }
        });
    }

    static final void syncFromCloud$lambda$81$lambda$80(Studio $studio, CloudPlace $cloud, RobloxCloud.AvatarSettings $s) {
        CloudPlace cloud = $studio.getCloud();
        if (!(cloud != null && cloud.getUniverseId() == $cloud.getUniverseId()) || $studio.isPlaying()) {
            return;
        }
        synchronized ($studio.getLock()) {
            Instance service = $studio.getModel().getService("StarterPlayer");
            service.set("GameSettingsAvatar", new EnumValue($s.getType()), false);
            Integer collision = $s.getCollision();
            if (collision != null) {
                service.set("GameSettingsR15Collision", new EnumValue(collision.intValue()), false);
            }
            for (Pair<String, String> pair : scaleKeys) {
                String strComponent1 = pair.component1();
                String strComponent2 = pair.component2();
                Float f = $s.getMin().get(strComponent2);
                Float f2 = $s.getMax().get(strComponent2);
                if (f != null && f2 != null) {
                    service.set("GameSettingsScaleRange" + strComponent1, new NumberRange(f.floatValue(), f2.floatValue()), false);
                }
            }
            Unit unit = Unit.INSTANCE;
        }
        OutputLog.INSTANCE.info("Game Settings: avatar type " + CollectionsKt.listOf(new String[]{"R6", "R15", "Player Choice"}).get($s.getType()) + " (from the game on Roblox)");
    }

    /* JADX WARN: Code duplicated, block: B:14:0x0036  */
    public final void rigBuilder(final Activity act, final Studio studio) {
        Intrinsics.checkNotNullParameter(act, "act");
        Intrinsics.checkNotNullParameter(studio, "studio");
        if (studio.isPlaying()) {
            toast(act, "Stop the playtest to insert a rig");
            return;
        }
        Avatar.Spec specTmp = Avatar.INSTANCE.getCurrent();
        final Avatar.Spec spec = (specTmp != null && specTmp.getCharacter() == null) ? specTmp : null;
        final ArrayList items = new ArrayList();
        items.add(TuplesKt.to("R6 — Block Rig", new Function0() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda23
            @Override // kotlin.jvm.functions.Function0
            public final Object invoke() {
                return AvatarUi.rigBuilder$lambda$83(act, studio);
            }
        }));
        items.add(TuplesKt.to("R15 — Block Rig", new Function0() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda24
            @Override // kotlin.jvm.functions.Function0
            public final Object invoke() {
                return AvatarUi.rigBuilder$lambda$84(act, studio);
            }
        }));
        if (spec != null) {
            items.add(TuplesKt.to("R6 — My Avatar (" + spec.getLabel() + ")", new Function0() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda25
                @Override // kotlin.jvm.functions.Function0
                public final Object invoke() {
                    return AvatarUi.rigBuilder$lambda$85(spec, act, studio);
                }
            }));
            items.add(TuplesKt.to("R15 — My Avatar (" + spec.getLabel() + ")", new Function0() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda26
                @Override // kotlin.jvm.functions.Function0
                public final Object invoke() {
                    return AvatarUi.rigBuilder$lambda$86(act, studio, spec);
                }
            }));
        }
        AlertDialog.Builder title = Dialogs.INSTANCE.themed(act).setTitle("Rig Builder");
        ArrayList arrayList = items;
        Collection arrayList2 = new ArrayList(CollectionsKt.collectionSizeOrDefault(arrayList, 10));
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            arrayList2.add((String) ((Pair) it.next()).getFirst());
        }
        title.setItems((CharSequence[]) ((List) arrayList2).toArray(new String[0]), new DialogInterface.OnClickListener() { // from class: com.goydevv.studrblx.ui.AvatarUi$$ExternalSyntheticLambda27
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                ((Function0) ((Pair) items.get(i)).getSecond()).invoke();
            }
        }).setNegativeButton("Cancel", (DialogInterface.OnClickListener) null).show();
    }

    static final Unit rigBuilder$lambda$83(Activity $act, Studio $studio) {
        INSTANCE.insertRig($act, $studio, PlaySession.INSTANCE.buildR6Rig("Rig"));
        return Unit.INSTANCE;
    }

    static final Unit rigBuilder$lambda$84(Activity $act, Studio $studio) {
        if (RobloxApi.INSTANCE.isGuest($act)) {
            INSTANCE.toast($act, "R15 body meshes download from Roblox; sign in to see them (they show as boxes until then)");
        }
        INSTANCE.insertRig($act, $studio, Avatar.INSTANCE.buildR15("Rig", new Avatar.Spec(Avatar.INSTANCE.getCLASSIC_COLORS(), null, null, null, null, CollectionsKt.emptyList(), "Rig", true, null, null, null, null, 3840, null)));
        return Unit.INSTANCE;
    }

    static final Unit rigBuilder$lambda$85(Avatar.Spec $spec, Activity $act, Studio $studio) {
        Instance m = PlaySession.INSTANCE.buildR6Rig(StringsKt.removePrefix($spec.getLabel(), (CharSequence) "@"));
        Avatar.INSTANCE.applyTo(m, Avatar.Spec.forRig$default($spec, false, null, 2, null));
        INSTANCE.insertRig($act, $studio, m);
        return Unit.INSTANCE;
    }

    static final Unit rigBuilder$lambda$86(Activity $act, Studio $studio, Avatar.Spec $spec) {
        INSTANCE.insertRig($act, $studio, Avatar.INSTANCE.buildR15(StringsKt.removePrefix($spec.getLabel(), (CharSequence) "@"), Avatar.Spec.forRig$default($spec, true, null, 2, null)));
        return Unit.INSTANCE;
    }

    private final void insertRig(Activity act, Studio studio, Instance m) {
        synchronized (studio.getLock()) {
            boolean z = true;
            Iterable iterableDescendants$default = Instance.descendants$default(m, null, 1, null);
            Collection arrayList = new ArrayList();
            for (Object obj : iterableDescendants$default) {
                if (((Instance) obj).isA("BasePart")) {
                    arrayList.add(obj);
                }
            }
            List<Instance> list = (List) arrayList;
            Pair<Vector3, Vector3> pairBoundsOf = Geometry.INSTANCE.boundsOf(list);
            if (pairBoundsOf == null) {
                return;
            }
            Vector3 vector3SpawnPoint = studio.spawnPoint(new Vector3(4.0f, 0.0f, 2.0f));
            Vector3 forward = studio.getCamera().getForward();
            // Translate only: rigs must be inserted with zero rotation (0,0,0), not turned toward the camera.
            CFrame cFrameTimes = CFrame.INSTANCE.at(new Vector3(vector3SpawnPoint.getX(), vector3SpawnPoint.getY() - pairBoundsOf.getFirst().getY(), vector3SpawnPoint.getZ()));
            for (Instance instance : list) {
                instance.set("CFrame", cFrameTimes.times(PartData.INSTANCE.cframe(instance)), false);
            }
            boolean z2 = z;
            Instance instanceFindFirstChild$default = Instance.findFirstChild$default(m, "HumanoidRootPart", false, 2, null);
            if (instanceFindFirstChild$default != null) {
                instanceFindFirstChild$default.set("Anchored", Boolean.valueOf(z2), false);
            }
            studio.getUndo().begin("Insert Rig");
            studio.reparent(m, studio.getModel().getWorkspace());
            studio.getUndo().commit();
            studio.getSelection().clear();
            studio.getSelection().add(m);
            Iterator it = studio.getListeners().iterator();
            while (it.hasNext()) {
                ((Studio.Listener) it.next()).onSelectionChanged();
            }
            studio.markDirty();
            toast(act, "Inserted " + m.getName());
        }
    }
}
