package com.goydevv.studrblx.ui;

import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.DragEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.goydevv.studrblx.core.Studio;
import com.goydevv.studrblx.model.ClassDb;
import com.goydevv.studrblx.model.DataModel;
import com.goydevv.studrblx.model.Instance;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import kotlin.Pair;
import kotlin.TuplesKt;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.comparisons.ComparisonsKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.sequences.SequencesKt;
import kotlin.text.StringsKt;

/* JADX INFO: compiled from: ExplorerPanel.kt */
/* JADX INFO: loaded from: classes5.dex */
public final class ExplorerPanel extends LinearLayout implements Studio.Listener {
    private final MainActions actions;
    private final Adapter adapter;
    private View dragAnchor;
    private boolean dragMoved;
    private Row dragRow;
    private Pair<Float, Float> dragStart;
    private Instance dropTarget;
    private final Set<Instance> expanded;
    private String filter;
    private Pair<? extends Instance, Long> lastClick;
    private final RecyclerView list;
    private final ImageView multiBtn;
    private Instance rangeAnchor;
    private List<Row> rows;
    private final Studio studio;

    @Override // com.goydevv.studrblx.core.Studio.Listener
    public void onDirtyChanged() {
        Studio.Listener.super.onDirtyChanged();
    }

    @Override // com.goydevv.studrblx.core.Studio.Listener
    public void onPlayStateChanged() {
        Studio.Listener.super.onPlayStateChanged();
    }

    @Override // com.goydevv.studrblx.core.Studio.Listener
    public void onPluginsChanged() {
        Studio.Listener.super.onPluginsChanged();
    }

    @Override // com.goydevv.studrblx.core.Studio.Listener
    public void onPropsChanged() {
        Studio.Listener.super.onPropsChanged();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: compiled from: ExplorerPanel.kt */
    static final class Row {
        private final int depth;
        private final boolean expanded;
        private final boolean hasChildren;
        private final Instance inst;
        private final String label;

        public Row(Instance inst, int depth, boolean hasChildren, boolean expanded, String label) {
            Intrinsics.checkNotNullParameter(inst, "inst");
            Intrinsics.checkNotNullParameter(label, "label");
            this.inst = inst;
            this.depth = depth;
            this.hasChildren = hasChildren;
            this.expanded = expanded;
            this.label = label;
        }

        public final int getDepth() {
            return this.depth;
        }

        public final boolean getExpanded() {
            return this.expanded;
        }

        public final boolean getHasChildren() {
            return this.hasChildren;
        }

        public final Instance getInst() {
            return this.inst;
        }

        public final String getLabel() {
            return this.label;
        }
    }

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public ExplorerPanel(Context ctx, Studio studio, MainActions actions) {
        super(ctx);
        Intrinsics.checkNotNullParameter(ctx, "ctx");
        Intrinsics.checkNotNullParameter(studio, "studio");
        Intrinsics.checkNotNullParameter(actions, "actions");
        this.studio = studio;
        this.actions = actions;
        this.expanded = Collections.newSetFromMap(new IdentityHashMap());
        this.rows = CollectionsKt.emptyList();
        this.list = new RecyclerView(ctx);
        this.adapter = new Adapter();
        this.filter = "";
        setOrientation(1);
        setBackgroundColor(Ui.INSTANCE.getPANEL());
        LinearLayout h = Ui.INSTANCE.header(ctx, "Explorer", Icons.general$default(Icons.INSTANCE, ctx, "Explorer", null, 4, null));
        this.multiBtn = Ui.iconButton$default(Ui.INSTANCE, ctx, Icons.INSTANCE.general(ctx, "SelectAll", "Multi"), 0.0f, new Function1() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$$ExternalSyntheticLambda3
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                return ExplorerPanel._init_$lambda$0(ExplorerPanel.this, (View) obj);
            }
        }, 4, null);
        h.addView(this.multiBtn);
        h.addView(Ui.iconButton$default(Ui.INSTANCE, ctx, Icons.INSTANCE.general(ctx, "InsertObject", "+"), 0.0f, new Function1() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$$ExternalSyntheticLambda4
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                return ExplorerPanel._init_$lambda$1(ExplorerPanel.this, (View) obj);
            }
        }, 4, null));
        h.addView(Ui.iconButton$default(Ui.INSTANCE, ctx, Icons.INSTANCE.general(ctx, "CollapseAll", "Col"), 0.0f, new Function1() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$$ExternalSyntheticLambda5
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                return ExplorerPanel._init_$lambda$2(ExplorerPanel.this, (View) obj);
            }
        }, 4, null));
        addView(h, Ui.lp$default(Ui.INSTANCE, -1, -2, 0.0f, 4, null));
        EditText search = Ui.input$default(Ui.INSTANCE, ctx, "", "Filter workspace (name or class)", false, false, 24, null);
        search.setTextSize(12.0f);
        search.addTextChangedListener(new TextWatcher() { // from class: com.goydevv.studrblx.ui.ExplorerPanel.4
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable s) {
                String string;
                String string2;
                ExplorerPanel explorerPanel = ExplorerPanel.this;
                if (s == null || (string2 = s.toString()) == null || (string = StringsKt.trim((CharSequence) string2).toString()) == null) {
                    string = "";
                }
                explorerPanel.filter = string;
                ExplorerPanel.this.rebuild();
            }
        });
        LinearLayout.LayoutParams layoutParamsLp$default = Ui.lp$default(Ui.INSTANCE, -1, -2, 0.0f, 4, null);
        layoutParamsLp$default.setMargins(Ui.INSTANCE.dp(ctx, 4.0f), Ui.INSTANCE.dp(ctx, 4.0f), Ui.INSTANCE.dp(ctx, 4.0f), Ui.INSTANCE.dp(ctx, 4.0f));
        Unit unit = Unit.INSTANCE;
        addView(search, layoutParamsLp$default);
        this.list.setLayoutManager(new LinearLayoutManager(ctx));
        this.list.setAdapter(this.adapter);
        this.list.setItemAnimator(null);
        addView(this.list, Ui.INSTANCE.lp(-1, 0, 1.0f));
        this.list.setOnDragListener(new View.OnDragListener() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$$ExternalSyntheticLambda6
            @Override // android.view.View.OnDragListener
            public final boolean onDrag(View view, DragEvent dragEvent) {
                return ExplorerPanel._init_$lambda$4(ExplorerPanel.this, view, dragEvent);
            }
        });
        expandDefaults();
        rebuild();
    }

    /* JADX INFO: compiled from: ExplorerPanel.kt */
    public static final class DragPayload {
        private final List<Instance> items;

        /* JADX WARN: Multi-variable type inference failed */
        public DragPayload(List<? extends Instance> items) {
            Intrinsics.checkNotNullParameter(items, "items");
            this.items = (List<Instance>) items;
        }

        public final List<Instance> getItems() {
            return this.items;
        }
    }

    public final boolean getMultiSelect() {
        return this.studio.getMultiSelect();
    }

    public final void setMultiSelect(boolean v) {
        this.studio.setMultiSelect(v);
    }

    static final Unit _init_$lambda$0(ExplorerPanel this$0, View it) {
        Intrinsics.checkNotNullParameter(it, "it");
        this$0.setMultiSelect(!this$0.getMultiSelect());
        this$0.updateMultiBtn();
        this$0.studio.toast(this$0.getMultiSelect() ? "Multi-select on: tap objects to add or remove them" : "Multi-select off");
        return Unit.INSTANCE;
    }

    static final Unit _init_$lambda$1(ExplorerPanel this$0, View it) {
        Intrinsics.checkNotNullParameter(it, "it");
        this$0.showInsertDialog((Instance) CollectionsKt.firstOrNull((List) this$0.studio.getSelection()));
        return Unit.INSTANCE;
    }

    static final Unit _init_$lambda$2(ExplorerPanel this$0, View it) {
        Intrinsics.checkNotNullParameter(it, "it");
        this$0.collapseAll();
        return Unit.INSTANCE;
    }

    static final boolean _init_$lambda$4(ExplorerPanel this$0, View view, DragEvent e) {
        Intrinsics.checkNotNull(e);
        return this$0.onDrag(e);
    }

    private final Row rowAt(float x, float y) {
        View v = this.list.findChildViewUnder(x, y);
        if (v == null) {
            return null;
        }
        int pos = this.list.getChildAdapterPosition(v);
        return (Row) CollectionsKt.getOrNull(this.rows, pos);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void startDrag(View v, Row r) {
        List items;
        Iterable selection = this.studio.getSelection();
        boolean z = true;
        boolean ok = false;
        if (!(selection instanceof Collection) || !((Collection) selection).isEmpty()) {
            Iterator it = selection.iterator();
            while (it.hasNext()) {
                if (((Instance) it.next()) == r.getInst()) {
                    z = false;
                    break;
                }
            }
        }
        if (z) {
            this.studio.select(CollectionsKt.listOf(r.getInst()));
        }
        synchronized (this.studio.getLock()) {
            Iterable selection2 = this.studio.getSelection();
            Collection arrayList = new ArrayList();
            for (Object obj : selection2) {
                if (!((Instance) obj).getIsService()) {
                    arrayList.add(obj);
                }
            }
            items = (List) arrayList;
        }
        this.dragRow = r;
        this.dragAnchor = v;
        this.dragStart = null;
        this.dragMoved = false;
        if (items.isEmpty()) {
            showContextMenu(v, r);
            return;
        }
        try {
            ok = v.startDragAndDrop(ClipData.newPlainText("instances", CollectionsKt.joinToString(items, ", ", "", "", -1, "...", new Function1() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$$ExternalSyntheticLambda2
                @Override // kotlin.jvm.functions.Function1
                public final Object invoke(Object obj2) {
                    return ExplorerPanel.startDrag$lambda$8((Instance) obj2);
                }
            })), new View.DragShadowBuilder(v), new DragPayload(items), 0);
        } catch (Exception e) {
        }
        if (ok) {
            return;
        }
        showContextMenu(v, r);
    }

    static final CharSequence startDrag$lambda$8(Instance it) {
        Intrinsics.checkNotNullParameter(it, "it");
        return it.getName();
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private final boolean onDrag(DragEvent e) {
        Row rowRowAt;
        Instance inst;
        Row rowRowAt2;
        Instance target;
        Object localState = e.getLocalState();
        Instance t = null;
        DragPayload payload = localState instanceof DragPayload ? (DragPayload) localState : null;
        switch (e.getAction()) {
            case 1:
                return payload != null;
            case 2:
                Pair<Float, Float> pair = this.dragStart;
                if (pair == null) {
                    this.dragStart = TuplesKt.to(Float.valueOf(e.getX()), Float.valueOf(e.getY()));
                } else if (((float) Math.hypot(e.getX() - pair.getFirst().floatValue(), e.getY() - pair.getSecond().floatValue())) > getResources().getDisplayMetrics().density * 22.0f) {
                    this.dragMoved = true;
                }
                if (this.dragMoved && (rowRowAt = rowAt(e.getX(), e.getY())) != null && (inst = rowRowAt.getInst()) != null && canDropInto(inst, payload)) {
                    t = inst;
                }
                if (t != this.dropTarget) {
                    this.dropTarget = t;
                    this.adapter.notifyDataSetChanged();
                }
                return true;
            case 3:
                if (!this.dragMoved || payload == null || (rowRowAt2 = rowAt(e.getX(), e.getY())) == null || (target = rowRowAt2.getInst()) == null || !canDropInto(target, payload)) {
                    return false;
                }
                synchronized (this.studio.getLock()) {
                    this.studio.getUndo().begin("Reparent");
                    for (Instance instance : payload.getItems()) {
                        if (instance != target && !target.isDescendantOf(instance) && instance.getParent() != target) {
                            this.studio.reparent(instance, target);
                        }
                    }
                    this.studio.getUndo().commit();
                    this.expanded.add(target);
                }
                this.studio.select(payload.getItems());
                return true;
            case 4:
                if (this.dropTarget != null) {
                    this.dropTarget = null;
                    this.adapter.notifyDataSetChanged();
                }
                Row r = this.dragRow;
                View a = this.dragAnchor;
                if (!this.dragMoved && !e.getResult() && r != null && a != null && a.isAttachedToWindow()) {
                    showContextMenu(a, r);
                }
                this.dragRow = null;
                this.dragAnchor = null;
                return true;
            case 5:
            default:
                return true;
            case 6:
                this.dragMoved = true;
                if (this.dropTarget != null) {
                    this.dropTarget = null;
                    this.adapter.notifyDataSetChanged();
                }
                return true;
        }
    }

    private final boolean canDropInto(Instance target, DragPayload payload) {
        Iterable items;
        if (payload == null || (items = payload.getItems()) == null) {
            return false;
        }
        Iterable<Instance> iterable = items;
        if ((iterable instanceof Collection) && ((Collection) iterable).isEmpty()) {
            return true;
        }
        for (Instance instance : iterable) {
            if (instance == target || target.isDescendantOf(instance)) {
                return false;
            }
        }
        return true;
    }

    private final void updateMultiBtn() {
        Drawable rippleDrawableRippleBg$default;
        ImageView imageView = this.multiBtn;
        if (!getMultiSelect()) {
            Ui ui = Ui.INSTANCE;
            Context context = getContext();
            Intrinsics.checkNotNullExpressionValue(context, "getContext(...)");
            rippleDrawableRippleBg$default = Ui.rippleBg$default(ui, context, 0, 0.0f, 4, null);
        } else {
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setColor(Ui.INSTANCE.getSELECT());
            Ui ui2 = Ui.INSTANCE;
            Context context2 = getContext();
            Intrinsics.checkNotNullExpressionValue(context2, "getContext(...)");
            gradientDrawable.setCornerRadius(ui2.dp(context2, 4.0f));
            rippleDrawableRippleBg$default = gradientDrawable;
        }
        imageView.setBackground(rippleDrawableRippleBg$default);
    }

    public final void refreshIcons() {
        this.adapter.notifyDataSetChanged();
    }

    private final void expandDefaults() {
        this.expanded.clear();
        synchronized (this.studio.getLock()) {
            Instance instanceFindService = this.studio.getModel().findService("Workspace");
            if (instanceFindService != null) {
                Boolean.valueOf(this.expanded.add(instanceFindService));
            }
        }
    }

    private final void collapseAll() {
        this.expanded.clear();
        rebuild();
    }

    private final List<Instance> visibleChildren(Instance i) {
        if (i instanceof DataModel) {
            final List<String> serviceNames = ClassDb.INSTANCE.getServiceNames();
            Iterable children = i.getChildren();
            Collection arrayList = new ArrayList();
            for (Object obj : children) {
                Instance instance = (Instance) obj;
                if (instance.getArchivable() || serviceNames.contains(instance.getClassName())) {
                    arrayList.add(obj);
                }
            }
            return CollectionsKt.sortedWith((List) arrayList, new Comparator() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$visibleChildren$$inlined$sortedBy$1
                /* JADX WARN: Multi-variable type inference failed */
                @Override // java.util.Comparator
                public final int compare(Object t, Object t2) {
                    int iIndexOf = serviceNames.indexOf(((Instance) t).getClassName());
                    if (iIndexOf < 0) {
                        iIndexOf = 999;
                    }
                    Integer numValueOf = Integer.valueOf(iIndexOf);
                    int iIndexOf2 = serviceNames.indexOf(((Instance) t2).getClassName());
                    return ComparisonsKt.compareValues(numValueOf, Integer.valueOf(iIndexOf2 >= 0 ? iIndexOf2 : 999));
                }
            });
        }
        if (!i.isA("PartOperation")) {
            return i.getChildren();
        }
        Iterable children2 = i.getChildren();
        Collection arrayList2 = new ArrayList();
        for (Object obj2 : children2) {
            if (!((Instance) obj2).isA("BasePart")) {
                arrayList2.add(obj2);
            }
        }
        return (List) arrayList2;
    }

    public final void rebuild() {
        String strFullName;
        ArrayList out = new ArrayList();
        synchronized (this.studio.getLock()) {
            DataModel model = this.studio.getModel();
            if (this.filter.length() > 0) {
                String lowerCase = this.filter.toLowerCase(Locale.ROOT);
                Intrinsics.checkNotNullExpressionValue(lowerCase, "toLowerCase(...)");
                for (Instance instance : Instance.descendants$default(model, null, 1, null)) {
                    if (out.size() > 2000) {
                        break;
                    }
                    String lowerCase2 = instance.getName().toLowerCase(Locale.ROOT);
                    Intrinsics.checkNotNullExpressionValue(lowerCase2, "toLowerCase(...)");
                    if (!StringsKt.contains((CharSequence) lowerCase2, (CharSequence) lowerCase, false)) {
                        String lowerCase3 = instance.getClassName().toLowerCase(Locale.ROOT);
                        Intrinsics.checkNotNullExpressionValue(lowerCase3, "toLowerCase(...)");
                        if (!Intrinsics.areEqual(lowerCase3, lowerCase)) {
                        }
                    }
                    String name = instance.getName();
                    Instance parent = instance.getParent();
                    if (parent == null || (strFullName = parent.fullName()) == null) {
                        strFullName = "";
                    }
                    out.add(new Row(instance, 0, false, false, name + "  ·  " + strFullName));
                }
            } else {
                rebuild$lambda$19$walk(this, out, model, 0);
            }
            Unit unit = Unit.INSTANCE;
        }
        this.rows = out;
        this.adapter.notifyDataSetChanged();
    }

    private static final void rebuild$lambda$19$walk(ExplorerPanel this$0, ArrayList<Row> arrayList, Instance i, int depth) {
        for (Instance c : this$0.visibleChildren(i)) {
            boolean open = this$0.expanded.contains(c);
            arrayList.add(new Row(c, depth, !this$0.visibleChildren(c).isEmpty(), open, c.getName()));
            if (open) {
                rebuild$lambda$19$walk(this$0, arrayList, c, depth + 1);
            }
        }
    }

    private final void revealSelection() {
        Instance first = (Instance) CollectionsKt.firstOrNull((List) this.studio.getSelection());
        if (first == null) {
            return;
        }
        synchronized (this.studio.getLock()) {
            for (Instance parent = first.getParent(); parent != null && !(parent instanceof DataModel); parent = parent.getParent()) {
                this.expanded.add(parent);
            }
            Unit unit = Unit.INSTANCE;
        }
        rebuild();
        int idx = 0;
        Iterator<Row> it = this.rows.iterator();
        while (true) {
            if (it.hasNext()) {
                if (((Row) it.next()).getInst() == first) {
                    break;
                } else {
                    idx++;
                }
            } else {
                idx = -1;
                break;
            }
        }
        if (idx >= 0) {
            RecyclerView.LayoutManager layoutManager = this.list.getLayoutManager();
            Intrinsics.checkNotNull(layoutManager, "null cannot be cast to non-null type androidx.recyclerview.widget.LinearLayoutManager");
            Ui ui = Ui.INSTANCE;
            Context context = getContext();
            Intrinsics.checkNotNullExpressionValue(context, "getContext(...)");
            ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(idx, ui.dp(context, 60.0f));
        }
    }

    @Override // com.goydevv.studrblx.core.Studio.Listener
    public void onTreeChanged() {
        rebuild();
    }

    @Override // com.goydevv.studrblx.core.Studio.Listener
    public void onSelectionChanged() {
        revealSelection();
    }

    @Override // com.goydevv.studrblx.core.Studio.Listener
    public void onModelReplaced() {
        expandDefaults();
        rebuild();
    }

    @Override // com.goydevv.studrblx.core.Studio.Listener
    public void onToolChanged() {
        updateMultiBtn();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void onRowClick(Row r) {
        long now = System.currentTimeMillis();
        Pair<? extends Instance, Long> pair = this.lastClick;
        if (pair != null && pair.getFirst() == r.getInst() && now - pair.getSecond().longValue() < 350 && r.getInst().isA("LuaSourceContainer")) {
            this.studio.openScript(r.getInst());
            this.lastClick = null;
            return;
        }
        this.lastClick = TuplesKt.to(r.getInst(), Long.valueOf(now));
        if (getMultiSelect() || this.studio.getModifierHeld()) {
            this.studio.toggleSelect(r.getInst());
        } else {
            this.studio.select(CollectionsKt.listOf(r.getInst()));
        }
        this.rangeAnchor = r.getInst();
    }

    private final void selectRange(Row r) {
        int i1;
        List<? extends Instance> listPlus;
        boolean z;
        Instance a = this.rangeAnchor;
        if (a == null) {
            this.studio.select(CollectionsKt.listOf(r.getInst()));
            return;
        }
        int i0 = 0;
        Iterator<Row> it = this.rows.iterator();
        while (true) {
            i1 = -1;
            if (!it.hasNext()) {
                i0 = -1;
                break;
            } else if (((Row) it.next()).getInst() == a) {
                break;
            } else {
                i0++;
            }
        }
        int i = 0;
        Iterator<Row> it2 = this.rows.iterator();
        while (it2.hasNext()) {
            if (((Row) it2.next()).getInst() == r.getInst()) {
                i1 = i;
                break;
            }
            i++;
        }
        if (i0 < 0 || i1 < 0) {
            this.studio.select(CollectionsKt.listOf(r.getInst()));
            return;
        }
        Iterable iterableSubList = this.rows.subList(Math.min(i0, i1), Math.max(i0, i1) + 1);
        Collection arrayList = new ArrayList(CollectionsKt.collectionSizeOrDefault(iterableSubList, 10));
        Iterator it3 = iterableSubList.iterator();
        while (it3.hasNext()) {
            arrayList.add(((Row) it3.next()).getInst());
        }
        List arrayList2 = (List) arrayList;
        Studio studio = this.studio;
        if (getMultiSelect()) {
            List list = CollectionsKt.toList(this.studio.getSelection());
            Collection arrayList3 = new ArrayList();
            for (Object obj : arrayList2) {
                Instance instance = (Instance) obj;
                Iterable selection = this.studio.getSelection();
                Instance a2 = a;
                if ((selection instanceof Collection) && ((Collection) selection).isEmpty()) {
                    z = true;
                } else {
                    Iterator it4 = selection.iterator();
                    while (true) {
                        if (!it4.hasNext()) {
                            z = true;
                            break;
                        }
                        Iterator it5 = it4;
                        if (((Instance) it4.next()) == instance) {
                            z = false;
                            break;
                        }
                        it4 = it5;
                    }
                }
                if (z) {
                    arrayList3.add(obj);
                }
                a = a2;
            }
            listPlus = CollectionsKt.plus((Collection) list, arrayList3);
        } else {
            listPlus = arrayList2;
        }
        studio.select(listPlus);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void toggleExpand(Row r) {
        if (r.getHasChildren()) {
            if (this.expanded.contains(r.getInst())) {
                this.expanded.remove(r.getInst());
            } else {
                this.expanded.add(r.getInst());
            }
            rebuild();
        }
    }

    private final void showContextMenu(View anchor, final Row r) {
        boolean z;
        Iterable selection = this.studio.getSelection();
        if (!(selection instanceof Collection) || !((Collection) selection).isEmpty()) {
            Iterator it = selection.iterator();
            while (true) {
                if (it.hasNext()) {
                    if (((Instance) it.next()) == r.getInst()) {
                        z = false;
                        break;
                    }
                } else {
                    z = true;
                    break;
                }
            }
        } else {
            z = true;
        }
        if (z) {
            this.studio.select(CollectionsKt.listOf(r.getInst()));
        }
        Ui ui = Ui.INSTANCE;
        Context context = getContext();
        Intrinsics.checkNotNullExpressionValue(context, "getContext(...)");
        PopupMenu m = new PopupMenu(ui.popupContext(context), anchor);
        final Instance i = r.getInst();
        final ArrayList items = new ArrayList();
        items.add(TuplesKt.to("Insert Object…", new Function0() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$$ExternalSyntheticLambda7
            @Override // kotlin.jvm.functions.Function0
            public final Object invoke() {
                return ExplorerPanel.showContextMenu$lambda$29(ExplorerPanel.this, i);
            }
        }));
        Instance ra = this.rangeAnchor;
        if (ra != null && ra != i) {
            items.add(TuplesKt.to("Select from “" + ra.getName() + "” to here", new Function0() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$$ExternalSyntheticLambda16
                @Override // kotlin.jvm.functions.Function0
                public final Object invoke() {
                    return ExplorerPanel.showContextMenu$lambda$30(ExplorerPanel.this, r);
                }
            }));
        }
        if (!i.getChildren().isEmpty()) {
            items.add(TuplesKt.to("Select Children", new Function0() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$$ExternalSyntheticLambda17
                @Override // kotlin.jvm.functions.Function0
                public final Object invoke() {
                    return ExplorerPanel.showContextMenu$lambda$31(ExplorerPanel.this, i);
                }
            }));
        }
        if (this.studio.getSelection().size() > 1) {
            items.add(TuplesKt.to("Select Group (parent Model)", new Function0() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$$ExternalSyntheticLambda18
                @Override // kotlin.jvm.functions.Function0
                public final Object invoke() {
                    return ExplorerPanel.showContextMenu$lambda$35(ExplorerPanel.this);
                }
            }));
        }
        if (i.isA("LuaSourceContainer")) {
            items.add(TuplesKt.to("Open Script", new Function0() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$$ExternalSyntheticLambda19
                @Override // kotlin.jvm.functions.Function0
                public final Object invoke() {
                    return ExplorerPanel.showContextMenu$lambda$36(ExplorerPanel.this, i);
                }
            }));
        }
        if (!i.getIsService()) {
            items.add(TuplesKt.to("Rename", new Function0() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$$ExternalSyntheticLambda20
                @Override // kotlin.jvm.functions.Function0
                public final Object invoke() {
                    return ExplorerPanel.showContextMenu$lambda$37(ExplorerPanel.this, i);
                }
            }));
            items.add(TuplesKt.to("Duplicate", new Function0() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$$ExternalSyntheticLambda21
                @Override // kotlin.jvm.functions.Function0
                public final Object invoke() {
                    return ExplorerPanel.showContextMenu$lambda$38(ExplorerPanel.this);
                }
            }));
            items.add(TuplesKt.to("Copy", new Function0() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$$ExternalSyntheticLambda22
                @Override // kotlin.jvm.functions.Function0
                public final Object invoke() {
                    return ExplorerPanel.showContextMenu$lambda$39(ExplorerPanel.this);
                }
            }));
            items.add(TuplesKt.to("Cut", new Function0() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$$ExternalSyntheticLambda23
                @Override // kotlin.jvm.functions.Function0
                public final Object invoke() {
                    return ExplorerPanel.showContextMenu$lambda$40(ExplorerPanel.this);
                }
            }));
        }
        if (this.studio.getHasClipboard()) {
            items.add(TuplesKt.to("Paste Into", new Function0() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$$ExternalSyntheticLambda24
                @Override // kotlin.jvm.functions.Function0
                public final Object invoke() {
                    return ExplorerPanel.showContextMenu$lambda$41(ExplorerPanel.this, i);
                }
            }));
        }
        if (!i.getIsService()) {
            items.add(TuplesKt.to("Delete", new Function0() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$$ExternalSyntheticLambda8
                @Override // kotlin.jvm.functions.Function0
                public final Object invoke() {
                    return ExplorerPanel.showContextMenu$lambda$42(ExplorerPanel.this);
                }
            }));
            items.add(TuplesKt.to("Group (Model)", new Function0() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$$ExternalSyntheticLambda9
                @Override // kotlin.jvm.functions.Function0
                public final Object invoke() {
                    return ExplorerPanel.showContextMenu$lambda$43(ExplorerPanel.this);
                }
            }));
            if (Intrinsics.areEqual(i.getClassName(), "Model") || Intrinsics.areEqual(i.getClassName(), "Folder")) {
                items.add(TuplesKt.to("Ungroup", new Function0() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$$ExternalSyntheticLambda10
                    @Override // kotlin.jvm.functions.Function0
                    public final Object invoke() {
                        return ExplorerPanel.showContextMenu$lambda$44(ExplorerPanel.this);
                    }
                }));
            }
            items.add(TuplesKt.to("Zoom To", new Function0() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$$ExternalSyntheticLambda11
                @Override // kotlin.jvm.functions.Function0
                public final Object invoke() {
                    return ExplorerPanel.showContextMenu$lambda$45(ExplorerPanel.this);
                }
            }));
            items.add(TuplesKt.to("Save to File (.rbxm)…", new Function0() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$$ExternalSyntheticLambda12
                @Override // kotlin.jvm.functions.Function0
                public final Object invoke() {
                    return ExplorerPanel.showContextMenu$lambda$46(ExplorerPanel.this);
                }
            }));
            items.add(TuplesKt.to("Add to Toolbox", new Function0() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$$ExternalSyntheticLambda13
                @Override // kotlin.jvm.functions.Function0
                public final Object invoke() {
                    return ExplorerPanel.showContextMenu$lambda$47(ExplorerPanel.this);
                }
            }));
            items.add(TuplesKt.to("Upload to Roblox…", new Function0() {
                @Override // kotlin.jvm.functions.Function0
                public final Object invoke() {
                    ExplorerPanel.this.actions.uploadSelectionToRoblox();
                    return Unit.INSTANCE;
                }
            }));
        }
        if (!i.getChildren().isEmpty()) {
            items.add(TuplesKt.to("Expand All", new Function0() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$$ExternalSyntheticLambda14
                @Override // kotlin.jvm.functions.Function0
                public final Object invoke() {
                    return ExplorerPanel.showContextMenu$lambda$48(ExplorerPanel.this, i);
                }
            }));
        }
        int i2 = 0;
        for (Object obj : items) {
            int i3 = i2 + 1;
            if (i2 < 0) {
                CollectionsKt.throwIndexOverflow();
            }
            m.getMenu().add(0, i2, i2, (String) ((Pair) obj).component1());
            i2 = i3;
        }
        m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$$ExternalSyntheticLambda15
            @Override // android.widget.PopupMenu.OnMenuItemClickListener
            public final boolean onMenuItemClick(MenuItem menuItem) {
                return ExplorerPanel.showContextMenu$lambda$50(items, menuItem);
            }
        });
        m.show();
    }

    static final Unit showContextMenu$lambda$29(ExplorerPanel this$0, Instance $i) {
        this$0.showInsertDialog($i);
        return Unit.INSTANCE;
    }

    static final Unit showContextMenu$lambda$30(ExplorerPanel this$0, Row $r) {
        this$0.selectRange($r);
        return Unit.INSTANCE;
    }

    static final Unit showContextMenu$lambda$31(ExplorerPanel this$0, Instance $i) {
        this$0.studio.select(this$0.visibleChildren($i));
        return Unit.INSTANCE;
    }

    /* JADX WARN: Code duplicated, block: B:18:0x0071  */
    static final Unit showContextMenu$lambda$35(ExplorerPanel this$0) {
        boolean z = false;
        Iterable selection = this$0.studio.getSelection();
        int i = 0;
        Collection arrayList = new ArrayList();
        Iterator it = selection.iterator();
        while (it.hasNext()) {
            Object obj = null;
            for (Object obj2 : SequencesKt.asIterable(SequencesKt.generateSequence(((Instance) it.next()).getParent(), (Function1<? super Instance, ? extends Instance>) new Function1() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$$ExternalSyntheticLambda25
                @Override // kotlin.jvm.functions.Function1
                public final Object invoke(Object obj3) {
                    return ExplorerPanel.showContextMenu$lambda$35$lambda$34$lambda$32((Instance) obj3);
                }
            }))) {
                Instance instance = (Instance) obj2;
                if (Intrinsics.areEqual(instance.getClassName(), "Model")) {
                    Instance parent = instance.getParent();
                    z = parent != null && parent.getIsService();
                }
                if (z) {
                    obj = obj2;
                }
            }
            Iterable iterable = selection;
            int i2 = i;
            Instance instance2 = (Instance) obj;
            if (instance2 != null) {
                arrayList.add(instance2);
            }
            selection = (List) iterable;
            i = i2;
        }
        List<? extends Instance> listDistinct = CollectionsKt.distinct((List) arrayList);
        if (!listDistinct.isEmpty()) {
            this$0.studio.select(listDistinct);
        }
        return Unit.INSTANCE;
    }

    static final Instance showContextMenu$lambda$35$lambda$34$lambda$32(Instance it) {
        Intrinsics.checkNotNullParameter(it, "it");
        return it.getParent();
    }

    static final Unit showContextMenu$lambda$36(ExplorerPanel this$0, Instance $i) {
        this$0.studio.openScript($i);
        return Unit.INSTANCE;
    }

    static final Unit showContextMenu$lambda$37(ExplorerPanel this$0, Instance $i) {
        this$0.rename($i);
        return Unit.INSTANCE;
    }

    static final Unit showContextMenu$lambda$38(ExplorerPanel this$0) {
        this$0.studio.duplicateSelection();
        return Unit.INSTANCE;
    }

    static final Unit showContextMenu$lambda$39(ExplorerPanel this$0) {
        this$0.studio.copySelection();
        return Unit.INSTANCE;
    }

    static final Unit showContextMenu$lambda$40(ExplorerPanel this$0) {
        this$0.studio.cutSelection();
        return Unit.INSTANCE;
    }

    static final Unit showContextMenu$lambda$41(ExplorerPanel this$0, Instance $i) {
        this$0.studio.paste($i);
        return Unit.INSTANCE;
    }

    static final Unit showContextMenu$lambda$42(ExplorerPanel this$0) {
        this$0.studio.deleteSelection();
        return Unit.INSTANCE;
    }

    static final Unit showContextMenu$lambda$43(ExplorerPanel this$0) {
        this$0.studio.groupSelection();
        return Unit.INSTANCE;
    }

    static final Unit showContextMenu$lambda$44(ExplorerPanel this$0) {
        this$0.studio.ungroupSelection();
        return Unit.INSTANCE;
    }

    static final Unit showContextMenu$lambda$45(ExplorerPanel this$0) {
        this$0.studio.focusSelection();
        return Unit.INSTANCE;
    }

    static final Unit showContextMenu$lambda$46(ExplorerPanel this$0) {
        this$0.actions.exportSelection();
        return Unit.INSTANCE;
    }

    static final Unit showContextMenu$lambda$47(ExplorerPanel this$0) {
        this$0.actions.addSelectionToToolbox();
        return Unit.INSTANCE;
    }

    static final Unit showContextMenu$lambda$48(ExplorerPanel this$0, Instance $i) {
        this$0.expandAll($i);
        return Unit.INSTANCE;
    }

    static final boolean showContextMenu$lambda$50(ArrayList $items, MenuItem mi) {
        ((Function0) ((Pair) $items.get(mi.getItemId())).getSecond()).invoke();
        return true;
    }

    private final void expandAll(Instance i) {
        synchronized (this.studio.getLock()) {
            this.expanded.add(i);
            int i2 = 0;
            for (Instance instance : Instance.descendants$default(i, null, 1, null)) {
                if (!instance.getChildren().isEmpty()) {
                    this.expanded.add(instance);
                    i2++;
                    if (i2 > 500) {
                        break;
                    }
                }
            }
            Unit unit = Unit.INSTANCE;
        }
        rebuild();
    }

    private final void rename(final Instance i) {
        Ui ui = Ui.INSTANCE;
        Context context = getContext();
        Intrinsics.checkNotNullExpressionValue(context, "getContext(...)");
        final EditText e = Ui.input$default(ui, context, i.getName(), null, false, false, 28, null);
        Dialogs dialogs = Dialogs.INSTANCE;
        Context context2 = getContext();
        Intrinsics.checkNotNullExpressionValue(context2, "getContext(...)");
        dialogs.themed(context2).setTitle("Rename").setView(e).setPositiveButton("OK", new DialogInterface.OnClickListener() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$$ExternalSyntheticLambda1
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                try {
                    ExplorerPanel.this.studio.setProp(i, "Name", e.getText().toString());
                } catch (Throwable _t) {
                    if (_t instanceof RuntimeException) throw (RuntimeException) _t;
                    if (_t instanceof Error) throw (Error) _t;
                    throw new RuntimeException(_t);
                }
            }
        }).setNegativeButton("Cancel", (DialogInterface.OnClickListener) null).show();
        e.requestFocus();
        e.selectAll();
    }

    public final void showInsertDialog(final Instance parent) {
        Dialogs dialogs = Dialogs.INSTANCE;
        Context context = getContext();
        Intrinsics.checkNotNullExpressionValue(context, "getContext(...)");
        dialogs.pickClass(context, "Insert Object", new Function1() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$$ExternalSyntheticLambda0
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                return ExplorerPanel.showInsertDialog$lambda$54(ExplorerPanel.this, parent, (String) obj);
            }
        });
    }

    static final Unit showInsertDialog$lambda$54(ExplorerPanel this$0, Instance $parent, String cls) {
        Intrinsics.checkNotNullParameter(cls, "cls");
        Studio studio = this$0.studio;
        Instance instance = null;
        if ($parent != null) {
            if (($parent.isA("BasePart") && ClassDb.INSTANCE.isA(cls, "BasePart")) ? false : true) {
                instance = $parent;
            }
        }
        studio.insertObject(cls, instance);
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: compiled from: ExplorerPanel.kt */
    final class VH extends RecyclerView.ViewHolder {
        private final ImageView arrow;
        private final ImageView icon;
        private final TextView name;
        private final LinearLayout row;
        private final View spacer;
        final /* synthetic */ ExplorerPanel this$0;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public VH(ExplorerPanel this$0, LinearLayout row, View spacer, ImageView arrow, ImageView icon, TextView name) {
            super(row);
            Intrinsics.checkNotNullParameter(row, "row");
            Intrinsics.checkNotNullParameter(spacer, "spacer");
            Intrinsics.checkNotNullParameter(arrow, "arrow");
            Intrinsics.checkNotNullParameter(icon, "icon");
            Intrinsics.checkNotNullParameter(name, "name");
            this.this$0 = this$0;
            this.row = row;
            this.spacer = spacer;
            this.arrow = arrow;
            this.icon = icon;
            this.name = name;
        }

        public final ImageView getArrow() {
            return this.arrow;
        }

        public final ImageView getIcon() {
            return this.icon;
        }

        public final TextView getName() {
            return this.name;
        }

        public final LinearLayout getRow() {
            return this.row;
        }

        public final View getSpacer() {
            return this.spacer;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: compiled from: ExplorerPanel.kt */
    final class Adapter extends RecyclerView.Adapter<VH> {
        public Adapter() {
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public int getItemCount() {
            return ExplorerPanel.this.rows.size();
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            Intrinsics.checkNotNullParameter(parent, "parent");
            Context ctx = parent.getContext();
            LinearLayout linearLayout = new LinearLayout(ctx);
            linearLayout.setOrientation(0);
            linearLayout.setGravity(16);
            Ui ui = Ui.INSTANCE;
            Intrinsics.checkNotNull(ctx);
            linearLayout.setMinimumHeight(ui.dp(ctx, 28.0f));
            linearLayout.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
            View spacer = new View(ctx);
            ImageView imageView = new ImageView(ctx);
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            ImageView imageView2 = new ImageView(ctx);
            imageView2.setScaleType(ImageView.ScaleType.FIT_CENTER);
            TextView textViewText$default = Ui.text$default(Ui.INSTANCE, ctx, "", 13.0f, 0, false, 24, null);
            textViewText$default.setPadding(Ui.INSTANCE.dp(ctx, 6.0f), 0, Ui.INSTANCE.dp(ctx, 4.0f), 0);
            linearLayout.addView(spacer, new LinearLayout.LayoutParams(0, 1));
            linearLayout.addView(imageView, new LinearLayout.LayoutParams(Ui.INSTANCE.dp(ctx, 20.0f), Ui.INSTANCE.dp(ctx, 28.0f)));
            linearLayout.addView(imageView2, new LinearLayout.LayoutParams(Ui.INSTANCE.dp(ctx, 16.0f), Ui.INSTANCE.dp(ctx, 16.0f)));
            linearLayout.addView(textViewText$default, new LinearLayout.LayoutParams(0, -2, 1.0f));
            return new VH(ExplorerPanel.this, linearLayout, spacer, imageView, imageView2, textViewText$default);
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public void onBindViewHolder(final VH h, int position) {
            boolean selected;
            Intrinsics.checkNotNullParameter(h, "h");
            final Row r = (Row) ExplorerPanel.this.rows.get(position);
            h.getSpacer().setLayoutParams(new LinearLayout.LayoutParams((Ui.INSTANCE.dp(h.getRow(), 14.0f) * r.getDepth()) + Ui.INSTANCE.dp(h.getRow(), 2.0f), 1));
            int select = 0;
            h.getArrow().setImageDrawable(r.getHasChildren() ? new Ui.ArrowDrawable(r.getExpanded(), 0, 2, null) : null);
            ImageView icon = h.getIcon();
            Ui ui = Ui.INSTANCE;
            Context context = h.getRow().getContext();
            Intrinsics.checkNotNullExpressionValue(context, "getContext(...)");
            icon.setImageDrawable(ui.icon(context, r.getInst()));
            TextView name = h.getName();
            String label = r.getLabel();
            if (label.length() == 0) {
                label = " ";
            }
            name.setText(label);
            Iterable selection = ExplorerPanel.this.studio.getSelection();
            if (!(selection instanceof Collection) || !((Collection) selection).isEmpty()) {
                Iterator it = selection.iterator();
                while (true) {
                    if (it.hasNext()) {
                        if (((Instance) it.next()) == r.getInst()) {
                            selected = true;
                            break;
                        }
                    } else {
                        selected = false;
                        break;
                    }
                }
            } else {
                selected = false;
            }
            boolean target = ExplorerPanel.this.dropTarget == r.getInst();
            LinearLayout row = h.getRow();
            if (target) {
                select = 1715178475;
            } else if (selected) {
                select = Ui.INSTANCE.getSELECT();
            }
            row.setBackgroundColor(select);
            h.getName().setTextColor((!selected || target) ? Ui.INSTANCE.getTEXT() : Ui.INSTANCE.getSELECT_TEXT());
            LinearLayout row2 = h.getRow();
            final ExplorerPanel explorerPanel = ExplorerPanel.this;
            row2.setOnClickListener(new View.OnClickListener() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$Adapter$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    explorerPanel.onRowClick(r);
                }
            });
            ImageView arrow = h.getArrow();
            final ExplorerPanel explorerPanel2 = ExplorerPanel.this;
            arrow.setOnClickListener(new View.OnClickListener() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$Adapter$$ExternalSyntheticLambda1
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    explorerPanel2.toggleExpand(r);
                }
            });
            LinearLayout row3 = h.getRow();
            final ExplorerPanel explorerPanel3 = ExplorerPanel.this;
            row3.setOnLongClickListener(new View.OnLongClickListener() { // from class: com.goydevv.studrblx.ui.ExplorerPanel$Adapter$$ExternalSyntheticLambda2
                @Override // android.view.View.OnLongClickListener
                public final boolean onLongClick(View view) {
                    return ExplorerPanel.Adapter.onBindViewHolder$lambda$8(explorerPanel3, h, r, view);
                }
            });
        }

        static final boolean onBindViewHolder$lambda$8(ExplorerPanel this$0, VH $h, Row $r, View it) {
            this$0.startDrag($h.getRow(), $r);
            return true;
        }
    }
}
