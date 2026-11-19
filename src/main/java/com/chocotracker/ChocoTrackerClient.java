package com.chocotracker;

import com.chocotracker.command.ChocoCommand;
import com.chocotracker.debug.DebugDumper;
import com.chocotracker.island.HypixelLocationBridge;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * ChocoTracker entrypoint. Client-side only: reads menus, chat, scoreboard and tab list.
 * It never clicks, never sends chat/commands, and never automates gameplay.
 */
public class ChocoTrackerClient implements ClientModInitializer {
	public static final String MOD_ID = "chocotracker";
	public static final Logger LOG = LoggerFactory.getLogger("ChocoTracker");

	public static Path dataDir() {
		return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
	}

	@Override
	public void onInitializeClient() {
		HypixelLocationBridge.init();

		ClientTickEvents.END_CLIENT_TICK.register(DebugDumper::onTick);
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> DebugDumper.onGameMessage(message, overlay));
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) -> ChocoCommand.register(dispatcher));
	}
}
