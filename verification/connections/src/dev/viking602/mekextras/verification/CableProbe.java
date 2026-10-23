package dev.viking602.mekextras.verification;

import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.component.containers.type.ContainerType;
import mekanism.common.item.block.ItemBlockEnergyCube;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.TileEntityEnergyCube;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** A separate test JAR: exercises native placement, FE delivery, networks and contextual models. */
@Mod("mekextras_connection_probe")
public final class CableProbe {
    private static final String[] CABLES={"mekanism:basic_universal_cable","mekanism:advanced_universal_cable","mekanism:elite_universal_cable","mekanism:ultimate_universal_cable","mekanism_extras:absolute_universal_cable","mekanism_extras:supreme_universal_cable","mekanism_extras:cosmic_universal_cable","mekanism_extras:infinite_universal_cable"};
    private static final String[] POWAH={"starter","basic","hardened","blazing","niotic","spirited","nitro"};
    private final List<Object> checks=new ArrayList<>();
    private final List<String> failures=new ArrayList<>();
    private Path directory;
    private int ticks,phase,wait,cycle;
    private boolean started,finished;
    public CableProbe(){if(Boolean.getBoolean("mekextras.connections.rodOnly"))new RodProbe();else NeoForge.EVENT_BUS.addListener(this::tick);}
    private void tick(ClientTickEvent.Post event){
        if(finished)return;var mc=Minecraft.getInstance();try{
            if(!started){
                if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;
                directory=mc.gameDirectory.toPath().toAbsolutePath().normalize();
                require(directory.equals(Path.of(System.getProperty("mekextras.connections.fixture")).toAbsolutePath().normalize())&&Files.isRegularFile(directory.resolve("SISE_AUTOTEST_ONLY")),"Not a marked test world");
                started=true;mc.options.pauseOnLostFocus=false;
                mc.createWorldOpenFlows().createFreshLevel("cable-world",new LevelSettings("Cable verification",GameType.CREATIVE,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),new WorldOptions(602,false,false),WorldPresets::createFlatWorldDimensions,mc.screen);phase=1;return;
            }
            if(++ticks>5000)throw new IllegalStateException("Timeout in phase "+phase);
            if(ticks%20==0)write("connection-progress.json",Map.of("phase",phase,"ticks",ticks,"cycle",cycle));
            if(phase==9&&mc.screen instanceof net.minecraft.client.gui.screens.BackupConfirmScreen confirmation){
                for(var child:confirmation.children())if(child instanceof net.minecraft.client.gui.components.Button button&&button.active&&button.getMessage().equals(net.minecraft.client.gui.screens.BackupConfirmScreen.BACKUP_AND_JOIN)){
                    checks.add(Map.of("testWorldBackupConfirmed",true));button.onPress(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER,0,0));return;
                }
            }
            if(phase==8&&mc.level==null&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){mc.createWorldOpenFlows().openWorld("cable-world",()->finish(false,"Reload cancelled"));phase=9;return;}
            if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.player.tickCount<40)return;
            var server=mc.getSingleplayerServer();
            if(phase==1){server.submit(()->setup()).join();wait=ticks+100;phase=2;return;}
            if(ticks<wait)return;
            if(phase==2){server.submit(()->fill()).join();wait=ticks+100;phase=3;return;}
            if(phase==3){server.submit(()->inspect()).join();models();screenshot("cables-powered");phase=4;wait=ticks+30;return;}
            if(phase==4){server.submit(()->{var p=player();p.connection.teleport(1.5,-57,-3.5,0,48);}).join();phase=5;wait=ticks+60;return;}
            if(phase==5){screenshot("cable-rod-connection");phase=6;wait=ticks+30;return;}
            if(phase==6){server.submit(()->{var p=player();p.connection.teleport(-5.5,-57,-2.5,-80,45);}).join();phase=7;wait=ticks+60;return;}
            if(phase==7){screenshot("creative-cube");server.submit(()->server.saveEverything(false,true,true)).join();write("connection-checkpoints.json",checks);mc.disconnect(new TitleScreen(),false);phase=8;return;}
            if(phase==9){server.submit(()->{for(int i=0;i<CABLES.length;i++)((owmii.powah.block.energizing.EnergizingRodBlockEntity)player().level().getBlockEntity(new BlockPos(2,-58,i*3))).getEnergy().setStored(0);}).join();phase=10;wait=ticks+80;return;}
            if(phase==10){server.submit(()->{for(int i=0;i<CABLES.length;i++)check(stored(new BlockPos(2,-58,i*3))>0,"power resumes after native save/reload, tier "+i);}).join();finish(Boolean.getBoolean("mekextras.connections.baseline")||failures.isEmpty(),"");}
        }catch(Throwable error){error.printStackTrace();finish(false,error.toString());}
    }
    private ServerPlayer player(){var mc=Minecraft.getInstance();return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private void setup(){
        var p=player();var world=p.level();p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();
        for(int x=-14;x<=24;x++)for(int z=-4;z<=23;z++){
            world.setBlockAndUpdate(new BlockPos(x,-61,z),Blocks.STONE.defaultBlockState());
            world.setBlockAndUpdate(new BlockPos(x,-60,z),Blocks.GRASS_BLOCK.defaultBlockState());
            for(int y=-59;y<=-56;y++)world.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
        }
        var ids=BuiltInRegistries.BLOCK.keySet().stream().filter(id->id.getNamespace().equals("powah")&&(id.getPath().contains("cable")||id.getPath().contains("energizing_rod"))).map(Object::toString).sorted().toList();
        checks.add(Map.of("powahBlocks",ids));
        // Each row proves an independent complete path with real native block entities.
        for(int i=0;i<CABLES.length;i++){
            int z=i*3;placeSource(new BlockPos(-3,-59,z),true);
            for(int x=-2;x<=1;x++)set(new BlockPos(x,-59,z),CABLES[i]);
            set(new BlockPos(2,-59,z),CABLES[i]);
            set(new BlockPos(2,-58,z),"powah:energizing_rod_starter");
        }
        // Additional network joints, both directions through Powah cables.
        for(int i=0;i<3;i++){
            int z=i*5;placeSource(new BlockPos(8,-59,z),true);
            set(new BlockPos(9,-59,z),i==0?"mekanism_extras:absolute_universal_cable":"powah:energy_cable_nitro");
            set(new BlockPos(10,-59,z),i==0?"powah:energy_cable_nitro":"mekanism_extras:absolute_universal_cable");
            set(new BlockPos(11,-59,z),i==2?"mekanism:basic_universal_cable":"powah:energy_cable_nitro");
            set(new BlockPos(12,-59,z),"powah:energy_cable_nitro");
            set(new BlockPos(12,-58,z),"powah:energizing_rod_starter");
        }
        placeSource(new BlockPos(-7,-59,0),false);
        set(new BlockPos(-6,-59,0),"mekanism_extras:absolute_universal_cable");
        set(new BlockPos(-5,-59,0),"mekanism_extras:absolute_universal_cable");
        set(new BlockPos(-5,-58,0),"powah:energizing_rod_starter");
        for(int i=0;i<POWAH.length;i++)for(int reverse=0;reverse<2;reverse++){
            int x=17+reverse*5,z=i*3;
            placeSource(new BlockPos(x,-59,z),true);
            set(new BlockPos(x+1,-59,z),reverse==0?"mekanism_extras:absolute_universal_cable":"powah:energy_cable_"+POWAH[i]);
            set(new BlockPos(x+2,-59,z),reverse==0?"powah:energy_cable_"+POWAH[i]:"mekanism_extras:absolute_universal_cable");
            set(new BlockPos(x+2,-58,z),"powah:energizing_rod_starter");
        }
        // An old uncharged default remains repairable, while the explicitly selected energy void stays empty.
        var legacyItem=new ItemStack(MekanismBlocks.CREATIVE_ENERGY_CUBE.asItem());
        legacyItem.set(mekanism.common.registries.MekanismDataComponents.SIDE_CONFIG,ItemBlockEnergyCube.SIDE_CONFIG);
        legacyItem.set(mekanism.common.registries.MekanismDataComponents.ATTACHED_ENERGY,0L);
        placeCustom(new BlockPos(-11,-59,0),legacyItem);
        var legacy=(TileEntityEnergyCube)world.getBlockEntity(new BlockPos(-11,-59,0));legacy.energyContainer().setEnergy(0,null);
        placeCustom(new BlockPos(-11,-59,3),ItemBlockEnergyCube.withCreativeSideConfig(ItemBlockEnergyCube.ALL_INPUT).toStack());
        // Trench matching the report, including a corner and a vertical device connection.
        for(int x=-2;x<=3;x++){world.setBlockAndUpdate(new BlockPos(x,-59,-2),Blocks.GRASS_BLOCK.defaultBlockState());set(new BlockPos(x,-59,-1),"mekanism_extras:absolute_universal_cable");}
        set(new BlockPos(1,-58,-1),"powah:energizing_rod_starter");
        set(new BlockPos(3,-59,0),"mekanism_extras:absolute_universal_cable");
        p.connection.teleport(7.5,-54,-7.5,38,43);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
    }
    private void set(BlockPos pos,String id){
        var block=BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));require(block!=Blocks.AIR,"Missing block "+id);
        player().level().setBlockAndUpdate(pos,block.defaultBlockState());
    }
    private void placeSource(BlockPos pos,boolean filled){
        var stack=filled?ContainerType.ENERGY.getFilledVariant(ItemBlockEnergyCube.withCreativeSideConfig(ItemBlockEnergyCube.ALL_OUTPUT),null):new ItemStack(MekanismBlocks.CREATIVE_ENERGY_CUBE.asItem());
        placeCustom(pos,stack);
    }
    private void placeCustom(BlockPos pos,ItemStack stack){
        var p=player();
        p.level().setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var hit=new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0,.5,0),Direction.UP,pos.below(),false);
        ((BlockItem)stack.getItem()).place(new BlockPlaceContext(new UseOnContext(p,InteractionHand.MAIN_HAND,hit)));
        require(p.level().getBlockEntity(pos) instanceof TileEntityEnergyCube,"Native cube item placement failed "+pos);
    }
    private void fill(){
        var w=player().level();
        for(int x=-2;x<=3;x++){
            var tile=(TileEntityTransmitter)w.getBlockEntity(new BlockPos(x,-59,-1));
            var cable=(mekanism.common.content.network.transmitter.UniversalCable)tile.getTransmitter();
            cable.getContainer().setEnergy(cable.getContainer().getCapacityAsLong(),null);
        }
    }
    private void inspect(){
        var w=player().level();
        for(int i=0;i<CABLES.length;i++){
            int z=i*3;var cable=(TileEntityTransmitter)w.getBlockEntity(new BlockPos(0,-59,z));
            var t=cable.getTransmitter();long energy=stored(new BlockPos(2,-58,z));
            check(energy>0,"native cube -> "+CABLES[i]+" -> Powah rod, stored="+energy);
            checks.add(Map.of("cable",CABLES[i],"west",t.getConnectionType(Direction.WEST).name(),"east",t.getConnectionType(Direction.EAST).name(),"network",t.hasTransmitterNetwork(),"rodEnergy",energy));
        }
        for(int i=0;i<3;i++)check(stored(new BlockPos(12,-58,i*5))>0,"mixed Powah/Mekanism direction "+i+" output="+stored(new BlockPos(12,-58,i*5)));
        var defaultCube=(TileEntityEnergyCube)w.getBlockEntity(new BlockPos(-7,-59,0));
        checks.add(Map.of("defaultCreativeCubeStored",defaultCube.energyContainer().getAmountAsLong(),"capacity",defaultCube.energyContainer().getCapacityAsLong(),"rodEnergy",stored(new BlockPos(-5,-58,0))));
        check(stored(new BlockPos(-5,-58,0))>0,"default creative cube supplies without extra setup");
        for(int i=0;i<POWAH.length;i++)for(int reverse=0;reverse<2;reverse++)check(stored(new BlockPos(19+reverse*5,-58,i*3))>0,"Powah "+POWAH[i]+" mixed connection, reverse="+reverse);
        for(int a=0;a<CABLES.length;a++)for(int b=0;b<CABLES.length;b++){
            var first=(TileEntityTransmitter)w.getBlockEntity(new BlockPos(0,-59,a*3));var other=(TileEntityTransmitter)w.getBlockEntity(new BlockPos(0,-59,b*3));
            check(first.getTransmitter().isValidTransmitter(other,Direction.EAST),"tier compatibility "+a+" -> "+b);
        }
        var legacy=(TileEntityEnergyCube)w.getBlockEntity(new BlockPos(-11,-59,0));check(legacy.energyContainer().getAmountAsLong()==Long.MAX_VALUE,"existing empty output cube recovers supply mode");
        var sink=(TileEntityEnergyCube)w.getBlockEntity(new BlockPos(-11,-59,3));
        try(var tx=Transaction.openRoot()){check(sink.energyContainer().insert(12345,tx,mekanism.api.AutomationType.EXTERNAL)==12345,"explicit void accepts energy");tx.commit();}
        check(sink.energyContainer().isEmpty(),"explicit void never becomes an energy source");
        set(new BlockPos(-11,-59,6),"mekanism:basic_energy_cube");var ordinary=(TileEntityEnergyCube)w.getBlockEntity(new BlockPos(-11,-59,6));ordinary.energyContainer().setEnergy(1000,null);
        try(var tx=Transaction.openRoot()){check(ordinary.energyContainer().extract(100,tx,mekanism.api.AutomationType.EXTERNAL)==100,"ordinary cube extracts normally");tx.commit();}
        check(ordinary.energyContainer().getAmountAsLong()==900,"ordinary cube remains finite");
        var variants=new ArrayList<ItemStack>();MekanismBlocks.CREATIVE_ENERGY_CUBE.asItem().addItems(MekanismBlocks.CREATIVE_ENERGY_CUBE.getItemHolder(),variants::add);
        check(variants.size()==2&&variants.getFirst().getOrDefault(mekanism.common.registries.MekanismDataComponents.ATTACHED_ENERGY,0L)==Long.MAX_VALUE,"creative tab prioritizes infinite supply");
        check(!variants.getFirst().getHoverName().equals(variants.getLast().getHoverName()),"source and void item names are distinct");
        // Creative source never depletes, including committed extraction.
        var source=(TileEntityEnergyCube)w.getBlockEntity(new BlockPos(-3,-59,0));long before=source.energyContainer().getAmountAsLong();
        try(var tx=Transaction.openRoot()){check(source.energyContainer().extract(12345,tx,mekanism.api.AutomationType.EXTERNAL)==12345,"creative native extraction");tx.commit();}
        check(source.energyContainer().getAmountAsLong()==before&&before==Long.MAX_VALUE,"creative source remains infinite");
    }
    private long stored(BlockPos pos){var h=player().level().getCapability(Capabilities.ENERGY.block(),pos,Direction.DOWN);return h==null?-1:h.getAmountAsLong();}
    private void models(){
        var mc=Minecraft.getInstance();
        for(var pos:List.of(new BlockPos(0,-59,-1),new BlockPos(1,-59,-1),new BlockPos(3,-59,-1))){
            var state=mc.level.getBlockState(pos);var tile=(TileEntityTransmitter)mc.level.getBlockEntity(pos);
            var data=mc.level.getModelData(pos).get(TileEntityTransmitter.TRANSMITTER_PROPERTY);
            var parts=new ArrayList<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart>();
            mc.getModelManager().getBlockStateModelSet().get(state).collectParts(mc.level,pos,state,RandomSource.create(0),parts);
            int quads=0;for(var part:parts){for(Direction d:Direction.values())quads+=part.getQuads(d).size();quads+=part.getQuads(null).size();}
            var entry=new LinkedHashMap<String,Object>();entry.put("position",pos.toShortString());entry.put("parts",parts.size());entry.put("quads",quads);entry.put("modelData",data!=null);
            for(var side:Direction.values())entry.put(side.getSerializedName(),Map.of("connected",tile.getTransmitter().getConnectionType(side).name(),"model",data==null?"missing":data.getConnectionType(side).name()));
            checks.add(entry);
        }
    }
    private void check(boolean ok,String text){checks.add(Map.of("passed",ok,"check",text));if(!ok)failures.add(text);}
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    private void screenshot(String name){var mc=Minecraft.getInstance();Screenshot.takeScreenshot(mc.getMainRenderTarget(),shot->{try(shot){shot.writeToFile(directory.resolve(name+".png"));}catch(Exception e){finish(false,e.toString());}});}
    private void write(String name,Object data){try{Files.writeString(directory.resolve(name),new GsonBuilder().setPrettyPrinting().create().toJson(data));}catch(Exception e){throw new RuntimeException(e);}}
    private void finish(boolean passed,String error){if(finished)return;finished=true;write("connection-result.json",Map.of("passed",passed,"error",error,"checks",checks,"failures",failures));Minecraft.getInstance().stop();}
}
