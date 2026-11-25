package com.goydevv.studrblx.ui;

import com.goydevv.studrblx.net.CloudPlace;
import kotlin.jvm.functions.Function0;

/* JADX INFO: compiled from: MainActions.kt */
/* JADX INFO: loaded from: classes5.dex */
public interface MainActions {
    void addSelectionToToolbox();

    void uploadSelectionToRoblox();

    void applyThemeChange();

    void exportSelection();

    void importToToolbox();

    void openPlaceBytes(String name, byte[] bytes, CloudPlace cloud);

    void pickPluginFile();

    void pluginsChanged();

    void saveBytesAs(String fileName, Function0<byte[]> producer);
}
