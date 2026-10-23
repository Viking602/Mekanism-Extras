package com.jerry.mekextras.client.model;
import com.jerry.mekextras.common.tile.transmitter.TileEntityExtraLogisticalTransporter;

import mekanism.api.text.EnumColor;
import mekanism.common.util.WorldUtils;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class ExtraTransporterTint implements BlockTintSource {

    public static final BlockTintSource INSTANCE = new ExtraTransporterTint();

    @Override
    public int color(BlockState state) {
        return -1;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        TileEntityExtraLogisticalTransporter transporter = WorldUtils.getTileEntity(TileEntityExtraLogisticalTransporter.class, level, pos);
        if (transporter != null) {
            EnumColor renderColor = transporter.getTransmitter().getColor();
            if (renderColor != null) {
                return renderColor.getPackedColor();
            }
        }
        return -1;
    }
}
