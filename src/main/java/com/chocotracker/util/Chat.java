package com.chocotracker.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Local-only chat output (never sent to the server). */
public final class Chat {
	private Chat() {}

	public static MutableComponent prefix() {
		return Component.literal("[").withStyle(ChatFormatting.DARK_GRAY)
				.append(Component.literal("Choco").withStyle(ChatFormatting.GOLD))
				.append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY));
	}

	public static void send(Component message) {
		Minecraft mc = Minecraft.getInstance();
		mc.execute(() -> mc.gui.hud.getChat().addClientSystemMessage(prefix().append(message)));
	}

	public static void info(String text) {
		send(Component.literal(text).withStyle(ChatFormatting.YELLOW));
	}
}
