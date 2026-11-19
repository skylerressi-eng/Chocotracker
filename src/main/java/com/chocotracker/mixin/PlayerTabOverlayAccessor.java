package com.chocotracker.mixin;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only access to the tab list header/footer. */
@Mixin(PlayerTabOverlay.class)
public interface PlayerTabOverlayAccessor {
	@Accessor("header")
	Component chocotracker$getHeader();

	@Accessor("footer")
	Component chocotracker$getFooter();
}
