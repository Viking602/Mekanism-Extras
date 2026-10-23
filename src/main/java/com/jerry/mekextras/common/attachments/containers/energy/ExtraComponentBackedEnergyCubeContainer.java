package com.jerry.mekextras.common.attachments.containers.energy;
import mekanism.common.component.containers.energy.*;

import mekanism.api.AutomationType;
import mekanism.api.functions.ConstantPredicates;
import com.jerry.mekextras.common.item.block.ItemBlockExtraEnergyCube;
import com.jerry.mekextras.common.tier.ECTier;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Range;

public class ExtraComponentBackedEnergyCubeContainer extends ComponentBackedEnergyContainer {

    public static ExtraComponentBackedEnergyCubeContainer create(ItemAccess attachedAccess) {
        if (!(attachedAccess.getResource().getItem() instanceof ItemBlockExtraEnergyCube item)) {
            throw new IllegalStateException("Attached to should always be an energy cube item");
        }
        return new ExtraComponentBackedEnergyCubeContainer(attachedAccess, item.getAdvancedTier());
    }

    private final boolean isCreative;

    private ExtraComponentBackedEnergyCubeContainer(ItemAccess attachedAccess, ECTier tier) {
        super(attachedAccess, ConstantPredicates.alwaysTrue(), ConstantPredicates.alwaysTrue(), tier::getCapacity, tier::getTransferRate);
        isCreative = false;
    }

    @Override
    @Range(from = 0, to = Integer.MAX_VALUE)
    public int insert(@Range(from = 0, to = Integer.MAX_VALUE) int amount, TransactionContext transaction, AutomationType automationType) {
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