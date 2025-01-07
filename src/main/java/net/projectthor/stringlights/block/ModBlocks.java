package net.projectthor.stringlights.block;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.projectthor.stringlights.StringLights;
import net.projectthor.stringlights.block.custom.Fastener;

public class ModBlocks {
    public static final Block FASTENER = registerBlock("fastener",
            new Fastener(AbstractBlock.Settings.create().breakInstantly()
                    .sounds(BlockSoundGroup.STONE)));

    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, Identifier.of(StringLights.MOD_ID, name), block);
    }

    private static void registerBlockItem(String name, Block block) {
        Registry.register(Registries.ITEM, Identifier.of(StringLights.MOD_ID, name),
              new BlockItem(block, new Item.Settings()));
    }

    public static void registerModBlocks() {
        StringLights.LOGGER.info("Registering Mod Blocks for " + StringLights.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
            entries.add(ModBlocks.FASTENER);
        });
    }
}
