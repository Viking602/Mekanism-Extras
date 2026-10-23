package com.jerry.mekextras.client.render.transmitter;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.RenderResizableCuboid;
import mekanism.client.render.transmitter.RenderTransmitterBase;
import mekanism.client.render.transmitter.TransmitterRenderState;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import mekanism.common.lib.transmitter.ConnectionType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Render connection contents directly through the 26.1 submission API. */
public abstract class ExtraTransmitterRenderer<T extends TileEntityTransmitter, S extends TransmitterRenderState> extends RenderTransmitterBase<T,S> {
    protected ExtraTransmitterRenderer(BlockEntityRendererProvider.Context context) { super(context); }

    @Override
    public void extractRenderState(T tile, S state, float partialTick, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay overlay) {
        super.extractRenderState(tile,state,partialTick,camera,overlay);
        state.connectionContents = new ArrayList<>();
        for (Direction side: Direction.values()) {
            ConnectionType connection = tile.getTransmitter().getConnectionType(side);
            if (connection != ConnectionType.NONE) state.connectionContents.add(side.getSerializedName()+connection.name());
        }
    }

    protected void contents(S state, PoseStack poses, SubmitNodeCollector nodes, CameraRenderState camera,
            RenderResizableCuboid.TexturePicker texture, int tint, int light, boolean core, float fill) {
        if (texture == null || fill <= 0) return;
        if (core) {
            mekanism.client.render.transmitter.TransmitterContentsModel.submit(state, poses, nodes, texture, tint, light);
            return;
        }
        final float min=.27F, max=.73F, top=min+(max-min)*Math.min(1,fill);
        byte coreFaces = RenderResizableCuboid.SideRender.ALL_FACES;
        List<String> connections=state.connectionContents == null ? List.of() : state.connectionContents;
        for (Direction side:Direction.values()) {
            String connection=connections.stream().filter(s->s.startsWith(side.getSerializedName())).findFirst().orElse(null);
            if (connection==null) continue;
            coreFaces &= (byte)~RenderResizableCuboid.SideRender.of(side);
            float x1=min,y1=min,z1=min,x2=max,y2=top,z2=max;
            // Pull and push ports have a narrower internal neck.
            if (!connection.endsWith("NORMAL")) { x1=z1=.32F; x2=z2=.68F; y1=.32F; y2=.32F+.36F*Math.min(1,fill); }
            switch(side) {
                case DOWN -> { y1=0; y2=min; }
                case UP -> { y1=top; y2=1; }
                case NORTH -> { z1=0; z2=min; }
                case SOUTH -> { z1=max; z2=1; }
                case WEST -> { x1=0; x2=min; }
                case EAST -> { x1=max; x2=1; }
            }
            byte faces=(byte)(RenderResizableCuboid.SideRender.ALL_FACES ^ RenderResizableCuboid.SideRender.of(side) ^ RenderResizableCuboid.SideRender.of(side.getOpposite()));
            cube(state,poses,nodes,camera,texture,tint,light,faces,x1,y1,z1,x2,y2,z2);
        }
        if(core) cube(state,poses,nodes,camera,texture,tint,light,coreFaces,min,min,min,max,top,max);
    }
    private void cube(S state, PoseStack poses, SubmitNodeCollector nodes, CameraRenderState camera,
            RenderResizableCuboid.TexturePicker texture,int tint,int light,byte faces,float x1,float y1,float z1,float x2,float y2,float z2) {
        RenderResizableCuboid.renderCube(faces,x1,y1,z1,x2,y2,z2,poses,Sheets.translucentBlockSheet(),nodes,tint,light,
            OverlayTexture.NO_OVERLAY,RenderResizableCuboid.FaceDisplay.FRONT,camera.pos,Vec3.atLowerCornerOf(state.blockPos),texture);
    }
}
