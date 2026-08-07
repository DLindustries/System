package dlindustries.vigillant.system.gui;

import dlindustries.vigillant.system.VigillantSystem;
import dlindustries.vigillant.system.gui.components.ModuleButton;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.modules.client.ClickGUI;
import dlindustries.vigillant.system.utils.*;
import net.minecraft.client.gui.DrawContext;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import static dlindustries.vigillant.system.VigillantSystem.mc;

public final class Window {
	public List<ModuleButton> moduleButtons = new ArrayList<>();
	public int x;
	public int y;
	private final int width;
	private final int height;
	public Color currentColor;
	private final Category category;
	public boolean dragging, extended;
	private int dragX, dragY;
	public ClickGui parent;
	public Window(int x, int y, int width, int height, Category category, ClickGui parent) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.dragging = false;
		this.extended = true;
		this.category = category;
		this.parent = parent;
		int offset = height;
		List<Module> sortedModules = new ArrayList<>(VigillantSystem.INSTANCE.getModuleManager().getModulesInCategory(category));
		System.out.println("Window for " + category.name + " has " + sortedModules.size() + " modules.");
		for (Module module : sortedModules) {
			moduleButtons.add(new ModuleButton(this, module, offset));
			offset += height;
		}
	}
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		int toAlpha = ClickGUI.alphaWindow.getValueInt();
		if (currentColor == null)
			currentColor = new Color(0, 0, 0, 0);
		else
			currentColor = new Color(0, 0, 0, currentColor.getAlpha());
		if (currentColor.getAlpha() != toAlpha)
			currentColor = ColorUtils.smoothAlphaTransition(0.05F, toAlpha, currentColor);
		updateButtons(delta);
		int contentHeight = getContentHeight();
		Color headerColor = new Color(0, 0, 0, Math.min(255, currentColor.getAlpha() + 25));
		Color outlineColor = new Color(90, 90, 90, Math.min(255, currentColor.getAlpha() + 40));
		double scaledRound = ClickGUI.roundQuads.getValue() * (double) mc.getWindow().getScaleFactor();
		RenderUtils.renderRoundedQuad(
				context.getMatrices(),
				currentColor,
				x, y, x + width, y + contentHeight,
				scaledRound, scaledRound, scaledRound, scaledRound,
				50
		);
		RenderUtils.renderRoundedQuad(
				context.getMatrices(),
				headerColor,
				x, y, x + width, y + height,
				scaledRound, scaledRound, 0, 0,
				50
		);
		RenderUtils.renderRoundedOutline(
				context,
				outlineColor,
				x, y, x + width, y + contentHeight,
				scaledRound, scaledRound, scaledRound, scaledRound,
				1.2, 30
		);
		context.fill(x, y + (height - 2), x + width, y + height, Utils.getMainColor(255, 0).getRGB());
		int totalWidth = TextRenderer.getWidth(category.name);
		int startX = x + (width / 2) - (totalWidth / 2);
		TextRenderer.drawString(category.name, context, startX, y + 6, Color.WHITE.getRGB());
		for (ModuleButton moduleButton : moduleButtons)
			moduleButton.render(context, mouseX, mouseY, delta);
	}
	public void keyPressed(int keyCode, int scanCode, int modifiers) {
		for (ModuleButton moduleButton : moduleButtons)
			moduleButton.keyPressed(keyCode, scanCode, modifiers);
	}
	public void onGuiClose() {
		currentColor = null;
		for (ModuleButton moduleButton : moduleButtons)
			moduleButton.onGuiClose();
		dragging = false;
	}
	public boolean isDraggingAlready() {
		for (Window window : parent.windows)
			if (window.dragging)
				return true;
		return false;
	}
	public void mouseClicked(double mouseX, double mouseY, int button) {
		if (isHovered(mouseX, mouseY)) {
			switch (button) {
				case 0:
					if (!parent.isDraggingAlready()) {
						dragging = true;
						dragX = (int) (mouseX - x);
						dragY = (int) (mouseY - y);
					}
					break;
				case 1:
					break;
			}
		}
		if (extended) {
			for (ModuleButton moduleButton : moduleButtons)
				moduleButton.mouseClicked(mouseX, mouseY, button);
		}
	}
	public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (extended) {
			for (ModuleButton moduleButton : moduleButtons)
				moduleButton.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
		}
	}
	public void updateButtons(float delta) {
		int offset = height;
		double speed;
		switch (ClickGUI.animationMode.getMode()) {
			case Positive -> speed = 1.5 * delta;  // noticeably faster
			case Off      -> speed = 10.0 * delta; // instant-ish
			default       -> speed = 0.5 * delta;  // Normal
		}
		for (ModuleButton moduleButton : moduleButtons) {
			moduleButton.animation.animate(speed,
					moduleButton.extended ? height * (moduleButton.settings.size() + 1) : height);

			double supHeight = moduleButton.animation.getValue();
			moduleButton.offset = offset;

			offset += Math.max(height, (int) supHeight);
		}
	}
	public void mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0 && dragging)
			dragging = false;
		for (ModuleButton moduleButton : moduleButtons)
			moduleButton.mouseReleased(mouseX, mouseY, button);
	}
	public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		this.setY((int) (y + (verticalAmount * 20)));
	}
	public boolean isContentHovered(double mouseX, double mouseY) {
		return mouseX > x
				&& mouseX < x + width
				&& mouseY > y
				&& mouseY < y + getContentHeight();
	}
	private int getContentHeight() {
		int total = height;
		for (ModuleButton moduleButton : moduleButtons) {
			total += Math.max(height, (int) moduleButton.animation.getValue());
		}
		return total;
	}
	public int getX() { return x; }
	public int getY() { return y; }
	public void setY(int y) { this.y = y; }
	public void setX(int x) { this.x = x; }
	public int getWidth() { return width; }
	public int getHeight() { return height; }
	public boolean isHovered(double mouseX, double mouseY) {
		return mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + height;
	}
	public boolean isPrevHovered(double mouseX, double mouseY) {
		return isHovered(mouseX, mouseY);
	}
	public void updatePosition(double mouseX, double mouseY, float delta) {
		if (dragging) {
			x = (int) MathUtils.goodLerp((float) 0.3 * delta, x, mouseX - dragX);
			y = (int) MathUtils.goodLerp((float) 0.3 * delta, y, mouseY - dragY);
		}
	}
}