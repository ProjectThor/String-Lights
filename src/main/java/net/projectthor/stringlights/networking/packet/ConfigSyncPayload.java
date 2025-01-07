package net.projectthor.stringlights.networking.packet;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.projectthor.stringlights.StringLights;
import net.projectthor.stringlights.client.ClientInitializer;
import net.projectthor.stringlights.util.Helper;

public record ConfigSyncPayload(float lightHangAmount, int maxLightRange) implements CustomPayload {
    public static final Id<ConfigSyncPayload> PAYLOAD_ID = new CustomPayload.Id<>(Helper.identifier("s2c_config_sync_packet_id"));
    public static final PacketCodec<PacketByteBuf, ConfigSyncPayload> PACKET_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.FLOAT, ConfigSyncPayload::lightHangAmount,
                    PacketCodecs.INTEGER, ConfigSyncPayload::maxLightRange,
                    ConfigSyncPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return PAYLOAD_ID;
    }

    public void apply(ClientPlayNetworking.Context context) {
        MinecraftClient client = context.client();
        if (client.isInSingleplayer()) {
            return;
        }
        try {
            StringLights.LOGGER.info("Received {} config from server", StringLights.MOD_ID);
            StringLights.runtimeConfig.setLightHangAmount(this.lightHangAmount);
            StringLights.runtimeConfig.setMaxLightRange(this.maxLightRange);
        } catch (Exception e) {
            StringLights.LOGGER.error("Could not deserialize config: ", e);
        }
        ClientInitializer.getInstance().getLightKnotEntityRenderer().ifPresent(r -> r.getLightRenderer().purge());
    }
}
