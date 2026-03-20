package com.cubefury.vendingmachine.blocks.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cubefury.vendingmachine.VMConfig;
import com.cubefury.vendingmachine.network.handlers.NetResetVMUser;
import com.cubefury.vendingmachine.trade.FavouritesTracker;
import com.cubefury.vendingmachine.trade.TradeCategory;
import com.cubefury.vendingmachine.trade.TradeDatabase;
import com.cubefury.vendingmachine.trade.TradeGroup;
import com.cubefury.vendingmachine.trade.TradeManager;
import com.cubefury.vendingmachine.util.BigItemStack;

import codechicken.nei.SearchField;
import codechicken.nei.api.ItemFilter;

public class TradeMainPanel extends ModularPanel {

    public boolean shiftHeld = false;
    public boolean ctrlHeld = false;
    private final MTEVendingMachineGui gui;
    private final PanelSyncManager syncManager;
    private final PosGuiData guiData;
    private EntityPlayer player = null;
    private int ticksOpen = 0;
    public UUID currentSelected = null;

    public TradeMainPanel(@NotNull String name, MTEVendingMachineGui gui, PosGuiData guiData,
        PanelSyncManager syncManager) {
        super(name);
        this.gui = gui;
        this.guiData = guiData;
        this.syncManager = syncManager;
    }

    @Override
    public boolean onKeyPressed(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_LSHIFT || keyCode == Keyboard.KEY_RSHIFT) {
            shiftHeld = true;
        }
        if (keyCode == Keyboard.KEY_LCONTROL || keyCode == Keyboard.KEY_RCONTROL) {
            ctrlHeld = true;
        }
        return super.onKeyPressed(typedChar, keyCode);
    }

    @Override
    public boolean onKeyRelease(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_LSHIFT || keyCode == Keyboard.KEY_RSHIFT) {
            shiftHeld = false;
        }
        if (keyCode == Keyboard.KEY_LCONTROL || keyCode == Keyboard.KEY_RCONTROL) {
            ctrlHeld = false;
            gui.setForceRefresh();
        }
        return super.onKeyRelease(typedChar, keyCode);
    }

    public void updateGui() {
        if (shiftHeld || ctrlHeld) {
            this.updateTradeInformation(gui.getCurrentTradeDisplayData());
        } else {
            Map<TradeCategory, List<TradeItemDisplay>> trades = formatTrades();
            gui.updateTradeDisplay(trades);
        }
    }

    private void updateTradeInformation(Map<TradeCategory, List<TradeItemDisplay>> currentData) {
        Map<UUID, Map<Integer, TradeItemDisplay>> tradeMap = new HashMap<>();
        List<TradeItemDisplay> favouritedTrades = FavouritesTracker.INSTANCE
            .filterTrades(currentData.get(TradeCategory.ALL));
        if (gui.favouritesTabWidget != null) {
            gui.favouritesTabWidget.setEnabled(!favouritedTrades.isEmpty());
        }
        currentData.put(TradeCategory.FAVOURITES, favouritedTrades);
        for (TradeItemDisplay tid : TradeManager.INSTANCE.tradeData) {
            tradeMap.putIfAbsent(tid.tgID, new HashMap<>());
            tradeMap.get(tid.tgID)
                .put(tid.tradeGroupOrder, tid);
        }

        currentData.forEach((k, v) -> {
            for (TradeItemDisplay tid : v) {
                TradeItemDisplay cur = tradeMap.get(tid.tgID)
                    .get(tid.tradeGroupOrder);
                tid.enabled = cur != null && cur.enabled;
                tid.hasCooldown = cur.cooldown > 0;
                tid.cooldown = cur.cooldown;
                tid.cooldownText = cur.cooldownText;
                tid.tradeableNow = cur.tradeableNow;
                tid.isFavourite = FavouritesTracker.INSTANCE.isFavourite(tid);
            }
        });
    }

    @Override
    public void onUpdate() {

        super.onUpdate();
        if (!this.guiData.isClient()) {
            return;
        }
        if (this.player == null && this.syncManager.isInitialised()) {
            this.player = syncManager.getPlayer();
        }
        if (TradeManager.INSTANCE.hasCurrencyUpdate) {
            MTEVendingMachineGui.setForceRefresh();
        }
        if (
            MTEVendingMachineGui.forceRefresh
                || (this.ticksOpen % VMConfig.vendingMachineSettings.gui_refresh_interval == 0 && player != null)
        ) {
            updateGui();
            MTEVendingMachineGui.resetForceRefresh();
            TradeManager.INSTANCE.hasCurrencyUpdate = false;
        }
        TradeCategory activeCategory = gui.getActiveTradeCategory();
        Map<TradeCategory, List<TradeItemDisplayWidget>> displayedTrades = VMConfig.gui.display_type == DisplayType.TILE
            ? gui.displayedTradesTiles
            : gui.displayedTradesList;

        this.currentSelected = null;
        for (TradeItemDisplayWidget display : displayedTrades.get(activeCategory)) {
            if (display.isBelowMouse()) {
                this.currentSelected = display.getDisplay().tgID;
                break;
            }
        }

        this.ticksOpen += 1;
    }

    public ItemStack convertToItemStack(BigItemStack stack) {
        ItemStack display = stack.getCombinedStacks()
            .get(0);
        display.stackSize = stack.stackSize;
        return display;
    }

    public Map<TradeCategory, List<TradeItemDisplay>> formatTrades() {
        Map<TradeCategory, List<TradeItemDisplay>> trades = new HashMap<>();
        trades.put(TradeCategory.ALL, new ArrayList<>());
        SortMode sortMode = VMConfig.gui.sort_mode;

        for (TradeItemDisplay tid : TradeManager.INSTANCE.tradeData) {
            TradeGroup group = TradeDatabase.INSTANCE.getTradeGroupFromId(tid.tgID);
            if (group == null) {
                continue;
            }
            tid.isFavourite = FavouritesTracker.INSTANCE.isFavourite(tid);
            TradeCategory category = group.getCategory();
            trades.putIfAbsent(category, new ArrayList<>());
            trades.get(category)
                .add(tid);
            trades.get(TradeCategory.ALL)
                .add(tid);
        }

        String searchString = gui.getSearchBarText();
        ItemFilter filter = SearchField.getFilter(searchString);

        for (TradeCategory category : trades.keySet()) {
            List<TradeItemDisplay> filteredTrades = trades.get(category);
            filteredTrades = filteredTrades.stream()
                .filter(tid -> tid.satisfiesSearch(filter, searchString.toLowerCase()))
                .collect(Collectors.toList());
            filteredTrades.sort((a, b) -> {
                // null case
                if (a == null && b == null) return 0;
                if (a == null) return 1;
                if (b == null) return -1;
                if (a.display.getItem() == null && b.display.getItem() == null) return 0;
                if (a.display.getItem() == null) return 1;
                if (b.display.getItem() == null) return -1;

                if (sortMode == SortMode.ALPHABET) {
                    if (a.isFavourite != b.isFavourite) {
                        return Boolean.compare(b.isFavourite, a.isFavourite);
                    }
                    return (a.display.getDisplayName()
                        .compareTo(b.display.getDisplayName()));
                } else if (sortMode == SortMode.SMART) {
                    // favourited
                    if (a.isFavourite != b.isFavourite) {
                        return Boolean.compare(b.isFavourite, a.isFavourite);
                    }

                    // enabled or has cooldown
                    int rankA = getRank(a);
                    int rankB = getRank(b);

                    if (rankA != rankB) {
                        return Integer.compare(rankA, rankB);
                    }

                    // cooldown time
                    int cooldownCmp = Long.compare(b.cooldown, a.cooldown);
                    if (cooldownCmp != 0) return cooldownCmp;

                    // display item ordering
                    int idCmp = Integer
                        .compare(Item.getIdFromItem(a.display.getItem()), Item.getIdFromItem(b.display.getItem()));
                    if (idCmp != 0) return idCmp;
                    int dmgCmp = Integer.compare(a.display.getItemDamage(), b.display.getItemDamage());
                    if (dmgCmp != 0) return dmgCmp;

                    // sort by tradegroup Order
                    return Integer.compare(a.tradeGroupOrder, b.tradeGroupOrder);
                }

                // impossible
                return 0;
            });
            trades.replace(category, filteredTrades);
        }
        List<TradeItemDisplay> favouritedTrades = FavouritesTracker.INSTANCE
            .filterTrades(trades.get(TradeCategory.ALL));
        trades.put(TradeCategory.FAVOURITES, favouritedTrades);
        return trades;
    }

    private static int getRank(TradeItemDisplay t) {
        if (!t.enabled) {
            return 5;
        }
        if (t.tradeableNow) {
            return t.hasCooldown ? 2 : 1;
        }
        return t.hasCooldown ? 4 : 3;
    }

    public void attemptPurchase(TradeItemDisplay display) {
        gui.attemptPurchase(display);
    }

    public void forceGuiRefresh() {
        gui.setForceRefresh();
    }

    @Override
    public void dispose() {
        this.gui.getBase()
            .resetCurrentUser(this.player);
        // We have to sync reset use manually since dispose() is only run client-side
        NetResetVMUser.sendReset(this.gui.getBase());
        super.dispose();
    }

    @Override
    public void onOpen(ModularScreen screen) {
        super.onOpen(screen);
        gui.restorePreviousSettings();
    }
}
