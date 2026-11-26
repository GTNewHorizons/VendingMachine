package com.cubefury.vendingmachine.util;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemPlaceholder extends Item {

    public static Item placeholder = new ItemPlaceholder();

    // Used solely for retaining info on missing items
    public ItemPlaceholder() {
        this.setTextureName("vendingmachine:placeholder");
        this.setUnlocalizedName("vendingmachine.placeholder");
    }

    public static BigItemStack getBigItemStackFrom(Item item, String name, int count, int damage, String oreDict,
        NBTTagCompound nbt) {
        if (item == null) {
            BigItemStack stack = new BigItemStack(ItemPlaceholder.placeholder, count, damage);
            stack.setTagCompound(new NBTTagCompound());
            stack.getTagCompound()
                .setString("orig_id", name);
            stack.getTagCompound()
                .setInteger("orig_meta", damage);
            if (nbt != null) {
                stack.getTagCompound()
                    .setTag("orig_tag", nbt);
            }
            return stack;
        } else if (item == ItemPlaceholder.placeholder) {
            if (nbt != null) {
                String idName = nbt.getString("orig_id");
                Item restored = (Item) Item.itemRegistry.getObject(idName);

                if (restored == null) {
                    try {
                        restored = Item.getItemById(Short.parseShort(idName));
                    } catch (Exception ignored) {}
                }

                if (restored != null) {
                    BigItemStack stack = new BigItemStack(
                        restored,
                        count,
                        nbt.hasKey("orig_meta") ? nbt.getInteger("orig_meta") : damage).setOreDict(oreDict);
                    if (nbt.hasKey("orig_tag")) {
                        stack.setTagCompound(nbt.getCompoundTag("orig_tag"));
                    }

                    return stack;
                } else if (damage > 0 && !nbt.hasKey("orig_meta")) {
                    nbt.setInteger("orig_meta", damage);
                    damage = 0;
                }
            }
        }
        BigItemStack stack = new BigItemStack(item, count, damage).setOreDict(oreDict);
        if (nbt != null) {
            stack.setTagCompound(nbt);
        }

        return stack;
    }

    /**
     * allows items to add custom lines of information to the mouseover description
     */
    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        if (!stack.hasTagCompound()) {
            tooltip.add("ERROR: Original information missing!");
            return;
        }

        tooltip.add(
            "Original ID: " + stack.getTagCompound()
                .getString("orig_id")
                + "/"
                + stack.getTagCompound()
                    .getInteger("orig_meta"));
    }

    /**
     * Called each tick as long the item is on a player inventory. Uses by maps to check if is on a player hand and
     * update it's contents.
     */
    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean held) {
        // This check only happens once a second
        if (
            !stack.hasTagCompound() || !(entity instanceof EntityPlayer player) || world.getTotalWorldTime() % 100 != 0
        ) {
            return;
        }

        NBTTagCompound tags = stack.getTagCompound();
        Item i = (Item) Item.itemRegistry.getObject(tags.getString("orig_id"));
        int m = stack.getItemDamage() > 0 ? stack.getItemDamage() : tags.getInteger("orig_meta");
        NBTTagCompound t = tags.hasKey("orig_tag") ? tags.getCompoundTag("orig_tag") : null;

        if (i != null) {
            ItemStack converted = new ItemStack(i, stack.stackSize, m);
            converted.setTagCompound(t);
            player.inventory.setInventorySlotContents(slot, converted);
        }
    }

}
