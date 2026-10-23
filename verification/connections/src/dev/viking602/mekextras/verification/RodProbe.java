package dev.viking602.mekextras.verification;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.vertex.*;
import java.nio.file.*;
import java.util.*;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.*;
import net.minecraft.world.Difficulty;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Delayed rod placement: compare transmitted connections with the actual baked casing extents. */
public final class RodProbe {
    private static final String[] CABLES={"mekanism:basic_universal_cable","mekanism:advanced_universal_cable","mekanism:elite_universal_cable","mekanism:ultimate_universal_cable","mekanism_extras:absolute_universal_cable","mekanism_extras:supreme_universal_cable","mekanism_extras:cosmic_universal_cable","mekanism_extras:infinite_universal_cable"};
    private final List<Object> checks=new ArrayList<>();
    private final List<String> failures=new ArrayList<>();
    private Path directory;private int phase,ticks,wait;private boolean started,finished;
    public RodProbe(){NeoForge.EVENT_BUS.addListener(this::tick);}
    private BlockPos pos(int tier,Direction side){return new BlockPos(tier%4*5,-58,tier/4*24+side.ordinal()*4);}
    private void tick(ClientTickEvent.Post event){
        if(finished)return;var mc=Minecraft.getInstance();try{
            if(!started){
                if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;
                directory=mc.gameDirectory.toPath().toAbsolutePath().normalize();
                if(!directory.equals(Path.of(System.getProperty("mekextras.connections.fixture")).toAbsolutePath().normalize())||!Files.isRegularFile(directory.resolve("SISE_AUTOTEST_ONLY")))throw new IllegalStateException("Unmarked fixture");
                started=true;mc.options.pauseOnLostFocus=false;
                mc.createWorldOpenFlows().createFreshLevel("rod-world",new LevelSettings("Rod rendering",GameType.CREATIVE,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),new WorldOptions(602,false,false),WorldPresets::createFlatWorldDimensions,mc.screen);phase=1;return;
            }
            if(++ticks>3500)throw new IllegalStateException("Timeout "+phase);
            if(ticks%20==0)write("rod-progress.json",Map.of("phase",phase,"ticks",ticks));
            if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.player.tickCount<60)return;
            var server=mc.getSingleplayerServer();
            if(phase==1){server.submit(()->{
                var w=server.overworld();var p=server.getPlayerList().getPlayer(mc.player.getUUID());p.getAbilities().flying=true;p.onUpdateAbilities();
                for(int x=-3;x<=18;x++)for(int z=-3;z<=46;z++){w.setBlockAndUpdate(new BlockPos(x,-61,z),Blocks.STONE.defaultBlockState());w.setBlockAndUpdate(new BlockPos(x,-60,z),Blocks.GRASS_BLOCK.defaultBlockState());}
                for(int i=0;i<CABLES.length;i++)for(var side:Direction.values())w.setBlockAndUpdate(pos(i,side),BuiltInRegistries.BLOCK.getValue(Identifier.parse(CABLES[i])).defaultBlockState());
                p.connection.teleport(6,-54,-5,0,35);
            }).join();phase=2;wait=ticks+100;return;}
            if(ticks<wait)return;
            if(phase==2){snapshot("before");server.submit(()->{
                var w=server.overworld();for(int i=0;i<CABLES.length;i++)for(var side:Direction.values())w.setBlockAndUpdate(pos(i,side).relative(side),BuiltInRegistries.BLOCK.getValue(Identifier.parse("powah:energizing_rod_starter")).defaultBlockState().setValue(BlockStateProperties.FACING,side.getOpposite()));
            }).join();phase=3;wait=ticks+120;return;}
            if(phase==3){snapshot("connected");aim(pos(4,Direction.UP));phase=4;wait=ticks+60;return;}
            if(phase==4){shot("rod-up-empty");server.submit(()->{
                for(int i=0;i<CABLES.length;i++)for(var side:Direction.values()){
                    var tile=(TileEntityTransmitter)server.overworld().getBlockEntity(pos(i,side));var c=(mekanism.common.content.network.transmitter.UniversalCable)tile.getTransmitter();c.getContainer().setEnergy(c.getContainer().getCapacityAsLong(),null);
                }
            }).join();phase=5;wait=ticks+100;return;}
            if(phase==5){snapshot("powered");shot("rod-up-powered");aim(pos(0,Direction.UP));phase=6;wait=ticks+60;return;}
            if(phase==6){shot("rod-basic-up");server.submit(()->{for(int i=0;i<CABLES.length;i++)for(var side:Direction.values())server.overworld().setBlockAndUpdate(pos(i,side).relative(side),Blocks.AIR.defaultBlockState());}).join();phase=7;wait=ticks+100;return;}
            if(phase==7){snapshot("removed");server.submit(()->{
                for(int i=0;i<CABLES.length;i++)for(var side:Direction.values())server.overworld().setBlockAndUpdate(pos(i,side).relative(side),BuiltInRegistries.BLOCK.getValue(Identifier.parse(CABLES[i])).defaultBlockState());
            }).join();phase=8;wait=ticks+100;return;}
            if(phase==8){snapshot("joined");server.submit(()->{
                for(int i=0;i<CABLES.length;i++)for(var side:Direction.values())server.overworld().setBlockAndUpdate(pos(i,side).relative(side),BuiltInRegistries.BLOCK.getValue(Identifier.parse("powah:energizing_rod_starter")).defaultBlockState().setValue(BlockStateProperties.FACING,side.getOpposite()));
            }).join();phase=9;wait=ticks+100;return;}
            if(phase==9){snapshot("reconnected");server.submit(()->{
                var w=server.overworld();var p=pos(4,Direction.UP);
                for(var side:List.of(Direction.EAST,Direction.WEST))w.setBlockAndUpdate(p.relative(side),BuiltInRegistries.BLOCK.getValue(Identifier.parse(CABLES[4])).defaultBlockState());
                var c=(mekanism.common.content.network.transmitter.UniversalCable)((TileEntityTransmitter)w.getBlockEntity(p)).getTransmitter();c.getContainer().setEnergy(c.getContainer().getCapacityAsLong(),null);
            }).join();aimClose(pos(4,Direction.UP));phase=10;wait=ticks+100;return;}
            if(phase==10){shot("rod-junction-close");server.submit(()->{
                for(int i=0;i<CABLES.length;i++)for(var side:Direction.values()){
                    var w=server.overworld();var p=pos(i,side);var c=(mekanism.common.content.network.transmitter.UniversalCable)((TileEntityTransmitter)w.getBlockEntity(p)).getTransmitter();c.getContainer().setEnergy(c.getContainer().getCapacityAsLong(),null);
                }
            }).join();phase=11;wait=ticks+100;return;}
            if(phase==11){server.submit(()->{
                var rows=new ArrayList<Object>();for(int i=0;i<CABLES.length;i++)for(var side:Direction.values()){
                    var rod=(owmii.powah.block.energizing.EnergizingRodBlockEntity)server.overworld().getBlockEntity(pos(i,side).relative(side));long energy=rod.getEnergy().getStored();rows.add(Map.of("cable",CABLES[i],"side",side.getSerializedName(),"rodEnergy",energy));if(energy<=0)failures.add("No power "+CABLES[i]+" "+side);
                }checks.add(Map.of("power",rows));
            }).join();finish(true,"");}
        }catch(Throwable e){e.printStackTrace();finish(false,e.toString());}
    }
    private void snapshot(String stage){
        var mc=Minecraft.getInstance();var rows=new ArrayList<Object>();
        for(int tier=0;tier<CABLES.length;tier++)for(var side:Direction.values()){
            var p=pos(tier,side);var tile=(TileEntityTransmitter)mc.level.getBlockEntity(p);if(tile==null)continue;
            var connection=tile.getTransmitter().getConnectionType(side);
            var cached=mc.level.getModelDataManager().getAt(p).get(TileEntityTransmitter.TRANSMITTER_PROPERTY);
            var direct=tile.getModelData().get(TileEntityTransmitter.TRANSMITTER_PROPERTY);
            if(!Objects.equals(cached,direct))failures.add(stage+" "+CABLES[tier]+" "+side+" stale model cache");
            var parts=new ArrayList<BlockStateModelPart>();var state=mc.level.getBlockState(p);
            mc.getModelManager().getBlockStateModelSet().get(state).collectParts(mc.level,p,state,RandomSource.create(602),parts);
            var output=new Bounds();var pose=new PoseStack().last();var instance=new QuadInstance();int plates=0;
            for(var part:parts){for(var d:Direction.values())for(var quad:part.getQuads(d)){output.putBakedQuad(pose,quad,instance);if(endPlate(quad,side))plates++;}for(var quad:part.getQuads(null)){output.putBakedQuad(pose,quad,instance);if(endPlate(quad,side))plates++;}}
            var bounds=output.box.build();double edge=side.getAxisDirection()==Direction.AxisDirection.POSITIVE?side.getAxis().choose(bounds.maxX,bounds.maxY,bounds.maxZ):side.getAxis().choose(bounds.minX,bounds.minY,bounds.minZ);
            boolean extendsToFace=side.getAxisDirection()==Direction.AxisDirection.POSITIVE?edge>=.999:edge<=.001;
            boolean terminal=stage.equals("connected")||stage.equals("powered")||stage.equals("reconnected");boolean expected=terminal||stage.equals("joined");
            var row=new LinkedHashMap<String,Object>();row.put("cable",CABLES[tier]);row.put("side",side.getSerializedName());row.put("connection",connection.name());row.put("cached",cached==null?"missing":cached.getConnectionType(side).name());row.put("direct",direct.getConnectionType(side).name());row.put("cacheMatches",Objects.equals(cached,direct));row.put("bounds",bounds.toString());row.put("edge",edge);row.put("extendsToDevice",extendsToFace);row.put("terminalPlates",plates);rows.add(row);
            if(expected&&!extendsToFace)failures.add(stage+" "+CABLES[tier]+" "+side+" missing connector edge="+edge);
            if(plates!=(terminal?1:0))failures.add(stage+" "+CABLES[tier]+" "+side+" terminal plate count="+plates);
        }
        checks.add(Map.of("stage",stage,"models",rows));write("rod-checkpoints.json",checks);
    }
    private boolean endPlate(net.minecraft.client.resources.model.geometry.BakedQuad quad,Direction side){
        if(quad.direction()!=side)return false;float target=side.getAxisDirection()==Direction.AxisDirection.POSITIVE?1:0;
        for(int i=0;i<4;i++){var v=quad.position(i);if(Math.abs(side.getAxis().choose(v.x(),v.y(),v.z())-target)>.0001)return false;}return true;
    }
    private void aimClose(BlockPos p){
        var mc=Minecraft.getInstance();mc.getSingleplayerServer().submit(()->{
            var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());double x=p.getX()+1.4,y=p.getY()+.5,z=p.getZ()-.4;var eye=new Vec3(x,y+player.getEyeHeight(),z);var delta=Vec3.atCenterOf(p).add(0,.5,0).subtract(eye);float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z)),pitch=(float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance()));player.connection.teleport(x,y,z,yaw,pitch);
        }).join();
    }
    private void aim(BlockPos p){
        var mc=Minecraft.getInstance();mc.getSingleplayerServer().submit(()->{
            var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());double x=p.getX()+2,y=p.getY()+1.4,z=p.getZ()-2;var eye=new Vec3(x,y+player.getEyeHeight(),z);var delta=Vec3.atCenterOf(p).add(0,.4,0).subtract(eye);float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z)),pitch=(float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance()));player.connection.teleport(x,y,z,yaw,pitch);
        }).join();
    }
    private void shot(String name){var mc=Minecraft.getInstance();Screenshot.takeScreenshot(mc.getMainRenderTarget(),image->{try(image){image.writeToFile(directory.resolve(name+".png"));}catch(Exception e){throw new RuntimeException(e);}});}
    private void write(String name,Object data){try{Files.writeString(directory.resolve(name),new GsonBuilder().setPrettyPrinting().create().toJson(data));}catch(Exception e){throw new RuntimeException(e);}}
    private void finish(boolean ok,String error){if(finished)return;finished=true;write("rod-result.json",Map.of("passed",ok&&(Boolean.getBoolean("mekextras.connections.baseline")||failures.isEmpty()),"error",error,"failures",failures,"checks",checks));Minecraft.getInstance().stop();}
    private static final class Bounds implements VertexConsumer{
        final AABB.Builder box=new AABB.Builder();public VertexConsumer addVertex(float x,float y,float z){box.include(new org.joml.Vector3f(x,y,z));return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){return this;}public VertexConsumer setColor(int argb){return this;}public VertexConsumer setUv(float u,float v){return this;}public VertexConsumer setUv1(int u,int v){return this;}public VertexConsumer setUv2(int u,int v){return this;}public VertexConsumer setNormal(float x,float y,float z){return this;}public VertexConsumer setLineWidth(float width){return this;}
    }
}
