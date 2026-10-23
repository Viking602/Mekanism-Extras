package com.jerry.genextras.common.item;

import java.util.function.Consumer;
import mekanism.api.text.EnumColor;
import mekanism.common.block.prefab.BlockBasicMultiblock;
import mekanism.common.item.block.ItemBlockTooltip;
import mekanism.common.util.text.BooleanStateDisplay.OnOff;
import mekanism.generators.common.GeneratorsLang;
import mekanism.generators.common.registries.GeneratorsDataComponents;
import com.jerry.genextras.common.tile.naquadah.TileEntityNaquadahReactorLogicAdapter;
import com.jerry.genextras.common.tile.naquadah.TileEntityNaquadahReactorLogicAdapter.NaquadahReactorLogic;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.transfer.access.ItemAccess;

public class ItemBlockNaquadahLogicAdapter extends ItemBlockTooltip<BlockBasicMultiblock<TileEntityNaquadahReactorLogicAdapter>> {

    public ItemBlockNaquadahLogicAdapter(BlockBasicMultiblock<TileEntityNaquadahReactorLogicAdapter> block, Properties properties) {
        super(block, true, properties.component(com.jerry.genextras.common.registries.GenExtraDataComponents.NAQUADAH_LOGIC_TYPE, NaquadahReactorLogic.DISABLED));
    }

    @Override
    protected void addDetails(ItemStack stack, ItemAccess itemAccess, Item.TooltipContext context, TooltipDisplay tooltipDisplay,
          Consumer<Component> tooltipAdder, TooltipFlag flag) {
        super.addDetails(stack, itemAccess, context, tooltipDisplay, tooltipAdder, flag);
        NaquadahReactorLogic logicType = stack.getOrDefault(com.jerry.genextras.common.registries.GenExtraDataComponents.NAQUADAH_LOGIC_TYPE, NaquadahReactorLogic.DISABLED);
        tooltipAdder.accept(GeneratorsLang.REACTOR_LOGIC_REDSTONE_MODE.translate(logicType.getColor(), logicType));
        tooltipAdder.accept(GeneratorsLang.REACTOR_LOGIC_ACTIVE_COOLING.translate(EnumColor.RED, OnOff.of(stack.getOrDefault(GeneratorsDataComponents.ACTIVE_COOLED, false))));
    }
}