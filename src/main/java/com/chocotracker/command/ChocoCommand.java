package com.chocotracker.command;

import com.chocotracker.debug.DebugDumper;
import com.chocotracker.util.Chat;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/** Client-side /choco command. Never sends anything to the server. */
public final class ChocoCommand {
	private ChocoCommand() {}

	public static void register(CommandDispatcher<FabricClientCommandSource> d) {
		d.register(literal("choco")
				.executes(c -> { Chat.info("Debug build: use /choco debug to record menus. Tracker features come after the parsers."); return 1; })
				.then(literal("debug")
						.executes(c -> { if (DebugDumper.isEnabled()) DebugDumper.disable(); else DebugDumper.enable(); return 1; })
						.then(literal("on").executes(c -> { DebugDumper.enable(); return 1; }))
						.then(literal("off").executes(c -> { DebugDumper.disable(); return 1; }))
						.then(literal("snapshot").executes(c -> {
							if (!DebugDumper.isEnabled()) DebugDumper.enable(); else DebugDumper.snapshot("manual");
							return 1;
						}))
						.then(literal("chat")
								.then(literal("all").executes(c -> { DebugDumper.setAllChat(true); Chat.info("Debug: logging ALL chat (other players' messages too)."); return 1; }))
								.then(literal("filtered").executes(c -> { DebugDumper.setAllChat(false); Chat.info("Debug: logging only chocolate/rabbit related chat."); return 1; })))));
	}
}
