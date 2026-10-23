package com.jerry.mekextras.common.tile.transmitter;

import com.jerry.mekextras.api.IExtraAlloyInteraction;
import com.jerry.mekextras.api.tier.AdvancedTier;
import com.jerry.mekextras.api.tier.IAdvancedTier;
import com.jerry.mekextras.common.util.IExtraUpgradeableTransmitter;

import mekanism.common.Mekanism;
import mekanism.common.advancements.MekanismCriteriaTriggers;
import mekanism.common.block.states.TransmitterType;
import mekanism.common.content.network.transmitter.BufferedTransmitter;
import mekanism.common.content.network.transmitter.Transmitter;
import mekanism.common.lib.transmitter.DynamicBufferedNetwork;
import mekanism.common.lib.transmitter.DynamicNetwork;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import mekanism.common.upgrade.transmitter.TransmitterUpgradeData;
import mekanism.common.util.WorldUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import mekanism.common.lib.transaction.TransactionHelper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public abstract class TileEntityExtraTransmitter extends TileEntityTransmitter implements IExtraAlloyInteraction {

    public TileEntityExtraTransmitter(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
    }

    @NotNull
    protected BlockState upgradeResult(@NotNull BlockState current, @NotNull AdvancedTier tier) {
        return current;
    }

    public static void tickServer(Level level, BlockPos blockPos, BlockState blockState, TileEntityExtraTransmitter entityTransmitter) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) entityTransmitter.onUpdateServer(serverLevel);
    }

    @Override
    public void onExtraAlloyInteraction(Player player, ItemStack stack, IAdvancedTier tier) {
        Level level = getLevel();
        if (level == null) return;
        BlockPos pos = getBlockPos();
        if (getTransmitter().hasTransmitterNetwork()) {
            DynamicNetwork<?, ?, ?> transmitterNetwork = getTransmitter().getTransmitterNetworkNN();
            List<Transmitter<?, ?, ?>> list = new ArrayList<>(transmitterNetwork.getTransmitters());
            list.sort(Comparator.comparingDouble(transmitter -> transmitter.getBlockPos().distSqr(pos)));
            boolean sharesSet = false;
            int upgraded = 0;
            for (Transmitter<?, ?, ?> transmitter : list) {
                if (transmitter instanceof IExtraUpgradeableTransmitter<?> upgradeableTransmitter && upgradeableTransmitter.canUpgrade(tier)) {
                    TileEntityExtraTransmitter transmitterTile = (TileEntityExtraTransmitter) transmitter.getTransmitterTile();
                    BlockState state = transmitterTile.getBlockState();
                    BlockState upgradeState = transmitterTile.upgradeResult(state, tier.getAdvanceTier());
                    if (state == upgradeState) {
                        //Skip if it would not actually upgrade anything
                        continue;
                    }
                    if (!sharesSet) {
                        if (transmitterNetwork instanceof DynamicBufferedNetwork dynamicNetwork) {
                            //Ensure we save the shares to the tiles so that they can properly take them, and they don't get voided
                            try (Transaction transaction = TransactionHelper.openTransactionSafe()) {
                                dynamicNetwork.validateSaveShares((BufferedTransmitter<?, ?, ?, ?>) transmitter, transaction);
                                transaction.commit();
                            }
                        }
                        sharesSet = true;
                    }
                    transmitter.startUpgrading();
                    TransmitterUpgradeData upgradeData = upgradeableTransmitter.getUpgradeData();
                    BlockPos transmitterPos = transmitter.getBlockPos();
                    if (upgradeData == null) {
                        Mekanism.logger.warn("Got no upgrade data for transmitter at position: {} in {} but it said it would be able to provide some.",
                              transmitterPos, level);
                    } else {
                        level.setBlockAndUpdate(transmitterPos, upgradeState);
                        TileEntityExtraTransmitter upgradedTile = WorldUtils.getTileEntity(TileEntityExtraTransmitter.class, level, transmitterPos);
                        if (upgradedTile == null) {
                            Mekanism.logger.warn("Error upgrading transmitter at position: {} in {}.", transmitterPos, level);
                        } else {
                            Transmitter<?, ?, ?> upgradedTransmitter = upgradedTile.getTransmitter();
                            if (upgradedTransmitter instanceof IExtraUpgradeableTransmitter) {
                                try (Transaction transaction = TransactionHelper.openTransactionSafe()) {
                                    transferUpgradeData((IExtraUpgradeableTransmitter<?>) upgradedTransmitter, upgradeData, transaction);
                                    transaction.commit();
                                }
                            } else {
                                Mekanism.logger.warn("Unhandled upgrade data.", new IllegalStateException());
                            }
                            upgraded++;
                            if (upgraded == 8) {
                                break;
                            }
                        }
                    }
                }
            }
            if (upgraded > 0) {
                //Invalidate the network so that it properly has new references to everything
                transmitterNetwork.invalidate(null);
                //TODO - 26.1: Figure out what other uses of ItemStack#shrink should be replaced with ItemStack#consume
                stack.consume(1, player);
                if (player instanceof ServerPlayer serverPlayer) {
                    MekanismCriteriaTriggers.ALLOY_UPGRADE.value().trigger(serverPlayer);
                }
            }
        }
    }

    private <DATA extends TransmitterUpgradeData> void transferUpgradeData(IExtraUpgradeableTransmitter<DATA> upgradeableTransmitter, TransmitterUpgradeData data, TransactionContext transaction) {
        if (upgradeableTransmitter.dataTypeMatches(data)) {
            upgradeableTransmitter.parseUpgradeData((DATA) data, transaction);
        } else {
            Mekanism.logger.warn("Unhandled upgrade data.", new IllegalStateException());
        }
    }

}
