package net.projectthor.stringlights.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.projectthor.stringlights.StringLights;
import net.projectthor.stringlights.networking.packet.ConfigSyncPayload;

@Config(name = StringLights.MOD_ID)
public class ModConfig implements ConfigData {
    @SuppressWarnings("UnnecessaryModifier")
    @ConfigEntry.Gui.Excluded
    private static final transient boolean IS_DEBUG_ENV = FabricLoader.getInstance().isDevelopmentEnvironment();

    @ConfigEntry.Gui.Tooltip(count = 3)
    private float lightHangAmount = 8.0F;
    @ConfigEntry.BoundedDiscrete(max = 32)
    @ConfigEntry.Gui.Tooltip(count = 2)
    private int maxLightRange = 16;
    @ConfigEntry.BoundedDiscrete(min = 1, max = 8)
    @ConfigEntry.Gui.Tooltip()
    private int quality = 4;

    @ConfigEntry.Gui.Tooltip()
    private boolean showToolTip = true;
    public float getLightHangAmount() {
        return lightHangAmount;
    }

    @SuppressWarnings("unused")
    public void setLightHangAmount(float lightHangAmount) {
        this.lightHangAmount = lightHangAmount;
    }

    public int getMaxLightRange() {
        return maxLightRange;
    }

    @SuppressWarnings("unused")
    public void setMaxLightRange(int maxLightRange) {
        this.maxLightRange = maxLightRange;
    }

    public int getQuality() {
        return quality;
    }

    @SuppressWarnings("unused")
    public void setQuality(int quality) {
        this.quality = quality;
    }

    public boolean doDebugDraw() {
        return IS_DEBUG_ENV && MinecraftClient.getInstance().getDebugHud().shouldShowDebugHud();
    }

    public void syncToClients(MinecraftServer server) {
        for (ServerPlayerEntity player : PlayerLookup.all(server)) {
            syncToClient(player);
        }
    }

    public void syncToClient(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new ConfigSyncPayload(lightHangAmount, maxLightRange));
    }

    public ModConfig copyFrom(ModConfig config) {
        this.lightHangAmount = config.lightHangAmount;
        this.maxLightRange = config.maxLightRange;
        this.quality = config.quality;
        this.showToolTip = config.showToolTip;
        return this;
    }

    public boolean doShowToolTip() {
        return showToolTip;
    }

}
