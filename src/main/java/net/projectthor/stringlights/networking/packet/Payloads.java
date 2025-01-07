package net.projectthor.stringlights.networking.packet;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.projectthor.stringlights.StringLights;

public class Payloads {

    public static void init() {
        StringLights.LOGGER.info("Register Custom Payloads for Networking.");
        PayloadTypeRegistry.playS2C().register(LightAttachPayload.PAYLOAD_ID, LightAttachPayload.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(MultiLightAttachPayload.PAYLOAD_ID, MultiLightAttachPayload.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(KnotChangePayload.PAYLOAD_ID, KnotChangePayload.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(ConfigSyncPayload.PAYLOAD_ID, ConfigSyncPayload.PACKET_CODEC);
    }
}
