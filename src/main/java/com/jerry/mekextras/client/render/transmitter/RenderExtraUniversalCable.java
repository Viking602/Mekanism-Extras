package com.jerry.mekextras.client.render.transmitter;

import mekanism.client.render.MekanismRenderer;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import mekanism.client.render.transmitter.TransmitterRenderState.CableRenderState;
import mekanism.common.base.ProfilerConstants;
import mekanism.common.content.network.EnergyNetwork;
import mekanism.common.content.network.transmitter.UniversalCable;
import com.jerry.mekextras.common.tile.transmitter.TileEntityExtraUniversalCable;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderExtraUniversalCable extends ExtraTransmitterRenderer<TileEntityExtraUniversalCable, CableRenderState> {

    public RenderExtraUniversalCable(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public CableRenderState createRenderState() {
        return new CableRenderState();
    }

    @Override
    public void extractRenderState(TileEntityExtraUniversalCable cable, CableRenderState state, float partialTick, Vec3 cameraPosition,
          ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        super.extractRenderState(cable, state, partialTick, cameraPosition, breakProgress);
        state.currentScale = 0;
        EnergyNetwork network = cable.getTransmitter().getTransmitterNetwork();
        if (network == null) {//TODO - 26.1: Does this race condition still exist?
            return;//race conditions, yay
        }
        state.currentScale = network.currentScale;
    }

    @Override
    public void submit(CableRenderState state, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState camera) {
        if (state.currentScale > 0) contents(state, poseStack, nodeCollector, camera, MekanismRenderer.getSinglePicker(MekanismRenderer.energyIcon), ARGB.color(ARGB.as8BitChannel(state.currentScale), 255,255,255), LightCoordsUtil.FULL_BRIGHT, true, 1);
    }

    @Override
    protected String getProfilerSection() {
        return ProfilerConstants.UNIVERSAL_CABLE;
    }

    @Override
    protected boolean shouldRenderTransmitter(TileEntityExtraUniversalCable tile, Vec3 camera) {
        if (super.shouldRenderTransmitter(tile, camera)) {
            UniversalCable cable = tile.getTransmitter();
            if (cable.hasTransmitterNetwork()) {
                EnergyNetwork network = cable.getTransmitterNetworkNN();
                //Note: We don't check if the network is empty as we don't actually ever sync the energy value to the client
                return network.currentScale > 0;
            }
        }
        return false;
    }
}
