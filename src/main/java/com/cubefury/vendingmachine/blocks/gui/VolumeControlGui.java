package com.cubefury.vendingmachine.blocks.gui;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.DoubleValue.Dynamic;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cubefury.vendingmachine.VMConfig;
import com.cubefury.vendingmachine.util.ColorUtils;
import com.cubefury.vendingmachine.util.VMMusicManager;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

public class VolumeControlGui {

    private boolean volumeChanged = false;

    public final ModularPanel createPanel(PanelSyncManager syncManager, IWidget parent) {
        ModularPanel panel = new ModularPanel("volume").coverChildren();
        addWidgets(panel, syncManager);
        panel.child(ButtonWidget.panelCloseButton());
        if (syncManager.isClient()) {
            panel.relative(parent)
                .topRel(1f, 5, 0);
            panel.onCloseAction(() -> {
                if (volumeChanged) {
                    ConfigurationManager.save(VMConfig.class);
                }
            });
        }
        return panel;
    }

    private void addWidgets(ModularPanel panel, PanelSyncManager syncManager) {
        if (!syncManager.isClient()) return;
        panel.coverChildren()
            .padding(5)
            .child(
                Flow.column()
                    .coverChildren()
                    .child(
                        IKey.lang("vendingmachine.gui.volume.title")
                            .asWidget()
                            .paddingRight(20)
                            .leftRel(0))
                    .child(
                        Flow.row()
                            .coverChildren()
                            .marginTop(5)
                            .child(
                                new SliderWidget().bounds(0.01f, 1f)
                                    .width(100)
                                    .height(10)
                                    .marginRight(5)
                                    .verticalCenter()
                                    .background(new Rectangle().color(ColorUtils.volumeSliderBackground.getColor()))
                                    .value(new Dynamic(() -> VMConfig.music.music_volume, value -> {
                                        VMConfig.music.music_volume = (float) value;
                                        VMMusicManager.onVolumeChange();
                                        volumeChanged = true;
                                    })))
                            .child(
                                IKey.dynamic(
                                    () -> StatCollector.translateToLocalFormatted(
                                        "vendingmachine.gui.volume.percent",
                                        getVolumeAsString()))
                                    .asWidget()
                                    .width(30)
                                    .verticalCenter())));
    }

    public static String getVolumeAsString() {
        return Integer.toString((int) (VMConfig.music.music_volume * 100));
    }

}
