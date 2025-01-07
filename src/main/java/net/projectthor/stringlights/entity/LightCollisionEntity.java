package net.projectthor.stringlights.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.projectthor.stringlights.light.LightLink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LightCollisionEntity extends Entity implements LightLinkEntity {


    @Nullable
    private LightLink link;

    @NotNull
    private Item linkSourceItem;


    public LightCollisionEntity(World world, double x, double y, double z, @NotNull LightLink link) {
        this(ModEntityTypes.LIGHT_COLLISION, world);
        this.link = link;
        this.setPosition(x, y, z);
        this.linkSourceItem = link.sourceItem;
    }

    public LightCollisionEntity(EntityType<? extends LightCollisionEntity> entityType, World world) {
        super(entityType, world);
    }

    @SuppressWarnings("unused")
    public @Nullable LightLink getLink() {
        return link;
    }

    public @NotNull Item getLinkSourceItem() {
        return linkSourceItem;
    }

    @Override
    public boolean canHit() {
        return !isRemoved();
    }


    @Override
    public boolean isPushable() {
        return false;
    }


    @Environment(EnvType.CLIENT)
    @Override
    public boolean shouldRender(double distance) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && player.isHolding(itemStack -> itemStack.isIn(ConventionalItemTags.SHEAR_TOOLS))) {
            return super.shouldRender(distance);
        } else {
            return false;
        }
    }

    @Override
    public boolean isFireImmune() {
        return super.isFireImmune();
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound tag) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound tag) {
    }


    @Override
    public boolean isCollidable() {
        return true;
    }


    @Override
    public boolean handleAttack(Entity attacker) {
        if (attacker instanceof PlayerEntity playerEntity) {
            this.damage(this.getDamageSources().playerAttack(playerEntity), 0.0F);
        } else {
            playSound(getHitSound(), 0.5F, 1.0F);
        }
        return true;
    }


    @Override
    public boolean damage(DamageSource source, float amount) {
        ActionResult result = LightLinkEntity.onDamageFrom(this, source, getHitSound());

        if (result.isAccepted()) {
            destroyLinks(result == ActionResult.SUCCESS);
            return true;
        }
        return false;
    }

    @Override
    public void destroyLinks(boolean mayDrop) {
        if (link != null) link.destroy(mayDrop);
    }

    private SoundEvent getHitSound() {
        if (link != null) {
            return LightLink.getSoundGroup(link.sourceItem).getHitSound();
        } else {
            return LightLink.getSoundGroup(null).getHitSound();
        }
    }


    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (LightLinkEntity.canDestroyWith(player.getStackInHand(hand))) {
            destroyLinks(!player.isCreative());
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {

    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entityTrackerEntry) {
        int id = Registries.ITEM.getRawId(linkSourceItem);
        return new EntitySpawnS2CPacket(this, entityTrackerEntry, id);
    }

    @Override
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);
        int rawLightItemSourceId = packet.getEntityData();
        linkSourceItem = Registries.ITEM.get(rawLightItemSourceId);
    }

    @Override
    public void tick() {
        if (getWorld().isClient()) return;
        if (link != null && link.needsBeDestroyed()) link.destroy(true);

        if (link == null || link.isDead()) {
            remove(Entity.RemovalReason.DISCARDED);
        }
    }
}
