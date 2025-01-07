package net.projectthor.stringlights.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.projectthor.stringlights.StringLights;
import net.projectthor.stringlights.client.ClientInitializer;
import net.projectthor.stringlights.client.render.entity.model.LightKnotEntityModel;
import net.projectthor.stringlights.client.render.entity.texture.LightTextureManager;
import net.projectthor.stringlights.entity.LightKnotEntity;
import net.projectthor.stringlights.light.LightLink;
import net.projectthor.stringlights.util.Helper;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

@Environment(EnvType.CLIENT)
public class LightKnotEntityRenderer extends EntityRenderer<LightKnotEntity> {
    private final LightKnotEntityModel<LightKnotEntity> model;
    private final LightRenderer lightRenderer = new LightRenderer();

    public LightKnotEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.model = new LightKnotEntityModel<>(context.getPart(ClientInitializer.LIGHT_KNOT));
    }

    public LightRenderer getLightRenderer() {
        return lightRenderer;
    }

    @Override
    public boolean shouldRender(LightKnotEntity entity, Frustum frustum, double x, double y, double z) {
        if (entity.ignoreCameraFrustum) return true;
        for (LightLink link : entity.getLinks()) {
            if (link.getPrimary() != entity) continue;
            if (link.getSecondary() instanceof PlayerEntity) return true;
            else if (link.getSecondary().shouldRender(x, y, z)) return true;
        }
        return super.shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public void render(LightKnotEntity lightKnotEntity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (lightKnotEntity.shouldRenderKnot()) {
            matrices.push();
            Vec3d leashOffset = lightKnotEntity.getLeashPos(tickDelta).subtract(lightKnotEntity.getLerpedPos(tickDelta));
            matrices.translate(leashOffset.x, leashOffset.y + 6.5f / 16f, leashOffset.z);
            matrices.scale(5 / 6f, 1, 5 / 6f);
            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(this.model.getLayer(getKnotTexture(lightKnotEntity.getLightItemSource())));
            this.model.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
            matrices.pop();
        }

        List<LightLink> links = lightKnotEntity.getLinks();
        for (LightLink link : links) {
            if (link.getPrimary() != lightKnotEntity || link.isDead()) continue;
            this.renderLightLink(link, tickDelta, matrices, vertexConsumers);
            if (StringLights.runtimeConfig.doDebugDraw()) {
                this.drawDebugVector(matrices, lightKnotEntity, link.getSecondary(), vertexConsumers.getBuffer(RenderLayer.LINES));
            }
        }

        if (StringLights.runtimeConfig.doDebugDraw()) {
            matrices.push();
            Text holdingCount = Text.literal("F: " + lightKnotEntity.getLinks().stream()
                    .filter(l -> l.getPrimary() == lightKnotEntity).count());
            Text heldCount = Text.literal("T: " + lightKnotEntity.getLinks().stream()
                    .filter(l -> l.getSecondary() == lightKnotEntity).count());
            matrices.translate(0, 0.25, 0);
            this.renderLabelIfPresent(lightKnotEntity, holdingCount, matrices, vertexConsumers, light, tickDelta);
            matrices.translate(0, 0.25, 0);
            this.renderLabelIfPresent(lightKnotEntity, heldCount, matrices, vertexConsumers, light, tickDelta);
            matrices.pop();
        }
        super.render(lightKnotEntity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
    private LightTextureManager getTextureManager() {
        return ClientInitializer.getInstance().getLightTextureManager();
    }

    private Identifier getKnotTexture(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        return getTextureManager().getKnotTexture(id);
    }

    private Identifier getLightTexture(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        return getTextureManager().getLightTexture(id);
    }

    private void renderLightLink(LightLink link, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider) {
        LightKnotEntity fromEntity = link.getPrimary();
        Entity toEntity = link.getSecondary();
        matrices.push();
        Vec3d srcPos = fromEntity.getPos().add(fromEntity.getLeashOffset());
        Vec3d dstPos;

        if (toEntity instanceof AbstractDecorationEntity) {
            dstPos = toEntity.getPos().add(toEntity.getLeashOffset(tickDelta));
        } else {
            dstPos = toEntity.getLeashPos(tickDelta);
        }

        Vec3d leashOffset = fromEntity.getLeashOffset();
        matrices.translate(leashOffset.x, leashOffset.y, leashOffset.z);

        Item sourceItem = link.getSourceItem();
        RenderLayer entityCutout = RenderLayer.getEntityCutoutNoCull(getLightTexture(sourceItem));

        VertexConsumer buffer = vertexConsumerProvider.getBuffer(entityCutout);
        if (StringLights.runtimeConfig.doDebugDraw()) {
            buffer = vertexConsumerProvider.getBuffer(RenderLayer.getLines());
        }

        Vec3d offset = Helper.getLightOffset(srcPos, dstPos);
        matrices.translate(offset.getX(), 0, offset.getZ());

        BlockPos blockPosOfStart = BlockPos.ofFloored(fromEntity.getCameraPosVec(tickDelta));
        BlockPos blockPosOfEnd = BlockPos.ofFloored(toEntity.getCameraPosVec(tickDelta));
        int blockLightLevelOfStart = fromEntity.getWorld().getLightLevel(LightType.BLOCK, blockPosOfStart);
        int blockLightLevelOfEnd = toEntity.getWorld().getLightLevel(LightType.BLOCK, blockPosOfEnd);
        int skyLightLevelOfStart = fromEntity.getWorld().getLightLevel(LightType.SKY, blockPosOfStart);
        int skyLightLevelOfEnd = fromEntity.getWorld().getLightLevel(LightType.SKY, blockPosOfEnd);

        Vec3d startPos = srcPos.add(offset.getX(), 0, offset.getZ());
        Vec3d endPos = dstPos.add(-offset.getX(), 0, -offset.getZ());
        Vector3f lightVec = new Vector3f((float) (endPos.x - startPos.x), (float) (endPos.y - startPos.y), (float) (endPos.z - startPos.z));

        float angleY = -(float) Math.atan2(lightVec.z(), lightVec.x());

        matrices.multiply(new Quaternionf().rotateXYZ(0, angleY, 0));

        if (toEntity instanceof AbstractDecorationEntity) {
            LightRenderer.BakeKey key = new LightRenderer.BakeKey(fromEntity.getPos(), toEntity.getPos());
            lightRenderer.renderBaked(buffer, matrices, key, lightVec, blockLightLevelOfStart, blockLightLevelOfEnd, skyLightLevelOfStart, skyLightLevelOfEnd);
        } else {
            lightRenderer.render(buffer, matrices, lightVec, blockLightLevelOfStart, blockLightLevelOfEnd, skyLightLevelOfStart, skyLightLevelOfEnd);
        }

        matrices.pop();
    }

    private void drawDebugVector(MatrixStack matrices, Entity fromEntity, Entity toEntity, VertexConsumer buffer) {
        if (toEntity == null) return;
        Matrix4f modelMat = matrices.peek().getPositionMatrix();
        Vec3d vec = toEntity.getPos().subtract(fromEntity.getPos());
        Vec3d normal = vec.normalize();
        buffer.vertex(modelMat, 0, 0, 0)
                .color(0, 255, 0, 255)
                .normal((float) normal.x, (float) normal.y, (float) normal.z);
        buffer.vertex(modelMat, (float) vec.x, (float) vec.y, (float) vec.z)
                .color(255, 0, 0, 255)
                .normal((float) normal.x, (float) normal.y, (float) normal.z);
    }

    @Override
    public Identifier getTexture(LightKnotEntity entity) {
        return getKnotTexture(entity.getLightItemSource());
    }

}
