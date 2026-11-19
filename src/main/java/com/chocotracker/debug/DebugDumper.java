package com.chocotracker.debug;

import com.chocotracker.ChocoTrackerClient;
import com.chocotracker.island.HypixelLocationBridge;
import com.chocotracker.util.Chat;
import com.chocotracker.util.HudText;
import com.chocotracker.util.TextUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Debug mode: writes the raw text of menus, relevant chat, the scoreboard and the tab list to
 * config/chocotracker/debug/dump-*.log so parsers can be written from real data.
 * Purely passive: it only reads what the client already received.
 */
public final class DebugDumper {
	private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
	private static final DateTimeFormatter LINE_TS = DateTimeFormatter.ofPattern("HH:mm:ss");
	/** Chat lines we log by default (everything else needs "/choco debug chat all"). */
	private static final Pattern CHAT_KEYWORDS = Pattern.compile(
			"(?i)rabbit|hoppity|chocolate|time tower|hotspot|hunt|egg|barn|factory|stray|duplicate|employee|multiplier|cps");
	private static final int STABLE_TICKS = 5;
	private static final long SAME_MENU_THROTTLE_MS = 15_000;

	private static boolean enabled;
	private static boolean allChat;
	private static BufferedWriter out;
	private static Path file;

	// Menu change detection
	private static int pendingHash;
	private static int stableFor;
	private static int lastDumpedHash;
	private static String lastDumpedNamesKey = "";
	private static long lastDumpedAt;

	// Level change → snapshot
	private static Object lastLevel;
	private static int snapshotIn = -1;

	private DebugDumper() {}

	public static boolean isEnabled() { return enabled; }
	public static Path file() { return file; }

	public static void enable() {
		if (enabled) return;
		try {
			Path dir = ChocoTrackerClient.dataDir().resolve("debug");
			Files.createDirectories(dir);
			file = dir.resolve("dump-" + LocalDateTime.now().format(FILE_TS) + ".log");
			out = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
			enabled = true;
			lastDumpedHash = 0;
			lastDumpedNamesKey = "";
			write("# ChocoTracker debug dump. Format codes: §0-§f named colors, §#RRGGBB custom colors, §l bold, §o italic, §k obfuscated, §r reset.");
			write("# Hypixel Mod API installed: " + HypixelLocationBridge.available());
			Chat.send(Component.literal("Debug ON. Writing to ").withStyle(ChatFormatting.YELLOW)
					.append(Component.literal(file.toString()).withStyle(ChatFormatting.WHITE)));
			snapshot("debug enabled");
		} catch (IOException e) {
			ChocoTrackerClient.LOG.error("Could not open debug log", e);
			Chat.info("Could not open debug log: " + e.getMessage());
		}
	}

	public static void disable() {
		if (!enabled) return;
		enabled = false;
		try {
			out.close();
		} catch (IOException ignored) {
		}
		out = null;
		Chat.send(Component.literal("Debug OFF. Log saved to ").withStyle(ChatFormatting.YELLOW)
				.append(Component.literal(file.toString()).withStyle(ChatFormatting.WHITE)));
	}

	public static void setAllChat(boolean all) { allChat = all; }
	public static boolean allChat() { return allChat; }

	// ---------------------------------------------------------------- events

	public static void onTick(Minecraft mc) {
		if (!enabled) return;

		if (mc.level != lastLevel) {
			lastLevel = mc.level;
			if (mc.level != null) snapshotIn = 60; // give the server ~3s to send scoreboard/tab
		}
		if (snapshotIn > 0 && --snapshotIn == 0) snapshot("island/world change");

		Screen screen = mc.gui.screen();
		if (!(screen instanceof AbstractContainerScreen<?> cs)) {
			pendingHash = 0;
			stableFor = 0;
			return;
		}
		int hash = menuHash(cs);
		if (hash != pendingHash) {
			pendingHash = hash;
			stableFor = 0;
			return;
		}
		if (++stableFor != STABLE_TICKS || hash == lastDumpedHash) return;

		// Menus like the Chocolate Factory update every second; only re-dump those occasionally
		// unless the set of items actually changed (e.g. a page flip).
		String namesKey = namesKey(cs);
		long now = System.currentTimeMillis();
		if (namesKey.equals(lastDumpedNamesKey) && now - lastDumpedAt < SAME_MENU_THROTTLE_MS) return;

		dumpMenu(cs);
		lastDumpedHash = hash;
		lastDumpedNamesKey = namesKey;
		lastDumpedAt = now;
	}

	public static void onGameMessage(Component message, boolean overlay) {
		if (!enabled || overlay) return;
		String legacy = TextUtil.tidy(TextUtil.toLegacy(message));
		if (!allChat && !CHAT_KEYWORDS.matcher(TextUtil.strip(legacy)).find()) return;
		write("[" + ts() + "] CHAT: " + legacy);
	}

	public static void onHypixelLocation(HypixelLocationBridge.Location loc) {
		if (!enabled) return;
		write("[" + ts() + "] HYPIXEL_LOCATION: " + loc);
	}

	// ---------------------------------------------------------------- dumping

	public static void snapshot(String reason) {
		if (!enabled) return;
		Minecraft mc = Minecraft.getInstance();
		StringBuilder sb = new StringBuilder();
		sb.append("\n========== SNAPSHOT (").append(reason).append(") @ ").append(ts()).append(" ==========\n");
		sb.append("HYPIXEL_LOCATION (last): ").append(HypixelLocationBridge.last).append('\n');
		sb.append("SIDEBAR TITLE: ").append(oneLine(HudText.sidebarTitle())).append('\n');
		List<String> side = HudText.sidebarLines();
		for (int i = 0; i < side.size(); i++) sb.append("  SIDEBAR[").append(i).append("]: ").append(TextUtil.tidy(side.get(i))).append('\n');
		if (mc.getConnection() != null) {
			sb.append("TAB HEADER: ").append(oneLine(HudText.tabHeader())).append('\n');
			List<String> tab = HudText.tabLines();
			for (int i = 0; i < tab.size(); i++) {
				String line = TextUtil.tidy(tab.get(i));
				if (!TextUtil.strip(line).isBlank()) sb.append("  TAB[").append(i).append("]: ").append(line).append('\n');
			}
			sb.append("TAB FOOTER: ").append(oneLine(HudText.tabFooter())).append('\n');
		}
		write(sb.toString());
		Chat.info("Debug: saved scoreboard + tab list snapshot.");
	}

	private static void dumpMenu(AbstractContainerScreen<?> cs) {
		StringBuilder sb = new StringBuilder();
		String title = TextUtil.tidy(TextUtil.toLegacy(cs.getTitle()));
		var menu = cs.getMenu();
		List<Slot> slots = containerSlots(cs);
		sb.append("\n========== MENU \"").append(title).append("\" | containerId=").append(menu.containerId)
				.append(" | slots=").append(slots.size()).append(" | ").append(menu.getClass().getSimpleName())
				.append(" @ ").append(ts()).append(" ==========\n");
		for (Slot slot : slots) {
			ItemStack stack = slot.getItem();
			if (stack.isEmpty()) continue;
			sb.append("[slot ").append(slot.index).append("] ")
					.append(BuiltInRegistries.ITEM.getKey(stack.getItem()))
					.append(" x").append(stack.getCount()).append('\n');
			sb.append("  NAME: ").append(TextUtil.tidy(TextUtil.toLegacy(stack.getHoverName()))).append('\n');
			ItemLore lore = stack.get(DataComponents.LORE);
			if (lore != null) {
				for (Component line : lore.lines()) sb.append("  LORE: ").append(TextUtil.tidy(TextUtil.toLegacy(line))).append('\n');
			}
			CustomData data = stack.get(DataComponents.CUSTOM_DATA);
			if (data != null) {
				String s = data.toString();
				if (s.length() > 400) s = s.substring(0, 400) + "...";
				sb.append("  DATA: ").append(s).append('\n');
			}
		}
		write(sb.toString());
		Chat.info("Debug: dumped menu \"" + TextUtil.strip(title) + "\" (" + slots.size() + " slots).");
	}

	// ---------------------------------------------------------------- helpers

	/** Slots of the opened container, excluding the player's own inventory. */
	private static List<Slot> containerSlots(AbstractContainerScreen<?> cs) {
		List<Slot> list = new ArrayList<>();
		for (Slot s : cs.getMenu().slots) if (!(s.container instanceof Inventory)) list.add(s);
		return list;
	}

	private static int menuHash(AbstractContainerScreen<?> cs) {
		int h = cs.getTitle().getString().hashCode() * 31 + cs.getMenu().containerId;
		for (Slot s : containerSlots(cs)) {
			ItemStack st = s.getItem();
			if (st.isEmpty()) continue;
			h = h * 31 + s.index;
			h = h * 31 + TextUtil.toLegacy(st.getHoverName()).hashCode();
			ItemLore lore = st.get(DataComponents.LORE);
			if (lore != null) for (Component c : lore.lines()) h = h * 31 + TextUtil.toLegacy(c).hashCode();
		}
		return h == 0 ? 1 : h;
	}

	private static String namesKey(AbstractContainerScreen<?> cs) {
		StringBuilder sb = new StringBuilder(cs.getTitle().getString()).append('|');
		for (Slot s : containerSlots(cs)) if (!s.getItem().isEmpty()) sb.append(s.getItem().getHoverName().getString()).append('|');
		return sb.toString();
	}

	private static String oneLine(String s) {
		return s == null ? "null" : TextUtil.tidy(s).replace("\n", " \\n ");
	}

	private static String ts() {
		return LocalDateTime.now().format(LINE_TS);
	}

	private static synchronized void write(String s) {
		if (out == null) return;
		try {
			out.write(s);
			out.newLine();
			out.flush();
		} catch (IOException e) {
			ChocoTrackerClient.LOG.error("Debug log write failed", e);
		}
	}
}
