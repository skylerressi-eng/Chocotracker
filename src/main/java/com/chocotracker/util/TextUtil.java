package com.chocotracker.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.Optional;
import java.util.regex.Pattern;

/** Converts chat components to a legacy "§"-coded string so colors survive into logs and parsers. */
public final class TextUtil {
	private static final Pattern FORMAT_CODE = Pattern.compile("§(#[0-9a-fA-F]{6}|[0-9a-fk-orA-FK-OR])");

	private TextUtil() {}

	/**
	 * Renders a component as text with formatting codes. Named colors become §0-§f,
	 * non-standard RGB colors become §#RRGGBB. Any § codes already embedded in the
	 * literal text (Hypixel sends some of these) are kept as-is.
	 */
	public static String toLegacy(Component component) {
		if (component == null) return "";
		StringBuilder sb = new StringBuilder();
		component.visit((style, text) -> {
			if (!text.isEmpty()) {
				sb.append(styleCodes(style)).append(text);
			}
			return Optional.empty();
		}, Style.EMPTY);
		return sb.toString();
	}

	private static String styleCodes(Style style) {
		StringBuilder sb = new StringBuilder("§r");
		TextColor color = style.getColor();
		if (color != null) {
			char named = namedColor(color.getValue());
			if (named != 0) sb.append('§').append(named);
			else sb.append(String.format("§#%06X", color.getValue() & 0xFFFFFF));
		}
		if (style.isObfuscated()) sb.append("§k");
		if (style.isBold()) sb.append("§l");
		if (style.isStrikethrough()) sb.append("§m");
		if (style.isUnderlined()) sb.append("§n");
		if (style.isItalic()) sb.append("§o");
		return sb.toString();
	}

	/** The 16 legacy colors, index = code 0-f. */
	public static final int[] LEGACY_RGB = {
			0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
			0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};
	private static final String CODES = "0123456789abcdef";

	private static char namedColor(int rgb) {
		for (int i = 0; i < 16; i++) if (LEGACY_RGB[i] == (rgb & 0xFFFFFF)) return CODES.charAt(i);
		return 0;
	}

	/** Removes all § codes, including §#RRGGBB. */
	public static String strip(String legacy) {
		return legacy == null ? "" : FORMAT_CODE.matcher(legacy).replaceAll("");
	}

	/** Collapses redundant "§r" resets for readability (does not change meaning for our parsers). */
	public static String tidy(String legacy) {
		return legacy.replaceAll("(§r)+(?=§r)", "").replaceAll("^§r", "");
	}
}
