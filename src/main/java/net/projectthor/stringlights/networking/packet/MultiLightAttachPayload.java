package net.projectthor.stringlights.networking.packet;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.projectthor.stringlights.util.Helper;

import java.util.List;

public record MultiLightAttachPayload(List<LightAttachPayload> packets) implements CustomPayload {
    public static final CustomPayload.Id<MultiLightAttachPayload> PAYLOAD_ID = new CustomPayload.Id<>(Helper.identifier("s2c_multi_light_attach_packet_id"));
    public static final PacketCodec<RegistryByteBuf, MultiLightAttachPayload> PACKET_CODEC = PacketCodec.of(MultiLightAttachPayload::encode, MultiLightAttachPayload::decode);

    private static MultiLightAttachPayload decode(RegistryByteBuf buf) {
        return new MultiLightAttachPayload(buf.readList(LightAttachPayload::new));
    }

    private static void encode(MultiLightAttachPayload packet, RegistryByteBuf buf) {
        buf.writeCollection(packet.packets, (LightAttachPayload::encode));
    }

    @Environment(EnvType.CLIENT)
    public void apply(ClientPlayNetworking.Context context) {
        this.packets.forEach(packet -> packet.apply(context));
    }

    @Override
    public Id<MultiLightAttachPayload> getId() {
        return PAYLOAD_ID;
    }
}