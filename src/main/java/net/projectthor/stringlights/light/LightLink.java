package net.projectthor.stringlights.light;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.LeadItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.projectthor.stringlights.entity.LightKnotEntity;
import net.projectthor.stringlights.networking.packet.LightAttachPayload;
import net.projectthor.stringlights.util.Helper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class LightLink {

    @NotNull
    private final LightKnotEntity primary;

    @NotNull
    private final Entity secondary;

    @NotNull
    public final Item sourceItem;

    public boolean removeSilently = false;

    private boolean alive = true;

    private LightLink(@NotNull LightKnotEntity primary, @NotNull Entity secondary, @NotNull Item sourceItem) {
        if (primary.equals(secondary))
            throw new IllegalStateException("Tried to create a link between a knot and itself");
        this.primary = Objects.requireNonNull(primary);
        this.secondary = Objects.requireNonNull(secondary);
        this.sourceItem = Objects.requireNonNull(sourceItem);
    }

    @Nullable
    public static LightLink create(@NotNull LightKnotEntity primary, @NotNull Entity secondary, @NotNull Item sourceItem) {
        LightLink link = new LightLink(primary, secondary, sourceItem);
        if (primary.getLinks().contains(link)) return null;

        primary.addLink(link);
        if (secondary instanceof LightKnotEntity secondaryKnot) {
            secondaryKnot.addLink(link);
        }
        if (!primary.getWorld().isClient()) {
            link.sendAttachLightPacket(primary.getWorld());
        }
        return link;
    }

    private void sendAttachLightPacket(World world) {
        assert world instanceof ServerWorld;

        Set<ServerPlayerEntity> trackingPlayers = getTrackingPlayers(world);
        for (ServerPlayerEntity player : trackingPlayers) {
            ServerPlayNetworking.send(player, new LightAttachPayload(this, true));
        }
    }

    private Set<ServerPlayerEntity> getTrackingPlayers(World world) {
        assert world instanceof ServerWorld;
        Set<ServerPlayerEntity> trackingPlayers = new HashSet<>(
                PlayerLookup.around((ServerWorld) world, getPrimary().getBlockPos(), LightKnotEntity.VISIBLE_RANGE));
        trackingPlayers.addAll(
                PlayerLookup.around((ServerWorld) world, getSecondary().getBlockPos(), LightKnotEntity.VISIBLE_RANGE));
        return trackingPlayers;
    }

    public boolean isDead() {
        return !alive;
    }

    public double getSquaredDistance() {
        return this.getPrimary().squaredDistanceTo(getSecondary());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LightLink link = (LightLink) o;

        boolean partnersEqual = getPrimary().equals(link.getPrimary()) && getSecondary().equals(link.getSecondary()) ||
                getPrimary().equals(link.getSecondary()) && getSecondary().equals(link.getPrimary());
        return alive == link.alive && partnersEqual;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPrimary(), getSecondary(), alive);
    }

    public boolean needsBeDestroyed() {
        return getPrimary().isRemoved() || getSecondary().isRemoved();
    }

    public void destroy(boolean mayDrop) {
        if (!alive) return;

        boolean drop = mayDrop;
        World world = getPrimary().getWorld();
        this.alive = false;

        if (world.isClient) {
            return;
        }

        if (getSecondary() instanceof PlayerEntity player && player.isCreative()) drop = false;
        if (!world.getGameRules().getBoolean(GameRules.DO_TILE_DROPS)) drop = false;

        if (drop) {
            ItemStack stack = new ItemStack(getSourceItem());
            if (getSecondary() instanceof PlayerEntity player) {
                player.giveItemStack(stack);
            } else {
                Vec3d middle = Helper.middleOf(getPrimary().getPos(), getSecondary().getPos());
                ItemEntity itemEntity = new ItemEntity(world, middle.x, middle.y, middle.z, stack);
                itemEntity.setToDefaultPickupDelay();
                world.spawnEntity(itemEntity);
            }
        }

        {
            sendDetachLightPacket(world);
        }
    }

    private void sendDetachLightPacket(World world) {
        assert world instanceof ServerWorld;

        Set<ServerPlayerEntity> trackingPlayers = getTrackingPlayers(world);

        for (ServerPlayerEntity player : trackingPlayers) {
            ServerPlayNetworking.send(player, new LightAttachPayload(this, false));
        }
    }

    public static BlockSoundGroup getSoundGroup(@Nullable Item sourceItem) {
        if (sourceItem instanceof BlockItem blockItem) {
            return blockItem.getBlock().getDefaultState().getSoundGroup();
        }
        if (sourceItem instanceof LeadItem) {
            return new BlockSoundGroup(1.0f,
                    1.0f,
                    SoundEvents.ENTITY_LEASH_KNOT_BREAK,
                    BlockSoundGroup.WOOL.getStepSound(),
                    SoundEvents.ENTITY_LEASH_KNOT_PLACE,
                    BlockSoundGroup.WOOL.getHitSound(),
                    BlockSoundGroup.WOOL.getFallSound()
            );
        }
        return BlockSoundGroup.CHAIN;
    }

    @NotNull
    public LightKnotEntity getPrimary() {
        return primary;
    }

    @NotNull
    public Entity getSecondary() {
        return secondary;
    }

    @NotNull
    public Item getSourceItem() {
        return sourceItem;
    }
}
