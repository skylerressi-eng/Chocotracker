package com.chocotracker.util;

import com.chocotracker.mixin.PlayerTabOverlayAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Reads (never modifies) the sidebar scoreboard and the tab list, as legacy-coded strings. */
public final class HudText {
	private HudText() {}

	/** Sidebar title, or null when there is no sidebar. */
	public static String sidebarTitle() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return null;
		Objective obj = mc.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
		return obj == null ? null : TextUtil.toLegacy(obj.getDisplayName());
	}

	/** Sidebar lines from top to bottom. */
	public static List<String> sidebarLines() {
		List<String> out = new ArrayList<>();
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return out;
		Scoreboard sb = mc.level.getScoreboard();
		Objective obj = sb.getDisplayObjective(DisplaySlot.SIDEBAR);
		if (obj == null) return out;
		List<PlayerScoreEntry> entries = new ArrayList<>(sb.listPlayerScores(obj));
		entries.removeIf(PlayerScoreEntry::isHidden);
		// Same order as vanilla: highest score first, then owner name.
		entries.sort(Comparator.comparingInt(PlayerScoreEntry::value).reversed()
				.thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER));
		for (PlayerScoreEntry e : entries) {
			PlayerTeam team = sb.getPlayersTeam(e.owner());
			Component line = PlayerTeam.formatNameForTeam(team, e.ownerName());
			out.add(TextUtil.toLegacy(line));
		}
		return out;
	}

	/** Tab list entries in display order. */
	public static List<String> tabLines() {
		List<String> out = new ArrayList<>();
		Minecraft mc = Minecraft.getInstance();
		ClientPacketListener conn = mc.getConnection();
		if (conn == null) return out;
		List<PlayerInfo> infos = new ArrayList<>(conn.getListedOnlinePlayers());
		infos.sort(Comparator.<PlayerInfo>comparingInt(p -> -p.getTabListOrder())
				.thenComparing(p -> p.getTeam() == null ? "" : p.getTeam().getName())
				.thenComparing(p -> p.getProfile().name(), String.CASE_INSENSITIVE_ORDER));
		for (PlayerInfo info : infos) {
			out.add(TextUtil.toLegacy(mc.gui.hud.getTabList().getNameForDisplay(info)));
		}
		return out;
	}

	public static String tabHeader() {
		Component c = ((PlayerTabOverlayAccessor) Minecraft.getInstance().gui.hud.getTabList()).chocotracker$getHeader();
		return c == null ? null : TextUtil.toLegacy(c);
	}

	public static String tabFooter() {
		Component c = ((PlayerTabOverlayAccessor) Minecraft.getInstance().gui.hud.getTabList()).chocotracker$getFooter();
		return c == null ? null : TextUtil.toLegacy(c);
	}
}
