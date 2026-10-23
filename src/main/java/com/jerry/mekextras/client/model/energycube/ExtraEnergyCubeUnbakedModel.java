package com.jerry.mekextras.client.model.energycube;

import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.neoforged.neoforge.client.model.AbstractUnbakedModel;
import net.neoforged.neoforge.client.model.StandardModelParameters;

// The unbaked model contains all the information read from the JSON.
// It provides the basic settings and geometry.
// Using AbstractUnbakedModel sets the Vanilla and NeoForge properties methods
public class ExtraEnergyCubeUnbakedModel extends AbstractUnbakedModel {

    private final ExtraEnergyCubeGeometry geometry;

    public ExtraEnergyCubeUnbakedModel(StandardModelParameters params, ExtraEnergyCubeGeometry geometry) {
        super(params);
        this.geometry = geometry;
    }

    @Override
    public UnbakedGeometry geometry() {
        // The geometry to used to construct the baked quads
        return this.geometry;
    }
}