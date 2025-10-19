package com.cubefury.vendingmachine.blocks;

import static com.cubefury.vendingmachine.api.enums.Textures.VUPLINK_OVERLAY_0;
import static com.cubefury.vendingmachine.api.enums.Textures.VUPLINK_OVERLAY_1;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.common.util.ForgeDirection;

import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.items.VMItems;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.implementations.IPowerChannelState;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
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
import appeng.util.IterationCounter;
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
        return new ITexture[] { aBaseTexture, TextureFactory.of(VUPLINK_OVERLAY_1) };
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture aBaseTexture) {
        return new ITexture[] { aBaseTexture, TextureFactory.of(VUPLINK_OVERLAY_0) };
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
        } catch (GridAccessException gae) {
            VendingMachine.LOG.warn("Could not access storage: ", gae);
            gae.printStackTrace();
        }
        return null;
    }

    public void refreshStorageContents() {
        IStorageGrid storage = accessStorage();
        if (storage == null) return;

        cachedItems = storage.getItemInventory()
            .getStorageList();
    }

    public boolean removeItem(ItemStack remove, boolean simulate) {
        if (remove == null || remove.stackSize <= 0) return true;
        IStorageGrid storage = accessStorage();
        if (storage == null) return false;

        MachineSource source = new MachineSource(this);
        if (!remove.isItemStackDamageable()) {
            IAEItemStack stack = storage.getItemInventory()
                .extractItems(AEItemStack.create(remove), simulate ? Actionable.SIMULATE : Actionable.MODULATE, source);
            return stack != null && stack.getStackSize() >= remove.stackSize;
        }

        if (cachedItems == null) {
            return false;
        }

        List<IAEItemStack> outputList = new ArrayList<>();
        for (IAEItemStack stack : cachedItems) {
            if (stack.getItem() == remove.getItem() && stack.getItemDamage() == remove.getItemDamage()) {
                outputList.add(stack);
            }
        }

        long numMatch = outputList.stream()
            .mapToLong(stack -> stack.getStackSize())
            .sum();
        if (simulate || numMatch < remove.stackSize) {
            return numMatch >= remove.stackSize;
        }

        // Simulate removing the needed count first, and add successful to modulateList for actual removal,
        // due to possible view-only items that can't be actually extracted
        long remain = remove.stackSize;
        List<IAEItemStack> modulateList = new ArrayList<>();
        for (IAEItemStack removable : outputList) {
            long toRemove = Math.min(removable.getStackSize(), remain);
            removable.setStackSize(toRemove);
            IAEItemStack stack = storage.getItemInventory()
                .extractItems(removable, Actionable.SIMULATE, source);
            if (stack != null && stack.getItemDamage() == remove.getItemDamage()) {
                modulateList.add(stack);
            } else {
                continue;
            }
            remain -= toRemove;
            if (remain <= 0) {
                break;
            }
        }
        if (remain > 0) {
            return false;
        }

        for (IAEItemStack modulate : modulateList) {
            storage.getItemInventory()
                .extractItems(modulate, Actionable.MODULATE, source);
        }
        return true;
    }
}
