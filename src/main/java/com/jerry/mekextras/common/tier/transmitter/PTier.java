package com.jerry.mekextras.common.tier.transmitter;

import com.jerry.mekextras.common.config.ExtraConfig;

import mekanism.common.tier.PipeTier;

public class PTier {

    public static int getPipePullAmount(PipeTier tier) {
        return switch (tier) {
            case BASIC -> com.google.common.primitives.Ints.saturatedCast(ExtraConfig.extraTierConfig.absoluteMechanicalPipePullAmount.get());
            case ADVANCED -> com.google.common.primitives.Ints.saturatedCast(ExtraConfig.extraTierConfig.supremeMechanicalPipePullAmount.get());
            case ELITE -> com.google.common.primitives.Ints.saturatedCast(ExtraConfig.extraTierConfig.cosmicMechanicalPipePullAmount.get());
            case ULTIMATE -> com.google.common.primitives.Ints.saturatedCast(ExtraConfig.extraTierConfig.infiniteMechanicalPipePullAmount.get());
        };
    }

    public static long getPipeCapacity(PipeTier tier) {
        return switch (tier) {
            case BASIC -> ExtraConfig.extraTierConfig.absoluteMechanicalPipeCapacity.get();
            case ADVANCED -> ExtraConfig.extraTierConfig.supremeMechanicalPipeCapacity.get();
            case ELITE -> ExtraConfig.extraTierConfig.cosmicMechanicalPipeCapacity.get();
            case ULTIMATE -> ExtraConfig.extraTierConfig.infiniteMechanicalPipeCapacity.get();
        };
    }
}
