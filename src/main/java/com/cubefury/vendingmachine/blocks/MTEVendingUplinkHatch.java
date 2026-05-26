package com.cubefury.vendingmachine.blocks;

import static com.cubefury.vendingmachine.api.enums.Textures.VUPLINK_OVERLAY_ACTIVE;
import static com.cubefury.vendingmachine.api.enums.Textures.VUPLINK_OVERLAY_INACTIVE;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.common.util.ForgeDirection;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.cubefury.vendingmachine.items.VMItems;
import com.cubefury.vendingmachine.trade.CurrencyItem;
import com.cubefury.vendingmachine.trade.CurrencyType;
import com.cubefury.vendingmachine.util.Wallet;

import appeng.api.config.Actionable;
import appeng.api.implementations.IPowerChannelState;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.api.util.AECableType;
import appeng.api.util.DimensionalCoord;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.render.TextureFactory;

public class MTEVendingUplinkHatch extends MTEHatch implements IGridProxyable, IPowerChannelState, IActionHost {

    protected AENetworkProxy gridProxy = null;
    protected boolean additionalConnection = false;
    private IItemList<IAEItemStack> cachedItems;
    private final LinkedList<IAEItemStack> pendingItemInject = new LinkedList<>();
    private long lastOutputTick = 0;

    public static final int mTier = 3;

    public MTEVendingUplinkHatch(int aID, String aName, String aNameRegional) {
        super(
            aID,
            aName,
            aNameRegional,
            mTier,
            0,
            new String[] { "Vending Machine Uplink hatch.", "Uses inputs directly from ME network." });
    }

    public MTEVendingUplinkHatch(MTEVendingUplinkHatch prototype) {
        super(prototype.mName, prototype.mTier, 0, prototype.mDescriptionArray, prototype.mTextures);
    }

    @Override
    public void onFacingChange() {
        updateValidGridProxySides();
    }

    @Override
    public boolean isFacingValid(ForgeDirection facing) {
        return true;
    }

    @Override
    public boolean isAccessAllowed(EntityPlayer aPlayer) {
        return true;
    }

    @Override
    public AENetworkProxy getProxy() {
        if (gridProxy == null) {
            if (getBaseMetaTileEntity() instanceof IGridProxyable) {
                gridProxy = new AENetworkProxy(this, "proxy", VMItems.uplinkHatch, true);
                gridProxy.setFlags(GridFlags.REQUIRE_CHANNEL);
                updateValidGridProxySides();
                if (getBaseMetaTileEntity().getWorld() != null) {
                    gridProxy.setOwner(
                        getBaseMetaTileEntity().getWorld()
                            .getPlayerEntityByName(getBaseMetaTileEntity().getOwnerName()));
                }
            }
        }
        return this.gridProxy;
    }

    @Override
    public boolean isPowered() {
        return getProxy() != null && getProxy().isPowered();
    }

    @Override
    public boolean isActive() {
        return getProxy() != null && getProxy().isActive();
    }

    @Override
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(
            getBaseMetaTileEntity().getWorld(),
            getBaseMetaTileEntity().getXCoord(),
            getBaseMetaTileEntity().getYCoord(),
            getBaseMetaTileEntity().getZCoord());
    }

    @Override
    public IGridNode getGridNode(ForgeDirection dir) {
        return getProxy().getNode();
    }

    @Override
    public IGridNode getActionableNode() {
        return getProxy().getNode();
    }

    @Override
    public void securityBreak() {}

    @Override
    public ITexture[] getTexturesActive(ITexture aBaseTexture) {
        return new ITexture[] { aBaseTexture, TextureFactory.of(VUPLINK_OVERLAY_ACTIVE) };
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture aBaseTexture) {
        return new ITexture[] { aBaseTexture, TextureFactory.of(VUPLINK_OVERLAY_INACTIVE) };
    }

    @Override
    public AECableType getCableConnectionType(ForgeDirection forgeDirection) {
        return isOutputFacing(forgeDirection) ? AECableType.SMART : AECableType.NONE;
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEVendingUplinkHatch(this);
    }

    @Override
    public void onFirstTick(IGregTechTileEntity baseMetaTileEntity) {
        super.onFirstTick(baseMetaTileEntity);
        getProxy().onReady();
    }

    @Override
    public void onPostTick(IGregTechTileEntity baseMetaTileEntity, long tick) {
        if (baseMetaTileEntity.isServerSide()) {
            if (tick % 20 == 0) {
                baseMetaTileEntity.setActive(isActive());
            }
            if (tick - lastOutputTick > 40) {
                flushInjectBuffer(tick);
            }
        }
        super.onPostTick(baseMetaTileEntity, tick);
    }

    private Function<IAEItemStack, IAEItemStack> getAENetworkInserter() {
        AENetworkProxy proxy = getProxy();
        if (!proxy.isActive()) return null;
        try {
            IEnergySource energy = proxy.getEnergy();
            IStorageGrid storage = accessStorage();
            if (storage == null) return null;
            MachineSource source = new MachineSource(this);
            return stack -> Platform.poweredInsert(energy, storage.getItemInventory(), stack, source);
        } catch (final GridAccessException ignored) {
            return null;
        }
    }

    private static List<IAEItemStack> toAEStacks(List<ItemStack> stacks) {
        return stacks.stream()
            .map(AEItemStack::create)
            .collect(Collectors.toList());
    }

    private void flushInjectBuffer(long tick) {
        Function<IAEItemStack, IAEItemStack> networkInserter = getAENetworkInserter();
        if (networkInserter == null) return;
        while (!pendingItemInject.isEmpty()) {
            IAEItemStack remain = networkInserter.apply(pendingItemInject.removeFirst());
            if (remain != null) {
                pendingItemInject.addFirst(remain);
                break;
            }
        }
        lastOutputTick = tick;

    }

    public void injectCoins(CurrencyItem ci) {
        if (ci.value == 0) return;
        injectItems(toAEStacks(ci.itemize()));
    }

    public void injectItems(List<IAEItemStack> stackList) {
        stackList.forEach(stack -> {
            for (IAEItemStack existing : pendingItemInject) {
                if (
                    existing.getItem()
                        .equals(stack.getItem())
                        && ItemStack.areItemStackTagsEqual(existing.getItemStack(), stack.getItemStack())
                ) {
                    existing.setStackSize(existing.getStackSize() + stack.getStackSize());
                    return;
                }
            }
            pendingItemInject.addLast(stack);
        });
    }

    private void updateValidGridProxySides() {
        if (additionalConnection) {
            getProxy().setValidSides(EnumSet.complementOf(EnumSet.of(ForgeDirection.UNKNOWN)));
        } else {
            getProxy().setValidSides(EnumSet.of(getBaseMetaTileEntity().getFrontFacing()));
        }
    }

    @Override
    public boolean onWireCutterRightClick(ForgeDirection side, ForgeDirection wrenchingSide, EntityPlayer aPlayer,
        float aX, float aY, float aZ, ItemStack aTool) {
        additionalConnection = !additionalConnection;
        updateValidGridProxySides();
        aPlayer.addChatComponentMessage(
            new ChatComponentTranslation("GT5U.hatch.additionalConnection." + additionalConnection));
        return true;
    }

    private IStorageGrid accessStorage() {
        try {
            return getProxy().getStorage();
        } catch (GridAccessException ignored) {}
        return null;
    }

    public void refreshStorageContents() {
        IStorageGrid storage = accessStorage();
        if (storage == null) return;

        cachedItems = storage.getItemInventory()
            .getStorageList();
    }

    public void updateCurrencyInto(Wallet wallet) {
        if (cachedItems == null) return;
        for (IAEItemStack aeStack : cachedItems) {
            CurrencyItem ci = CurrencyItem.fromItemStack(aeStack.getItemStack());
            if (ci == null) continue;
            wallet.addCount(ci.type, ci.value);
        }
    }

    public List<CurrencyItem> performTrade(Map<CurrencyType, Integer> currencies) {
        Map<CurrencyType, Integer> extracted = new HashMap<>();

        List<Pair<Integer, IAEItemStack>> candidateStacks = new ArrayList<>();
        for (IAEItemStack stack : cachedItems) {
            CurrencyItem curItem = CurrencyItem.fromItemStack(stack.getItemStack());
            if (curItem == null || !currencies.containsKey(curItem.type)) continue;
            ItemStack baseItem = stack.getItemStack()
                .copy();
            baseItem.stackSize = 1;
            candidateStacks
                .add(new ImmutablePair<>(Objects.requireNonNull(CurrencyItem.fromItemStack(baseItem)).value, stack));
        }
        candidateStacks.sort(Pair::compareTo);

        currencies.forEach((type, amount) -> {
            int valueLeft = amount;
            for (Pair<Integer, IAEItemStack> candidate : candidateStacks) {
                ItemStack stack = candidate.getRight()
                    .getItemStack()
                    .copy();
                if (!type.isMatchingType(stack)) continue;

                int coinValue = candidate.getLeft();
                stack.stackSize = Math.min(stack.stackSize, valueLeft / coinValue + (amount % coinValue == 0 ? 0 : 1));
                if (removeItem(stack, false, null, tracker -> {}) == 0) {
                    valueLeft -= coinValue * stack.stackSize;
                    if (extracted.containsKey(type)) {
                        extracted.put(type, extracted.get(type) + stack.stackSize * coinValue);
                    } else {
                        extracted.put(type, stack.stackSize * coinValue);
                    }
                }

                if (valueLeft <= 0) break;
            }
        });

        List<CurrencyItem> consumedItems = new ArrayList<>();
        extracted.forEach((type, value) -> consumedItems.add(new CurrencyItem(type, value)));
        return consumedItems;
    }

    public int removeItem(ItemStack remove, boolean simulate, String ore, Consumer<IAEItemStack> pulledStackTracker) {
        if (remove == null || remove.stackSize == 0) return 0;
        IStorageGrid storage = accessStorage();
        if (storage == null) return remove.stackSize;

        MachineSource source = new MachineSource(this);

        int remain = remove.stackSize;
        // Skip this branch if itemstack is damageable because AE's extractItems doesn't work for those
        if (!remove.isItemStackDamageable()) {
            IAEItemStack stack = storage.getItemInventory()
                .extractItems(AEItemStack.create(remove), simulate ? Actionable.SIMULATE : Actionable.MODULATE, source);
            if (stack != null) remain -= (int) stack.getStackSize();
            if (!simulate) pulledStackTracker.accept(stack);
        }

        if (remain == 0 || ore == null || cachedItems == null) return remain;

        for (IAEItemStack stack : cachedItems) {
            if (!MTEVendingMachine.matchItem(remove, stack.getItemStack(), ore)) continue;

            IAEItemStack copy = stack.copy();
            copy.setStackSize(Math.min(stack.getStackSize(), remain));

            IAEItemStack removed = storage.getItemInventory()
                .extractItems(copy, simulate ? Actionable.SIMULATE : Actionable.MODULATE, source);
            if (removed == null) continue;
            if (!simulate) pulledStackTracker.accept(removed);
            remain -= (int) copy.getStackSize();

            if (remain == 0) break;
        }

        return remain;

    }
}
