package com.jerry.mekextras.common.capabilities.fluid;
import mekanism.common.capabilities.fluid.*;

import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.transaction.ITransactionHelper;
import com.jerry.mekextras.common.tier.FTTier;
import com.jerry.mekextras.common.tile.TileEntityExtraFluidTank;
import mekanism.common.util.WorldUtils;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Range;
import org.jspecify.annotations.Nullable;

public class ExtraFluidTankFluidTank extends BasicFluidTank {

    public static ExtraFluidTankFluidTank create(TileEntityExtraFluidTank tile, @Nullable IContentsListener listener) {
        Objects.requireNonNull(tile, "Fluid tank tile entity cannot be null");
        return new ExtraFluidTankFluidTank(tile, listener);
    }

    private final TileEntityExtraFluidTank tile;
    private final boolean isCreative;

    private ExtraFluidTankFluidTank(TileEntityExtraFluidTank tile, @Nullable IContentsListener listener) {
        LongSupplier gameTimeSupplier = tile::getGameTime;
        IntSupplier rateLimit = tile.tier::getTransferRate;
        super(tile.tier.getCapacity(), ConstantPredicates.alwaysTrueBi(), ConstantPredicates.alwaysTrueBi(), ConstantPredicates.alwaysTrue(),
              //Only limit the internal rate to change the speed at which this can be filled or drained by an item stored in a slot
              ITransactionHelper.INSTANCE.createInternalOnlyRateLimit(gameTimeSupplier, rateLimit),
              ITransactionHelper.INSTANCE.createInternalOnlyRateLimit(gameTimeSupplier, rateLimit), listener);
        this.tile = tile;
        isCreative = false;
    }

    @Override
    @Range(from = 0, to = Integer.MAX_VALUE)
    public int insert(FluidResource resource, @Range(from = 0, to = Integer.MAX_VALUE) int amount, TransactionContext transaction, AutomationType automationType) {
        int inserted;
        if (isCreative) {
            if (isEmpty() && !automationType.isExternal()) {
                //If a player manually inserts into a creative tank (or internally, via a FluidInventorySlot), that is empty we need to allow setting the type,
                // Note: We check that it is not external insertion because an empty creative tanks acts as a "void" for automation
                try (Transaction simulation = Transaction.open(transaction)) {
                    if (super.insert(resource, amount, simulation, automationType) == 0) {
                        return 0;
                    }
                }
                //If we managed to insert anything, set the contents to the maximum amount of that item type
                // Note: We just set it as unchecked as we have already validated it
                setContents(resource, capacityAsLong(resource), transaction);
                //Return that we accepted the entire amount we were passed
                return amount;
            }
            //Return the result without actually changing the contents (accepting without providing any changes
            try (Transaction simulation = Transaction.open(transaction)) {
                inserted = super.insert(resource, amount, simulation, automationType);
            }
        } else {
            inserted = super.insert(resource, amount, transaction, automationType);
        }
        //Ensure we have the same type of fluid stored as we failed to insert, in which case we want to try to insert to the one above
        if (inserted < amount && resource().equals(resource)) {
            //If we have any leftover check if we can send it to the tank that is above
            TileEntityExtraFluidTank tileAbove = WorldUtils.getTileEntity(TileEntityExtraFluidTank.class, this.tile.getLevel(), this.tile.getBlockPos().above());
            if (tileAbove != null) {
                //Note: We do external so that it is not limited by the internal rate limits
                inserted += tileAbove.fluidTank.insert(resource, amount - inserted, transaction, AutomationType.EXTERNAL);
            }
        }
        return inserted;
    }

    @Override
    @Range(from = 0, to = Integer.MAX_VALUE)
    public int extract(FluidResource resource, @Range(from = 0, to = Integer.MAX_VALUE) int amount, TransactionContext transaction, AutomationType automationType) {
        if (isCreative) {
            //Return the result without actually changing the contents (accepting without providing any changes
            try (Transaction simulation = Transaction.open(transaction)) {
                return super.extract(resource, amount, simulation, automationType);
            }
        }
        return super.extract(resource, amount, transaction, automationType);
    }
}