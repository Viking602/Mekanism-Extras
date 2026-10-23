package com.jerry.mekextras.common.inventory.slot.chemical;

import java.util.function.Supplier;
import com.jerry.mekextras.common.tile.factory.TileEntityExtraFactory;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.IChemicalTank;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.inventory.slot.ChemicalInventorySlot;
import mekanism.common.util.ItemAccessUtils;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

public class ExtraFactoryChemicalInventorySlot extends ChemicalInventorySlot {
    private final TileEntityExtraFactory<?> factory;

    private ExtraFactoryChemicalInventorySlot(TileEntityExtraFactory<?> factory, IChemicalTank tank,
          Supplier<@Nullable Level> level, @Nullable IContentsListener listener, int x, int y) {
        super(tank, level, (resource, automation) -> !automation.isExternal() || !canFillOrConvert(tank, level, resource),
              (resource, automation) -> automation.isInternal() || canFillOrConvert(tank, level, resource), null, null, listener, x, y);
        this.factory = factory;
    }

    public static ExtraFactoryChemicalInventorySlot fillOrConverts(TileEntityExtraFactory<?> factory, IChemicalTank tank,
          Supplier<@Nullable Level> level, @Nullable IContentsListener listener, int x, int y) {
        return new ExtraFactoryChemicalInventorySlot(factory, tank, level, listener, x, y);
    }

    @Override
    public long capacityAsLong(ItemResource resource) {
        long base = super.capacityAsLong(resource);
        if (!resource.isEmpty() && ItemAccessUtils.sideEffectFreeAccess(resource).getCapability(Capabilities.CHEMICAL.item()) != null) {
            return base;
        }
        return base * factory.tier.processes * (8L << factory.tier.ordinal());
    }
}
