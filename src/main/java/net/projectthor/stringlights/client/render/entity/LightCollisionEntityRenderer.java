package net.projectthor.stringlights.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import net.projectthor.stringlights.entity.LightCollisionEntity;

@Environment(EnvType.CLIENT)
public class LightCollisionEntityRenderer extends EntityRenderer<LightCollisionEntity> {

    public LightCollisionEntityRenderer(EntityRendererFactory.Context dispatcher) {
        super(dispatcher);
    }

    @Override
    public Identifier getTexture(LightCollisionEntity entity) {
        return null;
    }
}
