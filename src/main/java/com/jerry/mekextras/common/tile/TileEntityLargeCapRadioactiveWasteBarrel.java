package com.jerry.mekextras.common.tile;
import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import com.jerry.mekextras.common.tier.RWBTier;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;


import mekanism.api.AutomationType;
import mekanism.api.IConfigurable;
import mekanism.api.IContentsListener;
import mekanism.api.MekanismAPITags;
import mekanism.api.RelativeSide;
import mekanism.api.SerializationConstants;
import mekanism.api.chemical.ChemicalResource;
import mekanism.api.chemical.IChemicalTank;
import mekanism.common.capabilities.Capabilities;
import com.jerry.mekextras.common.capabilities.chemical.StackedLargeCapWasteBarrel;
import mekanism.common.capabilities.holder.container.IContainerHolder;
import mekanism.common.capabilities.holder.container.MekContainerHelper;
import mekanism.common.capabilities.proxy.BelowContainerCache;
import mekanism.common.component.containers.type.ContainerType;
import mekanism.common.component.containers.type.IContainerType;
import mekanism.common.config.MekanismConfig;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerChemicalTankWrapper;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.util.ResourceUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.Nullable;

public class TileEntityLargeCapRadioactiveWasteBarrel extends TileEntityMekanism implements IConfigurable {

    private final RWBTier tier;
    public RWBTier getTier() { return tier; }
    public StackedLargeCapWasteBarrel getGasTank() { return chemicalTank; }
    private long lastProcessTick;
    @UnknownNullability//Initialized via getInitialChemicalTanks
    @WrappingComputerMethod(wrapper = ComputerChemicalTankWrapper.class, methodNames = {"getStored", "getCapacity", "getNeeded",
                                                                                        "getFilledPercentage"}, docPlaceholder = "barrel")
    StackedLargeCapWasteBarrel chemicalTank;

    @Nullable
    private BelowContainerCache<ChemicalResource, IChemicalTank> belowTankCache;

    private int processTicks;

    public TileEntityLargeCapRadioactiveWasteBarrel(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        tier = java.util.Objects.requireNonNull(ExtraAttribute.getAdvancedTier(blockProvider, RWBTier.class));
        super(blockProvider, pos, state);
        delaySupplier = NO_DELAY;
    }

    @Override
    public IContainerHolder<IChemicalTank> getInitialChemicalTanks(IContentsListener listener) {
        MekContainerHelper<IChemicalTank> builder = MekContainerHelper.forSide(facingSupplier);
        builder.addContainer(chemicalTank = StackedLargeCapWasteBarrel.create(this, listener), RelativeSide.TOP, RelativeSide.BOTTOM);
        return builder.build();
    }

    @Override
    protected boolean onUpdateServer(ServerLevel level) {
        boolean sendUpdatePacket = super.onUpdateServer(level);
        long gameTime = level.getGameTime();
        if (gameTime > lastProcessTick) {
            //If we are not on the same tick do stuff, otherwise ignore it (anti tick accelerator protection)
            lastProcessTick = gameTime;
            if (!chemicalTank.isEmpty()) {
                ChemicalResource chemicalType = chemicalTank.resource();
                int decayAmount = com.google.common.primitives.Ints.saturatedCast(tier.getDecayAmount());
                if (decayAmount > 0 && !chemicalType.is(MekanismAPITags.Chemicals.WASTE_BARREL_DECAY_BLACKLIST) &&
                    ++processTicks >= tier.getProcessTicks()) {
                    processTicks = 0;
                    try (Transaction transaction = Transaction.openRoot()) {
                        chemicalTank.extract(chemicalType, decayAmount, transaction, AutomationType.INTERNAL);
                        transaction.commit();
                    }
                }
            }
            if (getActive()) {
                if (belowTankCache == null) {
                    belowTankCache = new BelowContainerCache<>(Capabilities.CHEMICAL, level, worldPosition);
                }
                int toEmit = chemicalTank.amountAsInt();
                IChemicalTank below = belowTankCache.getContainer(StackedLargeCapWasteBarrel.class);
                if (below != null) {
                    //If the block below this barrel, is also a barrel. Only emit as much as it might be able to accept.
                    // This prevents it then trying to go up the chain back to this barrel and any ones above it
                    toEmit = Math.min(below.getNeededAsInt(ChemicalResource.EMPTY), toEmit);
                }
                ResourceUtils.emit(belowTankCache.getHandler(), chemicalTank, toEmit, null);
            }
            //Note: We don't need to do any checking here if the packet needs due to capacity changing as we do it
            // in TileentityMekanism after this method is called. And given radioactive waste barrels can only contain
            // radioactive substances the check for radiation scale also will work for syncing capacity for purposes
            // of when the client sneak right-clicks on the barrel
        }
        return sendUpdatePacket;
    }

    @Override
    public void setLevel(Level world) {
        super.setLevel(world);
        //Invalidate the cache as if the level changed then it might no longer be valid
        belowTankCache = null;
    }

    public StackedLargeCapWasteBarrel getChemicalTank() {
        return chemicalTank;
    }

    public double getChemicalScale() {
        return chemicalTank.amountAsLong() / (double) chemicalTank.capacityAsLong(chemicalTank.resource());
    }

    @Override
    public InteractionResult onSneakRightClick(Level level, Player player) {
        if (!level.isClientSide()) {
            setActive(!getActive());
            level.playSound(null, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3F, 1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onRightClick(Level level, Player player) {
        return InteractionResult.PASS;
    }

    @Override
    public void writeReducedUpdatedTag(ValueOutput output) {
        super.writeReducedUpdatedTag(output);
        output.putChild(SerializationConstants.CHEMICAL, chemicalTank);
        output.putInt(SerializationConstants.PROGRESS, processTicks);
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        super.handleUpdateTag(input);
        input.readChild(SerializationConstants.CHEMICAL, chemicalTank);
        processTicks = input.getIntOr(SerializationConstants.PROGRESS, processTicks);
    }

    @Override
    public int getRedstoneLevel() {
        return ContainerType.CHEMICAL.getRedstoneSignalFromContainer(chemicalTank);
    }

    @Override
    protected boolean makesComparatorDirty(IContainerType<?, ?> type) {
        return type == ContainerType.CHEMICAL;
    }
}
