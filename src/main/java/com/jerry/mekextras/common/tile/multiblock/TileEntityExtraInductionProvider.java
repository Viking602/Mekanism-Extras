package com.jerry.mekextras.common.tile.multiblock;

import mekanism.common.block.attribute.Attribute;
import com.jerry.mekextras.common.tier.IPTier;
import mekanism.common.tile.prefab.TileEntityInternalMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class TileEntityExtraInductionProvider extends TileEntityInternalMultiblock {

    public final IPTier tier;

    public TileEntityExtraInductionProvider(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        tier = java.util.Objects.requireNonNull(com.jerry.mekextras.common.block.attribute.ExtraAttribute.getAdvancedTier(blockProvider, IPTier.class));
        super(blockProvider, pos, state);
    }
}