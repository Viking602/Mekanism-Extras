package com.jerry.mekextras.common.item.block;
import mekanism.common.item.block.*;

import java.util.function.Consumer;
import mekanism.api.resource.LargeResourceStack;
import mekanism.api.text.EnumColor;
import mekanism.common.MekanismLang;
import mekanism.common.block.attribute.Attribute;
import com.jerry.mekextras.common.block.basic.BlockExtraBin;
import mekanism.common.component.LockData;
import mekanism.common.component.containers.type.ContainerType;
import mekanism.common.item.interfaces.IDroppableContents.IDroppableAttachmentContents;
import mekanism.common.registries.MekanismDataComponents;
import com.jerry.mekextras.common.tier.BTier;
import mekanism.common.util.text.TextUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;

public class ItemBlockExtraBin extends ItemBlockExtraTooltip<BlockExtraBin> implements IDroppableAttachmentContents {

    private final BTier tier;

    public ItemBlockExtraBin(BlockExtraBin block, Item.Properties properties) {
        tier = java.util.Objects.requireNonNull(com.jerry.mekextras.common.block.attribute.ExtraAttribute.getAdvancedTier(block, BTier.class));
        super(block, properties.component(MekanismDataComponents.LOCK, LockData.EMPTY));
    }

    @Override
    public BTier getAdvancedTier() {
        return tier;
    }

    @Override
    protected void addStats(ItemStack stack, ItemAccess itemAccess, Item.TooltipContext context, TooltipDisplay tooltipDisplay,
          Consumer<Component> tooltipAdder, TooltipFlag flag) {
        if (tier != null) {
            LargeResourceStack<ItemResource> contents = ContainerType.ITEM.getStoredContentsFromAttachment(itemAccess);
            if (contents.isEmpty()) {
                tooltipAdder.accept(MekanismLang.EMPTY.translateColored(EnumColor.DARK_RED));
            } else {
                tooltipAdder.accept(MekanismLang.STORING.translateColored(EnumColor.BRIGHT_GREEN, EnumColor.GRAY, contents.resource()));
                if (false) {
                    tooltipAdder.accept(MekanismLang.ITEM_AMOUNT.translateColored(EnumColor.PURPLE, EnumColor.GRAY, MekanismLang.INFINITE));
                } else {
                    tooltipAdder.accept(MekanismLang.ITEM_AMOUNT.translateColored(EnumColor.PURPLE, EnumColor.GRAY, TextUtils.format(contents.amount())));
                }
            }
            ItemResource lockType = stack.getOrDefault(MekanismDataComponents.LOCK, LockData.EMPTY).lock();
            if (!lockType.isEmpty()) {
                tooltipAdder.accept(MekanismLang.LOCKED.translateColored(EnumColor.AQUA, EnumColor.GRAY, lockType));
            }
            if (false) {
                tooltipAdder.accept(MekanismLang.CAPACITY.translateColored(EnumColor.INDIGO, EnumColor.GRAY, MekanismLang.INFINITE));
            } else {
                tooltipAdder.accept(MekanismLang.CAPACITY_ITEMS.translateColored(EnumColor.INDIGO, EnumColor.GRAY, TextUtils.format(tier.getStorage())));
            }
        }
    }

    @Override
    public boolean canContentsDrop(ItemResource itemType) {
        return true;
    }
}