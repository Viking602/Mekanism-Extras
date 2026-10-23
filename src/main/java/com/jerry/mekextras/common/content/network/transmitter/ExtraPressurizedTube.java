package com.jerry.mekextras.common.content.network.transmitter;
import com.google.common.primitives.Ints;
import com.jerry.mekextras.common.tier.transmitter.TTier;
import com.jerry.mekextras.common.tile.transmitter.TileEntityExtraTransmitter;
import com.jerry.mekextras.common.util.IExtraUpgradeableTransmitter;
import mekanism.api.AutomationType;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.content.network.transmitter.PressurizedTube;
import mekanism.common.lib.transmitter.ConnectionType;
import mekanism.common.upgrade.transmitter.ResourceTransmitterUpgradeData;
import mekanism.common.util.EnumUtils;
import mekanism.common.util.ResourceUtils;
import mekanism.api.chemical.ChemicalResource;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class ExtraPressurizedTube extends PressurizedTube implements IExtraUpgradeableTransmitter<ResourceTransmitterUpgradeData<ChemicalResource>> {
    public ExtraPressurizedTube(Holder<Block> blockProvider, TileEntityExtraTransmitter tile) {
        super(blockProvider, tile);
    }
    @Override public long getCapacity() { return TTier.getTubeCapacity(tier); }
    private int getAvailablePull() {
        var container = getContainer();
        return Math.min(Ints.saturatedCast(TTier.getTubePullAmount(tier)), container.getNeededAsInt(container.resource()));
    }
    @Override public void pullFromAcceptors(ServerLevel level) {
        if (!hasPullSide || getAvailablePull() <= 0) return;
        var container = getContainer();
        var acceptorCache = getAcceptorCache();
        for (Direction side : EnumUtils.DIRECTIONS) {
            if (!isConnectionType(side, ConnectionType.PULL)) continue;
            var connected = acceptorCache.getConnectedAcceptor(side);
            if (connected == null) continue;
            var resource = ResourceUtils.getTypeToExtract(container.resource(), connected, ConstantPredicates.alwaysTrue(), null);
            if (resource.isEmpty()) continue;
            try (Transaction transaction = Transaction.openRoot()) {
                int extracted = connected.extract(resource, getAvailablePull(), transaction);
                if (extracted > 0 && container.insert(resource, extracted, transaction, AutomationType.INTERNAL) == extracted) {
                    transaction.commit();
                    if (container.isFull()) break;
                }
            }
        }
    }
}
