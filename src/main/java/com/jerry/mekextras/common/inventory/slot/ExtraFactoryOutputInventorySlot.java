package com.jerry.mekextras.common.inventory.slot;

import com.jerry.mekextras.common.tile.factory.TileEntityExtraFactory;

import mekanism.api.IContentsListener;
import org.jspecify.annotations.NullMarked;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.slot.BasicInventorySlot;

import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

@NullMarked
public class ExtraFactoryOutputInventorySlot extends BasicInventorySlot {

    private final TileEntityExtraFactory<?> factory;

    public static ExtraFactoryOutputInventorySlot at(TileEntityExtraFactory<?> factory, @Nullable IContentsListener listener, int x, int y) {
        return new ExtraFactoryOutputInventorySlot(factory, listener, x, y);
    }

    private ExtraFactoryOutputInventorySlot(TileEntityExtraFactory<?> factory, @Nullable IContentsListener listener, int x, int y) {
        super(ConstantPredicates.alwaysTrueBi(), ConstantPredicates.internalOnly(), ConstantPredicates.alwaysTrue(), null, null, listener, x, y);
        setSlotType(ContainerSlotType.OUTPUT);
        this.factory = factory;
    }

    @Override
    public long capacityAsLong(net.neoforged.neoforge.transfer.item.ItemResource resource) {
        return super.capacityAsLong(resource) * (8L << factory.tier.ordinal());
    }
}
