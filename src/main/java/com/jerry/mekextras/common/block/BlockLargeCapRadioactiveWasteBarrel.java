package com.jerry.mekextras.common.block;

import mekanism.api.text.EnumColor;
import mekanism.common.MekanismLang;
import mekanism.common.block.prefab.BlockTile.BlockTileModel;
import mekanism.common.capabilities.chemical.StackedWasteBarrel;
import mekanism.common.content.blocktype.BlockTypeTile;
import com.jerry.mekextras.common.registries.ExtraBlockTypes;
import com.jerry.mekextras.common.tile.TileEntityLargeCapRadioactiveWasteBarrel;
import mekanism.common.util.WorldUtils;
import mekanism.common.util.text.TextUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

public class BlockLargeCapRadioactiveWasteBarrel extends BlockTileModel<TileEntityLargeCapRadioactiveWasteBarrel, BlockTypeTile<TileEntityLargeCapRadioactiveWasteBarrel>> {

    public BlockLargeCapRadioactiveWasteBarrel(BlockTypeTile<TileEntityLargeCapRadioactiveWasteBarrel> type, BlockBehaviour.Properties properties) {
        super(type, defaultProperties(properties).mapColor(MapColor.COLOR_BLACK));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        TileEntityLargeCapRadioactiveWasteBarrel tile = WorldUtils.getTileEntity(TileEntityLargeCapRadioactiveWasteBarrel.class, world, pos);
        if (tile == null) {
            return InteractionResult.PASS;
        } else if (!world.isClientSide()) {
            var chemicalTank = tile.getChemicalTank();
            Component text;
            if (chemicalTank.isEmpty()) {
                text = MekanismLang.NO_CHEMICAL.translateColored(EnumColor.GRAY);
            } else {
                text = MekanismLang.STORED_MB_PERCENTAGE.translateColored(EnumColor.ORANGE, EnumColor.ORANGE, chemicalTank.resource(), EnumColor.GRAY,
                      TextUtils.format(chemicalTank.amountAsLong()), TextUtils.getPercent(tile.getChemicalScale()));
            }
            player.sendSystemMessage(text);
        }
        return InteractionResult.SUCCESS;
    }
}
