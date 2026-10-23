package com.jerry.mekextras.common.integration.mekaf.inventory.slot;

import com.jerry.mekextras.common.integration.mekaf.tile.factory.base.TileEntityExtraAdvancedFactoryBase;

import mekanism.api.IContentsListener;
import org.jspecify.annotations.NullMarked;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.slot.BasicInventorySlot;

import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

@NullMarked
public class ExtraAdvancedFactoryOutputInventorySlot extends BasicInventorySlot {

    private final TileEntityExtraAdvancedFactoryBase<?> factory;

    public static ExtraAdvancedFactoryOutputInventorySlot at(TileEntityExtraAdvancedFactoryBase<?> factory, @Nullable IContentsListener listener, int x, int y) {
        return new ExtraAdvancedFactoryOutputInventorySlot(factory, listener, x, y);
    }

    private ExtraAdvancedFactoryOutputInventorySlot(TileEntityExtraAdvancedFactoryBase<?> factory, @Nullable IContentsListener listener, int x, int y) {
        super(ConstantPredicates.alwaysTrueBi(), ConstantPredicates.internalOnly(), ConstantPredicates.alwaysTrue(), null, null, listener, x, y);
        setSlotType(ContainerSlotType.OUTPUT);
        this.factory = factory;
    }

    @Override
    public long capacityAsLong(net.neoforged.neoforge.transfer.item.ItemResource resource) {
        return super.capacityAsLong(resource) * (8L << factory.tier.ordinal());
    }
}
