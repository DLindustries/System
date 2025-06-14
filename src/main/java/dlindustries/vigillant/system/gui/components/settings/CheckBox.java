package dlindustries.vigillant.system.gui.components.settings;

import dlindustries.vigillant.system.gui.components.ModuleButton;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.Setting;
import dlindustries.vigillant.system.utils.ColorUtils;
import dlindustries.vigillant.system.utils.TextRenderer;
import dlindustries.vigillant.system.utils.Utils;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public final class CheckBox extends RenderableSetting {
	private final BooleanSetting setting;
	private Color currentAlpha;

	public CheckBox(ModuleButton parent, Setting<?> setting, int offset) {
		super(parent, setting, offset);
		this.setting = (BooleanSetting) setting;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		int nameOffset = parentX() + 31;
		CharSequence chars = setting.getName();

		TextRenderer.drawString(chars, context, nameOffset, (parentY() + parentOffset() + offset) + 9, new Color(245, 245, 245, 255).getRGB());

		// Draw checkbox border (16x16)
		context.fill((parentX() + 7), (parentY() + parentOffset() + offset) + parentHeight() / 2 - 8, (parentX() + 23), (parentY() + parentOffset() + offset) + parentHeight() / 2 + 8, new Color(100, 100, 100, 255).getRGB());

		// Draw checkbox fill (14x14)
		context.fill((parentX() + 8), (parentY() + parentOffset() + offset) + parentHeight() / 2 - 7, (parentX() + 22), (parentY() + parentOffset() + offset) + parentHeight() / 2 + 7, setting.getValue() ? Utils.getMainColor(255, parent.settings.indexOf(this)).getRGB() : new Color(50, 50, 50, 255).getRGB());

		// Draw checkmark if checked (8x8 square)
		if (setting.getValue()) {
			context.fill((parentX() + 11), (parentY() + parentOffset() + offset) + parentHeight() / 2 - 4, (parentX() + 19), (parentY() + parentOffset() + offset) + parentHeight() / 2 + 4, new Color(245, 245, 245, 255).getRGB());
		}

		if (!parent.parent.dragging) {
			int toHoverAlpha = isHovered(mouseX, mouseY) ? 15 : 0;

			if (currentAlpha == null)
				currentAlpha = new Color(255, 255, 255, toHoverAlpha);
			else currentAlpha = new Color(255, 255, 255, currentAlpha.getAlpha());

			if (currentAlpha.getAlpha() != toHoverAlpha)
				currentAlpha = ColorUtils.smoothAlphaTransition(0.05F, toHoverAlpha, currentAlpha);

			context.fill(parentX(), parentY() + parentOffset() + offset, parentX() + parentWidth(), parentY() + parentOffset() + offset + parentHeight(), currentAlpha.getRGB());
		}
	}

	@Override
	public void keyPressed(int keyCode, int scanCode, int modifiers) {
		if(mouseOver && parent.extended) {
			if(keyCode == GLFW.GLFW_KEY_BACKSPACE)
				setting.setValue(setting.getOriginalValue());
		}

		super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void mouseClicked(double mouseX, double mouseY, int button) {
		if (isHovered(mouseX, mouseY) && button == GLFW.GLFW_MOUSE_BUTTON_LEFT)
			setting.toggle();

		super.mouseClicked(mouseX, mouseY, button);
	}
}
