package com.cubefury.vendingmachine.blocks;

import static com.cubefury.vendingmachine.VendingMachine.AUTHOR_CUBEFURY;
import static com.cubefury.vendingmachine.api.enums.Textures.VM_CASING_TEXTURE;
import static com.cubefury.vendingmachine.api.enums.Textures.VM_MACHINE_FRONT_OFF;
import static com.cubefury.vendingmachine.api.enums.Textures.VM_MACHINE_FRONT_ON;
import static com.cubefury.vendingmachine.api.enums.Textures.VM_MACHINE_FRONT_ON_GLOW;
import static com.cubefury.vendingmachine.api.enums.Textures.VM_OVERLAY;
import static com.cubefury.vendingmachine.api.enums.Textures.VM_OVERLAY_ACTIVE;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static gregtech.api.util.GTStructureUtility.ofHatchAdderOptional;
import static mcp.mobius.waila.api.SpecialChars.GREEN;
import static mcp.mobius.waila.api.SpecialChars.RED;
import static net.minecraft.util.StatCollector.translateToLocal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.IWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.oredict.OreDictionary;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import com.cubefury.vendingmachine.VMConfig;
import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.blocks.gui.MTEVendingMachineGui;
import com.cubefury.vendingmachine.blocks.gui.TradeItemDisplay;
import com.cubefury.vendingmachine.blocks.gui.WalletMode;
import com.cubefury.vendingmachine.network.handlers.NetTradeDisplaySync;
import com.cubefury.vendingmachine.network.handlers.NetTradeRequestSync;
import com.cubefury.vendingmachine.trade.CurrencyItem;
import com.cubefury.vendingmachine.trade.CurrencyType;
import com.cubefury.vendingmachine.trade.Trade;
import com.cubefury.vendingmachine.trade.TradeDatabase;
import com.cubefury.vendingmachine.trade.TradeGroup;
import com.cubefury.vendingmachine.trade.TradeManager;
import com.cubefury.vendingmachine.trade.TradeRequest;
import com.cubefury.vendingmachine.util.BigItemStack;
import com.cubefury.vendingmachine.util.OverlayHelper;
import com.cubefury.vendingmachine.util.Translator;
import com.cubefury.vendingmachine.util.Wallet;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.IAlignment;
import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.IAlignmentProvider;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;

import gregtech.api.covers.CoverRegistry;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ISecondaryDescribable;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.ICasingTextureProvider;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.render.RenderOverlay;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTUtil;
import gregtech.api.util.MultiblockTooltipBuilder;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

public class MTEVendingMachine extends MTEMultiBlockBase
    implements ISurvivalConstructable, ISecondaryDescribable, IAlignment, ICasingTextureProvider {

    private static final IStructureDefinition<MTEVendingMachine> STRUCTURE_DEFINITION = IStructureDefinition
        .<MTEVendingMachine>builder()
        .addShape("main", new String[][] { { "cc", "c~", "cc" } })
        .addElement(
            'c',
            ofChain(
                ofHatchAdderOptional(
                    MTEVendingMachine::addUplinkHatch,
                    VendingMachineBlocks.casingBlock.getTextureIndex(0),
                    1,
                    VendingMachineBlocks.casingBlock,
                    0),
                ofBlock(VendingMachineBlocks.casingBlock, 0)))
        .build();

    private MTEVendingUplinkHatch uplinkHatch = null;

    public static final int INPUT_SLOTS = 8;
    public static final int OUTPUT_SLOTS = 100;

    public static final int MAX_TRADES = 300;

    public static final int STRUCTURE_CHECK_TICKS = 20;

    private static final ITexture[] FACING_SIDE = { VM_CASING_TEXTURE };
    private static final ITexture[] FACING_FRONT = { TextureFactory.of(VM_MACHINE_FRONT_OFF) };
    private static final ITexture[] FACING_ACTIVE = { TextureFactory.of(VM_MACHINE_FRONT_ON), TextureFactory.builder()
        .addIcon(VM_MACHINE_FRONT_ON_GLOW)
        .glow()
        .build() };
    private static final String COIN_DROP_SOUND = "vendingmachine:coin_drop";
    private static final String ITEM_DROP_SOUND = "vendingmachine:item_drop";

    protected final List<RenderOverlay.OverlayTicket> overlayTickets = new ArrayList<>();

    private MultiblockTooltipBuilder tooltipBuilder;

    public int mUpdate = 0;

    private final boolean mIsAnimated;

    public final ItemStackHandler inputItems = new ItemStackHandler(INPUT_SLOTS);
    public final ItemStackHandler outputItems = new ItemStackHandler(OUTPUT_SLOTS);
    private final Queue<ItemStack> outputBuffer = new ConcurrentLinkedQueue<>();

    public final Queue<TradeRequest> pendingTrades = new LinkedBlockingQueue<>();
    private boolean newBufferedOutputs = false;
    private int ticksSinceOutput = 0;
    private int ticksSinceTradeUpdate = 0;
    public boolean syncTrades = false;

    private Map<BigItemStack, Integer> inputSlotCache = new HashMap<>();

    private EntityPlayer currentUser = null;

    public MTEVendingMachine(final int aID, final String aName, final String aNameRegional) {
        super(aID, aName, aNameRegional);
        this.mIsAnimated = true;
    }

    protected MTEVendingMachine(String aName) {
        super(aName);
        this.mIsAnimated = true;
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEVendingMachine(this.mName);
    }

    public boolean usingAnimations() {
        // Logger.INFO("Is animated? "+this.mIsAnimated);
        return this.mIsAnimated;
    }

    public void sendTradeRequest(TradeItemDisplay trade, WalletMode walletMode) {
        IGregTechTileEntity baseTile = getBaseMetaTileEntity();
        if (baseTile == null || !baseTile.isActive()) {
            return;
        }
        NetTradeRequestSync.sendTradeRequest(
            trade,
            baseTile.getWorld(),
            baseTile.getXCoord(),
            baseTile.getYCoord(),
            baseTile.getZCoord(),
            walletMode);
    }

    public void addTradeRequest(TradeRequest trade) {
        this.pendingTrades.add(trade);
    }

    public void dispenseItems() {
        if (!this.getActive()) {
            return;
        }
        if (!this.pendingTrades.isEmpty()) {
            TradeRequest tradeRequest = this.pendingTrades.poll();
            if (!processTradeOnServer(tradeRequest)) {
                VendingMachine.LOG.warn(
                    "Unable to complete trade. Either input items changed after trade submission, or a double click was sent.");
            }
            NetTradeRequestSync.sendAck(tradeRequest.player);
        }
        if (
            this.newBufferedOutputs
                || (!this.outputBuffer.isEmpty() && this.ticksSinceOutput % getDispensingDelay() == 0)
        ) {
            dispenseFirstNonNullIem();
            ticksSinceOutput = 0;
        }
        ticksSinceOutput = this.newBufferedOutputs ? 0 : ticksSinceOutput + 1;
        this.newBufferedOutputs = false;
        this.markDirty();
    }

    private int getDispensingDelay() {
        int baseDelay = 10;
        int queueSize = outputBuffer.size();
        double acceleration = Math.log(queueSize);
        if (acceleration < 1) {
            return baseDelay;
        }
        return (int) (baseDelay / acceleration);
    }

    private void dispenseFirstNonNullIem() {
        ItemStack dispensableStack = getNextDispensable();
        if (dispensableStack != null) {
            int targetSlot = getFirstEmptyOutputSlot();
            if (targetSlot != -1) {
                outputIntoSlot(dispensableStack, targetSlot);
                playSoundEffect(getSoundForDispensedItemstack(dispensableStack));
                this.outputBuffer.poll();
            }
        }
    }

    private @Nullable ItemStack getNextDispensable() {
        while (!this.outputBuffer.isEmpty()) {
            ItemStack next = this.outputBuffer.peek();
            if (next != null && next.stackSize > 0) {
                return next;
            }
            // impossible, but just in case
            this.outputBuffer.poll();
        }
        return null;
    }

    private int getFirstEmptyOutputSlot() {
        for (int i = 0; i < MTEVendingMachine.OUTPUT_SLOTS; i++) {
            if (this.outputItems.getStackInSlot(i) == null) {
                return i;
            }
        }
        return -1;
    }

    private void outputIntoSlot(ItemStack next, int slotIndex) {
        ItemStack output = next.copy();
        output.stackSize = next.stackSize;
        next.stackSize = 0;
        this.outputItems.setStackInSlot(slotIndex, output);
    }

    private static @NotNull String getSoundForDispensedItemstack(@NotNull ItemStack itemStack) {
        return CurrencyItem.fromItemStack(itemStack) != null ? COIN_DROP_SOUND : ITEM_DROP_SOUND;
    }

    private boolean processTradeOnServer(TradeRequest tradeRequest) {
        if (tradeRequest == null) return false;

        UUID playerId = tradeRequest.player.getUniqueID();
        TradeGroup tg = TradeDatabase.INSTANCE.getTradeGroupFromId(tradeRequest.tradeGroup);
        if (!TradeManager.INSTANCE.canExecuteTrade(playerId, tg)) return false;

        Trade trade = tg.getTrades()
            .get(tradeRequest.tradeGroupOrder);
        if (!checkTrade(trade, playerId, tradeRequest.walletMode, false)) return false;

        for (BigItemStack toItem : trade.toItems) {
            if (toItem == null) continue;
            dispenseItemStacks(toItem.getCombinedStacks());
        }
        TradeManager.INSTANCE.executeTrade(playerId, tg);

        this.sendTradeUpdate();
        this.markDirty();
        return true;
    }

    public void dispenseItemStacks(List<ItemStack> itemStacks) {
        this.outputBuffer.addAll(itemStacks);
        this.newBufferedOutputs = true;
    }

    public ItemStack @NotNull [] getCopyOfInputSlotItems() {
        ItemStack[] inputSlots = new ItemStack[MTEVendingMachine.INPUT_SLOTS];
        for (int i = 0; i < MTEVendingMachine.INPUT_SLOTS; i++) {
            ItemStack curStack = this.inputItems.getStackInSlot(i);
            inputSlots[i] = curStack == null ? null : curStack.copy();
        }
        return inputSlots;
    }

    /**
     * Play a sound effect coming from the VM to all nearby players
     * For the player currently using the vending machine, the sound is played at their location
     */
    public void playSoundEffect(String sound) {
        IGregTechTileEntity te = getBaseMetaTileEntity();
        if (te != null) {
            World world = te.getWorld();
            if (world instanceof WorldServer worldServer) {
                EntityPlayer player = getCurrentUser();
                float volume = getRandomVolume();
                float pitch = getRandomPitch();
                for (IWorldAccess worldAccess : worldServer.worldAccesses) {
                    worldAccess.playSoundToNearExcept(
                        player,
                        sound,
                        te.getXCoord() + 0.5f,
                        te.getYCoord() + 0.5f,
                        te.getZCoord() + 0.5f,
                        volume,
                        pitch);
                }
                if (player instanceof EntityPlayerMP mpPlayer) {
                    S29PacketSoundEffect packet = new S29PacketSoundEffect(
                        sound,
                        player.posX,
                        player.posY - player.yOffset,
                        player.posZ,
                        volume,
                        pitch);
                    mpPlayer.playerNetServerHandler.sendPacket(packet);
                }
            }
        }
    }

    private static float getRandomPitch() {
        return (float) (0.95d + (0.1d * Math.random()));
    }

    private static float getRandomVolume() {
        return (float) (0.6d + (0.4d * Math.random()));
    }

    @Override
    public boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    @Override
    public IStructureDefinition<MTEVendingMachine> getStructureDefinition() {
        return STRUCTURE_DEFINITION;
    }

    protected MultiblockTooltipBuilder getTooltip() {
        if (tooltipBuilder == null) {
            tooltipBuilder = new MultiblockTooltipBuilder();
            tooltipBuilder.addMachineType("Vending Machine")
                .addInfo("Who even restocks this...")
                .beginStructureBlock(1, 2, 3, false)
                .addController("Middle right, 2nd layer")
                .addCasing("0-5", "Vending Machine Casing", false)
                .addMiscHatch("0+", "ME Vending Uplink Hatch", "Any casing", 1)
                .addStructureInfo("")
                .addStructureFooter("Cannot be flipped onto its side")
                .toolTipFinisher(AUTHOR_CUBEFURY);
        }
        return tooltipBuilder;
    }

    @Override
    protected boolean forceUseMui2() {
        return true;
    }

    @Override
    protected @NotNull MTEVendingMachineGui getGui() {
        return new MTEVendingMachineGui(this);
    }

    @Override
    public boolean isTeleporterCompatible() {
        return false;
    }

    @Override
    public boolean isFacingValid(ForgeDirection facing) {
        return (facing.flag & (ForgeDirection.UP.flag | ForgeDirection.DOWN.flag)) == 0;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity baseMetaTileEntity, ForgeDirection side, ForgeDirection facing,
        int colorIndex, boolean active, boolean redstoneLevel) {
        if (side == facing) {
            if (baseMetaTileEntity == null) {
                return FACING_FRONT;
            }
            return active ? FACING_ACTIVE : FACING_FRONT;
        }
        return FACING_SIDE;
    }

    @Override
    public ITexture getCasingTexture() {
        return VM_CASING_TEXTURE;
    }

    protected void setTextureOverlay() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile == null || tile.isServerSide()) return;

        IIconContainer[] vmTextures;
        if (getBaseMetaTileEntity().isActive() && usingAnimations()) vmTextures = VM_OVERLAY_ACTIVE;
        else vmTextures = VM_OVERLAY;

        OverlayHelper.setVMOverlay(
            tile.getWorld(),
            tile.getXCoord(),
            tile.getYCoord(),
            tile.getZCoord(),
            getExtendedFacing(),
            vmTextures,
            overlayTickets);
    }

    @Override
    public String[] getDescription() {
        return getCurrentDescription();
    }

    @Override
    public boolean allowCoverOnSide(ForgeDirection side, ItemStack coverItem) {
        return (CoverRegistry.getCoverPlacer(coverItem)
            .allowOnPrimitiveBlock()) && (super.allowCoverOnSide(side, coverItem));
    }

    @Override
    public String[] getPrimaryDescription() {
        return getTooltip().getInformation();
    }

    @Override
    public String[] getSecondaryDescription() {
        return getTooltip().getStructureInformation();
    }

    @Override
    public boolean isDisplaySecondaryDescription() {
        return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        if (inputItems != null) {
            aNBT.setTag("inputs", inputItems.serializeNBT());
        }
        if (outputItems != null) {
            aNBT.setTag("outputs", outputItems.serializeNBT());
        }
        NBTTagList pendingOutputs = new NBTTagList();
        for (ItemStack itemStack : outputBuffer) {
            pendingOutputs.appendTag(itemStack.writeToNBT(new NBTTagCompound()));
        }
        aNBT.setTag("outputBuffer", pendingOutputs);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        boolean loadedLegacyData = false;

        NBTTagList pendingOutputs = aNBT.getTagList("outputBuffer", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < pendingOutputs.tagCount(); i++) {
            outputBuffer.add(ItemStack.loadItemStackFromNBT(pendingOutputs.getCompoundTagAt(i)));
        }

        if (inputItems != null) {
            inputItems.deserializeNBT(aNBT.getCompoundTag("inputs"));
            if (inputItems.getSlots() != MTEVendingMachine.INPUT_SLOTS) {
                loadedLegacyData = true;
                List<ItemStack> oldStacks = inputItems.getStacks();
                inputItems.setSize(MTEVendingMachine.INPUT_SLOTS);
                for (int i = 0; i < oldStacks.size(); i++) {
                    if (i >= MTEVendingMachine.INPUT_SLOTS) {
                        outputBuffer.add(oldStacks.get(i));
                    } else {
                        inputItems.setStackInSlot(i, oldStacks.get(i));
                    }
                }
            }
        }

        if (outputItems != null) {
            outputItems.deserializeNBT(aNBT.getCompoundTag("outputs"));
            if (outputItems.getSlots() != MTEVendingMachine.OUTPUT_SLOTS) {
                loadedLegacyData = true;
                List<ItemStack> oldStacks = outputItems.getStacks();
                outputItems.setSize(MTEVendingMachine.OUTPUT_SLOTS);
                for (int i = 0; i < oldStacks.size(); i++) {
                    if (i >= MTEVendingMachine.OUTPUT_SLOTS) {
                        outputBuffer.add(oldStacks.get(i));
                    } else {
                        outputItems.setStackInSlot(i, oldStacks.get(i));
                    }
                }
            }
        }

        if (loadedLegacyData) {
            this.markDirty();
        }
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, ForgeDirection side,
        ItemStack aStack) {
        return false;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, ForgeDirection side,
        ItemStack aStack) {
        return false;
    }

    @Override
    public byte getTileEntityBaseType() {
        return 0;
    }

    @Override
    public ExtendedFacing getExtendedFacing() {
        return ExtendedFacing.of(getBaseMetaTileEntity().getFrontFacing());
    }

    @Override
    public void setExtendedFacing(ExtendedFacing alignment) {
        boolean extendedFacingChanged = alignment != getExtendedFacing();
        getBaseMetaTileEntity().setFrontFacing(alignment.getDirection());
        if (extendedFacingChanged) {
            setTextureOverlay();
        }
    }

    @Override
    public void onTextureUpdate() {
        setTextureOverlay();
    }

    @Override
    public IAlignmentLimits getAlignmentLimits() {
        return (d, r, f) -> (d.flag & (ForgeDirection.UP.flag | ForgeDirection.DOWN.flag)) == 0;
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        if (getBaseMetaTileEntity() == null) {
            VendingMachine.LOG.warn("Check machine failed as Base MTE is null");
            return false;
        }
        this.uplinkHatch = null;
        return STRUCTURE_DEFINITION.check(
            this,
            "main",
            getBaseMetaTileEntity().getWorld(),
            getExtendedFacing(),
            getBaseMetaTileEntity().getXCoord(),
            getBaseMetaTileEntity().getYCoord(),
            getBaseMetaTileEntity().getZCoord(),
            1,
            1,
            0,
            !mMachine);
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTimer) {
        if (aBaseMetaTileEntity.isClientSide()) {
            if (!aBaseMetaTileEntity.isActive()) {
                OverlayHelper.clearVMOverlay(overlayTickets);
            }
            return;
        } else if (this.mUpdate++ % STRUCTURE_CHECK_TICKS == 0) {
            this.mMachine = checkMachine(aBaseMetaTileEntity, null);
        }
        aBaseMetaTileEntity.setActive(this.mMachine);
        if (!this.mMachine) return;
        dispenseItems();
        if (
            this.currentUser != null && (this.syncTrades
                || this.ticksSinceTradeUpdate++ >= VMConfig.vendingMachineSettings.gui_refresh_interval)
        ) {
            if (uplinkHatch != null) uplinkHatch.setRefreshCache();

            this.sendTradeUpdate();
            this.syncTrades = false;
        }
    }

    public void sendTradeUpdate() {
        this.ticksSinceTradeUpdate = 0;
        if (this.currentUser == null) {
            return;
        }
        NetTradeDisplaySync.syncTradesToClient((EntityPlayerMP) this.currentUser, this);
    }

    private static boolean matchOreDict(ItemStack stack, @Nonnull String oreDict) {
        return IntStream.of(OreDictionary.getOreIDs(stack))
            .mapToObj(OreDictionary::getOreName)
            .anyMatch(s -> s.equals(oreDict));
    }

    public static boolean matchItem(ItemStack base, ItemStack candidate, String oreDict) {
        if (oreDict == null) return base.isItemEqual(candidate);
        if (!matchOreDict(candidate, oreDict)) return false;
        return !base.isItemStackDamageable() || base.getItemDamage() == candidate.getItemDamage();
    }

    private static void extractRequiredStackFromSlots(ItemStack[] slots, ItemStack required, String oreDict,
        boolean simulate) {
        for (int i = slots.length - 1; i >= 0 && required.stackSize > 0; i--) {
            if (slots[i] == null) continue;
            if (matchItem(required, slots[i], oreDict)) {
                if (required.stackSize >= slots[i].stackSize) {
                    required.stackSize -= slots[i].stackSize;
                    if (!simulate) slots[i] = null;
                } else {
                    if (!simulate) slots[i].stackSize -= required.stackSize;
                    required.stackSize = 0;
                }
            }
        }
    }

    public boolean checkTrade(Trade trade, UUID player, WalletMode walletMode, boolean simulate) {
        ItemStack[] newInputs = getCopyOfInputSlotItems();
        Wallet preWallet = TradeManager.INSTANCE.getWallet(player, walletMode);
        Wallet postWallet = Wallet.copyOf(preWallet);
        List<BigItemStack> remainNCItems = removeItems(newInputs, trade.nonConsumedItems, true);
        List<BigItemStack> remainItems = removeItems(newInputs, trade.fromItems, false);
        List<CurrencyItem> remainCurrency = removeCurrency(trade.fromCurrency, postWallet);

        boolean success;
        if (uplinkHatch != null) {
            success = uplinkHatch.executeTrade(remainNCItems, remainItems, remainCurrency, simulate);
        } else {
            success = remainNCItems.isEmpty() && remainItems.isEmpty() && remainCurrency.isEmpty();
        }

        if (success && !simulate) {
            if (preWallet != null) {
                preWallet.resetAllCount();
                preWallet.merge(postWallet);
                TradeManager.INSTANCE.saveTeamData(player);
            }

            for (int i = 0; i < MTEVendingMachine.INPUT_SLOTS; i++) {
                this.inputItems.setStackInSlot(i, newInputs[i]);
            }
        }

        return success;
    }

    public int getUplinkCurrencyAmount(CurrencyType type) {
        if (uplinkHatch == null) return 0;
        return uplinkHatch.getCurrencyAmount(type);
    }

    public List<BigItemStack> removeItems(ItemStack[] slots, List<BigItemStack> fromItems, boolean simulate) {
        List<BigItemStack> remain = new ArrayList<>();
        for (BigItemStack stack : fromItems) {
            ItemStack required = ItemStack.copyItemStack(stack.getBaseStack());
            required.stackSize = stack.stackSize;
            extractRequiredStackFromSlots(slots, required, null, simulate);
            if (required.stackSize > 0 && stack.hasOreDict()) {
                extractRequiredStackFromSlots(slots, required, stack.getOreDict(), simulate);
            }

            if (required.stackSize == 0) continue;
            BigItemStack requiredBis = stack.copy();
            requiredBis.stackSize = required.stackSize;
            remain.add(requiredBis);
        }
        return remain;
    }

    public List<CurrencyItem> removeCurrency(List<CurrencyItem> fromCurrency, Wallet wallet) {
        if (wallet == null) return fromCurrency;
        return wallet.performTrade(fromCurrency);
    }

    public boolean getActive() {
        return this.getBaseMetaTileEntity() != null && this.getBaseMetaTileEntity()
            .isActive();
    }

    @Override
    public void onFirstTick(IGregTechTileEntity aBaseMetaTileEntity) {
        super.onFirstTick(aBaseMetaTileEntity);
        if (aBaseMetaTileEntity.isClientSide()) {
            StructureLibAPI.queryAlignment((IAlignmentProvider) aBaseMetaTileEntity);
            setTextureOverlay();
        }
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return STRUCTURE_DEFINITION.survivalBuild(
            this,
            stackSize,
            "main",
            getBaseMetaTileEntity().getWorld(),
            getExtendedFacing(),
            getBaseMetaTileEntity().getXCoord(),
            getBaseMetaTileEntity().getYCoord(),
            getBaseMetaTileEntity().getZCoord(),
            1,
            1,
            0,
            elementBudget,
            env,
            false);
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        STRUCTURE_DEFINITION.buildOrHints(
            this,
            stackSize,
            "main",
            getBaseMetaTileEntity().getWorld(),
            getExtendedFacing(),
            getBaseMetaTileEntity().getXCoord(),
            getBaseMetaTileEntity().getYCoord(),
            getBaseMetaTileEntity().getZCoord(),
            1,
            1,
            0,
            hintsOnly);
    }

    @Override
    public void onRemoval() {
        super.onRemoval();
        if (getBaseMetaTileEntity().isClientSide()) OverlayHelper.clearVMOverlay(overlayTickets);
    }

    @Override
    public void getWailaBody(ItemStack itemStack, List<String> currentTip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        final NBTTagCompound tag = accessor.getNBTData();
        if (!tag.getBoolean("isActive")) {
            currentTip.add(RED + translateToLocal("GT5U.waila.multiblock.status.incomplete"));
        } else {
            currentTip.add(GREEN + translateToLocal("GT5U.waila.multiblock.status.running_fine"));
        }
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        if (GTUtil.hasMultiblockInputConfiguration(aPlayer.getHeldItem())) {
            if (aBaseMetaTileEntity.isServerSide()) {
                if (GTUtil.loadMultiblockInputConfiguration(this, aPlayer)) {
                    aPlayer.addChatComponentMessage(new ChatComponentTranslation("GT5U.MULTI_MACHINE_CONFIG.LOAD"));
                } else {
                    aPlayer
                        .addChatComponentMessage(new ChatComponentTranslation("GT5U.MULTI_MACHINE_CONFIG.LOAD.FAIL"));
                }
            }
            return true;
        }
        if (canUse(aPlayer)) {
            this.currentUser = aPlayer;
            // force trade state update now
            this.ticksSinceTradeUpdate = VMConfig.vendingMachineSettings.gui_refresh_interval;
            openGui(aPlayer);
        } else {
            aPlayer.addChatComponentMessage(new ChatComponentTranslation("vendingmachine.gui.error.player_using"));
        }
        return true;
    }

    private boolean canUse(EntityPlayer aPlayer) {
        return this.currentUser == null || this.currentUser == aPlayer;
    }

    public EntityPlayer getCurrentUser() {
        return this.currentUser;
    }

    public void resetCurrentUser(EntityPlayer aPlayer) {
        if (this.currentUser == aPlayer) {
            this.currentUser = null;
        }
    }

    private boolean addUplinkHatch(IGregTechTileEntity aBaseMetaTileEntity, int aBaseCasingIndex) {
        if (this.uplinkHatch != null) return false;
        if (aBaseMetaTileEntity == null) return false;
        IMetaTileEntity aMetaTileEntity = aBaseMetaTileEntity.getMetaTileEntity();
        if (aMetaTileEntity == null) return false;
        if (!(aMetaTileEntity instanceof MTEVendingUplinkHatch uplinkHatch)) return false;
        uplinkHatch.updateTexture(aBaseCasingIndex);
        uplinkHatch.updateCraftingIcon(uplinkHatch.getMachineCraftingIcon());
        this.uplinkHatch = uplinkHatch;
        return true;
    }

    public void fillPlayerInventoryWithDispensedItems() {
        EntityPlayer player = getCurrentUser();
        if (player == null) {
            return;
        }
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            ItemStack stack = outputItems.getStackInSlot(i);
            if (stack == null) continue;
            ItemStack toAdd = stack.copy();
            boolean fullyAdded = player.inventory.addItemStackToInventory(toAdd);
            outputItems.setStackInSlot(i, toAdd.stackSize <= 0 ? null : toAdd);
            if (!fullyAdded) {
                break;
            }
        }
    }

    @Override
    public int getSizeInventory() {
        return INPUT_SLOTS + OUTPUT_SLOTS;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        if (index < INPUT_SLOTS) {
            return inputItems.getStackInSlot(index);
        }
        return outputItems.getStackInSlot(index - INPUT_SLOTS);
    }

    @Override
    public boolean isValidSlot(int aIndex) {
        return aIndex < INPUT_SLOTS + OUTPUT_SLOTS;
    }

    @Override
    public boolean shouldDropItemAt(int index) {
        return true;
    }

    @Override
    public void setInventorySlotContents(int aIndex, ItemStack aStack) {
        if (aIndex < INPUT_SLOTS) {
            inputItems.setStackInSlot(aIndex, aStack);
        } else {
            outputItems.setStackInSlot(aIndex - INPUT_SLOTS, aStack);
        }
    }
}
