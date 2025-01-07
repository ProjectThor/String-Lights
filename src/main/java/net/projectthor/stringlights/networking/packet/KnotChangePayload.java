package net.projectthor.stringlights.networking.packet;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.projectthor.stringlights.StringLights;
import net.projectthor.stringlights.entity.LightKnotEntity;
import net.projectthor.stringlights.util.Helper;

public record KnotChangePayload(int knotId, Item sourceItem) implements CustomPayload {
    public static final Id<KnotChangePayload> PAYLOAD_ID = new CustomPayload.Id<>(Helper.identifier("s2c_knot_change_type_packet_id"));
    public static final PacketCodec<RegistryByteBuf, KnotChangePayload> PACKET_CODEC =
            PacketCodec.of((value, buf) -> {
                buf.writeVarInt(value.knotId);
                buf.writeVarInt(Item.getRawId(value.sourceItem));
            }, buf -> new KnotChangePayload(buf.readVarInt(), Item.byRawId(buf.readVarInt())));


    @Override
    public Id<KnotChangePayload> getId() {
        return PAYLOAD_ID;
    }

    public void apply(ClientPlayNetworking.Context context) {
        MinecraftClient client = context.client();
        client.execute(() -> {
            if (client.world == null) return;
            Entity entity = client.world.getEntityById(knotId);
            if (entity instanceof LightKnotEntity knot) {
                knot.updateLightType(sourceItem);
            } else {
                logBadActionTarget(entity, knotId);
            }
        });
    }

    private static void logBadActionTarget(Entity target, int targetId) {
        StringLights.LOGGER.warn(String.format("Tried to %s %s (#%d) which is not %s",
                "change type of", target, targetId, "light knot"
        ));
    }
}