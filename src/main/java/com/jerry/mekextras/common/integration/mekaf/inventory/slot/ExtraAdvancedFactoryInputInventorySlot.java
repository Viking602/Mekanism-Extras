package com.jerry.mekextras.common.integration.mekaf.inventory.slot;
import com.jerry.mekextras.api.ExtraUpgrade;
import com.jerry.mekextras.api.mixin.IMixinMachineEnergyContainer;
import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import com.jerry.mekextras.common.util.ExtraUpgradeUtils;

import com.jerry.mekextras.common.integration.mekaf.tile.factory.TileEntityExtraLiquifyingFactory;
import com.jerry.mekextras.common.integration.mekaf.tile.factory.TileEntityExtraPRCFactory;
import com.jerry.mekextras.common.integration.mekaf.tile.factory.base.TileEntityExtraItemToChemicalFactory;
import com.jerry.mekextras.common.integration.mekaf.tile.factory.base.TileEntityExtraItemToItemAdvancedFactory;

import mekanism.api.IContentsListener;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.fluid.IFluidTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;

import net.minecraft.world.item.Item;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ExtraAdvancedFactoryInputInventorySlot extends InputInventorySlot {
    private final com.jerry.mekextras.common.integration.mekaf.tile.factory.base.TileEntityExtraAdvancedFactoryBase<?> factory;

    public static ExtraAdvancedFactoryInputInventorySlot create(TileEntityExtraItemToItemAdvancedFactory<?> factory, int process, IInventorySlot outputSlot, @Nullable IContentsListener listener, int x, int y) {
        Objects.requireNonNull(factory, "Factory cannot be null");
        Objects.requireNonNull(outputSlot, "Item output tank cannot be null");
        return new ExtraAdvancedFactoryInputInventorySlot(factory, process, outputSlot, listener, x, y);
    }

    private ExtraAdvancedFactoryInputInventorySlot(TileEntityExtraItemToItemAdvancedFactory<?> factory, int process, IInventorySlot outputSlot, @Nullable IContentsListener listener, int x, int y) {
        super(Item.ABSOLUTE_MAX_STACK_SIZE, (stack, automationType) -> factory.isItemValidForSlot(stack) && factory.inputProducesOutput(process, stack, outputSlot, false),
                factory::isValidInputItem, null, null, listener, x, y);
        this.factory = factory;
    }

    public static ExtraAdvancedFactoryInputInventorySlot create(TileEntityExtraItemToChemicalFactory<?> factory, int process, IChemicalTank outputTank, @Nullable IContentsListener listener, int x, int y) {
        Objects.requireNonNull(factory, "Factory cannot be null");
        Objects.requireNonNull(outputTank, "Chemical output tank cannot be null");
        return new ExtraAdvancedFactoryInputInventorySlot(factory, process, outputTank, listener, x, y);
    }

    private ExtraAdvancedFactoryInputInventorySlot(TileEntityExtraItemToChemicalFactory<?> factory, int process, IChemicalTank outputTank, @Nullable IContentsListener listener, int x, int y) {
        super(Item.ABSOLUTE_MAX_STACK_SIZE, (stack, automationType) -> factory.isItemValidForSlot(stack) && factory.inputProducesOutput(process, stack, outputTank, false),
                factory::isValidInputItem, null, null, listener, x, y);
        this.factory = factory;
    }

    public static ExtraAdvancedFactoryInputInventorySlot create(TileEntityExtraPRCFactory factory, int process, IInventorySlot outputSlot, IChemicalTank outputTank, @Nullable IContentsListener listener, int x, int y) {
        Objects.requireNonNull(factory, "Factory cannot be null");
        Objects.requireNonNull(outputTank, "Chemical output tank cannot be null");
        return new ExtraAdvancedFactoryInputInventorySlot(factory, process, outputSlot, outputTank, listener, x, y);
    }

    private ExtraAdvancedFactoryInputInventorySlot(TileEntityExtraPRCFactory factory, int process, IInventorySlot outputSlot, IChemicalTank outputTank, @Nullable IContentsListener listener, int x, int y) {
        super(Item.ABSOLUTE_MAX_STACK_SIZE, (stack, automationType) -> factory.isItemValidForSlot(stack.toStack()) && factory.inputProducesOutput(process, stack.toStack(), outputSlot, outputTank, false),
                stack -> factory.isValidInputItem(stack.toStack()), null, null, listener, x, y);
        this.factory = factory;
    }

    public static ExtraAdvancedFactoryInputInventorySlot create(TileEntityExtraLiquifyingFactory factory, int process, IInventorySlot outputSlot, IFluidTank outputTank, @Nullable IContentsListener listener, int x, int y) {
        Objects.requireNonNull(factory, "Factory cannot be null");
        Objects.requireNonNull(outputTank, "Fluid output tank cannot be null");
        return new ExtraAdvancedFactoryInputInventorySlot(factory, process, outputSlot, outputTank, listener, x, y);
    }

    private ExtraAdvancedFactoryInputInventorySlot(TileEntityExtraLiquifyingFactory factory, int process, IInventorySlot outputSlot, IFluidTank outputTank, @Nullable IContentsListener listener, int x, int y) {
        super(Item.ABSOLUTE_MAX_STACK_SIZE, (stack, automationType) -> factory.isItemValidForSlot(stack.toStack()) && factory.inputProducesOutput(process, stack.toStack(), outputSlot, outputTank, false),
                stack -> factory.isValidInputItem(stack.toStack()), null, null, listener, x, y);
        this.factory = factory;
    }
    @Override
    public long capacityAsLong(net.neoforged.neoforge.transfer.item.ItemResource resource) {
        return super.capacityAsLong(resource) * (8L << factory.tier.ordinal());
    }
}
