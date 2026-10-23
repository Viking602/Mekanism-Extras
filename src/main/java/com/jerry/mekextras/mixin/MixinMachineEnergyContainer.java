package com.jerry.mekextras.mixin;

import com.jerry.mekextras.api.ExtraUpgrade;
import com.jerry.mekextras.api.mixin.IMixinMachineEnergyContainer;

import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.Upgrade;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.util.MekanismUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(value = MachineEnergyContainer.class, remap = false)
public abstract class MixinMachineEnergyContainer<TILE extends TileEntityMekanism> extends BasicEnergyContainer implements IMixinMachineEnergyContainer {

    @Shadow
    @Final
    protected TILE tile;

    @Shadow
    protected int currentEnergyPerTick;

    @Shadow
    public abstract void setMaxEnergy(long maxEnergy);

    @Shadow
    public abstract long getBaseMaxEnergy();

    @Shadow
    public abstract void updateMaxEnergy();

    @Shadow
    public abstract void updateEnergyPerTick();

    protected MixinMachineEnergyContainer(long maxEnergy, Predicate<@NotNull AutomationType> canExtract, Predicate<@NotNull AutomationType> canInsert, @Nullable IContentsListener listener) {
        super(maxEnergy, canExtract, canInsert, null, null, listener);
    }

    @Inject(method = "getEnergyPerTick", at = @At(value = "RETURN"), cancellable = true)
    public void mixinGetEnergyPerTick(CallbackInfoReturnable<Integer> cir) {
        if (tile.supportsUpgrade(ExtraUpgrade.CREATIVE)) {
            cir.setReturnValue((tile.getUpgrades(ExtraUpgrade.CREATIVE) > 0) ? 0 : currentEnergyPerTick);
        }
    }

    @Override
    public void mekanism_Extras$extraUpdateMaxEnergy() {
        if (tile.supportsUpgrade(Upgrade.ENERGY) || tile.supportsUpgrade(ExtraUpgrade.CREATIVE)) {
            if ((tile.getUpgrades(ExtraUpgrade.CREATIVE) > 0)) {
                setMaxEnergy(Long.MAX_VALUE);
            } else {
                long capacity = MekanismUtils.getMaxEnergy(tile, getBaseMaxEnergy());
                setMaxEnergy(capacity);
                // Modern containers preserve overfilled storage when capacity drops.
                // Creative energy must not remain after removing the creative upgrade.
                if (getAmountAsLong() > capacity) setEnergy(capacity, null);
            }
        }
    }

    @Override
    public void mekanism_Extras$extraRecalculateUpgrades(Upgrade upgrade) {
        if (upgrade == ExtraUpgrade.CREATIVE) {
            mekanism_Extras$extraUpdateMaxEnergy();
            if (getCapacityAsLong() == Long.MAX_VALUE) {
                setEnergy(Long.MAX_VALUE, null);
            }
        } else if (upgrade == Upgrade.ENERGY) {
            if (getCapacityAsLong() != Long.MAX_VALUE) {
                updateMaxEnergy();
                updateEnergyPerTick();
            }
        }
    }
}
