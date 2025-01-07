package net.projectthor.stringlights.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import net.projectthor.stringlights.StringLights;
import net.projectthor.stringlights.item.custom.StringLightsItem;

public class ModItems {
    public static final Item STRING_LIGHTS = registerItem( "string_lights", new StringLightsItem(new Item.Settings()));


    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(StringLights.MOD_ID, name), item);
    }

    public static void registerModItems() {
        StringLights.LOGGER.info("Registering Mod Items for " + StringLights.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COLORED_BLOCKS).register(entries -> {
            entries.add(STRING_LIGHTS);
        });
    }

}
