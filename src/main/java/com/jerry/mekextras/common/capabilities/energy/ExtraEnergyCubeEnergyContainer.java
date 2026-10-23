package com.jerry.mekextras.common.capabilities.energy;
import mekanism.common.capabilities.energy.*;

import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.transaction.ITransactionHelper;
import mekanism.api.transaction.RateLimitTracker;
import com.jerry.mekextras.common.tier.ECTier;
import com.jerry.mekextras.common.tile.TileEntityExtraEnergyCube;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Range;
import org.jspecify.annotations.Nullable;

public class ExtraEnergyCubeEnergyContainer extends BasicEnergyContainer {

    public static ExtraEnergyCubeEnergyContainer create(TileEntityExtraEnergyCube tile, @Nullable IContentsListener listener) {
        Objects.requireNonNull(tile, "Energy cube tile cannot be null");
        ECTier tier = tile.getTier();
        LongSupplier gameTimeSupplier = tile::getGameTime;
        IntSupplier rateLimit = tier::getTransferRate;
        //Only limit the internal rate to change the speed at which this can be filled or drained by an item stored in a slot
        return new ExtraEnergyCubeEnergyContainer(tier, ITransactionHelper.INSTANCE.createInternalOnlyRateLimit(gameTimeSupplier, rateLimit),
              ITransactionHelper.INSTANCE.createInternalOnlyRateLimit(gameTimeSupplier, rateLimit), listener);
    }

    private final boolean isCreative;

    private ExtraEnergyCubeEnergyContainer(ECTier tier, @Nullable RateLimitTracker insertionRateLimiter, @Nullable RateLimitTracker extractionRateLimiter,
          @Nullable IContentsListener listener) {
        super(tier.getCapacity(), ConstantPredicates.alwaysTrue(), ConstantPredicates.alwaysTrue(), insertionRateLimiter, extractionRateLimiter, listener);
        isCreative = false;
    }

    @Override
    @Range(from = 0, to = Integer.MAX_VALUE)
    public int insert(@Range(from = 0, to = Integer.MAX_VALUE) int amount, TransactionContext transaction, AutomationType automationType) {
        //Note: Unlike other creative items, the creative energy cube does not allow changing it to always full
        if (isCreative) {
            //Return the result without actually changing the contents (accepting without providing any changes)
            try (Transaction simulation = Transaction.open(transaction)) {
                return super.insert(amount, simulation, automationType);
            }
        }
        return super.insert(amount, transaction, automationType);
    }

    @Override
    @Range(from = 0, to = Integer.MAX_VALUE)
    public int extract(@Range(from = 0, to = Integer.MAX_VALUE) int amount, TransactionContext transaction, AutomationType automationType) {
        if (isCreative) {
            //Return the result without actually changing the contents (accepting without providing any changes
            try (Transaction simulation = Transaction.open(transaction)) {
                return super.extract(amount, simulation, automationType);
            }
        }
        return super.extract(amount, transaction, automationType);
    }
}