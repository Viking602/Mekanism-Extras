package com.jerry.genextras.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mekanism.api.text.EnumColor;
import mekanism.client.model.ModelEnergyCore;
import mekanism.client.render.tileentity.MultiblockTileEntityRenderer;
import mekanism.client.render.tileentity.RenderEnergyCube;
import com.jerry.genextras.client.render.RenderNaquadahReactor.NaquadahRenderState;
import mekanism.generators.common.GeneratorsProfilerConstants;
import com.jerry.genextras.common.content.naquadah.NaquadahReactorMultiblockData;
import com.jerry.genextras.common.tile.naquadah.TileEntityNaquadahReactorController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderNaquadahReactor extends MultiblockTileEntityRenderer<NaquadahReactorMultiblockData, TileEntityNaquadahReactorController, NaquadahRenderState> {

    private static final double SCALE = 100_000_000;

    private final ModelPart energyCore;

    public RenderNaquadahReactor(BlockEntityRendererProvider.Context context) {
        super(context);
        this.energyCore = context.bakeLayer(RenderEnergyCube.CORE_LAYER);
    }

    @Override
    public NaquadahRenderState createRenderState() {
        return new NaquadahRenderState();
    }

    @Override
    public void extractRenderState(TileEntityNaquadahReactorController controller, NaquadahRenderState state, float partialTick, Vec3 cameraPosition,
          ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        super.extractRenderState(controller, state, partialTick, cameraPosition, breakProgress);
        NaquadahReactorMultiblockData multiblock = controller.getMultiblock();
        state.scaledTemp = Math.round(multiblock.getLastPlasmaTemp() / SCALE);
        //TODO - 26.1: Is this what we should be using in BERs or should we use the game time?
        state.ticks = Minecraft.getInstance().levelRenderer.getTicks() + partialTick;
    }

    @Override
    public void submit(NaquadahRenderState state, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, -3.5, 0.5);
        float scale = 1 + 0.7F * sinDegrees(3.14F * state.scaledTemp + 135);
        renderPart(state, poseStack, nodeCollector, EnumColor.DARK_GREEN, scale, -6, -7, 0, 36);

        scale = 1 + 0.8F * sinDegrees(3 * state.scaledTemp);
        renderPart(state, poseStack, nodeCollector, EnumColor.YELLOW, scale, 4, 4, 0, 36);

        scale = 1 - 0.9F * sinDegrees(4 * state.scaledTemp + 90);
        renderPart(state, poseStack, nodeCollector, EnumColor.WHITE, scale, 5, -3, -35, 106);

        poseStack.popPose();
        //TODO - 26.1: We used to force end and draw the buffer, I am guessing that is no longer something that we want to be doing
        //endIfNeeded(renderer, ModelEnergyCore.RENDER_TYPE);
    }

    private static float sinDegrees(float degrees) {
        return Mth.sin((degrees % 360) * Mth.DEG_TO_RAD);
    }

    @Override
    protected String getProfilerSection() {
        return "naquadahReactor";
    }

    private void renderPart(NaquadahRenderState state, PoseStack poseStack, SubmitNodeCollector nodeCollector, EnumColor color, float scale, int mult1, int mult2,
          int shift1, int shift2) {
        poseStack.pushPose();
        poseStack.scale(2 * scale, 2 * scale, 2 * scale);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.ticks * mult1 + shift1));
        poseStack.mulPose(RenderEnergyCube.coreVec.rotationDegrees(state.ticks * mult2 + shift2));
        nodeCollector.submitModelPart(
              this.energyCore,
              poseStack,
              //TODO - 26.1: Figure out the render type
              ModelEnergyCore.RENDER_TYPE,
              //TODO - 26.1: Do we want to be using the state's light level instead?
              LightCoordsUtil.FULL_BRIGHT,
              OverlayTexture.NO_OVERLAY,
              null,//TODO - 26.1: Do we need to specify the texture or is doing so in the render type good enough?
              color.getPackedColor(),
              null//No break overlay for the core
        );
        poseStack.popPose();
    }

    @Override
    protected boolean shouldRender(TileEntityNaquadahReactorController tile, NaquadahReactorMultiblockData multiblock, Vec3 camera) {
        return multiblock.isBurning();
    }

    public static class NaquadahRenderState extends BlockEntityRenderState {

        public long scaledTemp;
        public float ticks;
    }
}