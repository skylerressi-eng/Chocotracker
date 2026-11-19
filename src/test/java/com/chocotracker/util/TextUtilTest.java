package com.chocotracker.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextUtilTest {
	@Test
	void namedAndHexColors() {
		Component c = Component.literal("Rabbit ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFAA00)).withBold(true))
				.append(Component.literal("X").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x123456))));
		String legacy = TextUtil.tidy(TextUtil.toLegacy(c));
		// children inherit the parent style (bold)
		assertEquals("§6§lRabbit §r§#123456§lX", legacy);
		assertEquals("Rabbit X", TextUtil.strip(legacy));
	}

	@Test
	void keepsEmbeddedLegacyCodes() {
		assertEquals("§r§dMYTHIC", TextUtil.toLegacy(Component.literal("§dMYTHIC")));
		assertEquals("MYTHIC", TextUtil.strip("§r§dMYTHIC"));
	}
}
