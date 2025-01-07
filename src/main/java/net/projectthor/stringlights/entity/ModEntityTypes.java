package net.projectthor.stringlights.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.projectthor.stringlights.StringLights;
import net.projectthor.stringlights.util.Helper;

public class ModEntityTypes {

    public static final EntityType<LightKnotEntity> LIGHT_KNOT;
    public static final EntityType<LightCollisionEntity> LIGHT_COLLISION;

    static {
        LIGHT_KNOT = Registry.register(
                Registries.ENTITY_TYPE, Helper.identifier("light_knot"),
                EntityType.Builder.create((EntityType.EntityFactory<LightKnotEntity>) LightKnotEntity::new, SpawnGroup.MISC)
                        .trackingTickInterval(Integer.MAX_VALUE).alwaysUpdateVelocity(false)
                        .dimensions(0.375f, 0.5F)
                        .spawnableFarFromPlayer()
                        .makeFireImmune()
                        .build("light_knot")
        );
        LIGHT_COLLISION = Registry.register(
                Registries.ENTITY_TYPE, Helper.identifier("light_collision"),
                EntityType.Builder.create((EntityType.EntityFactory<LightCollisionEntity>) LightCollisionEntity::new, SpawnGroup.MISC)
                        .maxTrackingRange(1).trackingTickInterval(Integer.MAX_VALUE).alwaysUpdateVelocity(false)
                        .dimensions(0.25f, 0.375f)
                        .disableSaving()
                        .disableSummon()
                        .makeFireImmune()
                        .build("light_collision")
        );
    }

    public static void init() {
        StringLights.LOGGER.info("Initialized entity types.");
    }
}
