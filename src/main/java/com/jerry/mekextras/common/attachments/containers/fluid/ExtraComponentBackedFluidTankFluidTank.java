package com.jerry.mekextras.common.attachments.containers.fluid;
import mekanism.common.component.containers.fluid.*;

import mekanism.api.AutomationType;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.component.containers.resource.AttachedResources;
import com.jerry.mekextras.common.item.block.machine.ItemBlockExtraFluidTank;
import com.jerry.mekextras.common.tier.FTTier;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Range;

public class ExtraComponentBackedFluidTankFluidTank extends ComponentBackedFluidTank {

    private final boolean isCreative;

    public static ExtraComponentBackedFluidTankFluidTank create(ItemAccess attachedAccess, int tankIndex) {
        if (!(attachedAccess.getResource().getItem() instanceof ItemBlockExtraFluidTank item)) {
            throw new IllegalStateException("Attached to should always be a fluid tank item");
        }
        return new ExtraComponentBackedFluidTankFluidTank(attachedAccess, tankIndex, item.getAdvancedTier());
    }

    private ExtraComponentBackedFluidTankFluidTank(ItemAccess attachedAccess, int tankIndex, FTTier tier) {
        super(attachedAccess, tankIndex, ConstantPredicates.alwaysTrueBi(), ConstantPredicates.alwaysTrueBi(), ConstantPredicates.alwaysTrue(), tier::getCapacity, tier::getTransferRate);
        isCreative = false;
    }

    @Override
    @Range(from = 0, to = Integer.MAX_VALUE)
    protected int insert(AttachedResources<FluidResource> attached, FluidResource currentType, @Range(from = 0, to = Long.MAX_VALUE) long currentAmount, long capacity,
          FluidResource resource, @Range(from = 0, to = Integer.MAX_VALUE) int amount, TransactionContext transaction, AutomationType automationType) {
        if (isCreative) {
            //Return the result without actually changing the contents (accepting without providing any changes)
            try (Transaction simulation = Transaction.open(transaction)) {
                return super.insert(attached, currentType, currentAmount, capacity, resource, amount, simulation, automationType);
            }
        }
        return super.insert(attached, currentType, currentAmount, capacity, resource, amount, transaction, automationType);
    }

    @Override
    @Range(from = 0, to = Integer.MAX_VALUE)
    protected int extract(AttachedResources<FluidResource> attached, FluidResource currentType, @Range(from = 0, to = Long.MAX_VALUE) long currentAmount,
          FluidResource resource, @Range(from = 0, to = Integer.MAX_VALUE) int amount, TransactionContext transaction, AutomationType automationType) {
        if (isCreative) {
            try (Transaction simulation = Transaction.open(transaction)) {
                //Use a sub transaction that is not committed to effectively just simulate what will happen without making any changes
                return super.extract(attached, currentType, currentAmount, resource, amount, simulation, automationType);
            }
        }
        return super.extract(attached, currentType, currentAmount, resource, amount, transaction, automationType);
    }
}