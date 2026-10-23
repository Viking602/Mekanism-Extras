package com.jerry.mekextras.client.render.transmitter;

import mekanism.client.render.MekanismRenderer;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import mekanism.client.render.transmitter.TransmitterRenderState.TubeRenderState;
import mekanism.common.base.ProfilerConstants;
import mekanism.common.content.network.ChemicalNetwork;
import mekanism.common.content.network.transmitter.PressurizedTube;
import com.jerry.mekextras.common.tile.transmitter.TileEntityExtraPressurizedTube;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderExtraPressurizedTube extends ExtraTransmitterRenderer<TileEntityExtraPressurizedTube, TubeRenderState> {

    public RenderExtraPressurizedTube(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public TubeRenderState createRenderState() {
        return new TubeRenderState();
    }

    @Override
    public void extractRenderState(TileEntityExtraPressurizedTube tube, TubeRenderState state, float partialTick, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        super.extractRenderState(tube, state, partialTick, cameraPosition, breakProgress);
        state.chemicalTexture = null;
        ChemicalNetwork network = tube.getTransmitter().getTransmitterNetwork();
        if (network == null || network.getLastType().isEmpty()) {//TODO - 26.1: Does this race condition still exist?
            return;//race conditions, yay
        }
        state.currentScale = Math.max(0.2F, network.currentScale);
        state.chemicalTexture = MekanismRenderer.getChemicalTexture(network.getLastType());
        //TODO - 26.1: Do we need to make this ARGB as technically alpha is currently empty
        state.chemicalTint = MekanismRenderer.getTint(network.getLastType().typeHolder());
    }

    @Override
    public void submit(TubeRenderState state, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState camera) {
        if (state.chemicalTexture != null) contents(state, poseStack, nodeCollector, camera, MekanismRenderer.getSinglePicker(state.chemicalTexture), ARGB.color(ARGB.as8BitChannel(state.currentScale), state.chemicalTint), LightCoordsUtil.FULL_BRIGHT, true, 1);
    }

    @Override
    protected String getProfilerSection() {
        return ProfilerConstants.PRESSURIZED_TUBE;
    }

    @Override
    protected boolean shouldRenderTransmitter(TileEntityExtraPressurizedTube tile, Vec3 camera) {
        if (super.shouldRenderTransmitter(tile, camera)) {
            PressurizedTube tube = tile.getTransmitter();
            if (tube.hasTransmitterNetwork()) {
                ChemicalNetwork network = tube.getTransmitterNetworkNN();
                return !network.getLastType().isEmpty() && !network.getContainer().isEmpty() && network.currentScale > 0;
            }
        }
        return false;
    }
}
