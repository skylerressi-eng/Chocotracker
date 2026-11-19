package com.chocotracker.island;

import com.chocotracker.ChocoTrackerClient;
import com.chocotracker.debug.DebugDumper;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Optional integration with the official Hypixel Mod API (the "hypixel-mod-api" Fabric mod).
 * Classes from net.hypixel are only touched when that mod is installed, so ChocoTracker
 * still loads without it and falls back to scoreboard / tab list parsing.
 */
public final class HypixelLocationBridge {
	/** Last location reported by the Hypixel Mod API, or null. */
	public static volatile Location last;

	public record Location(String serverName, String serverType, String lobbyName, String mode, String map) {}

	private HypixelLocationBridge() {}

	public static boolean available() {
		return FabricLoader.getInstance().isModLoaded("hypixel-mod-api");
	}

	public static void init() {
		if (!available()) {
			ChocoTrackerClient.LOG.info("hypixel-mod-api not installed; using scoreboard/tab list for island detection");
			return;
		}
		try {
			Impl.register();
			ChocoTrackerClient.LOG.info("Subscribed to Hypixel Mod API location events");
		} catch (Throwable t) {
			ChocoTrackerClient.LOG.warn("Could not hook Hypixel Mod API, falling back to scoreboard/tab list", t);
		}
	}

	/** Isolated so the JVM only loads Hypixel classes if this is called. */
	private static final class Impl {
		static void register() {
			var api = net.hypixel.modapi.HypixelModAPI.getInstance();
			api.subscribeToEventPacket(net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket.class);
			api.createHandler(net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket.class, p -> {
				Location loc = new Location(
						p.getServerName(),
						p.getServerType().map(Object::toString).orElse(null),
						p.getLobbyName().orElse(null),
						p.getMode().orElse(null),
						p.getMap().orElse(null));
				last = loc;
				DebugDumper.onHypixelLocation(loc);
			});
		}
	}
}
