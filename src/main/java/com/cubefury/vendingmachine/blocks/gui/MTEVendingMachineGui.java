package com.cubefury.vendingmachine.blocks.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.IntValue;
import com.cleanroommc.modularui.value.IntValue.Dynamic;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.EnumSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.SingleChildWidget;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
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
import com.cubefury.vendingmachine.trade.FavouritesTracker;
import com.cubefury.vendingmachine.trade.TradeCategory;
import com.cubefury.vendingmachine.trade.TradeDatabase;
import com.cubefury.vendingmachine.trade.TradeGroup;
import com.cubefury.vendingmachine.trade.TradeManager;
import com.cubefury.vendingmachine.util.BigItemStack;
import com.cubefury.vendingmachine.util.Translator;
import com.cubefury.vendingmachine.util.Wallet;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamManager;

import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTWidgetThemes;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

public class MTEVendingMachineGui extends MTEMultiBlockBaseGui {

    private final MTEVendingMachine base;

    public static boolean forceRefresh = false;

    private boolean ejectItems = false;
    private boolean ejectCoins = false;
    private final Map<CurrencyType, Boolean> ejectSingleCoin = new HashMap<>();
    public final Map<TradeCategory, List<TradeItemDisplayWidget>> displayedTradesTiles = new HashMap<>();
    public final Map<TradeCategory, List<TradeItemDisplayWidget>> displayedTradesList = new HashMap<>();
    public final Set<TradeCategory> highlightedTabs = new HashSet<>();
    private final List<TradeCategory> tradeCategories = new ArrayList<>();
    private final List<InterceptingSlot> inputSlots = new ArrayList<>();

    private PosGuiData guiData;
    private final PagedWidget.Controller tabController;
    public IWidget favouritesTabWidget;
    private final SearchBar searchBar;
    private CycleButtonWidget walletButton;

    public static String lastSearch = "";
    public static int lastPage = 0;

    public WalletMode walletMode = WalletMode.PERSONAL;
    public boolean shouldSyncWalletMode = true;

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

        this.tradeCategories.add(TradeCategory.FAVOURITES);
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
        panel.onCloseAction(() -> {
            if (VendingMachine.proxy.isClient()) {
                FavouritesTracker.INSTANCE.saveFavourites();
            }
        });
        panel.child(createCategoryTabs(this.tabController));
        Flow mainColumn = Flow.column()
            .width(170);
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
            Flow.column()
                .size(20)
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
        Flow buttonColumn = Flow.column()
            .width(8)
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
        Flow tabColumn = Flow.column()
            .width(40)
            .height(300)
            .left(-29)
            .top(40)
            .coverChildren();

        for (int i = 0; i < this.tradeCategories.size(); i++) {
            int index = i;
            tabColumn.child(
                new VendingPageButton(i, tabController).tab(GuiTextures.TAB_LEFT, -1)
                    .overlay(new DynamicDrawable(() -> {

                        if (highlightedTabs.contains(this.tradeCategories.get(index))) {
                            return GuiTextures.TAB_HIGHLIGHT.asIcon()
                                .size(20, 20);
                        }
                        return IDrawable.EMPTY;
                    }),
                        this.tradeCategories.get(index)
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

            if (tradeCategories.get(i) == TradeCategory.FAVOURITES) {
                favouritesTabWidget = tabColumn.getChildren()
                    .get(
                        tabColumn.getChildren()
                            .size() - 1);
            }
        }
        return tabColumn;
    }

    public TradeCategory getActiveTradeCategory() {
        return this.tradeCategories.get(this.tabController.getActivePageIndex());
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
                    .textAlign(Alignment.Center)
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
        UUID playerId = NameCache.INSTANCE.getUUIDFromPlayer(base.getCurrentUser());
        Wallet wallet = TradeManager.INSTANCE.getWallet(playerId, walletMode);
        if (wallet == null || wallet.getCount(type) <= 0) {
            this.ejectSingleCoin.put(type, false);
            return;
        }
        for (ItemStack ejectable : new CurrencyItem(type, wallet.getCount(type)).itemize()) {
            base.spawnItem(ejectable);
        }
        wallet.resetCount(type);
        TradeManager.INSTANCE.saveTeamData(playerId);
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
        UUID playerId = NameCache.INSTANCE.getUUIDFromPlayer(base.getCurrentUser());
        Wallet wallet = TradeManager.INSTANCE.getWallet(playerId, walletMode);

        if (wallet == null) {
            ejectCoins = false;
            return;
        }

        for (CurrencyType type : CurrencyType.values()) {
            if (wallet.getCount(type) > 0) {
                for (ItemStack ejectable : new CurrencyItem(type, wallet.getCount(type)).itemize()) {
                    base.spawnItem(ejectable);
                }
            }
        }

        wallet.resetAllCount();
        TradeManager.INSTANCE.saveTeamData(playerId);
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
            .height(214)
            .right(-48)
            .top(40)
            .widgetTheme(WidgetThemes.BACKGROUND_SIDEPANEL)
            .child(
                Flow.column()
                    .child(
                        GuiTextures.INPUT_SPRITE.asWidget()
                            .leftRel(0.5f)
                            .top(8)
                            .width(30)
                            .height(20))
                    .child(
                        (IWidget) new TextWidget(IKey.lang("vendingmachine.gui.in")).textAlign(Alignment.CENTER)
                            .top(8)
                            .widthRel(1.0f))
                    .child(
                        Flow.row()
                            .child(createInputSlots().center())
                            .top(20)
                            .height(18 * 4))
                    .child(
                        Flow.row()
                            .child(
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
                            .top(98)
                            .height(18))
                    .child(
                        GuiTextures.OUTPUT_SPRITE.asWidget()
                            .leftRel(0.5f)
                            .bottom(73)
                            .width(30)
                            .height(20))
                    .child(
                        (IWidget) new TextWidget(IKey.lang("vendingmachine.gui.out")).textAlign(Alignment.CENTER)
                            .bottom(81)
                            .widthRel(1.0f))
                    .child(
                        Flow.row()
                            .child(createOutputSlots().center())
                            .bottom(6)
                            .height(18 * 4))
                    .right(1));
    }

    private SlotGroupWidget createInputSlots() {
        UUID playerId = NameCache.INSTANCE.getUUIDFromPlayer(getBase().getCurrentUser());
        return SlotGroupWidget.builder()
            .matrix("II", "II", "II", "II")
            .key('I', index -> {
                InterceptingSlot slot = new InterceptingSlot(base.inputItems, index, this.base);
                this.inputSlots.add(slot);
                return new ItemSlot().slot(
                    slot.slotGroup("inputSlotGroup")
                        .changeListener((newItem, onlyAmountChanged, client, init) -> {
                            boolean hasCoin = slot.intercept(newItem, client, playerId, walletMode);
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
            .matrix("II", "II", "II", "II")
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
                StringBuilder nameLine = new StringBuilder();
                if (toItem.stackSize > 1) {
                    nameLine.append(
                        IKey.str(toItem.stackSize + " ")
                            .style(IKey.AQUA));
                }
                nameLine.append(IKey.RESET);
                ItemStack baseStack = toItem.getBaseStack();
                boolean hasCustomName = false;
                if (
                    baseStack.stackTagCompound != null
                        && baseStack.stackTagCompound.hasKey("display", Constants.NBT.TAG_COMPOUND)
                ) {
                    NBTTagCompound nbt = baseStack.stackTagCompound.getCompoundTag("display");
                    if (nbt.hasKey("Name", Constants.NBT.TAG_STRING)) {
                        nameLine.append(
                            IKey.str(nbt.getString("Name") + " ")
                                .style(IKey.AQUA, IKey.ITALIC));
                        nameLine.append(IKey.RESET);
                        hasCustomName = true;
                    }
                }
                if (hasCustomName) {
                    nameLine.append(
                        IKey.str(
                            "(" + baseStack.getItem()
                                .getItemStackDisplayName(baseStack) + ")")
                            .style(IKey.AQUA));
                } else {
                    nameLine.append(
                        IKey.str(
                            baseStack.getItem()
                                .getItemStackDisplayName(baseStack))
                            .style(IKey.AQUA));
                }
                builder.addLine(nameLine.toString());
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
                                    .getDisplayName()
                                + (fromItem.hasOreDict()
                                    ? " (" + IKey.lang("vendingmachine.gui.alternative_oredict")
                                        + " "
                                        + fromItem.getOreDict()
                                        + ")"
                                    : ""))
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

            TradeGroup tg = TradeDatabase.INSTANCE.getTradeGroupFromId(cur.tgID);
            if (
                tg != null && tg.getTrades()
                    .size() > 1
            ) {
                builder.addLine(
                    IKey.str(
                        Translator.translate(
                            "vendingmachine.gui.shared_trades_tooltip",
                            tg.getTrades()
                                .size() - 1))
                        .style(IKey.AQUA, IKey.ITALIC));
            }
            builder.emptyLine();

            builder.addLine(
                IKey.str(Translator.translate("vendingmachine.gui.trade_hint"))
                    .style(IKey.GRAY));
            builder.addLine(
                IKey.str(Translator.translate("vendingmachine.gui.favourite_hint"))
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

            tradeList.child(Flow.row().height(2));

            // Incomplete Structure status message
            Flow statusRow = Flow.row().height(10).width(TRADE_ROW_WIDTH).marginLeft(2)
                .child(new TextWidget(IKey.lang("vendingmachine.gui.error.incomplete_structure")))
                .setEnabledIf(slot -> !this.getBase().getActive());
            tradeList.child(statusRow);

            // Higher first row top margin
            Flow row = new TradeRow().height(TILE_ITEM_HEIGHT + 4).width(TRADE_ROW_WIDTH).marginLeft(2);

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
                        if (!this.getBase().getActive()) {
                            return false;
                        }
                        TradeItemDisplayWidget display = ((TradeItemDisplayWidget) slot);
                        return VMConfig.gui.display_type == display.displayType && display.getDisplay() != null;
                    })
                    .margin(2));
                if (i % TILE_ITEMS_PER_ROW == TILE_ITEMS_PER_ROW - 1) {
                    tradeList.child(row);

                    row = new TradeRow().height(TILE_ITEM_HEIGHT + 4).width(TRADE_ROW_WIDTH).marginLeft(2);
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
                        if (!this.getBase().getActive()) {
                            return false;
                        }
                        TradeItemDisplayWidget display = ((TradeItemDisplayWidget) slot);
                        return VMConfig.gui.display_type == display.displayType && display.getDisplay() != null;
                    }));
                tradeList.child(row);
                row = new TradeRow().height(LIST_ITEM_HEIGHT).width(TRADE_ROW_WIDTH).marginLeft(2);
            }

            tradeList.child(Flow.row().height(2)); // bottom padding for last row
            paged.addPage(tradeList);
        }

        return Flow.row().child(paged.top(0))
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
        Flow parent = Flow.row()
            .width(162)
            .height(36)
            .top(172)
            .left(3);
        Flow coinColumn = Flow.column()
            .width(COIN_COLUMN_WIDTH);
        int coinCount = 0;

        for (CurrencyType type : CurrencyType.values()) {
            coinColumn.child(createCoinDisplay(panel, type, syncManager));
            if (++coinCount % COIN_COLUMN_ROW_COUNT == 0) {
                parent.child(coinColumn.left(3 + COIN_COLUMN_WIDTH * (coinCount / COIN_COLUMN_ROW_COUNT - 1)));
                coinColumn = Flow.column()
                    .width(COIN_COLUMN_WIDTH);
            }
        }
        coinColumn.child(
            walletButton = new CycleButtonWidget().width(COIN_COLUMN_WIDTH)
                .marginTop(6)
                .overlay(
                    IKey.dynamicKey(() -> IKey.lang(walletMode.getLocalizedName()))
                        .scale(0.75f))
                .stateCount(SortMode.values().length)
                .value(new Dynamic(() -> walletMode.ordinal(), val -> {
                    VMConfig.gui.wallet_mode = walletMode = WalletMode.values()[val];
                    shouldSyncWalletMode = true;
                    setForceRefresh();
                }))
                .tooltipDynamic(builder -> {
                    builder.clearText();
                    builder
                        .addLine(IKey.lang("vendingmachine.gui.display_wallet") + " " + walletMode.getLocalizedName());
                })
                .tooltipAutoUpdate(true));

        if (coinColumn.hasChildren()) {
            parent.child(coinColumn.left(3 + COIN_COLUMN_WIDTH * (coinCount / COIN_COLUMN_ROW_COUNT)));
        }
        return parent;
    }

    private IWidget createCoinDisplay(TradeMainPanel panel, CurrencyType type, PanelSyncManager syncManager) {
        IntSyncValue coinSyncValue = syncManager.findSyncHandler("coinAmount_" + type.id, 0, IntSyncValue.class);
        return Flow.row()
            .child(
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
        return Flow.row()
            .widthRel(1)
            .height(76)
            .leftRel(0)
            .anchorLeft(0)
            .bottom(5)
            .childIf(
                base.doesBindPlayerInventory(),
                () -> SlotGroupWidget.playerInventory(false)
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
            IntSyncValue coinAmountSyncer = new IntSyncValue(() -> {
                Wallet wallet = TradeManager.INSTANCE.getWallet(playerId, walletMode);
                return wallet == null ? 0 : wallet.getCount(type);
            });
            syncManager.syncValue("coinAmount_" + type.id, coinAmountSyncer);

            BooleanSyncValue ejectCoinSyncer = new BooleanSyncValue(() -> this.ejectSingleCoin.get(type), val -> {
                this.ejectSingleCoin.put(type, val);
                if (val) {
                    doEjectCoin(type);
                }
            });
            syncManager.syncValue("ejectCoin_" + type.id, ejectCoinSyncer);
        }

        Team team = TeamManager.getTeamByPlayer(playerId);
        BooleanSyncValue hasTeamSyncer = new BooleanSyncValue(
            () -> team != null && (VMConfig.team.soloTeam || team.getMembers()
                .size() > 1),
            val -> walletButton.setEnabled(val));
        syncManager.syncValue("hasTeam", hasTeamSyncer);

        // Block modifications from server -> client
        EnumSyncValue<WalletMode> walletModeSyncer = new EnumSyncValue<>(
            WalletMode.class,
            () -> walletMode,
            newWalletMode -> {},
            () -> walletMode,
            newWalletMode -> walletMode = newWalletMode);
        syncManager.syncValue("walletMode", walletModeSyncer);
    }

    public void attemptPurchase(TradeItemDisplay display) {
        submitTradesToServer(display);
        forceRefresh = true;
    }

    private void submitTradesToServer(TradeItemDisplay trade) {
        if (!trade.isTradeableNow(walletMode) || !trade.enabled) {
            return;
        }
        base.sendTradeRequest(trade, walletMode);
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
        if (favouritesTabWidget != null) {
            favouritesTabWidget.setEnabled(
                !trades.get(TradeCategory.FAVOURITES)
                    .isEmpty());
            if (
                trades.get(TradeCategory.FAVOURITES)
                    .isEmpty() && this.tabController.getActivePageIndex() == 0
            ) {
                this.tabController.setPage(1);
            }
        }
        this.updateTradeDisplay(trades, displayedTradesTiles);
        this.updateTradeDisplay(trades, displayedTradesList);
        this.updateTabHighlighting(trades);
    }

    private void updateTabHighlighting(Map<TradeCategory, List<TradeItemDisplay>> trades) {
        this.highlightedTabs.clear();
        if (
            this.searchBar.getText()
                .equals("")
        ) {
            return;
        }
        for (Map.Entry<TradeCategory, List<TradeItemDisplay>> entry : trades.entrySet()) {
            if (
                !entry.getValue()
                    .isEmpty()
            ) {
                this.highlightedTabs.add(entry.getKey());
            }
        }
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

    public SearchBar getSearchBar() {
        return this.searchBar;
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
