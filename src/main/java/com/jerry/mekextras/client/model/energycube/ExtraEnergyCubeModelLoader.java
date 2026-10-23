package com.jerry.mekextras.client.model.energycube;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.jerry.mekextras.MekanismExtras;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.model.StandardModelParameters;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;

public class ExtraEnergyCubeModelLoader implements UnbakedModelLoader<ExtraEnergyCubeUnbakedModel>, ResourceManagerReloadListener {
    public static final ExtraEnergyCubeModelLoader INSTANCE = new ExtraEnergyCubeModelLoader();
    public static final Identifier ID = MekanismExtras.rl("energy_cube");

    private ExtraEnergyCubeModelLoader() {}

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        // Handle any cache clearing logic
    }

    @Override
    public ExtraEnergyCubeUnbakedModel read(JsonObject obj, JsonDeserializationContext context) throws JsonParseException {
        // Use the given JsonObject and, if needed, the JsonDeserializationContext to get properties from the model JSON.
        // The MyUnbakedModel constructor may have constructor parameters (see below).

        // Read the data used to create the quads
        ExtraEnergyCubeGeometry geometry = ExtraEnergyCubeGeometry.parse(obj, context);

        // For the basic parameters provided by vanilla and NeoForge, you can use the StandardModelParameters
        StandardModelParameters params = StandardModelParameters.parse(obj, context);

        return new ExtraEnergyCubeUnbakedModel(params, geometry);
    }
}