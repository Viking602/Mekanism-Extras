package com.jerry.mekextras.common.recipe.bin;

import com.jerry.mekextras.common.attachments.containers.item.ExtraComponentBackedBinInventorySlot;
import mekanism.common.component.containers.type.ContainerType;
import com.jerry.mekextras.common.item.block.ItemBlockExtraBin;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;

//Note: We don't bother checking anywhere to ensure the bin's item stack size is one, as we only allow bins
// to be in stacks of one anyway. If this changes at some point, then we will need to adjust this recipe
public abstract class ExtraBinRecipe extends CustomRecipe {

    protected static ExtraComponentBackedBinInventorySlot convertToSlot(ItemAccess binAccess) {
        ItemResource resource = binAccess.getResource();
        if (!resource.isEmpty() && resource.getItem() instanceof ItemBlockExtraBin && ContainerType.ITEM.createContainer(binAccess, 0) instanceof ExtraComponentBackedBinInventorySlot binSlot) {
            return binSlot;
        }
        throw new IllegalStateException("Expected bin stack to have an inventory");
    }

}