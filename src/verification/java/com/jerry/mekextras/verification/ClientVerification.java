package com.jerry.mekextras.verification;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@Mod(value="mekanism_extras_verification",dist=Dist.CLIENT)
public final class ClientVerification {
    private static boolean started,finished;
    private static int ticks,stage,page;
    private static List<Item> items;
    private static final List<String> checks=new ArrayList<>();
    private static final String[] screens={"absolute_enriching_factory","supreme_enriching_factory","cosmic_enriching_factory","infinite_enriching_factory","absolute_energy_cube","infinite_fluid_tank","absolute_washing_factory","infinite_planting_factory"};
    public ClientVerification(){if(Boolean.getBoolean("mekextras.verify.client"))NeoForge.EVENT_BUS.addListener(ClientVerification::tick);}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks.add(message);}
    private static void screenshot(String name){var mc=Minecraft.getInstance();Screenshot.takeScreenshot(mc.getMainRenderTarget(),shot->{try(shot){shot.writeToFile(mc.gameDirectory.toPath().resolve(name+".png"));}catch(Exception e){finish(false,e.toString());}});}
    private static void tick(ClientTickEvent.Post event){
        if(finished)return;var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
        try{
            if(!started){
                if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;
                check(Set.of("test-mekanismextras-client-20261011","test-mekaddons-fullpack-20261011").contains(mc.gameDirectory.toPath().toAbsolutePath().normalize().getFileName().toString()),"isolated fixture");
                items=BuiltInRegistries.ITEM.stream().filter(x->BuiltInRegistries.ITEM.getKey(x).getNamespace().equals("mekanism_extras")).toList();
                for(var item:items){var id=BuiltInRegistries.ITEM.getKey(item);check(mc.getResourceManager().getResource(Identifier.fromNamespaceAndPath(id.getNamespace(),"items/"+id.getPath()+".json")).isPresent(),"model definition "+id);}
                started=true;
                mc.createWorldOpenFlows().createFreshLevel("extras-"+System.currentTimeMillis(),new LevelSettings("Extras verification",GameType.CREATIVE,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),new WorldOptions(602,false,false),WorldPresets::createFlatWorldDimensions,mc.screen);return;
            }
            if(++ticks>6000)throw new AssertionError("client timeout");
            if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.player.tickCount<60||ticks%70!=0)return;
            var server=mc.getSingleplayerServer();
            if(stage==0&&Boolean.getBoolean("mekextras.verify.worldOnly")){
                server.submit(()->WorldVerification.setup(server,server.getPlayerList().getPlayer(mc.player.getUUID()))).join();
                mc.options.hideGui=true;stage=3;return;
            }
            if(stage==3){server.submit(()->WorldVerification.fill(server)).join();stage=4;return;}
            if(stage==4){
                var cube=(com.jerry.mekextras.common.tile.TileEntityExtraEnergyCube)mc.level.getBlockEntity(new BlockPos(0,90,40));
                check(cube!=null&&mc.getBlockEntityRenderDispatcher().getRenderer(cube)!=null,"world energy renderer registered");
                var pipe=(com.jerry.mekextras.common.tile.transmitter.TileEntityExtraMechanicalPipe)mc.level.getBlockEntity(new BlockPos(4,92,43));
                var network=pipe.getTransmitter().getTransmitterNetwork();
                check(network!=null&&!network.getLastType().isEmpty()&&network.currentScale>0,"client fluid network contents and scale synchronized");
                var renderer=(com.jerry.mekextras.client.render.transmitter.RenderExtraMechanicalPipe)(Object)mc.getBlockEntityRenderDispatcher().getRenderer(pipe);
                check(renderer.shouldRender(pipe,mc.player.position()),"fluid renderer accepts synchronized pipe");
                var state=renderer.createRenderState();renderer.extractRenderState(pipe,state,0,mc.player.position(),null);
                check(state.fluidTexture!=null&&net.minecraft.util.ARGB.alpha(state.fluidTint)>0,"fluid renderer has visible material tint="+Integer.toHexString(state.fluidTint)+" scale="+state.currentScale);
                var tube=(com.jerry.mekextras.common.tile.transmitter.TileEntityExtraPressurizedTube)mc.level.getBlockEntity(new BlockPos(7,92,43));
                var chemicalNetwork=tube.getTransmitter().getTransmitterNetwork();
                check(chemicalNetwork!=null&&!chemicalNetwork.getLastType().isEmpty()&&chemicalNetwork.currentScale>0,"client chemical network contents and scale synchronized");
                var tubeRenderer=(com.jerry.mekextras.client.render.transmitter.RenderExtraPressurizedTube)(Object)mc.getBlockEntityRenderDispatcher().getRenderer(tube);
                check(tubeRenderer.shouldRender(tube,mc.player.position()),"chemical renderer accepts synchronized tube");
                var tubeState=tubeRenderer.createRenderState();tubeRenderer.extractRenderState(tube,tubeState,0,mc.player.position(),null);
                check(tubeState.chemicalTexture!=null&&tubeState.currentScale>0,"chemical renderer has visible material");
                screenshot("extras-world");finish(true,"");return;
            }
            if(stage==0){
                if(page>0){check(mc.screen instanceof net.minecraft.client.gui.screens.inventory.ContainerScreen,"inventory page "+page+" rendered");screenshot("extras-items-"+page);}
                if(page*54<items.size()){
                    var inventory=new SimpleContainer(54);for(int n=0;n<54&&page*54+n<items.size();n++)inventory.setItem(n,new ItemStack(items.get(page*54+n)));
                    server.submit(()->{var player=server.getPlayerList().getPlayer(mc.player.getUUID());player.closeContainer();player.openMenu(new SimpleMenuProvider((id,inv,p)->new ChestMenu(MenuType.GENERIC_9x6,id,inv,inventory,6),Component.literal("Extras 26.1.2 / "+(page+1))));}).join();page++;return;
                }
                page=0;stage++;
            }
            if(stage==2){
                server.submit(()->{var player=server.getPlayerList().getPlayer(mc.player.getUUID());var tile=(mekanism.common.tile.base.TileEntityMekanism)server.overworld().getBlockEntity(new BlockPos(0,90,0));tile.openGui(player);}).join();
                page++;stage=1;return;
            }
            if(stage==1){
                if(page>0){check(mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen,"GUI rendered "+screens[page-1]);screenshot("extras-gui-"+screens[page-1]);}
                if(page<screens.length){
                    var id=Identifier.fromNamespaceAndPath("mekanism_extras",screens[page]);
                    server.submit(()->{var player=server.getPlayerList().getPlayer(mc.player.getUUID());player.closeContainer();var pos=new BlockPos(0,90,0);var block=BuiltInRegistries.BLOCK.getValue(id);server.overworld().setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());server.overworld().setBlockAndUpdate(pos,block.defaultBlockState());player.teleportTo(server.overworld(),0,90,-4,Set.of(),0,0,false);var tile=(mekanism.common.tile.base.TileEntityMekanism)server.overworld().getBlockEntity(pos);check(tile!=null,"GUI tile "+id);}).join();stage=2;return;
                }
                server.submit(()->WorldVerification.setup(server,server.getPlayerList().getPlayer(mc.player.getUUID()))).join();mc.options.hideGui=true;stage=3;
            }
        }catch(Throwable t){t.printStackTrace();finish(false,t.toString());}
    }
    private static void finish(boolean passed,String error){if(finished)return;finished=true;var mc=Minecraft.getInstance();try{Files.writeString(mc.gameDirectory.toPath().resolve("mekextras-client-verification.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("passed",passed,"error",error,"checks",checks)));}catch(Exception e){e.printStackTrace();}mc.stop();}
}
