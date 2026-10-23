package com.jerry.mekextras.api.recipes.outputs;
import java.util.function.IntSupplier;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.SawmillRecipe.ChanceOutput;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.outputs.IOutputHandler;
import mekanism.api.recipes.outputs.OutputHelper;
import net.minecraft.world.item.ItemStackTemplate;
public final class ExtraOutputHelper {
    private ExtraOutputHelper() {}
    public static IOutputHandler<ItemStackTemplate> getOutputHandler(IInventorySlot slot, RecipeError error, IntSupplier maxOperations) {
        return OutputHelper.getOutputHandler(slot, error);
    }
    public static IOutputHandler<ChanceOutput> getOutputHandler(IInventorySlot main, RecipeError error, IInventorySlot secondary, RecipeError secondaryError, IntSupplier maxOperations) {
        return OutputHelper.getOutputHandler(main, error, secondary, secondaryError);
    }
}
