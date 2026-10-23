package com.jerry.mekextras.common.content.matrix;

import com.google.common.primitives.Ints;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import mekanism.api.AutomationType;
import mekanism.api.MekanismPreconditions;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.math.MathUtils;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.lib.transaction.SimpleLongJournal;
import com.jerry.mekextras.common.tier.IPTier;
import com.jerry.mekextras.common.tile.multiblock.TileEntityExtraInductionCell;
import com.jerry.mekextras.common.tile.multiblock.TileEntityExtraInductionProvider;
import mekanism.common.util.EnergyUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Range;
import org.jspecify.annotations.Nullable;

public class ReinforcedMatrixEnergyContainer implements IEnergyContainer {

    private final Map<BlockPos, IPTier> providers = new Object2ObjectOpenHashMap<>();
    private final Map<BlockPos, IEnergyContainer> cells = new Object2ObjectOpenHashMap<>();
    private final Set<BlockPos> invalidPositions = new ObjectOpenHashSet<>();
    private final SimpleLongJournal queuedInput = new SimpleLongJournal();
    private final SimpleLongJournal queuedOutput = new SimpleLongJournal();

    private final SimpleLongJournal lastInput = new SimpleLongJournal();
    private final SimpleLongJournal lastOutput = new SimpleLongJournal();

    private final SimpleLongJournal cachedTotal = new SimpleLongJournal();
    private long transferCap = 0L;
    private long storageCap = 0L;

    private final ReinforcedMatrixMultiblockData multiblock;

    public ReinforcedMatrixEnergyContainer(ReinforcedMatrixMultiblockData multiblock) {
        this.multiblock = multiblock;
    }

    public void addCell(BlockPos pos, TileEntityExtraInductionCell cell) {
        //As we already have the two different variables just pass them instead of accessing world to get tile again
        MachineEnergyContainer<TileEntityExtraInductionCell> energyContainer = cell.energyContainer();
        cells.put(pos, energyContainer);
        recalculateTotals(null);
    }

    public void addProvider(BlockPos pos, TileEntityExtraInductionProvider provider) {
        providers.put(pos, provider.tier);
        recalculateTotals(null);
    }

    public void removeInternal(BlockPos pos) {
        if (cells.containsKey(pos) || providers.containsKey(pos)) {
            // Flush accepted transfers while the departing cell is still available.
            tick(Collections.emptyList(), null, null, null);
            cells.remove(pos);
            providers.remove(pos);
            recalculateTotals(null);
        }
    }

    private void recalculateTotals(@Nullable TransactionContext transaction) {
        if (transaction != null) cachedTotal.updateSnapshots(transaction);
        cachedTotal.value = 0;
        storageCap = 0;
        transferCap = 0;
        for (IEnergyContainer cell : cells.values()) {
            storageCap = MathUtils.addClamped(storageCap, cell.getCapacityAsLong());
            cachedTotal.value = MathUtils.addClamped(cachedTotal.value, cell.getAmountAsLong());
        }
        for (IPTier tier : providers.values()) transferCap = MathUtils.addClamped(transferCap, tier.getOutput());
    }

    public void invalidate() {
        //Force save
        tick(Collections.emptyList(), null, null, null);
        //And reset everything
        cells.clear();
        providers.clear();
        queuedOutput.value = 0L;
        queuedInput.value = 0L;
        lastOutput.value = 0L;
        lastInput.value = 0L;
        cachedTotal.value = 0L;
        transferCap = 0L;
        storageCap = 0L;
    }

    public boolean tick(Collection<BlockCapabilityCache<EnergyHandler, @Nullable Direction>> targets, @Nullable EnergyInventorySlot energyInputSlot,
          @Nullable EnergyInventorySlot energyOutputSlot, @Nullable TransactionContext transaction) {
        if (!invalidPositions.isEmpty()) {
            for (BlockPos invalidPosition : invalidPositions) {
                cells.remove(invalidPosition);
                providers.remove(invalidPosition);
            }
            invalidPositions.clear();
        }
        if (queuedInput.value != queuedOutput.value || !targets.isEmpty() || energyInputSlot != null || energyOutputSlot != null) {
            //Note: The cases where queued input and output might not be equal is:
            // - If something inserted into our container
            // - If something forcibly extracted from our container
            try (Transaction subTransaction = Transaction.open(transaction)) {
                //Note: Start by trying to transfer energy into/out of the slots' contents (this gets limited by whatever remaining energy rate we have)
                if (energyInputSlot != null) {
                    energyInputSlot.drainContainerIntoSlot(subTransaction);
                }
                if (energyOutputSlot != null) {
                    energyOutputSlot.fillContainerOrConvert(subTransaction);
                }
                if (!targets.isEmpty()) {
                    //Next we emit any power we
                    long sent = EnergyUtils.emit(targets, getRemainingOutput(), subTransaction);
                    if (sent > 0) {
                        //Increase how much we are outputting by the amount we accepted
                        queuedOutput.updateSnapshots(subTransaction);
                        //Increase how much we are inputting
                        queuedOutput.value += sent;
                    }
                }

                if (queuedInput.value < queuedOutput.value) {
                    //queuedInput is smaller - we are removing energy
                    removeEnergy(-getQueuedChange(), subTransaction);
                } else if (queuedInput.value > queuedOutput.value) {
                    //queuedInput is larger - we are adding energy
                    addEnergy(getQueuedChange(), subTransaction);
                }
                subTransaction.commit();
            }
        }
        if (transaction != null) lastInput.updateSnapshots(transaction);
        if (transaction != null) lastOutput.updateSnapshots(transaction);
        if (transaction != null) queuedInput.updateSnapshots(transaction);
        if (transaction != null) queuedOutput.updateSnapshots(transaction);
        lastInput.value = queuedInput.value;
        lastOutput.value = queuedOutput.value;
        queuedInput.value = 0L;
        queuedOutput.value = 0L;

        return getLastInput() > 0 || getLastOutput() > 0;
    }

    private void addEnergy(long energy, TransactionContext transaction) {
        if (transaction != null) cachedTotal.updateSnapshots(transaction);
        for (IEnergyContainer container : cells.values()) {
            //Note: inserting into the cell's energy container handles marking the cell for saving if it changes
            long stored = container.getAmountAsLong();
            long needed = container.getCapacityAsLong() - stored;
            if (needed > 0) {
                if (energy <= needed) {
                    container.setEnergy(stored + energy, transaction);
                    //Nothing left to add
                    break;
                }
                container.setEnergy(stored + needed, transaction);
                energy -= needed;
            }
        }
        recalculateTotals(transaction);
    }

    private void removeEnergy(long energy, TransactionContext transaction) {
        if (transaction != null) cachedTotal.updateSnapshots(transaction);
        for (IEnergyContainer container : cells.values()) {
            //Note: extracting from the cell's energy container handles marking the cell for saving if it changes
            long stored = container.getAmountAsLong();
            if (stored > 0) {
                if (energy <= stored) {
                    container.setEnergy(stored - energy, transaction);
                    //Nothing left to remove
                    break;
                }
                container.setEnergy(0, transaction);
                energy -= stored;
            }
        }
        recalculateTotals(transaction);
    }

    private long getQueuedChange() {
        return queuedInput.value - queuedOutput.value;
    }

    /// @return The energy post queue when this container next actually updates/saves to disk
    @Override
    public long getAmountAsLong() {
        return cachedTotal.value + getQueuedChange();
    }

    @Override
    public void setEnergy(@Range(from = 0, to = Long.MAX_VALUE) long energy, @Nullable TransactionContext transaction) {
        //Throws a RuntimeException as specified is allowed when something unexpected happens
        // As setEnergy is more meant to be used as an internal method
        throw new RuntimeException("Unexpected call to setEnergy. The matrix energy container does not support directly setting the energy.");
    }

    @Override
    @Range(from = 0, to = Integer.MAX_VALUE)
    public int insert(@Range(from = 0, to = Integer.MAX_VALUE) int amount, TransactionContext transaction, AutomationType automationType) {
        MekanismPreconditions.checkNonNegative(amount);
        if (amount == 0 || !multiblock.isFormed() || !isValidForInsertion(automationType)) {
            return 0;
        }
        int toAdd = Ints.saturatedCast(Math.min(Math.min(amount, getRemainingInput()), getNeededAsLong()));
        if (toAdd > 0) {
            if (transaction != null) queuedInput.updateSnapshots(transaction);
            //Increase how much we are inputting
            queuedInput.value += toAdd;
        }
        return toAdd;
    }

    @Override
    @Range(from = 0, to = Integer.MAX_VALUE)
    public int extract(@Range(from = 0, to = Integer.MAX_VALUE) int amount, TransactionContext transaction, AutomationType automationType) {
        MekanismPreconditions.checkNonNegative(amount);
        if (isEmpty() || amount == 0 || !multiblock.isFormed() || !isValidForExtraction(automationType)) {
            return 0;
        }
        //We limit it overall by the amount we can extract plus how much energy we have
        // as we want to be as accurate as possible with the values we return
        // It is possible that the energy we have stored is a lot less than the amount we
        // can output at once such as if the matrix is almost empty.
        int toRemove = Ints.saturatedCast(Math.min(Math.min(amount, getRemainingOutput()), getAmountAsLong()));
        if (toRemove > 0) {
            //Increase how much we are outputting by the amount we sent
            if (transaction != null) queuedOutput.updateSnapshots(transaction);
            //Increase how much we are inputting
            queuedOutput.value += toRemove;
        }
        return toRemove;
    }

    @Override
    public boolean isValidForExtraction(AutomationType automationType) {
        //Don't allow forcibly extracting from our container so that we ensure that the contents of the matrix's slots get first pickings at being filled
        return !automationType.isExternal();
    }

    @Override
    @Range(from = 0, to = Long.MAX_VALUE)
    public long getCapacityAsLong() {
        return storageCap;
    }

    @Override
    public void serialize(ValueOutput output) {
        //Note: We don't actually have any specific serialization
    }

    @Override
    public void deserialize(ValueInput input) {
    }

    @Override
    public void copyContents(IEnergyContainer other, @Nullable TransactionContext transaction) {
    }

    private long getRemainingInput() {
        return transferCap - queuedInput.value;
    }

    private long getRemainingOutput() {
        return transferCap - queuedOutput.value;
    }

    public long getMaxTransfer() {
        return transferCap;
    }

    public long getLastInput() {
        return lastInput.value;
    }

    public long getLastOutput() {
        return lastOutput.value;
    }

    public int getCells() {
        return cells.size();
    }

    public int getProviders() {
        return providers.size();
    }
}