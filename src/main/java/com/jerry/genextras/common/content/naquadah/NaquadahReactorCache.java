package com.jerry.genextras.common.content.naquadah;

import mekanism.api.SerializationConstants;
import mekanism.common.lib.multiblock.MultiblockCache;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class NaquadahReactorCache extends MultiblockCache<NaquadahReactorMultiblockData> {

    private double plasmaTemperature = -1;
    private int injectionRate = -1;
    private boolean burning;

    private int getInjectionRate() {
        if (injectionRate == -1) {
            //If it never got set default to 2
            return 2;
        }
        //Otherwise, return the actual so that it can be manually set down to zero
        return injectionRate;
    }

    @Override
    public void merge(MultiblockCache<NaquadahReactorMultiblockData> mergeCache, RejectContents rejectContents) {
        super.merge(mergeCache, rejectContents);
        plasmaTemperature = Math.max(plasmaTemperature, ((NaquadahReactorCache) mergeCache).plasmaTemperature);
        injectionRate = Math.max(injectionRate, ((NaquadahReactorCache) mergeCache).injectionRate);
        burning |= ((NaquadahReactorCache) mergeCache).burning;
    }

    @Override
    public void apply(NaquadahReactorMultiblockData data, TransactionContext transaction) {
        super.apply(data, transaction);
        if (plasmaTemperature >= 0) {
            data.setPlasmaTemp(plasmaTemperature, transaction);
        }
        data.setInjectionRate(getInjectionRate(), transaction);
        data.setBurning(burning);
        data.updateTemperatures();
    }

    @Override
    public void sync(NaquadahReactorMultiblockData data, TransactionContext transaction) {
        super.sync(data, transaction);
        plasmaTemperature = data.getPlasmaTemp();
        injectionRate = data.getInjectionRate();
        burning = data.isBurning();
    }

    @Override
    public void load(ValueInput input) {
        super.load(input);
        //TODO - 26.1: These used to just get instead of only getting if present, should the fallback be zero or the existing value?
        plasmaTemperature = input.getDoubleOr(SerializationConstants.PLASMA_TEMP, plasmaTemperature);
        injectionRate = input.getIntOr(SerializationConstants.INJECTION_RATE, injectionRate);
        burning = input.getBooleanOr(SerializationConstants.BURNING, burning);
    }

    @Override
    public void save(ValueOutput output) {
        super.save(output);
        output.putDouble(SerializationConstants.PLASMA_TEMP, plasmaTemperature);
        output.putInt(SerializationConstants.INJECTION_RATE, getInjectionRate());
        output.putBoolean(SerializationConstants.BURNING, burning);
    }
}
