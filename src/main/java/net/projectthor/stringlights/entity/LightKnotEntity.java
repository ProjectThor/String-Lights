package net.projectthor.stringlights.entity;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.decoration.BlockAttachedEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.projectthor.stringlights.StringLights;
import net.projectthor.stringlights.light.LightLink;
import net.projectthor.stringlights.networking.packet.KnotChangePayload;
import net.projectthor.stringlights.networking.packet.LightAttachPayload;
import net.projectthor.stringlights.networking.packet.MultiLightAttachPayload;
import net.projectthor.stringlights.tag.ModTags;
import net.projectthor.stringlights.util.PacketCreator;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class LightKnotEntity extends BlockAttachedEntity implements LightLinkEntity {

    public static final double VISIBLE_RANGE = 2048.00;

    private static final byte GRACE_PERIOD = 100;

    private final ObjectList<LightLink> links = new ObjectArrayList<>();

    private final ObjectList<NbtElement> incompleteLinks = new ObjectArrayList<>();
    private final static String SOURCE_ITEM_KEY = "SourceItem";

    private int obstructionCheckTimer = 0;

    private Item lightItemSource = Items.CHAIN;

    private byte graceTicks = GRACE_PERIOD;

    @Environment(EnvType.CLIENT)
    private BlockState attachTarget;

    public LightKnotEntity(EntityType<? extends LightKnotEntity> entityType, World world) {
        super(entityType, world);
    }

    public LightKnotEntity(World world, BlockPos pos, Item source) {
        super(ModEntityTypes.LIGHT_KNOT, world, pos);
        setPosition((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D);
        this.lightItemSource = source;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
    }

    @Override
    public void setPosition(double x, double y, double z) {
        super.setPosition((double) MathHelper.floor(x) + 0.5D, (double) MathHelper.floor(y) + 0.5D,(double) MathHelper.floor(z) + 0.5D);
    }

    public Item getLightItemSource() {
        return lightItemSource;
    }

    public void setLightItemSource(Item lightItemSource) {
        this.lightItemSource = lightItemSource;
    }

    public void setGraceTicks(byte graceTicks) {
        this.graceTicks = graceTicks;
    }

    protected void updateAttachmentPosition() {
        setPos(attachedBlockPos.getX() + 0.50, attachedBlockPos.getY() + 0.50, attachedBlockPos.getZ() + 0.50);
        double w = getType().getWidth() / 2.0;
        double h = getType().getHeight();
        setBoundingBox(new Box(getX() - w, getY(), getZ() - w, getX() + w, getY() + h, getZ() + w ));
    }

    @Override
    public void tick() {
        if (getWorld().isClient()) {
            links.removeIf(LightLink::isDead);
            attachTarget = getWorld().getBlockState(attachedBlockPos);
            return;
        }
        attemptTickInVoid();

        boolean anyConverted = convertIncompleteLinks();
        updateLinks();
        removeDeadLinks();

        if (graceTicks < 0 || (anyConverted && incompleteLinks.isEmpty())) {
            graceTicks = 0;
        } else if (graceTicks > 0) {
            graceTicks--;
        }
    }

    private boolean convertIncompleteLinks() {
        if (!incompleteLinks.isEmpty()) {
            return incompleteLinks.removeIf(this::deserializeLightTag);
        }
        return false;
    }

    private void updateLinks() {
        double squaredMaxRange = getMaxRange() * getMaxRange();
        for (LightLink link : links) {
            if (link.isDead()) continue;

            if (!isAlive()) {
                link.destroy(true);
            } else if (link.getPrimary() == this && link.getSquaredDistance() > squaredMaxRange) {
                link.destroy(true);
            }
        }

        if (obstructionCheckTimer++ == 100) {
            obstructionCheckTimer = 0;
            if (!canStayAttached()) {
                destroyLinks(true);
            }
        }
    }

    private void removeDeadLinks() {
        boolean playBreakSound = false;
        for (LightLink link : links) {
            if (link.needsBeDestroyed()) link.destroy(true);
            if (link.isDead() && !link.removeSilently) playBreakSound = true;
        }
        if (playBreakSound) onBreak(null);

        links.removeIf(LightLink::isDead);
        if (links.isEmpty() && incompleteLinks.isEmpty() && graceTicks <= 0) {
            remove(RemovalReason.DISCARDED);
        }
    }

    private boolean deserializeLightTag(NbtElement element) {
        if (element == null || getWorld().isClient()) {
            return true;
        }

        assert element instanceof NbtCompound;
        NbtCompound tag = (NbtCompound) element;

        Item source = Registries.ITEM.get(Identifier.tryParse(tag.getString(SOURCE_ITEM_KEY)));

        if (tag.contains("UUID")) {
            UUID uuid = tag.getUuid("UUID");
            Entity entity = ((ServerWorld) getWorld()).getEntity(uuid);
            if (entity != null) {
                LightLink.create(this, entity, source);
                return true;
            }
        } else if (tag.contains("RelX") || tag.contains("RelY") || tag.contains("RelZ")) {
            BlockPos blockPos = new BlockPos(tag.getInt("RelX"), tag.getInt("RelY"), tag.getInt("RelZ"));
            blockPos = getBlockPosAsFacingRelative(blockPos, Direction.fromRotation(this.getYaw()));
            LightKnotEntity entity = LightKnotEntity.getKnotAt(getWorld(), blockPos.add(attachedBlockPos));
            if (entity != null) {
                LightLink.create(this, entity, source);
                return true;
            }
        } else {
            StringLights.LOGGER.warn("Chain knot NBT is missing UUID or relative position.");
        }

        if (graceTicks <= 0) {
            dropItem(source);
            onBreak(null);
            return true;
        }

        return false;
    }

    public static double getMaxRange() {
        return StringLights.runtimeConfig.getMaxLightRange();
    }

    public boolean canStayAttached() {
        BlockState blockState = getWorld().getBlockState(attachedBlockPos);
        return canAttachTo(blockState);
    }

    @Override
    public void destroyLinks(boolean mayDrop) {
        for (LightLink link : links) {
            link.destroy(mayDrop);
        }
        graceTicks = 0;
    }

    @Override
    public void onBreak(@Nullable Entity entity) {
        playSound(getSoundGroup().getBreakSound(), 1.0f, 1.0f);
    }

    private BlockPos getBlockPosAsFacingRelative(BlockPos relPos, Direction facing) {
        BlockRotation rotation = BlockRotation.values()[facing.getHorizontal()];
        return relPos.rotate(rotation);
    }

    @Nullable
    public static LightKnotEntity getKnotAt(World world, BlockPos pos) {
        List<LightKnotEntity> results = world.getNonSpectatingEntities(LightKnotEntity.class,
                Box.of(Vec3d.of(pos), 2, 2, 2));

        for (LightKnotEntity current : results) {
            if (current.getAttachedBlockPos().equals(pos)) {
                return current;
            }
        }

        return null;
    }

    public static boolean canAttachTo(BlockState blockState) {
        if (blockState != null) {
            return blockState.isIn(ModTags.LIGHT_CONNECTIBLE);
        }
        return false;
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player,
                new MultiLightAttachPayload(
                        this.getLinks()
                                .stream()
                                .filter(lightLink -> lightLink.getPrimary().getId() == this.getId())
                                .map(lightLink -> new LightAttachPayload(lightLink, true))
                                .toList()));
    }

    @Override
    public float applyMirror(BlockMirror mirror) {
        if (mirror != BlockMirror.NONE) {
            for (NbtElement element : incompleteLinks) {
                if (element instanceof NbtCompound link) {
                    if (link.contains("RelX")) {
                        link.putInt("RelX", -link.getInt("RelX"));
                    }
                }
            }
        }

        float yaw = MathHelper.wrapDegrees(this.getYaw());
        return switch (mirror) {
            case LEFT_RIGHT -> 180 - yaw;
            case FRONT_BACK -> -yaw;
            default -> yaw;
        };
    }

    @Override
    public boolean handleAttack(Entity attacker) {
        if (attacker instanceof PlayerEntity playerEntity) {
            damage(this.getDamageSources().playerAttack(playerEntity), 0.0F);
        } else {
            playSound(getSoundGroup().getHitSound(), 0.5F, 1.0F);
        }
        return true;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        ActionResult result = LightLinkEntity.onDamageFrom(this, source, getSoundGroup().getHitSound());

        if (result.isAccepted()) {
            destroyLinks(result == ActionResult.SUCCESS);
            return true;
        }
        return false;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound root) {
        root.putString(SOURCE_ITEM_KEY, Registries.ITEM.getId(lightItemSource).toString());
        NbtList linksTag = new NbtList();

        for (LightLink link : links) {
            if (link.isDead()) continue;
            if (link.getPrimary() != this) continue;
            Entity secondary = link.getSecondary();
            NbtCompound compoundTag = new NbtCompound();
            compoundTag.putString(SOURCE_ITEM_KEY, Registries.ITEM.getId(link.getSourceItem()).toString());
            if (secondary instanceof PlayerEntity) {
                UUID uuid = secondary.getUuid();
                compoundTag.putUuid("UUID", uuid);
            } else if (secondary instanceof BlockAttachedEntity) {
                BlockPos srcPos = this.attachedBlockPos;
                BlockPos dstPos = ((BlockAttachedEntity) secondary).getAttachedBlockPos();
                BlockPos relPos = dstPos.subtract(srcPos);
                Direction inverseFacing = Direction.fromRotation(Direction.SOUTH.asRotation() - getYaw());
                relPos = getBlockPosAsFacingRelative(relPos, inverseFacing);
                compoundTag.putInt("RelX", relPos.getX());
                compoundTag.putInt("RelY", relPos.getY());
                compoundTag.putInt("RelZ", relPos.getZ());
            }
            linksTag.add(compoundTag);
        }

        linksTag.addAll(incompleteLinks);

        if (!linksTag.isEmpty()) {
            root.put("Lights", linksTag);
        }
    }

    public void readCustomDataFromNbt(NbtCompound root) {
        if (root.contains("Lights")) {
            incompleteLinks.addAll(root.getList("Lights", NbtElement.COMPOUND_TYPE));
        }
        lightItemSource = Registries.ITEM.get(Identifier.tryParse(root.getString(SOURCE_ITEM_KEY)));
    }

    @Environment(EnvType.CLIENT)
    @Override
    public boolean shouldRender(double distance) {
        return true;
    }

    @Override
    public Vec3d getLeashOffset() {
        return new Vec3d(0, 4.5 / 16, 0);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Vec3d getLeashPos(float f) {
        return getLerpedPos(f).add(0, 4.5 / 16, 0);
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        ItemStack handStack = player.getStackInHand(hand);
        if (getWorld().isClient()) {
            if (handStack.isIn(ModTags.LIGHT_ITEMS)) {
                return ActionResult.SUCCESS;
            }

            if (LightLinkEntity.canDestroyWith(handStack)) {
                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        }

        boolean madeConnection = tryAttachHeldLights(player);
        if (madeConnection) {
            onPlace();
            return ActionResult.CONSUME;
        }

        boolean broke = false;
        for (LightLink link : links) {
            if (link.getSecondary() == player) {
                broke = true;
                link.destroy(true);
            }
        }
        if (broke) {
            return ActionResult.CONSUME;
        }

        if (handStack.isIn(ModTags.LIGHT_ITEMS)) {
            onPlace();
            LightLink.create(this, player, handStack.getItem());
            updateLightType(handStack.getItem());
            if (!player.isCreative()) {
                player.getStackInHand(hand).decrement(1);
            }

            return ActionResult.CONSUME;
        }

        if (LightLinkEntity.canDestroyWith(handStack)) {
            destroyLinks(!player.isCreative());
            graceTicks = 0;
            return ActionResult.CONSUME;
        }

        return ActionResult.PASS;
    }

    public boolean tryAttachHeldLights(PlayerEntity player) {
        boolean hasMadeConnection = false;
        List<LightLink> attachableLinks = getHeldLightsInRange(player, getAttachedBlockPos());
        for (LightLink link : attachableLinks) {
            if (link.getPrimary() == this) continue;

            LightLink newLink = LightLink.create(link.getPrimary(), this, link.getSourceItem());

            if (newLink != null) {
                link.destroy(false);
                link.removeSilently = true;
                hasMadeConnection = true;
            }
        }
        return hasMadeConnection;
    }

    public void onPlace() {
        playSound(getSoundGroup().getPlaceSound(), 1.0F, 1.0F);
    }

    private BlockSoundGroup getSoundGroup() {
        return LightLink.getSoundGroup(lightItemSource);
    }

    public void updateLightType(Item sourceItem) {
        this.lightItemSource = sourceItem;

        if (!getWorld().isClient()) {
            Collection<ServerPlayerEntity> trackingPlayers = PlayerLookup.around((ServerWorld) getWorld(), getBlockPos(), LightKnotEntity.VISIBLE_RANGE);
            KnotChangePayload payload = new KnotChangePayload(getId(), sourceItem);
            trackingPlayers.forEach(player -> ServerPlayNetworking.send(player, payload));
        }
    }

    public static List<LightLink> getHeldLightsInRange(PlayerEntity player, BlockPos target) {
        Box searchBox = Box.of(Vec3d.of(target), getMaxRange() * 2, getMaxRange() * 2, getMaxRange() * 2);
        List<LightKnotEntity> otherKnots = player.getWorld().getNonSpectatingEntities(LightKnotEntity.class, searchBox);

        List<LightLink> attachableLinks = new ArrayList<>();

        for (LightKnotEntity source : otherKnots) {
            for (LightLink link : source.getLinks()) {
                if (link.getSecondary() != player) continue;
                // We found a knot that is connected to the player.
                attachableLinks.add(link);
            }
        }
        return attachableLinks;
    }

    public List<LightLink> getLinks() {
        return links;
    }

    @Override
    public SoundCategory getSoundCategory() {
        return SoundCategory.BLOCKS;
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entityTrackerEntry) {
        int id = Registries.ITEM.getRawId(lightItemSource);
        return new EntitySpawnS2CPacket(this, id, this.getAttachedBlockPos());
    }

    @Override
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);
        int rawLightItemSourceId = packet.getEntityData();
        lightItemSource = Registries.ITEM.get(rawLightItemSourceId);
    }

    @Environment(EnvType.CLIENT)
    public boolean shouldRenderKnot() {
        return attachTarget == null || !attachTarget.isIn(BlockTags.WALLS);
    }

    public void addLink(LightLink link) {
        links.add(link);
    }
}
