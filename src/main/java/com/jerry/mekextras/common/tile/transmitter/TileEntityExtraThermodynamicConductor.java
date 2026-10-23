package com.jerry.mekextras.common.tile.transmitter;

import com.jerry.mekextras.api.tier.AdvancedTier;
import mekanism.common.block.states.BlockStateHelper;
import mekanism.common.block.states.TransmitterType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.resolver.manager.HeatHandlerManager;
import com.jerry.mekextras.common.content.network.transmitter.ExtraThermodynamicConductor;
import mekanism.common.lib.transmitter.ConnectionType;
import com.jerry.mekextras.common.registries.ExtraBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class TileEntityExtraThermodynamicConductor extends TileEntityExtraTransmitter {

    public TileEntityExtraThermodynamicConductor(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
        addCapabilityResolver(new HeatHandlerManager(direction -> {
            ExtraThermodynamicConductor conductor = getTransmitter();
            //TODO - 26.1 (heat): Should we make this a full on anonymous class and implement canInsert/canExtract? The fact that we check this stuff for exposing the cap
            // makes me think that we should, but in some ways it would also make sense to expose it on all sides regardless?
            if (direction != null && (conductor.getConnectionTypeRaw(direction) == ConnectionType.NONE) || conductor.isRedstoneActivated()) {
                //If we actually have a side, and our connection type on that side is none, or we are currently activated by redstone,
                // then return that we have no capacitors
                return null;
            }
            return conductor.getHeatCapacitor(direction);
        }));
    }

    @Override
    protected ExtraThermodynamicConductor createTransmitter(Holder<Block> blockProvider) {
        return new ExtraThermodynamicConductor(blockProvider, this);
    }

    @Override
    public ExtraThermodynamicConductor getTransmitter() {
        return (ExtraThermodynamicConductor) super.getTransmitter();
    }

    @Override
    public TransmitterType getTransmitterType() {
        return TransmitterType.THERMODYNAMIC_CONDUCTOR;
    }

    @Override
    protected BlockState upgradeResult(BlockState current, AdvancedTier tier) {
        return BlockStateHelper.copyStateData(current, switch (tier) {
            case ABSOLUTE -> ExtraBlocks.ABSOLUTE_THERMODYNAMIC_CONDUCTOR;
            case SUPREME -> ExtraBlocks.SUPREME_THERMODYNAMIC_CONDUCTOR;
            case COSMIC -> ExtraBlocks.COSMIC_THERMODYNAMIC_CONDUCTOR;
            case INFINITE -> ExtraBlocks.INFINITE_THERMODYNAMIC_CONDUCTOR;
        });
    }

    @Override
    public void sideChanged(Direction side, ConnectionType old, ConnectionType type) {
        super.sideChanged(side, old, type);
        if (type == ConnectionType.NONE) {
            //We no longer have a capability, invalidate it, which will also notify the level
            invalidateCapability(Capabilities.HEAT, side);
        } else if (old == ConnectionType.NONE) {
            //Notify any listeners to our position that we now do have a capability
            //Note: We don't invalidate our impls because we know they are already invalid, so we can short circuit setting them to null from null
            invalidateCapabilities();
        }
    }

    @Override
    public void redstoneChanged(boolean powered) {
        super.redstoneChanged(powered);
        if (powered) {
            //The transmitter now is powered by redstone and previously was not
            //Note: While at first glance the below invalidation may seem over aggressive, it is not actually that aggressive as
            // if a cap has not been initialized yet on a side then invalidating it will just NO-OP
            invalidateCapabilityAll(Capabilities.HEAT);
        } else {
            //Notify any listeners to our position that we now do have a capability
            //Note: We don't invalidate our impls because we know they are already invalid, so we can short circuit setting them to null from null
            invalidateCapabilities();
        }
    }
}