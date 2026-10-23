package com.jerry.mekextras.common.tile.factory;
import com.jerry.mekextras.api.ExtraUpgrade;
import com.jerry.mekextras.api.mixin.IMixinMachineEnergyContainer;
import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import com.jerry.mekextras.common.util.ExtraUpgradeUtils;


import java.util.List;
import java.util.Set;
import mekanism.api.IContentsListener;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.SawmillRecipe;
import mekanism.api.recipes.SawmillRecipe.ChanceOutput;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.cache.OneInputCachedRecipe;
import mekanism.api.recipes.inputs.IInputHandler;
import mekanism.api.recipes.inputs.InputHelper;
import mekanism.api.recipes.outputs.IOutputHandler;
import mekanism.api.recipes.outputs.OutputHelper;
import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekanism.common.Mekanism;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.capabilities.holder.container.MekContainerHelper;
import mekanism.common.integration.computer.ComputerException;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import com.jerry.mekextras.common.inventory.slot.ExtraFactoryInputInventorySlot;
import com.jerry.mekextras.common.inventory.slot.ExtraFactoryOutputInventorySlot;
import mekanism.common.inventory.warning.WarningTracker.WarningType;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.recipe.lookup.ISingleRecipeLookupHandler.ItemRecipeLookupHandler;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.SingleItem;
import mekanism.common.recipe.lookup.cache.SingleInputRecipeCache.CheckRecipeType;
import mekanism.common.recipe.lookup.monitor.FactoryRecipeCacheLookupMonitor;
import com.jerry.mekextras.common.tier.ExtraFactoryTier;
import mekanism.common.tile.machine.TileEntityPrecisionSawmill;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.upgrade.SawmillUpgradeData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public class TileEntityExtraSawingFactory extends TileEntityExtraFactory<SawmillRecipe> implements ItemRecipeLookupHandler<SawmillRecipe> {

    private static final CheckRecipeType<Item, ItemResource, SawmillRecipe, ItemResource, ItemResource> CAN_OUTPUTS_STACK = (recipe, input, outputContents, secondaryOutputContents) -> {
        ChanceOutput chanceOutput = recipe.getOutput(input);
        if (outputContents.isEmpty() || outputContents.matches(chanceOutput.getMainOutput())) {
            //If the input is good and the primary output matches, make sure that the secondary
            // output of this recipe will stack with what is currently in the secondary slot
            if (secondaryOutputContents.isEmpty()) {
                return true;
            }
            ItemStackTemplate secondaryOutput = chanceOutput.getMaxSecondaryOutput();
            return secondaryOutput == null || secondaryOutputContents.matches(secondaryOutput);
        }
        return false;
    };
    private static final List<RecipeError> TRACKED_ERROR_TYPES = List.of(
          RecipeError.NOT_ENOUGH_ENERGY,
          RecipeError.NOT_ENOUGH_INPUT,
          RecipeError.NOT_ENOUGH_OUTPUT_SPACE,
          TileEntityPrecisionSawmill.NOT_ENOUGH_SPACE_SECONDARY_OUTPUT_ERROR,
          RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT
    );
    private static final Set<RecipeError> GLOBAL_ERROR_TYPES = Set.of(RecipeError.NOT_ENOUGH_ENERGY);

    protected final IInputHandler<Item, ItemStack>[] inputHandlers;
    protected final IOutputHandler<ChanceOutput>[] outputHandlers;

    public TileEntityExtraSawingFactory(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        ExtraFactoryTier tier = java.util.Objects.requireNonNull(ExtraAttribute.getAdvancedTier(blockProvider, ExtraFactoryTier.class));
        inputHandlers = new IInputHandler[tier.processes];
        outputHandlers = new IOutputHandler[tier.processes];
        super(blockProvider, pos, state, TRACKED_ERROR_TYPES, GLOBAL_ERROR_TYPES);
    }

    @Override
    protected void addSlots(MekContainerHelper<IInventorySlot> builder, IContentsListener listener, IContentsListener updateSortingListener) {
        int baseX = 27;
        int baseXMult = 19;
        for (int i = 0; i < tier.processes; i++) {
            int xPos = baseX + (i * baseXMult);
            FactoryRecipeCacheLookupMonitor<SawmillRecipe> lookupMonitor = recipeCacheLookupMonitors[i];
            IContentsListener updateSortingAndUnpause = () -> {
                updateSortingListener.onContentsChanged();
                lookupMonitor.unpause();
            };
            ExtraFactoryOutputInventorySlot outputSlot = ExtraFactoryOutputInventorySlot.at(this, updateSortingAndUnpause, xPos, 57);
            ExtraFactoryOutputInventorySlot secondaryOutputSlot = ExtraFactoryOutputInventorySlot.at(this, updateSortingAndUnpause, xPos, 77);
            //Note: As we are an item factory that has comparator's based on items we can just use the monitor as a listener directly
            ExtraFactoryInputInventorySlot inputSlot = ExtraFactoryInputInventorySlot.create(this, i, outputSlot, secondaryOutputSlot, lookupMonitor, xPos, 13);
            int index = i;
            builder.addContainer(inputSlot).tracksWarnings(slot -> slot.warning(WarningType.NO_MATCHING_RECIPE, getWarningCheck(RecipeError.NOT_ENOUGH_INPUT, index)));
            builder.addContainer(outputSlot).tracksWarnings(slot -> slot.warning(WarningType.NO_SPACE_IN_OUTPUT, getWarningCheck(RecipeError.NOT_ENOUGH_OUTPUT_SPACE, index)));
            builder.addContainer(secondaryOutputSlot).tracksWarnings(slot -> slot.warning(WarningType.NO_SPACE_IN_OUTPUT,
                  getWarningCheck(TileEntityPrecisionSawmill.NOT_ENOUGH_SPACE_SECONDARY_OUTPUT_ERROR, index)));
            inputHandlers[i] = InputHelper.getInputHandler(inputSlot, RecipeError.NOT_ENOUGH_INPUT);
            outputHandlers[i] = OutputHelper.getOutputHandler(outputSlot, RecipeError.NOT_ENOUGH_OUTPUT_SPACE, secondaryOutputSlot,
                  TileEntityPrecisionSawmill.NOT_ENOUGH_SPACE_SECONDARY_OUTPUT_ERROR);
            processInfoSlots[i] = new ProcessInfo(i, inputSlot, outputSlot, secondaryOutputSlot);
        }
    }

    @Override
    public boolean isItemValidForSlot(ItemResource itemType) {
        //contains recipe in general already validated by isValidInputItem
        return true;
    }

    @Override
    public boolean isValidInputItem(ItemResource itemType) {
        return containsRecipe(itemType);
    }

    @Override
    protected int getNeededInput(SawmillRecipe recipe, ItemResource inputType) {
        return recipe.getInput().getNeededAmount(inputType);
    }

    @Override
    protected boolean isCachedRecipeValid(@Nullable CachedRecipe<SawmillRecipe> cached, ItemResource itemType) {
        return cached != null && cached.getRecipe().getInput().testType(itemType);
    }

    @Nullable
    @Override
    protected SawmillRecipe findRecipe(ItemResource fallbackInput, IInventorySlot outputSlot, @Nullable IInventorySlot secondaryOutputSlot) {
        ItemResource extra = secondaryOutputSlot == null ? ItemResource.EMPTY : secondaryOutputSlot.resource();
        return getRecipeType().getInputCache().findTypeBasedRecipe(level, fallbackInput, outputSlot.resource(), extra, CAN_OUTPUTS_STACK);
    }

    @Override
    public IMekanismRecipeTypeProvider<SingleRecipeInput, SawmillRecipe, SingleItem<SawmillRecipe>> getRecipeType() {
        return MekanismRecipeType.SAWING;
    }

    @Override
    public IRecipeViewerRecipeType<SawmillRecipe> recipeViewerType() {
        return RecipeViewerRecipeType.SAWING;
    }

    @Nullable
    @Override
    public SawmillRecipe getRecipe(int cacheIndex) {
        return findFirstRecipe(inputHandlers[cacheIndex]);
    }

    @Override
    public CachedRecipe<SawmillRecipe> createNewCachedRecipe(SawmillRecipe recipe, int cacheIndex) {
        return new OneInputCachedRecipe<>(recipe, recheckAllRecipeErrors[cacheIndex], inputHandlers[cacheIndex], outputHandlers[cacheIndex])
              .setErrorsChanged(errors -> errorTracker.onErrorsChanged(errors, cacheIndex))
              .setCanHolderFunction(this::canFunction)
              .setActive(active -> setActiveState(active, cacheIndex))
              .setEnergyRequirements(energyContainer::getEnergyPerTick, energyContainer)
              .setRequiredTicks(this::getTicksRequired)
              .setOnFinish(this::markForSave)
              .setOperatingTicksChanged(operatingTicks -> progress[cacheIndex] = operatingTicks)
              .setBaselineMaxOperations(this::getOperationsPerTick);
    }

    @Override
    public void parseUpgradeData(IUpgradeData upgradeData, Provider provider, TransactionContext transaction) {
        if (upgradeData instanceof SawmillUpgradeData) {
            //Validate we have the correct type of data before passing it upwards
            super.parseUpgradeData(upgradeData, provider, transaction);
        } else {
            Mekanism.logger.warn("Unhandled upgrade data.", new Throwable());
        }
    }

    @Override
    public SawmillUpgradeData getUpgradeData(HolderLookup.Provider provider) {
        return new SawmillUpgradeData(provider, redstone, getControlType(), energyContainer, progress, energySlot, inputSlots, outputSlots, isSorting(), getComponents(), problemPath());
    }

    //Methods relating to IComputerTile
    @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getSecondaryOutput", docPlaceholder = "secondary output slot")
    IInventorySlot getSecondaryOutputSlot(int process) throws ComputerException {
        validateValidProcess(process);
        IInventorySlot secondaryOutputSlot = processInfoSlots[process].secondaryOutputSlot();
        //This should never be null, but in case it is, handle it
        if (secondaryOutputSlot == null) {
            throw new ComputerException("Process: '%d' has a null secondary output slot, this should not be possible for sawing factories", process);
        }
        return secondaryOutputSlot;
    }
    //End methods IComputerTile
}