package net.projectthor.stringlights.entity;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;


public interface LightLinkEntity {

    static ActionResult onDamageFrom(Entity self, DamageSource source, SoundEvent hitSound) {
        if (self.isInvulnerableTo(source)) {
            return ActionResult.FAIL;
        }
        if (self.getWorld().isClient) {
            return ActionResult.PASS;
        }

        if (source.isIn(DamageTypeTags.IS_EXPLOSION)) {
            return ActionResult.SUCCESS;
        }
        if (source.getSource() instanceof PlayerEntity player) {
            if (canDestroyWith(player.getMainHandStack())) {
                return ActionResult.success(!player.isCreative());
            }
        }

        if (!source.isIn(DamageTypeTags.IS_PROJECTILE)) {
            self.playSound(hitSound, 0.5f, 1.0f);
        }
        return ActionResult.FAIL;
    }

    static boolean canDestroyWith(ItemStack item) {
        return item.isIn(ConventionalItemTags.SHEAR_TOOLS);
    }

    void destroyLinks(boolean mayDrop);
}
