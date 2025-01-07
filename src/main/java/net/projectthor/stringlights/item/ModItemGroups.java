package net.projectthor.stringlights.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.projectthor.stringlights.StringLights;
import net.projectthor.stringlights.block.ModBlocks;

public class ModItemGroups {
    public static final ItemGroup STRING_LIGHTS_ITEMS_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(StringLights.MOD_ID, "string_lights_items"),
            FabricItemGroup.builder().icon(() -> new ItemStack(ModItems.STRING_LIGHTS))
                    .displayName(Text.translatable("itemgroup.stringlights.string_lights_items"))
                    .entries((displayContext, entries) -> {
                        entries.add(ModItems.STRING_LIGHTS);
                        entries.add(ModBlocks.FASTENER);
                    }).build());

    public static void registerItemGroups() {
        StringLights.LOGGER.info("Registering Item Groups for " + StringLights.MOD_ID);
    }
}
