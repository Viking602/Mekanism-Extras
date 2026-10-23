package com.jerry.mekextras.verification;

import java.util.*;
import com.jerry.mekextras.common.tile.*;
import com.jerry.mekextras.common.tile.multiblock.*;
import com.jerry.mekextras.common.tile.factory.*;
import com.jerry.mekextras.common.content.matrix.*;
import com.jerry.mekextras.common.recipe.bin.*;
import com.jerry.genextras.common.tile.naquadah.*;
import com.jerry.genextras.common.content.naquadah.*;
import mekanism.api.AutomationType;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.component.containers.type.ContainerType;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.util.ItemAccessUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class DeepVerification {
    private static final List<String> checks=new ArrayList<>();
    private static void check(boolean ok,String s){if(!ok)throw new AssertionError(s);checks.add(s);}
    private static int index;
    private static BlockEntity place(MinecraftServer server,String name){
        var id=Identifier.parse(name.contains(":")?name:"mekanism_extras:"+name);var block=BuiltInRegistries.BLOCK.getValue(id);check(block!=Blocks.AIR,"registered "+id);
        var pos=new BlockPos(index++*3,90,100);var level=server.overworld();level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos,block.defaultBlockState());return level.getBlockEntity(pos);
    }
    static List<String> verify(MinecraftServer server)throws Exception {
        var casing=(TileEntityReinforcedInductionCasing)place(server,"reinforced_induction_casing");
        var data=new ReinforcedMatrixMultiblockData(casing);data.setFormedForce(true);var matrix=data.energyContainer();
        var a=(TileEntityExtraInductionCell)place(server,"infinite_induction_cell");var b=(TileEntityExtraInductionCell)place(server,"infinite_induction_cell");
        a.energyContainer().setEnergy(4_000_000_000L,null);b.energyContainer().setEnergy(5_000_000_000L,null);
        matrix.addCell(a.getBlockPos(),a);matrix.addCell(b.getBlockPos(),b);
        matrix.addProvider(casing.getBlockPos(),(TileEntityExtraInductionProvider)place(server,"infinite_induction_provider"));
        check(matrix.getAmountAsLong()==9_000_000_000L,"matrix aggregate exceeds 32 bits");
        check(matrix.getCapacityAsLong()==Long.MAX_VALUE,"matrix capacity saturates safely");
        matrix.addCell(a.getBlockPos(),a);check(matrix.getCells()==2&&matrix.getAmountAsLong()==9_000_000_000L,"duplicate cell registration preserves aggregate");
        try(var tx=Transaction.openRoot()){check(matrix.insert(1000,tx,AutomationType.INTERNAL)==1000,"matrix queued insertion");}
        check(matrix.getAmountAsLong()==9_000_000_000L,"matrix insertion rollback");
        try(var tx=Transaction.openRoot()){matrix.extract(1000,tx,AutomationType.INTERNAL);matrix.tick(List.of(),null,null,tx);}
        check(matrix.getAmountAsLong()==9_000_000_000L&&a.energyContainer().getAmountAsLong()+b.energyContainer().getAmountAsLong()==9_000_000_000L,"matrix flushed transaction rollback restores cells and aggregate");
        try(var tx=Transaction.openRoot()){matrix.insert(1000,tx,AutomationType.INTERNAL);tx.commit();}
        matrix.tick(List.of(),null,null,null);check(matrix.getAmountAsLong()==9_000_001_000L,"matrix flush conserves input");
        long remaining=b.energyContainer().getAmountAsLong();matrix.removeInternal(a.getBlockPos());
        check(matrix.getCapacityAsLong()==Long.MAX_VALUE&&matrix.getAmountAsLong()==remaining,"removing saturated cell preserves remaining capacity and energy");

        var base=(mekanism.common.tile.TileEntityBin)place(server,"mekanism:ultimate_bin");
        var iron=ItemResource.of(Items.IRON_INGOT);base.getBinSlot().setContents(iron,1000,null);base.getBinSlot().setLocked(true);
        var bin=(TileEntityExtraBin)place(server,"absolute_bin");
        try(var tx=Transaction.openRoot()){bin.parseUpgradeData(base.getUpgradeData(server.registryAccess()),server.registryAccess(),tx);tx.commit();}
        check(bin.getBinSlot().amountAsLong()==1000&&bin.getBinSlot().isLocked(),"vanilla to Extras bin upgrade retains contents and lock");
        try(var tx=Transaction.openRoot()){bin.getBinSlot().extract(iron,1000,tx,AutomationType.INTERNAL);tx.commit();}
        var next=(TileEntityExtraBin)place(server,"supreme_bin");
        try(var tx=Transaction.openRoot()){next.parseUpgradeData(bin.getUpgradeData(server.registryAccess()),server.registryAccess(),tx);tx.commit();}
        try(var tx=Transaction.openRoot()){check(next.getBinSlot().insert(ItemResource.of(Items.DIRT),1,tx,AutomationType.INTERNAL)==0,"empty bin retains type lock through upgrade");}
        base.getBinSlot().setContents(ItemResource.EMPTY,0,null);
        var empty=(TileEntityExtraBin)place(server,"absolute_bin");
        try(var tx=Transaction.openRoot()){empty.parseUpgradeData(base.getUpgradeData(server.registryAccess()),server.registryAccess(),tx);tx.commit();}
        check(empty.getBinSlot().isLocked()&&empty.getBinSlot().getLockType().equals(iron),"empty vanilla bin lock survives upgrade");
        var binItem=new ItemStack(bin.getBlockState().getBlock());
        var input=CraftingInput.of(2,1,List.of(binItem,new ItemStack(Items.IRON_INGOT,64)));
        check(ExtraBinInsertRecipe.INSTANCE.matches(input,server.overworld()),"bin insertion recipe matches");
        var crafted=ExtraBinInsertRecipe.INSTANCE.assemble(input);var contents=ContainerType.ITEM.createContainer(ItemAccessUtils.sideEffectFreeAccess(crafted),0);
        check(contents.amountAsLong()==1,"bin craft consumes one item per grid slot");
        var extract=CraftingInput.of(1,1,List.of(crafted));check(ExtraBinExtractRecipe.INSTANCE.assemble(extract).is(Items.IRON_INGOT),"bin extraction result");
        var remainder=ExtraBinExtractRecipe.INSTANCE.getRemainingItems(extract).getFirst();
        check(ContainerType.ITEM.createContainer(ItemAccessUtils.sideEffectFreeAccess(remainder),0).isEmpty(),"bin extraction remainder conserves quantity");

        var reactorCasing=(TileEntityNaquadahReactorCasing)place(server,"naquadah_reactor_casing");var reactor=new NaquadahReactorMultiblockData(reactorCasing);reactor.setFormedForce(true);
        check(reactor.getInjectionRate()==2&&reactor.waterTank.capacityAsLong(net.neoforged.neoforge.transfer.fluid.FluidResource.EMPTY)>0,"reactor initializes coolant capacity");
        reactor.setInjectionRate(98);check(reactor.steamTank.capacityAsLong(mekanism.api.chemical.ChemicalResource.EMPTY)>Integer.MAX_VALUE,"reactor steam capacity exceeds 32 bits");
        try(var tx=Transaction.openRoot()){reactor.setInjectionRate(20,tx);}
        check(reactor.getInjectionRate()==98,"reactor injection setting rollback");
        reactor.heatCapacitor.setHeat(1e15,null);var transfer=NaquadahReactorMultiblockData.class.getDeclaredMethod("transferHeat",net.neoforged.neoforge.transfer.transaction.TransactionContext.class);transfer.setAccessible(true);
        try(var tx=Transaction.openRoot()){transfer.invoke(reactor,tx);tx.commit();}
        check(reactor.energyContainer.getAmountAsLong()>Integer.MAX_VALUE,"reactor generation is not capped to 32 bits per tick");

        var factory=(TileEntityExtraFactory<?>)place(server,"absolute_smelting_factory");factory.energyContainer().setEnergy(factory.energyContainer().getCapacityAsLong(),null);
        var getInput=TileEntityExtraFactory.class.getDeclaredMethod("getInputSlot",int.class);getInput.setAccessible(true);var getOutput=TileEntityExtraFactory.class.getDeclaredMethod("getOutputSlot",int.class);getOutput.setAccessible(true);
        ((IInventorySlot)getInput.invoke(factory,0)).setContents(ItemResource.of(Items.RAW_IRON),1,null);
        for(int t=0;t<250;t++){factory.energyContainer().setEnergy(factory.energyContainer().getCapacityAsLong(),null);TileEntityMekanism.tickServer(server.overworld(),factory.getBlockPos(),factory.getBlockState(),factory);}
        long count=0;for(int n=0;n<9;n++){var slot=(IInventorySlot)getOutput.invoke(factory,n);if(slot.resource().is(Items.IRON_INGOT))count+=slot.amountAsLong();}
        check(count==1,"factory processes actual smelting recipe: count="+count+", progress="+Arrays.toString(factory.progress)+", input="+((IInventorySlot)getInput.invoke(factory,0)).asStack()+", energy="+factory.energyContainer().getAmountAsLong()+", usage="+factory.energyContainer().getEnergyPerTick()+", recipe="+mekanism.common.recipe.MekanismRecipeType.SMELTING.getInputCache().containsInput(server.overworld(),ItemResource.of(Items.RAW_IRON)));
        var stackUpgrade=com.jerry.mekextras.api.ExtraUpgrade.STACK;
        var creativeUpgrade=com.jerry.mekextras.api.ExtraUpgrade.CREATIVE;
        check(factory.getComponent().addUpgrades(stackUpgrade,2)==2,"factory accepts stack upgrades");
        ((IInventorySlot)getInput.invoke(factory,0)).setContents(ItemResource.of(Items.RAW_IRON),64,null);
        check(factory.getComponent().addUpgrades(creativeUpgrade,1)==1,"factory accepts creative upgrade");
        check(factory.energyContainer().getEnergyPerTick()==0,"creative upgrade removes energy consumption");
        TileEntityMekanism.tickServer(server.overworld(),factory.getBlockPos(),factory.getBlockState(),factory);
        long beforeRemoval=factory.energyContainer().getAmountAsLong();
        check(((IInventorySlot)getOutput.invoke(factory,0)).amountAsLong()>1,"creative factory processes immediately");
        check(((IInventorySlot)getInput.invoke(factory,0)).amountAsLong()+((IInventorySlot)getOutput.invoke(factory,0)).amountAsLong()==65,"stacked creative processing preserves total items");
        factory.getComponent().removeUpgrade(creativeUpgrade,true);
        check(factory.getUpgrades(creativeUpgrade)==0&&factory.energyContainer().getEnergyPerTick()>0,"removing creative restores energy usage");
        check(factory.energyContainer().getAmountAsLong()<beforeRemoval&&factory.energyContainer().getCapacityAsLong()<Long.MAX_VALUE,"removing creative clears unbounded stored energy");
        return checks;
    }
}
