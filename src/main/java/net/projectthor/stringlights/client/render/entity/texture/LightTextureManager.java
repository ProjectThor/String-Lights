package net.projectthor.stringlights.client.render.entity.texture;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.profiler.Profiler;
import net.projectthor.stringlights.StringLights;
import net.projectthor.stringlights.client.ClientInitializer;
import net.projectthor.stringlights.util.Helper;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;


public class LightTextureManager implements SimpleResourceReloadListener<Map<Identifier, JsonElement>> {
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String MODEL_FILE_LOCATION = "models/entity/" + StringLights.MOD_ID;
    private static final int EXPECTED_UNIQUE_LIGHT_COUNT = 64;
    private final Object2ObjectMap<Identifier, Identifier> lightTextures = new Object2ObjectOpenHashMap<>(EXPECTED_UNIQUE_LIGHT_COUNT);
    private final Object2ObjectMap<Identifier, Identifier> knotTextures = new Object2ObjectOpenHashMap<>(EXPECTED_UNIQUE_LIGHT_COUNT);

    @Override
    public Identifier getFabricId() {
        return Helper.identifier("light_models");
    }

    @Override
    public CompletableFuture<Map<Identifier, JsonElement>> load(ResourceManager manager, Profiler profiler, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            HashMap<Identifier, JsonElement> map = new HashMap<>();
            JsonDataLoader.load(manager, MODEL_FILE_LOCATION, GSON, map);
            return map;
        });
    }

    @Override
    public CompletableFuture<Void> apply(Map<Identifier, JsonElement> data, ResourceManager manager, Profiler profiler, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            clearCache();
            data.forEach((identifier, jsonElement) -> {
                Pair<Identifier, Identifier> textures = extractLightTextures(identifier, jsonElement);
                lightTextures.put(identifier, textures.getLeft());
                knotTextures.put(identifier, textures.getRight());
            });

            return null;
        });
    }

    private static Pair<Identifier, Identifier> extractLightTextures(Identifier itemId, JsonElement jsonElement) {
        //Default
        Identifier lightTextureId = defaultLightTextureId(itemId);
        Identifier knotTextureId = defaultKnotTextureId(itemId);

        if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonObject texturesObject = jsonObject.getAsJsonObject("textures");

            if (texturesObject.has("light") && texturesObject.get("light").isJsonPrimitive()) {
                lightTextureId = Identifier.tryParse(texturesObject.get("light").getAsString()+ ".png");
            }
            if (texturesObject.has("knot") && texturesObject.get("knot").isJsonPrimitive()) {
                knotTextureId = Identifier.tryParse(texturesObject.get("knot").getAsString()+ ".png");
            }
        }

        return new Pair<>(lightTextureId, knotTextureId);
    }

    public void clearCache() {
        ClientInitializer.getInstance()
                .getLightKnotEntityRenderer()
                .ifPresent(it -> it.getLightRenderer().purge());
        lightTextures.clear();
        knotTextures.clear();

    }

    private static @NotNull Identifier defaultLightTextureId(Identifier itemId) {
        return Identifier.of(itemId.getNamespace(), "textures/block/%s.png".formatted(itemId.getPath()));
    }
    private static @NotNull Identifier defaultKnotTextureId(Identifier itemId) {
        return Identifier.of(itemId.getNamespace(), "textures/item/%s.png".formatted(itemId.getPath()));
    }

    public Identifier getLightTexture(Identifier sourceItemId) {
        return lightTextures.computeIfAbsent(sourceItemId, (Identifier id) -> {
            // Default location.
            StringLights.LOGGER.warn("Did not find a model file for the light '%s', assuming default path.".formatted(sourceItemId));
            return defaultLightTextureId(id);
        });
    }

    public Identifier getKnotTexture(Identifier sourceItemId) {
        return knotTextures.computeIfAbsent(sourceItemId, (Identifier id) -> {
            // Default location.
            StringLights.LOGGER.warn("Did not find a model file for the knot '%s', assuming default path.".formatted(sourceItemId));
            return defaultKnotTextureId(id);
        });
    }
}
