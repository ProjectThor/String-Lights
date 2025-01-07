package net.projectthor.stringlights.networking.packet;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.Registries;
import net.projectthor.stringlights.StringLights;
import net.projectthor.stringlights.entity.LightKnotEntity;
import net.projectthor.stringlights.light.IncompleteLightLink;
import net.projectthor.stringlights.light.LightLink;
import net.projectthor.stringlights.util.Helper;

public record LightAttachPayload(int primaryEntityId, int secondaryEntityId, int lightTypeId,
                                 boolean attach) implements CustomPayload {
    public static final CustomPayload.Id<LightAttachPayload> PAYLOAD_ID = new CustomPayload.Id<>(Helper.identifier("s2c_light_attach_packet_id"));
    public static final PacketCodec<RegistryByteBuf, LightAttachPayload> PACKET_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.INTEGER, LightAttachPayload::primaryEntityId,
                    PacketCodecs.INTEGER, LightAttachPayload::secondaryEntityId,
                    PacketCodecs.INTEGER, LightAttachPayload::lightTypeId,
                    PacketCodecs.BOOL, LightAttachPayload::attach,
                    LightAttachPayload::new);

    public static final ObjectList<IncompleteLightLink> incompleteLinks = new ObjectArrayList<>(256);

    public LightAttachPayload(LightLink link, boolean attach) {
        this(link.getPrimary().getId(), link.getSecondary().getId(), Registries.ITEM.getRawId(link.getSourceItem()), attach);
    }

    public LightAttachPayload(PacketByteBuf buf) {
        this(buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean());
    }

    public static void encode(PacketByteBuf buf1, LightAttachPayload packet) {
        buf1.writeInt(packet.primaryEntityId());
        buf1.writeInt(packet.secondaryEntityId());
        buf1.writeInt(packet.lightTypeId());
        buf1.writeBoolean(packet.attach());
    }


    private void applyDetach(ClientPlayerEntity clientPlayerEntity, PacketSender packetSender) {
        ClientWorld world = clientPlayerEntity.clientWorld;
        Entity primary = world.getEntityById(primaryEntityId);

        if (!(primary instanceof LightKnotEntity primaryKnot)) {
            StringLights.LOGGER.warn(String.format("Tried to detach from %s (#%d) which is not a light knot",
                    primary, primaryEntityId
            ));
            return;
        }
        Entity secondary = world.getEntityById(secondaryEntityId);
        incompleteLinks.removeIf(link -> {
            if (link.primary == primaryKnot && link.secondaryId == secondaryEntityId) {
                link.destroy();
                return true;
            }
            return false;
        });

        if (secondary == null) {
            return;
        }

        for (LightLink link : primaryKnot.getLinks()) {
            if (link.getSecondary() == secondary) {
                link.destroy(true);
            }
        }
    }

    private void applyAttach(ClientPlayerEntity clientPlayerEntity, PacketSender packetSender) {
        ClientWorld world = clientPlayerEntity.clientWorld;
        Entity primary = world.getEntityById(primaryEntityId);

        if (!(primary instanceof LightKnotEntity primaryKnot)) {
            StringLights.LOGGER.warn(String.format("Tried to attach from %s (#%d) which is not a light knot",
                    primary, primaryEntityId
            ));
            return;
        }
        Entity secondary = world.getEntityById(secondaryEntityId);

        Item lightType = Registries.ITEM.get(lightTypeId);

        if (secondary == null) {
            incompleteLinks.add(new IncompleteLightLink(primaryKnot, secondaryEntityId, lightType));
        } else {
            LightLink.create(primaryKnot, secondary, lightType);
        }
    }

    @Override
    public Id<LightAttachPayload> getId() {
        return PAYLOAD_ID;
    }

    @Environment(EnvType.CLIENT)
    public void apply(ClientPlayNetworking.Context context) {
        if (attach){
            applyAttach(context.player(), context.responseSender());
            return;
        }
        applyDetach(context.player(), context.responseSender());
    }
}
