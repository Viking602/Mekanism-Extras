package com.jerry.genextras.common.tile.naquadah;

import mekanism.api.lasers.ILaserReceptor;
import mekanism.common.lib.transaction.TransactionHelper;
import com.jerry.genextras.common.content.naquadah.NaquadahReactorMultiblockData;
import com.jerry.genextras.common.registries.GenExtraBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class TileEntityLeadCoatedLaserFocusMatrix extends TileEntityNaquadahReactorCasing implements ILaserReceptor {

    public TileEntityLeadCoatedLaserFocusMatrix(BlockPos pos, BlockState state) {
        super(GenExtraBlocks.LEAD_COATED_LASER_FOCUS_MATRIX, pos, state);
    }

    @Override
    public int receiveLaserEnergy(int energy, TransactionContext transaction) {
        NaquadahReactorMultiblockData multiblock = getMultiblock();
        if (multiblock.isFormed()) {
            multiblock.addTemperatureFromEnergyInput(energy, transaction);
            return energy;
        }
        return 0;
    }

    @Override
    public InteractionResult onRightClick(Level level, Player player) {
        if (!level.isClientSide() && player.isCreative()) {
            NaquadahReactorMultiblockData multiblock = getMultiblock();
            if (multiblock.isFormed()) {
                try (Transaction transaction = TransactionHelper.openTransactionSafe()) {
                    multiblock.setPlasmaTemp(10 * NaquadahReactorMultiblockData.BURN_TEMPERATURE, transaction);
                    transaction.commit();
                }
                return InteractionResult.SUCCESS_SERVER;
            }
        }
        //TODO - 26.1: Don't call super on the client side if it was successful?
        return super.onRightClick(level, player);
    }

    @Override
    public boolean canLasersDig() {
        return false;
    }
}