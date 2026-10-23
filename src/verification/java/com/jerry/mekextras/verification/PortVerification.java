package com.jerry.mekextras.verification;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import mekanism.api.AutomationType;
import mekanism.api.chemical.ChemicalResource;
import mekanism.common.registries.MekanismChemicals;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.transfer.transaction.Transaction;

@Mod("mekanism_extras_verification")
public final class PortVerification {
    private static final List<String> checks=new ArrayList<>();
    private static final List<String> failures=new ArrayList<>();
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);checks.add(message);}
    public PortVerification(){if(Boolean.getBoolean("mekextras.verify"))NeoForge.EVENT_BUS.addListener(PortVerification::verify);}
    private static void verify(ServerStartedEvent event){
        var server=event.getServer();var level=server.overworld();var result=new LinkedHashMap<String,Object>();
        int i=0;
        try {
            for(var block:BuiltInRegistries.BLOCK){
                var id=BuiltInRegistries.BLOCK.getKey(block);
                if(!id.getNamespace().equals("mekanism_extras"))continue;
                var pos=new BlockPos((i%16)*3,90,(i/16)*3);i++;
                try {
                    level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(pos,block.defaultBlockState());
                    check(level.getBlockState(pos).is(block),"placed "+id);
                    var tile=level.getBlockEntity(pos);
                    if(block instanceof EntityBlock){
                        check(tile!=null,"tile created "+id);
                        var saved=tile.saveWithFullMetadata(server.registryAccess());
                        var copy=BlockEntity.loadStatic(pos,block.defaultBlockState(),saved,server.registryAccess());
                        check(copy!=null&&copy.getType()==tile.getType(),"tile save reload "+id);
                    }
                    if(tile instanceof com.jerry.mekextras.common.tile.TileEntityExtraChemicalTank tankTile){
                        var oxygen=ChemicalResource.of(MekanismChemicals.OXYGEN.get());
                        var tank=tankTile.getChemicalTanks().getFirst();
                        check(tank.capacityAsLong(oxygen)>0,"chemical capacity "+id);
                        try(var tx=Transaction.openRoot()){check(tank.insert(oxygen,1000,tx,AutomationType.INTERNAL)==1000,"chemical insertion "+id);}
                        check(tank.amountAsLong()==0,"chemical rollback "+id);
                        long amount=Math.min(6_000_000_000L,tank.capacityAsLong(oxygen));
                        tank.setContents(oxygen,amount,null);
                        var copy=(com.jerry.mekextras.common.tile.TileEntityExtraChemicalTank)BlockEntity.loadStatic(pos,block.defaultBlockState(),tile.saveWithFullMetadata(server.registryAccess()),server.registryAccess());
                        check(copy.getChemicalTanks().getFirst().amountAsLong()==amount,"chemical persistence "+amount+" "+id);
                    }
                    if(tile instanceof com.jerry.mekextras.common.tile.TileEntityExtraEnergyCube mek){
                        var energy=mek.energyContainer();
                        check(energy!=null&&energy.getCapacityAsLong()>0,"64-bit energy capacity "+id);
                        long amount=Math.min(6_000_000_000L,energy.getCapacityAsLong());
                        energy.setEnergy(amount,null);
                        try(var tx=Transaction.openRoot()){check(energy.extract(1000,tx,AutomationType.INTERNAL)==1000,"energy extraction "+id);}
                        check(energy.getAmountAsLong()==amount,"energy rollback "+id);
                        var copy=(com.jerry.mekextras.common.tile.TileEntityExtraEnergyCube)BlockEntity.loadStatic(pos,block.defaultBlockState(),tile.saveWithFullMetadata(server.registryAccess()),server.registryAccess());
                        check(copy.energyContainer().getAmountAsLong()==amount,"64-bit energy persistence "+id);
                    }
                }catch(Throwable t){t.printStackTrace();failures.add(id+": "+t);}
            }
            var recipes=server.getRecipeManager().getRecipes();
            long recipeCount=recipes.stream().filter(r->r.id().identifier().getNamespace().equals("mekanism_extras")).count();
            check(recipeCount==249,"all 249 recipes loaded (actual "+recipeCount+")");
            checks.addAll(DeepVerification.verify(server));
        }catch(Throwable t){t.printStackTrace();failures.add(t.toString());}
        finally{
            result.put("status",failures.isEmpty()?"passed":"failed");result.put("blocks",i);result.put("checks",checks);result.put("failures",failures);
            try{Files.writeString(server.getServerDirectory().resolve("mekextras-verification.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));}catch(Exception t){t.printStackTrace();}
            server.halt(false);
        }
    }
}
