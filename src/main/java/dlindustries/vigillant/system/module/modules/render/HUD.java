package dlindustries.vigillant.system.module.modules.render;

import dlindustries.vigillant.system.event.events.HudListener;
import dlindustries.vigillant.system.gui.ClickGui;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.modules.client.NameProtect;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.TextRenderer;
import dlindustries.vigillant.system.utils.Utils;
import net.minecraft.client.gui.DrawContext;
import java.awt.*;
import java.util.List;

public final class HUD extends Module implements HudListener {
	private static final CharSequence system = EncryptedString.of("System |");
	private final BooleanSetting info = new BooleanSetting(EncryptedString.of("Info"), true);
	private final BooleanSetting coords = new BooleanSetting(EncryptedString.of("Coords"), true)
			.setDescription(EncryptedString.of("Renders your current coordinates"));
	private final BooleanSetting modules = new BooleanSetting("Array list", false)
			.setDescription(EncryptedString.of("Renders module list"));
	public HUD() {
		super(EncryptedString.of("HUD"),
				EncryptedString.of("Overlay info as you play"),
				-1,
				Category.RENDER);
		addSettings(info, coords, modules);
	}
	@Override
	public void onEnable() {
		eventManager.add(HudListener.class, this);
		super.onEnable();
	}
	@Override
	public void onDisable() {
		eventManager.remove(HudListener.class, this);
		super.onDisable();
	}
	@Override
	public void onRenderHud(HudEvent event) {
		if (mc.currentScreen instanceof ClickGui) return;
		if (mc.currentScreen == dlindustries.vigillant.system.VigillantSystem.INSTANCE.clickGui) return;
		DrawContext context = event.context;
		float scaleFactor = (float) mc.getWindow().getScaleFactor();
		float invScale = 1.0f / scaleFactor;
		context.getMatrices().pushMatrix();
		context.getMatrices().scale(invScale, invScale);
		if (info.getValue() && mc.player != null) {
			String playerName = mc.player.getName().getString();
			NameProtect nameProtect =
					dlindustries.vigillant.system.VigillantSystem.INSTANCE.getModuleManager().getModule(
							NameProtect.class);
			if (nameProtect != null) {
				playerName = nameProtect.replaceName(playerName);
			}
			String serverName = (mc.getCurrentServerEntry() == null ? "None" : mc.getCurrentServerEntry().address);
			String buildString = "System";
			String hudText = String.format("%s | %s | %s | %d FPS", buildString, playerName, serverName, mc.getCurrentFps());
			int textX = 15;
			int textY = 15;
			int textWidth = TextRenderer.getWidth(hudText);
			int textHeight = mc.textRenderer.fontHeight;
			int bgPadding = 8;
			context.fill(
					textX - bgPadding,
					textY - bgPadding,
					textX + textWidth + bgPadding,
					textY + textHeight + bgPadding,
					new Color(0, 0, 0, 175).getRGB()
			);
			TextRenderer.drawString(
					hudText,
					context,
					textX,
					textY,
					Utils.getMainColor(255, 4).getRGB()
			);
		}
		if (coords.getValue() && mc.player != null) {
			int x = (int) mc.player.getX();
			int y = (int) mc.player.getY();
			int z = (int) mc.player.getZ();
			String coordText = String.format("XYZ: %d / %d / %d", x, y, z);
			int textX = 15;
			int textY = 45;
			int textWidth = TextRenderer.getWidth(coordText);
			int textHeight = mc.textRenderer.fontHeight;
			int bgPadding = 8;
			context.fill(
					textX - bgPadding,
					textY - bgPadding,
					textX + textWidth + bgPadding,
					textY + textHeight + bgPadding,
					new Color(0, 0, 0, 175).getRGB()
			);
			TextRenderer.drawString(
					coordText,
					context,
					textX,
					textY,
					Utils.getMainColor(255, 4).getRGB()
			);
		}
		if (modules.getValue()) {
			int offset = 120;
			List<Module> enabledModules = dlindustries.vigillant.system.VigillantSystem.INSTANCE
					.getModuleManager()
					.getEnabledModules()
					.stream()
					.sorted((m1, m2) ->
							Integer.compare(
									TextRenderer.getWidth(m2.getName()),
									TextRenderer.getWidth(m1.getName())
							)
					)
					.toList();
			for (Module module : enabledModules) {
				int charOffset = 6 + TextRenderer.getWidth(module.getName());
				context.fill(
						0,
						offset - 4,
						charOffset + 5,
						offset + (mc.textRenderer.fontHeight * 2) - 1,
						new Color(0, 0, 0, 175).getRGB()
				);
				context.fillGradient(
						0,
						offset - 4,
						2,
						offset + (mc.textRenderer.fontHeight * 2),
						Utils.getMainColor(255, enabledModules.indexOf(module)).getRGB(),
						Utils.getMainColor(255, enabledModules.indexOf(module) + 1).getRGB()
				);
				TextRenderer.drawString(
						module.getName(),
						context,
						8,
						offset,
						Utils.getMainColor(255, enabledModules.indexOf(module)).getRGB()
				);
				offset += (mc.textRenderer.fontHeight * 2) + 3;
			}
		}
		context.getMatrices().popMatrix();
	}
}