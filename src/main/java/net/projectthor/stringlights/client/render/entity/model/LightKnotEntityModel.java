package net.projectthor.stringlights.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.entity.Entity;


@Environment(EnvType.CLIENT)
public class LightKnotEntityModel<T extends Entity> extends SinglePartEntityModel<T> {
    private final ModelPart lightKnot;

    public LightKnotEntityModel(ModelPart root) {
        this.lightKnot = root.getChild("knot");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData bb_main = modelPartData.addChild("knot", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, -12.5F, 0.0F));

        bb_main.addChild("knot_child", ModelPartBuilder.create().uv(3, 1).cuboid(-1.0F, -1.5F, 3.0F, 3.0F, 6.0F, 0.0F, new Dilation(0.0F))
                .uv(0, 1).cuboid(-1.0F, -1.5F, -3.0F, 3.0F, 0.0F, 6.0F, new Dilation(0.0F))
                .uv(0, 9).mirrored().cuboid(-1.0F, 4.5F, -3.0F, 3.0F, 0.0F, 6.0F, new Dilation(0.0F)).mirrored(false)
                .uv(3, 6).cuboid(-1.0F, -1.5F, -3.0F, 3.0F, 6.0F, 0.0F, new Dilation(0.0F)), ModelTransform.of(-1.5F, 7.0F, 0.0F, 0.0F, 0.0F, -1.5708F));
        return TexturedModelData.of(modelData, 16, 16);
    }

    @Override
    public ModelPart getPart() {
        return lightKnot;
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
    }
}
