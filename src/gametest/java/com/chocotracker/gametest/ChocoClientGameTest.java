package com.chocotracker.gametest;

import com.chocotracker.debug.DebugDumper;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.nio.file.Files;
import java.util.List;

/** Exercises the debug dumper against a fake Hypixel-style menu, chat line and sidebar. */
public class ChocoClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext ctx) {
		try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {

			sp.getServer().runCommand("scoreboard objectives add sb dummy \"SKYBLOCK\"");
			sp.getServer().runCommand("scoreboard objectives setdisplay sidebar sb");
			// Hypixel draws sidebar lines with team prefixes on fake players; mimic that.
			sp.getServer().runCommand("team add line1");
			sp.getServer().runCommand("team modify line1 prefix {\"text\":\" ⏣ \",\"color\":\"gray\",\"extra\":[{\"text\":\"Hub\",\"color\":\"aqua\"}]}");
			sp.getServer().runCommand("team join line1 fakeline");
			sp.getServer().runCommand("scoreboard players set fakeline sb 2");

			ctx.runOnClient(mc -> DebugDumper.enable());
			ctx.waitTicks(70); // snapshot after level change fires at 60 ticks

			sp.getServer().runCommand("tellraw @a {\"text\":\"HOPPITY'S HUNT You found a test rabbit!\",\"color\":\"gold\"}");
			sp.getServer().runCommand("tellraw @a {\"text\":\"unrelated chat line\"}");

			ctx.setScreen(() -> {
				var mc = net.minecraft.client.Minecraft.getInstance();
				SimpleContainer c = new SimpleContainer(54);
				ItemStack head = new ItemStack(Items.PLAYER_HEAD);
				head.set(DataComponents.CUSTOM_NAME, Component.literal("§6Test Rabbit"));
				head.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("§7Resident: §aHub"), Component.literal("§6§lLEGENDARY RABBIT"))));
				c.setItem(10, head);
				ChestMenu menu = ChestMenu.sixRows(99, mc.player.getInventory(), c);
				return new ContainerScreen(menu, mc.player.getInventory(), Component.literal("Hoppity's Collection (1/2)"));
			});
			ctx.waitTicks(10);
			ctx.takeScreenshot("chocotracker-debug-menu");
			ctx.setScreen(() -> null);

			ctx.runOnClient(mc -> DebugDumper.disable());
			String log;
			try {
				log = Files.readString(DebugDumper.file());
			} catch (java.io.IOException e) {
				throw new AssertionError(e);
			}
			System.out.println("----- CHOCOTRACKER DEBUG LOG -----\n" + log + "\n----- END -----");
			check(log, "MENU \"Hoppity's Collection (1/2)\"");
			check(log, "NAME: §6Test Rabbit");
			check(log, "LORE: §7Resident: §aHub");
			check(log, "CHAT: §6HOPPITY'S HUNT You found a test rabbit!");
			check(log, "SIDEBAR TITLE: SKYBLOCK");
			check(log, "SIDEBAR[0]: §7 ⏣ §r§bHub");
			if (log.contains("unrelated chat line")) throw new AssertionError("chat filter did not filter");
		}
	}

	private static void check(String log, String needle) {
		if (!log.contains(needle)) throw new AssertionError("Debug log missing: " + needle);
	}
}
