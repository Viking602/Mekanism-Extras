package com.jerry.mekextras.common.content.network.transmitter;
import com.jerry.mekextras.common.tile.transmitter.TileEntityExtraTransmitter;
import com.jerry.mekextras.common.util.IExtraUpgradeableTransmitter;
import mekanism.common.content.network.transmitter.LogisticalTransporter;
import mekanism.common.upgrade.transmitter.LogisticalTransporterUpgradeData;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
public class ExtraLogisticalTransporter extends LogisticalTransporter implements IExtraUpgradeableTransmitter<LogisticalTransporterUpgradeData> {
    public ExtraLogisticalTransporter(Holder<Block> blockProvider, TileEntityExtraTransmitter tile) { super(blockProvider, tile); }
}
