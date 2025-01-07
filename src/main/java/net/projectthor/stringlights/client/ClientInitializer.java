package net.projectthor.stringlights.client;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.client.player.ClientPickBlockGatherCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.projectthor.stringlights.StringLights;
import net.projectthor.stringlights.client.render.entity.LightCollisionEntityRenderer;
import net.projectthor.stringlights.client.render.entity.LightKnotEntityRenderer;
import net.projectthor.stringlights.client.render.entity.model.LightKnotEntityModel;
import net.projectthor.stringlights.client.render.entity.texture.LightTextureManager;
import net.projectthor.stringlights.config.ModConfig;
import net.projectthor.stringlights.entity.LightKnotEntity;
import net.projectthor.stringlights.entity.ModEntityTypes;
import net.projectthor.stringlights.item.custom.StringLightsItem;
import net.projectthor.stringlights.networking.packet.ConfigSyncPayload;
import net.projectthor.stringlights.util.Helper;

import java.util.Optional;

@Environment(EnvType.CLIENT)
public class ClientInitializer implements ClientModInitializer {

    public static final EntityModelLayer LIGHT_KNOT = new EntityModelLayer(Helper.identifier("light_knot"), "main");
    private static ClientInitializer instance;
    private final LightTextureManager lightTextureManager = new LightTextureManager();
    private LightKnotEntityRenderer lightKnotEntityRenderer;
    private LightPacketHandler lightPacketHandler;


    @Override
    public void onInitializeClient() {
        instance = this;
        initRenderers();

        registerNetworkEventHandlers();
        registerClientEventHandlers();

        registerConfigSync();

        ItemTooltipCallback.EVENT.register(StringLightsItem::infoToolTip);


    }

    private static void registerConfigSync() {
        ConfigHolder<ModConfig> configHolder = AutoConfig.getConfigHolder(ModConfig.class);
        configHolder.registerSaveListener((holder, modConfig) -> {
            ClientInitializer clientInitializer = ClientInitializer.getInstance();

            if (clientInitializer != null) {
                clientInitializer.getLightKnotEntityRenderer().ifPresent(renderer -> renderer.getLightRenderer().purge());
            }
            MinecraftServer server = MinecraftClient.getInstance().getServer();
            if (server != null) {
                StringLights.LOGGER.info("Syncing config to clients");
                StringLights.fileConfig.syncToClients(server);
                StringLights.runtimeConfig.copyFrom(StringLights.fileConfig);
            }
            return ActionResult.PASS;
        });
    }

    private void initRenderers() {
        StringLights.LOGGER.info("Initializing Renderers.");
        EntityRendererRegistry.register(ModEntityTypes.LIGHT_KNOT, ctx -> {
            lightKnotEntityRenderer = new LightKnotEntityRenderer(ctx);
            return lightKnotEntityRenderer;
        });
        EntityRendererRegistry.register(ModEntityTypes.LIGHT_COLLISION, LightCollisionEntityRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(LIGHT_KNOT, LightKnotEntityModel::getTexturedModelData);
    }

    private void registerNetworkEventHandlers() {
        lightPacketHandler = new LightPacketHandler();

        ClientPlayConnectionEvents.INIT.register((handler, client) -> {
            StringLights.runtimeConfig.copyFrom(StringLights.fileConfig);
            getLightKnotEntityRenderer().ifPresent(r -> r.getLightRenderer().purge());
        });

        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.PAYLOAD_ID, ConfigSyncPayload::apply);
    }

    private void registerClientEventHandlers() {
        ClientPickBlockGatherCallback.EVENT.register((player, result) -> {
            if (result instanceof EntityHitResult) {
                Entity entity = ((EntityHitResult) result).getEntity();
                if (entity instanceof LightKnotEntity knot) {
                    return new ItemStack(knot.getLightItemSource());
                }
            }
            return ItemStack.EMPTY;
        });

        ClientTickEvents.START_WORLD_TICK.register(world -> lightPacketHandler.tick());
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(lightTextureManager);
    }

    public static ClientInitializer getInstance() {
        return instance;
    }

    public Optional<LightKnotEntityRenderer> getLightKnotEntityRenderer() {
        return Optional.ofNullable(lightKnotEntityRenderer);
    }

    public LightTextureManager getLightTextureManager() {
        return lightTextureManager;
    }
}
