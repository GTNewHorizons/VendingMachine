package com.cubefury.vendingmachine.blocks.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.IntValue;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.SingleChildWidget;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Column;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Row;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cubefury.vendingmachine.VMConfig;
import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.blocks.MTEVendingMachine;
import com.cubefury.vendingmachine.gui.GuiTextures;
import com.cubefury.vendingmachine.gui.WidgetThemes;
import com.cubefury.vendingmachine.network.handlers.NetTradeDisplaySync;
import com.cubefury.vendingmachine.storage.NameCache;
import com.cubefury.vendingmachine.trade.CurrencyItem;
import com.cubefury.vendingmachine.trade.CurrencyType;
import com.cubefury.vendingmachine.trade.TradeCategory;
import com.cubefury.vendingmachine.trade.TradeDatabase;
import com.cubefury.vendingmachine.trade.TradeManager;
import com.cubefury.vendingmachine.util.BigItemStack;
import com.cubefury.vendingmachine.util.Translator;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTWidgetThemes;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

public class MTEVendingMachineGui extends MTEMultiBlockBaseGui {

    private final MTEVendingMachine base;

    public static boolean forceRefresh = false;

    private boolean ejectItems = false;
    private boolean ejectCoins = false;
    private final Map<CurrencyType, Boolean> ejectSingleCoin = new HashMap<>();
    private final Map<TradeCategory, List<TradeItemDisplayWidget>> displayedTradesTiles = new HashMap<>();
    private final Map<TradeCategory, List<TradeItemDisplayWidget>> displayedTradesList = new HashMap<>();
    private final List<TradeCategory> tradeCategories = new ArrayList<>();
    private final List<InterceptingSlot> inputSlots = new ArrayList<>();

    private PosGuiData guiData;
    private final PagedWidget.Controller tabController;
    private final SearchBar searchBar;

    public static String lastSearch = "";
    public static int lastPage = 0;
    public static SortMode sortMode = VMConfig.gui.sort_mode;

    public static final int CUSTOM_UI_HEIGHT = 320;

    // Trade Item Display
    public static final int TRADE_ROW_WIDTH = 154;
    public static final int TILE_ITEMS_PER_ROW = 3;
    public static final int TILE_ITEM_HEIGHT = 25;
    public static final int TILE_ITEM_WIDTH = 47;
    public static final int LIST_ITEM_HEIGHT = 14;
    public static final int LIST_ITEM_WIDTH = 153;

    private static final int COIN_COLUMN_WIDTH = 40;
    private static final int COIN_COLUMN_ROW_COUNT = 4;

    public MTEVendingMachineGui(MTEVendingMachine base) {
        super(base);
        this.base = base;

        for (CurrencyType type : CurrencyType.values()) {
            ejectSingleCoin.put(type, false);
        }

        this.tradeCategories.add(TradeCategory.ALL);
        this.tradeCategories.addAll(TradeDatabase.INSTANCE.getTradeCategories());

        for (TradeCategory c : this.tradeCategories) {
            displayedTradesTiles.put(c, new ArrayList<>(MTEVendingMachine.MAX_TRADES));
            for (int i = 0; i < MTEVendingMachine.MAX_TRADES; i++) {
                displayedTradesTiles.get(c)
                    .add(new TradeItemDisplayWidget(null, this.base, DisplayType.TILE));
            }
            displayedTradesList.put(c, new ArrayList<>(MTEVendingMachine.MAX_TRADES));
            for (int i = 0; i < MTEVendingMachine.MAX_TRADES; i++) {
                displayedTradesList.get(c)
                    .add(new TradeItemDisplayWidget(null, this.base, DisplayType.LIST));
            }
        }

        this.tabController = VendingMachine.proxy.isClient() ? new PagedWidget.Controller() : null;
        this.searchBar = VendingMachine.proxy.isClient() ? createSearchBar() : null;
    }

    public MTEVendingMachine getBase() {
        return base;
    }

    public static void setForceRefresh() {
        forceRefresh = true;
    }

    @Override
    public ModularPanel build(PosGuiData guiData, PanelSyncManager syncManager, UISettings uiSettings) {
        this.guiData = guiData;

        registerSyncValues(syncManager);
        ModularPanel panel = new TradeMainPanel("MTEMultiBlockBase", this, guiData, syncManager)
            .size(178, CUSTOM_UI_HEIGHT)
            .padding(4);
        panel.child(createCategoryTabs(this.tabController));
        Flow mainColumn = new Column().width(170);
        if (VendingMachine.proxy.isClient()) { // client side sort and filtering
            panel.child(createQolButtonColumn());
            mainColumn.child(
                createTitleTextStyle(
                    IKey.lang("gt.blockmachines.multimachine.vendingmachine.name.gui")
                        .style(IKey.DARK_GRAY)
                        .get()))
                .child(this.searchBar)
                .child(createTradeUI((TradeMainPanel) panel, this.tabController));
            mainColumn.child(createCoinInventoryRow((TradeMainPanel) panel, syncManager));
        }
        mainColumn.child(createInventoryRow());
        panel.child(mainColumn);
        panel.child(
            new Column().size(20)
                .right(5));
        panel.child(createIOColumn());
        return panel;
    }

    public void restorePreviousSettings() {
        if (this.tabController.isInitialised()) {
            this.tabController.setPage(lastPage);
        }
        this.searchBar.setText(lastSearch);
    }

    public IWidget createQolButtonColumn() {
        Flow buttonColumn = new Column().width(8)
            .height(20)
            .left(-17)
            .top(1)
            .coverChildren();
        buttonColumn.child(
            new CycleButtonWidget().size(14)
                .overlay(
                    new DynamicDrawable(
                        () -> VMConfig.gui.display_type.getTexture()
                            .size(14)))
                .stateCount(DisplayType.values().length)
                .value(new IntValue.Dynamic(() -> VMConfig.gui.display_type.ordinal(), val -> {
                    VMConfig.gui.display_type = DisplayType.values()[val];
                    ConfigurationManager.save(VMConfig.class);
                }))
                .tooltipDynamic(builder -> {
                    builder.clearText();
                    builder.addLine(
                        IKey.lang("vendingmachine.gui.display_mode") + " "
                            + VMConfig.gui.display_type.getLocalizedName());
                })
                .tooltipAutoUpdate(true));
        buttonColumn.child(
            new CycleButtonWidget().size(14)
                .top(17)
                .overlay(
                    new DynamicDrawable(
                        () -> VMConfig.gui.sort_mode.getTexture()
                            .size(14)))
                .stateCount(SortMode.values().length)
                .value(new IntValue.Dynamic(() -> VMConfig.gui.sort_mode.ordinal(), val -> {
                    VMConfig.gui.sort_mode = SortMode.values()[val];
                    ConfigurationManager.save(VMConfig.class);
                }))
                .tooltipDynamic(builder -> {
                    builder.clearText();
                    builder.addLine(
                        IKey.lang("vendingmachine.gui.display_sort") + " " + VMConfig.gui.sort_mode.getLocalizedName());
                    setForceRefresh();
                })
                .tooltipAutoUpdate(true));
        return buttonColumn;
    }

    public IWidget createCategoryTabs(PagedWidget.Controller tabController) {
        Flow tabColumn = new Column().width(40)
            .height(100)
            .left(-29)
            .top(40)
            .coverChildren();

        for (int i = 0; i < this.tradeCategories.size(); i++) {
            int index = i;
            tabColumn.child(
                new VendingPageButton(i, tabController).tab(GuiTextures.TAB_LEFT, -1)
                    .overlay(
                        this.tradeCategories.get(i)
                            .getTexture()
                            .asIcon()
                            .margin(6)
                            .center())
                    .tooltipBuilder(builder -> {
                        builder.clearText();
                        builder.addLine(
                            Translator.translate(
                                this.tradeCategories.get(index)
                                    .getUnlocalized_name()));
                    }));
        }
        return tabColumn;
    }

    // why is the original method private lmao
    private IWidget createTitleTextStyle(String title) {
        return new SingleChildWidget<>().coverChildren()
            .topRel(0, -4, 1)
            .leftRel(0, -4, 0)
            .widgetTheme(GTWidgetThemes.BACKGROUND_TITLE)
            .child(
                IKey.str(title)
                    .asWidget()
                    .alignment(Alignment.Center)
                    .widgetTheme(GTWidgetThemes.TEXT_TITLE)
                    .marginLeft(5)
                    .marginRight(5)
                    .marginTop(5)
                    .marginBottom(1));
    }

    private SearchBar createSearchBar() {
        return new SearchBar(this).width(162)
            .left(3)
            .top(5)
            .height(14);
    }

    // Eject code is in GUI instead of MTE since the syncers are per-gui instance
    private void doEjectCoin(CurrencyType type) {
        if (this.guiData.isClient() || !this.base.getActive()) {
            return;
        }
        UUID currentUser = NameCache.INSTANCE.getUUIDFromPlayer(base.getCurrentUser());
        if (
            !TradeManager.INSTANCE.playerCurrency.containsKey(currentUser)
                || !TradeManager.INSTANCE.playerCurrency.get(currentUser)
                    .containsKey(type)
        ) {
            this.ejectSingleCoin.put(type, false);
            return;
        }
        for (ItemStack ejectable : new CurrencyItem(
            type,
            TradeManager.INSTANCE.playerCurrency.get(currentUser)
                .get(type)).itemize()) {
            base.spawnItem(ejectable);
        }
        TradeManager.INSTANCE.resetCurrency(currentUser, type);
        this.ejectSingleCoin.put(type, false);
    }

    private void doEjectCoins() {
        if (this.guiData.isClient()) {
            return;
        }

        if (!this.base.getActive()) {
            ejectCoins = false;
            return;
        }

        UUID currentUser = NameCache.INSTANCE.getUUIDFromPlayer(base.getCurrentUser());
        if (!TradeManager.INSTANCE.playerCurrency.containsKey(currentUser)) {
            ejectCoins = false;
            return;
        }

        Map<CurrencyType, Integer> coins = TradeManager.INSTANCE.playerCurrency
            .getOrDefault(currentUser, new HashMap<>());
        for (Map.Entry<CurrencyType, Integer> entry : coins.entrySet()) {
            for (ItemStack ejectable : new CurrencyItem(entry.getKey(), entry.getValue()).itemize()) {
                base.spawnItem(ejectable);
            }
        }
        TradeManager.INSTANCE.resetCurrency(currentUser, null);
        ejectCoins = false;
    }

    private void doEjectItems() {
        if (this.guiData.isClient()) {
            return;
        }
        if (!this.base.getActive()) {
            ejectItems = false;
            return;
        }

        for (int i = 0; i < MTEVendingMachine.INPUT_SLOTS; i++) {
            ItemStack stack = base.inputItems.getStackInSlot(i);
            if (stack != null) {
                base.inputItems.setStackInSlot(i, null);
                base.spawnItem(stack.copy());
            }
        }
        ejectItems = false;
    }

    private IWidget createIOColumn() {
        return new ParentWidget<>().excludeAreaInRecipeViewer()
            .width(50)
            .height(178)
            .right(-48)
            .top(40)
            .widgetTheme(WidgetThemes.BACKGROUND_SIDEPANEL)
            .child(
                new Column().child(
                    GuiTextures.INPUT_SPRITE.asWidget()
                        .leftRel(0.5f)
                        .top(8)
                        .width(30)
                        .height(20))
                    .child(
                        new Row().child(createInputSlots().center())
                            .top(20)
                            .height(18 * 3))
                    .child(
                        new Row().child(
                            new ToggleButton().overlay(GTGuiTextures.OVERLAY_BUTTON_CYCLIC)
                                .tooltipBuilder(t -> t.addLine(IKey.lang("vendingmachine.gui.item_eject")))
                                .syncHandler("ejectItems")
                                .right(6))
                            .child(
                                new ToggleButton().overlay(
                                    GuiTextures.EJECT_COINS.asIcon()
                                        .size(14))
                                    .tooltipBuilder(t -> t.addLine(IKey.lang("vendingmachine.gui.coin_eject")))
                                    .syncHandler("ejectCoins")
                                    .left(6))
                            .top(80)
                            .height(18))
                    .child(
                        GuiTextures.OUTPUT_SPRITE.asWidget()
                            .leftRel(0.5f)
                            .bottom(52)
                            .width(30)
                            .height(20))
                    .child(
                        new Row().child(createOutputSlots().center())
                            .bottom(6)
                            .height(18 * 3))
                    .right(1));
    }

    private SlotGroupWidget createInputSlots() {
        return SlotGroupWidget.builder()
            .matrix("II", "II", "II")
            .key('I', index -> {
                InterceptingSlot slot = new InterceptingSlot(base.inputItems, index, this.base);
                this.inputSlots.add(slot);
                return new ItemSlot().slot(
                    slot.slotGroup("inputSlotGroup")
                        .changeListener((newItem, onlyAmountChanged, client, init) -> {
                            boolean hasCoin = slot.intercept(
                                newItem,
                                client,
                                this.getBase()
                                    .getCurrentUser());
                            if (client) {
                                return;
                            }
                            // server side force refresh
                            // Not syncing the trades to client on slot change will cause a short refresh delay, but
                            // might be worth
                            // for huge AE systems
                            NetTradeDisplaySync.syncTradesToClient(
                                (EntityPlayerMP) this.getBase()
                                    .getCurrentUser(),
                                this.getBase());
                            if (hasCoin) {
                                this.refreshInputSlots();
                            }
                            base.markDirty();
                        }));
            })
            .build();
    }

    private SlotGroupWidget createOutputSlots() {
        return SlotGroupWidget.builder()
            .matrix("II", "II", "II")
            .key('I', index -> {
                return new ItemSlot().slot(
                    new ModularSlot(base.outputItems, index).accessibility(false, true)
                        .slotGroup("outputSlotGroup")
                        .changeListener((newitem, onlyAmountChanged, client, init) -> { base.markDirty(); }));
            })
            .build();
    }

    private void constructTradeTooltip(RichTooltip builder, TradeItemDisplay cur) {
        if (cur != null) {
            for (BigItemStack toItem : cur.toItems) {
                builder.addLine(
                    IKey.str(
                        toItem.stackSize + " "
                            + toItem.getBaseStack()
                                .getDisplayName())
                        .style(IKey.AQUA));
                // builder.add(new ItemDrawable(toItem.getBaseStack()));
            }
            builder.emptyLine();

            if (!cur.fromCurrency.isEmpty() || !cur.fromItems.isEmpty()) {
                builder.addLine(
                    IKey.lang("vendingmachine.gui.required_inputs")
                        .style(IKey.DARK_GREEN, IKey.ITALIC));
                for (CurrencyItem currencyItem : cur.fromCurrency) {
                    builder.addLine(
                        IKey.str(currencyItem.value + " " + currencyItem.type.getLocalizedName())
                            .style(IKey.DARK_GREEN));
                }
                for (BigItemStack fromItem : cur.fromItems) {
                    builder.addLine(
                        IKey.str(
                            fromItem.stackSize + " "
                                + fromItem.getBaseStack()
                                    .getDisplayName())
                            .style(IKey.DARK_GREEN));
                }
                builder.emptyLine();
            }
            if (!cur.ncItems.isEmpty()) {
                builder.addLine(
                    IKey.lang("vendingmachine.gui.nc_inputs")
                        .style(IKey.DARK_GREEN, IKey.ITALIC));
                for (BigItemStack fromItem : cur.ncItems) {
                    builder.addLine(
                        IKey.str(
                            fromItem.stackSize + " "
                                + fromItem.getBaseStack()
                                    .getDisplayName())
                            .style(IKey.DARK_GREEN));
                }
                builder.emptyLine();
            }

            builder.addLine(
                IKey.str(Translator.translate("vendingmachine.gui.trade_hint"))
                    .style(IKey.GRAY));
        }
    }

    // spotless:off
    private IWidget createTradeUI(TradeMainPanel rootPanel, PagedWidget.Controller tabController) {
        PagedWidget<?> paged = new PagedWidget<>()
            .name("paged")
            .width(162)
            .controller(tabController)
            .background(GuiTextures.TEXT_FIELD_BACKGROUND)
            .height(146);
        for (TradeCategory category : this.tradeCategories) {
            ListWidget<IWidget, ?> tradeList = new ListWidget<>()
                .name("items")
                .width(161)
                .top(1)
                .height(144)
                .collapseDisabledChild(true);

            tradeList.child(new Row().height(2));
            // Higher first row top margin
            Flow row = new TradeRow().height(TILE_ITEM_HEIGHT +2).width(TRADE_ROW_WIDTH).marginLeft(2);

            // Tiles Display
            for (int i = 0; i < MTEVendingMachine.MAX_TRADES; i++) {
                int index = i;
                displayedTradesTiles.get(category).get(i).setRootPanel(rootPanel);
                row.child(displayedTradesTiles.get(category).get(i)
                    .tooltipDynamic(builder -> {
                        builder.clearText();
                        synchronized (displayedTradesTiles) {
                            if (index < displayedTradesTiles.get(category).size()) {
                                constructTradeTooltip(builder, displayedTradesTiles.get(category).get(index).getDisplay());
                                }
                            }
                    })
                    .tooltipAutoUpdate(true)
                    .setEnabledIf(slot -> {
                        TradeItemDisplayWidget display = ((TradeItemDisplayWidget) slot);
                        return VMConfig.gui.display_type == display.displayType && display.getDisplay() != null;
                    })
                    .margin(2));
                if (i % TILE_ITEMS_PER_ROW == TILE_ITEMS_PER_ROW - 1) {
                    tradeList.child(row);

                    row = new TradeRow().height(TILE_ITEM_HEIGHT +2).width(TRADE_ROW_WIDTH).marginLeft(2);
                }
            }
            if (row.hasChildren()) {
                tradeList.child(row);
            }

            // List Display
            row = new TradeRow().height(LIST_ITEM_HEIGHT).width(TRADE_ROW_WIDTH).marginLeft(2);
            for (int i = 0; i < MTEVendingMachine.MAX_TRADES; i++) {
                int index = i;
                displayedTradesList.get(category).get(i).setRootPanel(rootPanel);
                row.child(displayedTradesList.get(category).get(i)
                    .tooltipDynamic(builder -> {
                        builder.clearText();
                        synchronized (displayedTradesList) {
                            if (index < displayedTradesList.get(category).size()) {
                                constructTradeTooltip(builder, displayedTradesList.get(category).get(index).getDisplay());
                            }
                        }
                    })
                    .tooltipAutoUpdate(true)
                    .setEnabledIf(slot -> {
                        TradeItemDisplayWidget display = ((TradeItemDisplayWidget) slot);
                        return VMConfig.gui.display_type == display.displayType && display.getDisplay() != null;
                    }));
                tradeList.child(row);
                row = new TradeRow().height(LIST_ITEM_HEIGHT).width(TRADE_ROW_WIDTH).marginLeft(2);
            }

            tradeList.child(new Row().height(2)); // bottom padding for last row
            paged.addPage(tradeList);
        }

        return new Row().child(paged.top(0))
            .left(3)
            .top(24);
    }
    // spotless:on

    private static String getReadableStringFromCoinAmount(int amount) {
        if (amount < 10000) {
            return "" + amount;
        } else if (amount < 1000000) {
            return amount / 1000 + "K";
        } else {
            return amount / 1000000 + "M";
        }
    }

    private IWidget createCoinInventoryRow(TradeMainPanel panel, PanelSyncManager syncManager) {
        Flow parent = new Row() // .background(GuiTextures.TEXT_FIELD_BACKGROUND)
            .width(162)
            .height(36)
            .top(172)
            .left(3);
        Flow coinColumn = new Column().width(COIN_COLUMN_WIDTH);
        int coinCount = 0;

        for (CurrencyType type : CurrencyType.values()) {
            coinColumn.child(createCoinDisplay(panel, type, syncManager));
            if (++coinCount % COIN_COLUMN_ROW_COUNT == 0) {
                parent.child(coinColumn.left(3 + COIN_COLUMN_WIDTH * (coinCount / COIN_COLUMN_ROW_COUNT - 1)));
                coinColumn = new Column().width(COIN_COLUMN_WIDTH);
            }
        }
        if (coinColumn.hasChildren()) {
            parent.child(coinColumn.left(3 + COIN_COLUMN_WIDTH * (coinCount / COIN_COLUMN_ROW_COUNT)));
        }
        return parent;
    }

    private IWidget createCoinDisplay(TradeMainPanel panel, CurrencyType type, PanelSyncManager syncManager) {
        IntSyncValue coinSyncValue = syncManager.findSyncHandler("coinAmount_" + type.id, 0, IntSyncValue.class);
        return new Row().child(
            new CoinButton(panel, type).overlay(
                type.texture.asIcon()
                    .size(12))
                .size(12)
                .left(0)
                .syncHandler("ejectCoin_" + type.id)
                .tooltipDynamic((builder) -> {
                    builder.clearText();
                    builder.addLine(coinSyncValue.getValue() + " " + type.getLocalizedName());
                    builder.emptyLine();
                    builder.addLine(
                        IKey.str(Translator.translate("vendingmachine.gui.single_coin_type_eject_hint"))
                            .style(IKey.GRAY, IKey.ITALIC));
                    builder.setAutoUpdate(true);
                }))
            .child(
                IKey.dynamic(() -> getReadableStringFromCoinAmount(coinSyncValue.getValue()))
                    .scale(0.8f)
                    .asWidget()
                    .top(3)
                    .left(14)
                    .width(21))
            .height(14);
    }

    // why is the original method private lmao
    private IWidget createInventoryRow() {
        return new Row().widthRel(1)
            .height(76)
            .alignX(0)
            .bottom(5)
            .childIf(
                base.doesBindPlayerInventory(),
                SlotGroupWidget.playerInventory(false)
                    .marginLeft(4));
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);

        syncManager.registerSlotGroup("inputSlotGroup", 2, true);
        syncManager.registerSlotGroup("outputSlotGroup", 2, false);

        BooleanSyncValue ejectItemsSyncer = new BooleanSyncValue(() -> this.ejectItems, val -> {
            this.ejectItems = val;
            if (this.ejectItems) {
                doEjectItems();
            }
        });
        syncManager.syncValue("ejectItems", ejectItemsSyncer);

        BooleanSyncValue ejectCoinsSyncer = new BooleanSyncValue(() -> this.ejectCoins, val -> {
            this.ejectCoins = val;
            if (this.ejectCoins) {
                doEjectCoins();
            }
        });
        syncManager.syncValue("ejectCoins", ejectCoinsSyncer);

        UUID playerId = NameCache.INSTANCE.getUUIDFromPlayer(getBase().getCurrentUser());
        for (CurrencyType type : CurrencyType.values()) {
            IntSyncValue coinAmountSyncer = new IntSyncValue(
                () -> TradeManager.INSTANCE.playerCurrency.getOrDefault(playerId, Collections.emptyMap())
                    .getOrDefault(type, 0));
            syncManager.syncValue("coinAmount_" + type.id, coinAmountSyncer);

            BooleanSyncValue ejectCoinSyncer = new BooleanSyncValue(() -> this.ejectSingleCoin.get(type), val -> {
                this.ejectSingleCoin.put(type, val);
                if (val) {
                    doEjectCoin(type);
                }
            });
            syncManager.syncValue("ejectCoin_" + type.id, ejectCoinSyncer);
        }

    }

    public void attemptPurchase(TradeItemDisplay display) {
        submitTradesToServer(display);
        forceRefresh = true;
    }

    private void submitTradesToServer(TradeItemDisplay trade) {
        if (!trade.tradeableNow || !trade.enabled) {
            return;
        }
        base.sendTradeRequest(trade);
    }

    public static void resetForceRefresh() {
        forceRefresh = false;
    }

    private void updateTradeDisplay(Map<TradeCategory, List<TradeItemDisplay>> trades,
        Map<TradeCategory, List<TradeItemDisplayWidget>> display) {
        synchronized (display) {
            for (Map.Entry<TradeCategory, List<TradeItemDisplayWidget>> entry : display.entrySet()) {
                int displayedSize = trades.get(entry.getKey()) == null ? 0
                    : trades.get(entry.getKey())
                        .size();
                for (int i = 0; i < MTEVendingMachine.MAX_TRADES; i++) {
                    if (i < displayedSize) {
                        entry.getValue()
                            .get(i)
                            .setDisplay(
                                trades.get(entry.getKey())
                                    .get(i));
                    } else {
                        entry.getValue()
                            .get(i)
                            .setDisplay(null);
                    }
                }
            }
        }
    }

    public void updateTradeDisplay(Map<TradeCategory, List<TradeItemDisplay>> trades) {
        this.updateTradeDisplay(trades, displayedTradesTiles);
        this.updateTradeDisplay(trades, displayedTradesList);
    }

    public Map<TradeCategory, List<TradeItemDisplay>> getCurrentTradeDisplayData() {
        Map<TradeCategory, List<TradeItemDisplay>> currentData = new HashMap<>();

        synchronized (displayedTradesTiles) {
            this.displayedTradesTiles.forEach((k, v) -> {
                currentData.put(
                    k,
                    v.stream()
                        .map(TradeItemDisplayWidget::getDisplay)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
            });
        }

        synchronized (displayedTradesList) {
            this.displayedTradesList.forEach((k, v) -> {
                currentData.get(k)
                    .addAll(
                        v.stream()
                            .map(TradeItemDisplayWidget::getDisplay)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()));
            });
        }

        return currentData;
    }

    public String getSearchBarText() {
        return this.searchBar.getText();
    }

    // server-side sync for all input slots
    // during next tick after any input
    private void refreshInputSlots() {
        for (InterceptingSlot slot : this.inputSlots) {
            slot.getSyncHandler()
                .forceSyncItem();
        }
    }

}
