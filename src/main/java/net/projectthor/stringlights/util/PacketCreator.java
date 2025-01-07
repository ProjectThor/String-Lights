package net.projectthor.stringlights.util;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.annotation.Nullable;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientCommonPacketListener;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.projectthor.stringlights.entity.LightKnotEntity;
import net.projectthor.stringlights.light.LightLink;

import java.util.List;
import java.util.function.Function;

public class PacketCreator {
//    /**
//     * Creates a spawn packet for {@code entity} with additional data from {@code extraData}.
//     *
//     * @param entity    The entity to spawn
//     * @param packetID  The spawn packet id
//     * @param extraData Extra data supplier
//     * @return A S2C packet
//     */
//    public static Packet<ClientCommonPacketListener> createSpawn(Entity entity, Identifier packetID, Function<PacketByteBuf, PacketByteBuf> extraData) {
//       if (entity.getWorld().isClient)
//          throw new IllegalStateException("Called on the logical client!");
//       PacketByteBuf byteBuf = new PacketByteBuf(Unpooled.buffer());
//       byteBuf.writeVarInt(Registries.ENTITY_TYPE.getRawId(entity.getType()));
//       byteBuf.writeUuid(entity.getUuid());
//       byteBuf.writeVarInt(entity.getId());
//
  //      PacketBufUtil.writeVec3d(byteBuf, entity.getPos());
//        // pitch and yaw don't matter so don't send them
//        byteBuf = extraData.apply(byteBuf);
//
 //        return ServerPlayNetworking.createS2CPacket(packetID, byteBuf);
 //   }
//
//    /**
//     * Creates a multi attach packet for a knot
//     *
//     * @param knot the primary knot
//     * @return Packet or null if no data is to be sent
//     */
//    @Nullable
//    public static Packet<ClientPlayPacketListener> createMultiAttach(LightKnotEntity knot) {
//        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
//        List<LightLink> links = knot.getLinks();
//        IntList ids = new IntArrayList(links.size());
//        IntList types = new IntArrayList(links.size());
//        for (LightLink link : links) {
//            if (link.getPrimary() == knot) {
//                ids.add(link.getSecondary().getId());
//                types.add(Registries.ITEM.getRawId(link.getSourceItem()));
 //           }
//        }
//        if (!ids.isEmpty()) {
//            buf.writeInt(knot.getId());
//            buf.writeIntList(ids);
//            buf.writeIntList(types);
//
 //            return ServerPlayNetworking.createS2CPacket(NetworkingPackets.S2C_MULTI_LIGHT_ATTACH_PACKET_ID, buf);
//       }
//        return null;
//    }
}
