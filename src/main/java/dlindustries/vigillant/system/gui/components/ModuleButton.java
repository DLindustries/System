package dlindustries.vigillant.system.gui.components;

import dlindustries.vigillant.system.VigillantSystem;
import dlindustries.vigillant.system.gui.ClickGui;
import dlindustries.vigillant.system.gui.Window;
import dlindustries.vigillant.system.gui.components.settings.*;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.modules.client.ClickGUI;
import dlindustries.vigillant.system.module.setting.*;
import dlindustries.vigillant.system.utils.*;
import net.minecraft.client.gui.DrawContext;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static dlindustries.vigillant.system.VigillantSystem.mc;

public final class ModuleButton {
	public List<RenderableSetting> settings = new ArrayList<>();
	public Window parent;
	public Module module;
	public int offset;
	public boolean extended;
	public int settingOffset;
	public Color currentColor;
	public Color defaultColor = Color.WHITE;
	public Color currentAlpha;
	private Color searchHighlightColor = new Color(255, 255, 255, 0);
	public AnimationUtils animation = new AnimationUtils(0);
	public ModuleButton(Window parent, Module module, int offset) {
		this.parent = parent;
		this.module = module;
		this.offset = offset;
		this.extended = false;
		settingOffset = parent.getHeight();
		for (Setting<?> setting : module.getSettings()) {
			if (setting instanceof BooleanSetting booleanSetting)
				settings.add(new CheckBox(this, booleanSetting, settingOffset));
			else if (setting instanceof NumberSetting numberSetting)
				settings.add(new Slider(this, numberSetting, settingOffset));
			else if (setting instanceof ModeSetting<?> modeSetting)
				settings.add(new ModeBox(this, modeSetting, settingOffset));
			else if (setting instanceof KeybindSetting keybindSetting)
				settings.add(new KeybindBox(this, keybindSetting, settingOffset));
			else if (setting instanceof StringSetting stringSetting)
				settings.add(new StringBox(this, stringSetting, settingOffset));
			else if (setting instanceof MinMaxSetting minMaxSetting)
				settings.add(new MinMaxSlider(this, minMaxSetting, settingOffset));
			settingOffset += parent.getHeight();
		}
	}
	private String getSearchQuery() {
		if (parent.parent instanceof ClickGui clickGui)
			return clickGui.getSearchQuery();
		return "";
	}
	private boolean matchesSearch(String query) {
		if (query.isEmpty()) return false;
		return module.getName().toString().toLowerCase().contains(query.toLowerCase());
	}
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		for (RenderableSetting renderableSetting : settings)
			renderableSetting.onUpdate();

		if (currentColor == null)
			currentColor = new Color(0, 0, 0, 0);
		else currentColor = new Color(0, 0, 0, currentColor.getAlpha());
		int toAlpha = ClickGUI.alphaWindow.getValueInt();
		currentColor = ColorUtils.smoothAlphaTransition(0.05F, toAlpha, currentColor);
		Color toColor = module.isEnabled()
				? Utils.getMainColor(255, VigillantSystem.INSTANCE.getModuleManager()
				.getModulesInCategory(module.getCategory()).indexOf(module))
				: Color.WHITE;

		if (defaultColor != toColor)
			defaultColor = ColorUtils.smoothColorTransition(0.1F, toColor, defaultColor);
		int moduleIndex = VigillantSystem.INSTANCE.getModuleManager()
				.getModulesInCategory(module.getCategory()).indexOf(module);
		context.fill(
				parent.getX(),
				parent.getY() + offset,
				parent.getX() + parent.getWidth(),
				parent.getY() + parent.getHeight() + offset,
				currentColor.getRGB()
		);
		String query = getSearchQuery();
		boolean matches = matchesSearch(query);
		if (!query.isEmpty()) {
			if (matches) {
				searchHighlightColor = ColorUtils.smoothAlphaTransition(
						0.08F, 55, searchHighlightColor);
				Color themeBase  = Utils.getMainColor(searchHighlightColor.getAlpha(), moduleIndex);
				Color themeBase2 = Utils.getMainColor(searchHighlightColor.getAlpha(), moduleIndex + 1);
				context.fillGradient(
						parent.getX(),
						parent.getY() + offset,
						parent.getX() + parent.getWidth(),
						parent.getY() + parent.getHeight() + offset,
						themeBase.getRGB(),
						themeBase2.getRGB()
				);
				context.fillGradient(
						parent.getX(),
						parent.getY() + offset,
						parent.getX() + 3,
						parent.getY() + parent.getHeight() + offset,
						Utils.getMainColor(255, moduleIndex).getRGB(),
						Utils.getMainColor(255, moduleIndex + 1).getRGB()
				);
			} else {
				searchHighlightColor = ColorUtils.smoothAlphaTransition(
						0.08F, 0, searchHighlightColor);
				context.fill(
						parent.getX(),
						parent.getY() + offset,
						parent.getX() + parent.getWidth(),
						parent.getY() + parent.getHeight() + offset,
						new Color(0, 0, 0, 110).getRGB()
				);
				context.fillGradient(
						parent.getX(),
						parent.getY() + offset,
						parent.getX() + 2,
						parent.getY() + parent.getHeight() + offset,
						Utils.getMainColor(140, moduleIndex).getRGB(),
						Utils.getMainColor(140, moduleIndex + 1).getRGB()
				);
			}
		} else {
			searchHighlightColor = ColorUtils.smoothAlphaTransition(
					0.08F, 0, searchHighlightColor);
			context.fillGradient(
					parent.getX(),
					parent.getY() + offset,
					parent.getX() + 2,
					parent.getY() + parent.getHeight() + offset,
					Utils.getMainColor(255, moduleIndex).getRGB(),
					Utils.getMainColor(255, moduleIndex + 1).getRGB()
			);
		}
		if (parent.moduleButtons.get(parent.moduleButtons.size() - 1) == this) {
			context.fillGradient(
					parent.getX(),
					parent.getY() + offset + parent.getHeight() - 2,
					parent.getX() + parent.getWidth(),
					parent.getY() + offset + parent.getHeight(),
					Utils.getMainColor(255, moduleIndex).getRGB(),
					Utils.getMainColor(255, moduleIndex + 1).getRGB()
			);
		}
		CharSequence nameChars  = module.getName();
		int totalWidth          = TextRenderer.getWidth(nameChars);
		int parentCenterX       = parent.getX() + parent.getWidth() / 2;
		int textCenterX         = parentCenterX - totalWidth / 2;
		int nameColor = (!query.isEmpty() && !matchesSearch(query))
				? new Color(160, 160, 160, 200).getRGB()
				: defaultColor.getRGB();
		TextRenderer.drawString(nameChars, context, textCenterX, parent.getY() + offset + 8, nameColor);
		renderHover(context, mouseX, mouseY, delta);
		renderSettings(context, mouseX, mouseY, delta);
		for (RenderableSetting renderableSetting : settings)
			if (extended) renderableSetting.renderDescription(context, mouseX, mouseY, delta);
		if (isHovered(mouseX, mouseY) && !parent.dragging) {
			CharSequence chars      = module.getDescription();
			int tw                  = TextRenderer.getWidth(chars);
			int parentCenter        = mc.getWindow().getFramebufferWidth() / 2;
			int textCenter          = parentCenter - tw / 2;
			RenderUtils.renderRoundedQuad(
					context.getMatrices(),
					new Color(100, 100, 100, 100),
					textCenter - 5,
					((double) mc.getWindow().getFramebufferHeight() / 2) + 294,
					textCenter + tw + 5,
					((double) mc.getWindow().getFramebufferHeight() / 2) + 318,
					3,
					10
			);
			TextRenderer.drawString(chars, context, textCenter,
					(mc.getWindow().getFramebufferHeight() / 2) + 300, Color.WHITE.getRGB());
		}
	}
	private void renderHover(DrawContext context, int mouseX, int mouseY, float delta) {
		if (!parent.dragging) {
			int toHoverAlpha = isHovered(mouseX, mouseY) ? 15 : 0;
			if (currentAlpha == null)
				currentAlpha = new Color(255, 255, 255, toHoverAlpha);
			else currentAlpha = new Color(255, 255, 255, currentAlpha.getAlpha());
			if (currentAlpha.getAlpha() != toHoverAlpha)
				currentAlpha = ColorUtils.smoothAlphaTransition(0.05F, toHoverAlpha, currentAlpha);
			context.fill(
					parent.getX(),
					parent.getY() + offset,
					parent.getX() + parent.getWidth(),
					parent.getY() + parent.getHeight() + offset,
					currentAlpha.getRGB()
			);
		}
	}
	private void renderSettings(DrawContext context, int mouseX, int mouseY, float delta) {
		boolean showSettings = extended;
		for (RenderableSetting renderableSetting : settings)
			if (showSettings)
				renderableSetting.render(context, mouseX, mouseY, delta);
		for (RenderableSetting renderableSetting : settings) {
			if (showSettings) {
				if (renderableSetting instanceof Slider slider) {
					RenderUtils.renderCircle(context.getMatrices(), new Color(0, 0, 0, 170),
							(slider.parentX() + (Math.max(slider.lerpedOffsetX, 2.5))),
							slider.parentY() + slider.offset + slider.parentOffset() + 27.5, 6, 15);
					RenderUtils.renderCircle(context.getMatrices(), slider.currentColor1.brighter(),
							(slider.parentX() + (Math.max(slider.lerpedOffsetX, 2.5))),
							slider.parentY() + slider.offset + slider.parentOffset() + 27.5, 5, 15);
				} else if (renderableSetting instanceof MinMaxSlider slider) {
					RenderUtils.renderCircle(context.getMatrices(), new Color(0, 0, 0, 170),
							(slider.parentX() + (Math.max(slider.lerpedOffsetMinX, 2.5))),
							slider.parentY() + slider.offset + slider.parentOffset() + 27.5, 6, 15);
					RenderUtils.renderCircle(context.getMatrices(), slider.currentColor1.brighter(),
							(slider.parentX() + (Math.max(slider.lerpedOffsetMinX, 2.5))),
							slider.parentY() + slider.offset + slider.parentOffset() + 27.5, 5, 15);
					RenderUtils.renderCircle(context.getMatrices(), new Color(0, 0, 0, 170),
							(slider.parentX() + (Math.max(slider.lerpedOffsetMaxX, 2.5))),
							slider.parentY() + slider.offset + slider.parentOffset() + 27.5, 6, 15);
					RenderUtils.renderCircle(context.getMatrices(), slider.currentColor1.brighter(),
							(slider.parentX() + (Math.max(slider.lerpedOffsetMaxX, 2.5))),
							slider.parentY() + slider.offset + slider.parentOffset() + 27.5, 5, 15);
				}
			}
		}
	}
	public void onExtend() {
		for (ModuleButton moduleButton : parent.moduleButtons)
			moduleButton.extended = false;
	}
	public void keyPressed(int keyCode, int scanCode, int modifiers) {
		for (RenderableSetting setting : settings)
			setting.keyPressed(keyCode, scanCode, modifiers);
	}
	public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (extended)
			for (RenderableSetting renderableSetting : settings)
				renderableSetting.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}
	public void mouseClicked(double mouseX, double mouseY, int button) {
		if (isHovered(mouseX, mouseY)) {
			if (button == 0)
				module.toggle();
			if (button == 1) {
				if (module.getSettings().isEmpty()) return;
				if (!extended)
					onExtend();
				extended = !extended;
			}
		}
		if (extended) {
			for (RenderableSetting renderableSetting : settings)
				renderableSetting.mouseClicked(mouseX, mouseY, button);
		}
	}
	public void onGuiClose() {
		this.currentAlpha        = null;
		this.currentColor        = null;
		this.searchHighlightColor = new Color(255, 255, 255, 0);

		for (RenderableSetting renderableSetting : settings)
			renderableSetting.onGuiClose();
	}
	public void mouseReleased(double mouseX, double mouseY, int button) {
		for (RenderableSetting renderableSetting : settings)
			renderableSetting.mouseReleased(mouseX, mouseY, button);
	}
	public boolean isHovered(double mouseX, double mouseY) {
		return mouseX > parent.getX()
				&& mouseX < parent.getX() + parent.getWidth()
				&& mouseY > parent.getY() + offset
				&& mouseY < parent.getY() + offset + parent.getHeight();
	}
}