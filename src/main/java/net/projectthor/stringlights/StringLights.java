package net.projectthor.stringlights;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import net.fabricmc.api.ModInitializer;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.projectthor.stringlights.block.ModBlocks;
import net.projectthor.stringlights.config.ModConfig;
import net.projectthor.stringlights.entity.ModEntityTypes;
import net.projectthor.stringlights.item.ModItemGroups;
import net.projectthor.stringlights.item.ModItems;
import net.projectthor.stringlights.item.custom.StringLightsItem;
import net.projectthor.stringlights.networking.packet.Payloads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringLights implements ModInitializer {
	public static final String MOD_ID = "stringlights";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static ModConfig fileConfig;
	public static ModConfig runtimeConfig;

	@Override
	public void onInitialize() {
		ModItems.registerModItems();
		ModItemGroups.registerItemGroups();
		ModBlocks.registerModBlocks();

		ModEntityTypes.init();
		Payloads.init();
		AutoConfig.register(ModConfig.class, Toml4jConfigSerializer::new);
		ConfigHolder<ModConfig> configHolder = AutoConfig.getConfigHolder(ModConfig.class);
		fileConfig = configHolder.getConfig();
		runtimeConfig = new ModConfig().copyFrom(fileConfig);

		UseBlockCallback.EVENT.register(StringLightsItem::lightUseEvent);

		ServerPlayConnectionEvents.INIT.register((handler, server) -> fileConfig.syncToClient(handler.getPlayer()));
	}
}