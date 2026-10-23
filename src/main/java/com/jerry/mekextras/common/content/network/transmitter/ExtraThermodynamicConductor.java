package com.jerry.mekextras.common.content.network.transmitter;
import com.jerry.mekextras.common.tile.transmitter.TileEntityExtraTransmitter;
import com.jerry.mekextras.common.util.IExtraUpgradeableTransmitter;
import mekanism.common.content.network.transmitter.ThermodynamicConductor;
import mekanism.common.upgrade.transmitter.ThermodynamicConductorUpgradeData;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
public class ExtraThermodynamicConductor extends ThermodynamicConductor implements IExtraUpgradeableTransmitter<ThermodynamicConductorUpgradeData> {
    public ExtraThermodynamicConductor(Holder<Block> blockProvider, TileEntityExtraTransmitter tile) { super(blockProvider, tile); }
    @Override
    public void onContentsChanged() {
        // Chunk deserialization restores heat before the block entity receives its level.
        if (getLevel() != null) super.onContentsChanged();
    }
}
