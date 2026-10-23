package com.jerry.mekextras.common.content.network.transmitter;
import com.jerry.mekextras.common.tier.transmitter.CTier;
import com.jerry.mekextras.common.tile.transmitter.TileEntityExtraTransmitter;
import com.jerry.mekextras.common.util.IExtraUpgradeableTransmitter;
import mekanism.api.AutomationType;
import mekanism.api.energy.IEnergyContainer;
import mekanism.common.content.network.transmitter.UniversalCable;
import mekanism.common.lib.transmitter.ConnectionType;
import mekanism.common.lib.transmitter.acceptor.AcceptorCache;
import mekanism.common.upgrade.transmitter.UniversalCableUpgradeData;
import mekanism.common.util.EnumUtils;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
public class ExtraUniversalCable extends UniversalCable implements IExtraUpgradeableTransmitter<UniversalCableUpgradeData> {
    public ExtraUniversalCable(Holder<Block> blockProvider, TileEntityExtraTransmitter tile) { super(blockProvider, tile); }
    @Override public long getCapacity() { return CTier.getCapacityAsLong(tier); }
    public long getCapacityAsFloatingLong() { return getCapacity(); }
    @Override
    public void pullFromAcceptors(ServerLevel level) {
        if (!hasPullSide || getAvailablePull() <= 0) {
            return;
        }
        IEnergyContainer buffer = getContainer();
        AcceptorCache<EnergyHandler> acceptorCache = getAcceptorCache();
        for (Direction side : EnumUtils.DIRECTIONS) {
            if (!isConnectionType(side, ConnectionType.PULL)) {
                continue;
            }
            EnergyHandler connectedAcceptor = acceptorCache.getConnectedAcceptor(side);
            if (connectedAcceptor != null) {
                try (Transaction transaction = Transaction.openRoot()) {
                    int extracted = connectedAcceptor.extract(getAvailablePull(), transaction);
                    if (extracted > 0 && buffer.insert(extracted, transaction, AutomationType.INTERNAL) == extracted) {
                        //If we received some resource and are able to insert it all, then actually extract it and insert it into our thing.
                        // Note: We extract first after simulating ourselves because if the target gave a faulty simulation value, we want to handle it properly
                        // and not accidentally dupe anything, and we know our simulation we just performed on taking it is valid
                        transaction.commit();
                        if (EnergyHandlerUtil.isFull(buffer)) {
                            break;
                        }
                    }
                }
            }
        }
    }

    private int getAvailablePull() {
        return Math.min(com.google.common.primitives.Ints.saturatedCast(getCapacity()), getContainer().getNeededAsInt());
    }

}
