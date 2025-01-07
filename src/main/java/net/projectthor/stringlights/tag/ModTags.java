package net.projectthor.stringlights.tag;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.projectthor.stringlights.util.Helper;


public class ModTags {
    public static final TagKey<Block> LIGHT_CONNECTIBLE = makeTag(RegistryKeys.BLOCK, Helper.identifier("light_connectible"));
    public static final TagKey<Item> LIGHT_ITEMS = makeTag(RegistryKeys.ITEM, Helper.identifier("light_items"));

    public static <T> TagKey<T> makeTag(RegistryKey<Registry<T>> registry, Identifier id) {
        return TagKey.of(registry, id);
    }


}