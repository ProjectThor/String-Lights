package net.projectthor.stringlights.light;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.projectthor.stringlights.entity.LightKnotEntity;

@Environment(EnvType.CLIENT)
public class IncompleteLightLink {

    public final LightKnotEntity primary;

    public final int secondaryId;

    public final Item sourceItem;

    private boolean alive = true;

    public IncompleteLightLink(LightKnotEntity primary, int secondaryId, Item sourceItem) {
        this.primary = primary;
        this.secondaryId = secondaryId;
        this.sourceItem = sourceItem;
    }

    public boolean tryCompleteOrRemove() {
        if (isDead()) return true;
        Entity secondary = primary.getWorld().getEntityById(secondaryId);
        if (secondary == null) return false;
        LightLink.create(primary, secondary, sourceItem);
        return true;
    }

    public boolean isDead() {
        return !alive || this.primary.isRemoved();
    }

    public void destroy() {
        if (!alive) return;
        this.alive = false;
    }
}
