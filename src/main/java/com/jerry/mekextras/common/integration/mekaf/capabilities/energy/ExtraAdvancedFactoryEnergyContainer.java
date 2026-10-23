package com.jerry.mekextras.common.integration.mekaf.capabilities.energy;
import com.jerry.mekextras.api.ExtraUpgrade;
import com.jerry.mekextras.api.mixin.IMixinMachineEnergyContainer;
import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import com.jerry.mekextras.common.util.ExtraUpgradeUtils;

import com.jerry.mekextras.common.integration.mekaf.tile.factory.base.TileEntityExtraAdvancedFactoryBase;

import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.block.attribute.AttributeEnergy;
import mekanism.common.capabilities.energy.MachineEnergyContainer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class ExtraAdvancedFactoryEnergyContainer extends MachineEnergyContainer<TileEntityExtraAdvancedFactoryBase<?>> {

    public static ExtraAdvancedFactoryEnergyContainer input(TileEntityExtraAdvancedFactoryBase<?> tile, @Nullable IContentsListener listener) {
        AttributeEnergy electricBlock = validateBlock(tile);
        return new ExtraAdvancedFactoryEnergyContainer(electricBlock.getStorage(), electricBlock.getUsage(), notExternal, ConstantPredicates.alwaysTrue(), tile, listener);
    }

    private ExtraAdvancedFactoryEnergyContainer(long maxEnergy, int energyPerTick, Predicate<@NotNull AutomationType> canExtract,
                                           Predicate<@NotNull AutomationType> canInsert, TileEntityExtraAdvancedFactoryBase<?> tile, @Nullable IContentsListener listener) {
        super(maxEnergy, energyPerTick, canExtract, canInsert, tile, listener);
    }

    @Override
    public int getBaseEnergyPerTick() {
        return Math.toIntExact(super.getBaseEnergyPerTick() + tile.getRecipeEnergyRequired());
    }
}
