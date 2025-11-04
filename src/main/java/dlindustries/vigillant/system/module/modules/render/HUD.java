package dlindustries.vigillant.system.module.modules.render;

import dlindustries.vigillant.system.event.events.HudListener;
import dlindustries.vigillant.system.gui.ClickGui;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.modules.client.ClickGUI;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.ModeSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.RenderUtils;
import dlindustries.vigillant.system.utils.TextRenderer;
import dlindustries.vigillant.system.utils.Utils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.util.List;

public final class HUD extends Module implements HudListener {
	public enum ImageOption {
		CAT("cat.png"),
		COLD("cold.png"),
		FRIEREN("frieren.png"),
		JOE("joe.png"),
		SKIBIDI("skibidi.png"),
		JOHN("john.png"),
		NUMBERCRAC("67.png");


		private final String fileName;

		ImageOption(String fileName) {
			this.fileName = fileName;
		}

		public String getFileName() {
			return fileName;
		}
	}

	private static final CharSequence system = EncryptedString.of("System |");
	private final BooleanSetting info = new BooleanSetting(EncryptedString.of("Info"), true);
	private final BooleanSetting modules = new BooleanSetting("Modules", false)
			.setDescription(EncryptedString.of("Renders module array list"));


	private final ModeSetting<ImageOption> imageMode = new ModeSetting<>(
			EncryptedString.of("Image"),
			ImageOption.CAT,
			ImageOption.class
	).setDescription(EncryptedString.of("Choose which image to display in HUD"));

	public HUD() {
		super(EncryptedString.of("HUD"),
				EncryptedString.of("Overlay the system as you play"),
				-1,
				Category.RENDER);
		addSettings(info, modules, imageMode);
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
		if (mc.currentScreen != dlindustries.vigillant.system.system.INSTANCE.clickGui) {
			DrawContext context = event.context;
			boolean customFont = ClickGUI.customFont.getValue();

			if (!(mc.currentScreen instanceof ClickGui)) {
				if (info.getValue() && mc.player != null) {
					RenderUtils.unscaledProjection();


					ImageOption selectedImage = ImageOption.CAT;
					try {
						selectedImage = imageMode.getMode();
						if (selectedImage == null) selectedImage = ImageOption.CAT;
					} catch (Exception ignored) {

						selectedImage = ImageOption.CAT;
					}

					String imageName = selectedImage.getFileName();
					Identifier hudIconId = Identifier.of("system", "images/" + imageName);

					int iconSize = 60;
					int iconX = 10;
					int iconY = 10;
					int cornerRadius = 8;
					int cornerSegments = 15;


					String playerName = mc.player.getName().getString();
					dlindustries.vigillant.system.module.modules.client.NameProtect nameProtect =
							dlindustries.vigillant.system.system.INSTANCE.getModuleManager().getModule(
									dlindustries.vigillant.system.module.modules.client.NameProtect.class);
					if (nameProtect != null) {
						playerName = nameProtect.replaceName(playerName);
					}

					String serverName = (mc.getCurrentServerEntry() == null ? "None" : mc.getCurrentServerEntry().address);
					String buildString = "System";
					String hudText = String.format("%s | %s | %s", buildString, playerName, serverName);


					int textX = iconX + iconSize + 10;
					int textY = iconY + (iconSize - mc.textRenderer.fontHeight) / 2;


					int bgPaddingX = 10;
					int bgPaddingY = 10;
					int textWidth = TextRenderer.getWidth(hudText);

					RenderUtils.renderRoundedQuad(
							context.getMatrices(),
							new Color(35, 35, 35, 180),
							iconX - bgPaddingX,
							iconY - bgPaddingY,
							textX + textWidth + bgPaddingX,
							iconY + iconSize + bgPaddingY,
							5,
							15
					);


					context.drawTexture(
							hudIconId,
							iconX,
							iconY,
							0,
							0,
							iconSize,
							iconSize,
							iconSize,
							iconSize
					);

					TextRenderer.drawString(
							hudText,
							context,
							textX,
							textY,
							Utils.getMainColor(255, 4).getRGB()
					);

					RenderUtils.scaledProjection();
				}

				if (modules.getValue()) {
					int offset = 120;
					List<Module> enabledModules = dlindustries.vigillant.system.system.INSTANCE
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
						RenderUtils.unscaledProjection();

						int charOffset = 6 + TextRenderer.getWidth(module.getName());

						RenderUtils.renderRoundedQuad(
								context.getMatrices(),
								new Color(0, 0, 0, 175),
								0,
								offset - 4,
								charOffset + 5,
								offset + (mc.textRenderer.fontHeight * 2) - 1,
								0,
								0
						);

						context.fillGradient(
								0,
								offset - 4,
								2,
								offset + (mc.textRenderer.fontHeight * 2),
								Utils.getMainColor(255, enabledModules.indexOf(module)).getRGB(),
								Utils.getMainColor(255, enabledModules.indexOf(module) + 1).getRGB()
						);

						int charOffset2 = customFont ? 5 : 8;
						TextRenderer.drawString(
								module.getName(),
								context,
								charOffset2,
								offset + (customFont ? 1 : 0),
								Utils.getMainColor(255, enabledModules.indexOf(module)).getRGB()
						);

						offset += (mc.textRenderer.fontHeight * 2) + 3;
						RenderUtils.scaledProjection();
					}
				}
			}
		}
	}

}