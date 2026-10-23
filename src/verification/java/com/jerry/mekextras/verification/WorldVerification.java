package com.jerry.mekextras.verification;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import com.jerry.mekextras.common.tile.*;
import com.jerry.mekextras.common.tile.transmitter.*;
final class WorldVerification {
    static void setup(MinecraftServer server,ServerPlayer player){
        var level=server.overworld();player.closeContainer();
        for(int x=-1;x<=12;x++)for(int z=31;z<=46;z++)level.setBlockAndUpdate(new BlockPos(x,89,z),Blocks.SMOOTH_STONE.defaultBlockState());
        String[] single={"cosmic_energy_cube","infinite_fluid_tank","infinite_chemical_tank","infinite_bin"};
        for(int n=0;n<single.length;n++)level.setBlockAndUpdate(new BlockPos(n*2,90,40),BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("mekanism_extras",single[n])).defaultBlockState());
        String[] pipes={"absolute_universal_cable","supreme_mechanical_pipe","cosmic_pressurized_tube","infinite_thermodynamic_conductor"};
        for(int n=0;n<4;n++)for(int x=0;x<3;x++)level.setBlockAndUpdate(new BlockPos(n*3+x,92,43),BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("mekanism_extras",pipes[n])).defaultBlockState());
        player.teleportTo(level,5.5,90,32,Set.of(),0,0,false);
    }
    static void fill(MinecraftServer server){
        var level=server.overworld();
        ((TileEntityExtraEnergyCube)level.getBlockEntity(new BlockPos(0,90,40))).energyContainer().setEnergy(8_000_000_000L,null);
        var water=net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER);
        var oxygen=mekanism.api.chemical.ChemicalResource.of(mekanism.common.registries.MekanismChemicals.CHLORINE.get());
        var fluid=((TileEntityExtraFluidTank)level.getBlockEntity(new BlockPos(2,90,40))).fluidTank;fluid.setContents(water,fluid.capacityAsLong(water)/2,null);
        var chemical=((TileEntityExtraChemicalTank)level.getBlockEntity(new BlockPos(4,90,40))).getChemicalTanks().getFirst();chemical.setContents(oxygen,chemical.capacityAsLong(oxygen)/2,null);
        var cable=((TileEntityExtraUniversalCable)level.getBlockEntity(new BlockPos(1,92,43))).getTransmitter().getContainer();cable.setEnergy(cable.getCapacityAsLong()/2,null);
        var pipe=((TileEntityExtraMechanicalPipe)level.getBlockEntity(new BlockPos(4,92,43))).getTransmitter().getContainers().getFirst();pipe.setContents(water,pipe.capacityAsLong(water)/2,null);
        var tube=((TileEntityExtraPressurizedTube)level.getBlockEntity(new BlockPos(7,92,43))).getTransmitter().getContainers().getFirst();tube.setContents(oxygen,tube.capacityAsLong(oxygen)/2,null);
        var conductor=((TileEntityExtraThermodynamicConductor)level.getBlockEntity(new BlockPos(10,92,43))).getTransmitter();conductor.buffer.setHeat(conductor.buffer.getHeatCapacity()*1200,null);
    }
}
