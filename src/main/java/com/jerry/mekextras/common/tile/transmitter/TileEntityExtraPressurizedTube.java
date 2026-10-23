package com.jerry.mekextras.common.tile.transmitter;

import com.mojang.serialization.Codec;
import mekanism.api.chemical.ChemicalResource;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.math.MathUtils;
import com.jerry.mekextras.api.tier.AdvancedTier;
import mekanism.common.block.states.BlockStateHelper;
import mekanism.common.block.states.TransmitterType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.content.network.ChemicalNetwork;
import mekanism.common.content.network.transmitter.PressurizedTube;
import com.jerry.mekextras.common.content.network.transmitter.ExtraPressurizedTube;
import mekanism.common.lib.radiation.RadiationManager;
import com.jerry.mekextras.common.registries.ExtraBlocks;
import mekanism.common.tile.interfaces.ITileRadioactive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class TileEntityExtraPressurizedTube extends TileEntityExtraResourceTransmitter<ChemicalResource, IChemicalTank, ChemicalNetwork, PressurizedTube> implements ITileRadioactive {

    public TileEntityExtraPressurizedTube(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state, Capabilities.CHEMICAL);
    }

    @Override
    protected ExtraPressurizedTube createTransmitter(Holder<Block> blockProvider) {
        return new ExtraPressurizedTube(blockProvider, this);
    }

    @Override
    public ExtraPressurizedTube getTransmitter() { return (ExtraPressurizedTube) super.getTransmitter(); }

    @Override
    public TransmitterType getTransmitterType() {
        return TransmitterType.PRESSURIZED_TUBE;
    }

    @Override
    protected Codec<ChemicalResource> resourceCodec() {
        return ChemicalResource.CODEC;
    }

    @Override
    protected BlockState upgradeResult(BlockState current, AdvancedTier tier) {
        return BlockStateHelper.copyStateData(current, switch (tier) {
            case ABSOLUTE -> ExtraBlocks.ABSOLUTE_PRESSURIZED_TUBE;
            case SUPREME -> ExtraBlocks.SUPREME_PRESSURIZED_TUBE;
            case COSMIC -> ExtraBlocks.COSMIC_PRESSURIZED_TUBE;
            case INFINITE -> ExtraBlocks.INFINITE_PRESSURIZED_TUBE;
        });
    }

    @Override
    public float getRadiationScale() {
        if (!RadiationManager.isGlobalRadiationEnabled()) {
            return 0;
        }
        PressurizedTube tube = getTransmitter();
        if (isRemote()) {
            if (tube.hasTransmitterNetwork()) {
                ChemicalNetwork network = tube.getTransmitterNetworkNN();
                if (!network.getLastType().isEmpty() && !network.getContainer().isEmpty() && network.getLastType().isRadioactive()) {
                    //Note: This may act as full when the network isn't actually full if there is radioactive stuff
                    // going through it, but it shouldn't matter too much
                    return network.currentScale;
                }
            }
            return 0;
        }
        return tube.getRadiationScale();
    }

    @Override
    public int getRadiationParticleCount() {
        return MathUtils.clampToInt(3 * getRadiationScale());
    }

    //Methods relating to IComputerTile
    @Override
    public String getComputerName() {
        return getTransmitter().getTier().getBaseTier().getLowerName() + "PressurizedTube";
    }
    //End methods IComputerTile
}