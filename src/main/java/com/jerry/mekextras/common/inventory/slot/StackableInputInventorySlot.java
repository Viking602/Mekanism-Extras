package com.jerry.mekextras.common.inventory.slot;

import com.jerry.mekextras.common.tier.ExtraFactoryTier;

import mekanism.api.IContentsListener;
import org.jspecify.annotations.NullMarked;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.slot.InputInventorySlot;

import net.neoforged.neoforge.transfer.item.ItemResource;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Predicate;

@NullMarked
public class StackableInputInventorySlot extends InputInventorySlot {

    private final ExtraFactoryTier tier;

    public static StackableInputInventorySlot at(ExtraFactoryTier tier, Predicate<@NotNull ItemResource> isItemValid, @Nullable IContentsListener listener, int x, int y) {
        return at(tier, ConstantPredicates.alwaysTrue(), isItemValid, listener, x, y);
    }

    public static StackableInputInventorySlot at(ExtraFactoryTier tier, Predicate<@NotNull ItemResource> insertPredicate, Predicate<@NotNull ItemResource> isItemValid, @Nullable IContentsListener listener,
                                                 int x, int y) {
        Objects.requireNonNull(insertPredicate, "Insertion check cannot be null");
        Objects.requireNonNull(isItemValid, "Item validity check cannot be null");
        return new StackableInputInventorySlot(tier, insertPredicate, isItemValid, listener, x, y);
    }

    protected StackableInputInventorySlot(ExtraFactoryTier tier, Predicate<@NotNull ItemResource> insertPredicate, Predicate<@NotNull ItemResource> isItemValid, @Nullable IContentsListener listener, int x, int y) {
        super(net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE, (resource, automation) -> insertPredicate.test(resource), isItemValid, null, null, listener, x, y);
        this.tier = tier;
        setSlotType(ContainerSlotType.EXTRA);
    }

    @Override
    public long capacityAsLong(ItemResource resource) {
        return super.capacityAsLong(resource) * (8L << tier.ordinal());
    }
}
