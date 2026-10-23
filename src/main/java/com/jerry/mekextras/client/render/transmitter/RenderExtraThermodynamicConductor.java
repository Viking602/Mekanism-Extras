package com.jerry.mekextras.client.render.transmitter;

import mekanism.client.render.MekanismRenderer;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import mekanism.client.render.transmitter.TransmitterRenderState.ConductorRenderState;
import mekanism.common.base.ProfilerConstants;
import mekanism.common.content.network.transmitter.ThermodynamicConductor;
import com.jerry.mekextras.common.tile.transmitter.TileEntityExtraThermodynamicConductor;
import mekanism.common.util.HeatUtils;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderExtraThermodynamicConductor extends ExtraTransmitterRenderer<TileEntityExtraThermodynamicConductor, ConductorRenderState> {

    public RenderExtraThermodynamicConductor(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ConductorRenderState createRenderState() {
        return new ConductorRenderState();
    }

    @Override
    public void extractRenderState(TileEntityExtraThermodynamicConductor conductor, ConductorRenderState state, float partialTick, Vec3 cameraPosition,
          ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        super.extractRenderState(conductor, state, partialTick, cameraPosition, breakProgress);
        ThermodynamicConductor transmitter = conductor.getTransmitter();
        state.tempColor = HeatUtils.getColorFromTemp(transmitter.getTemperature(), transmitter.getBaseColor()).argb();
    }

    @Override
    public void submit(ConductorRenderState state, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState camera) {
        contents(state, poseStack, nodeCollector, camera, MekanismRenderer.getSinglePicker(MekanismRenderer.heatIcon), state.tempColor, LightCoordsUtil.FULL_BRIGHT, true, 1);
    }

    @Override
    protected String getProfilerSection() {
        return ProfilerConstants.THERMODYNAMIC_CONDUCTOR;
    }
}
