package dlindustries.vigillant.system.gui;

import dlindustries.vigillant.system.system;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.modules.client.ClickGUI;
import dlindustries.vigillant.system.utils.ColorUtils;
import dlindustries.vigillant.system.utils.RenderUtils;
import dlindustries.vigillant.system.utils.TextRenderer;
import dlindustries.vigillant.system.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static dlindustries.vigillant.system.system.mc;

public final class ClickGui extends Screen {
	public List<Window> windows = new ArrayList<>();
	public Color currentColor;

	private static final Identifier BACKGROUND_IMAGE = Identifier.of("immediatelyfast", "images/background-army.png");

	public ClickGui() {
		super(Text.empty());
		int offsetX = 50;
		for (Category category : Category.values()) {
			windows.add(new Window(offsetX, 50, 230, 30, category, this));
			offsetX += 250;
		}
	}


	public boolean isDraggingAlready() {
		for (Window window : windows)
			if (window.dragging)
				return true;
		return false;
	}


	// Add to ClickGui.java
	private void renderPlayerName(DrawContext context) {
		if (mc.player == null || mc.getWindow() == null) return;

		RenderUtils.scaledProjection();

		// Player Name
		String playerName = mc.player.getName().getString();
		String playerText = "Player | " + playerName;

		// System Text
		String systemText = "Credits: Deepseek R1, GPT 3.5 turbo, Manus, DL-industries";

		int screenWidth = mc.getWindow().getScaledWidth();
		int screenHeight = mc.getWindow().getScaledHeight();

		int textWidthPlayer = TextRenderer.getWidth(playerText);
		int textWidthSystem = TextRenderer.getWidth(systemText);
		int textHeight = 9;  // Default text height for Minecraft font

		float scale = 0.5f;  // Smaller scale for both texts

		int scaledTextWidthPlayer = Math.round(textWidthPlayer * scale);
		int scaledTextHeight = Math.round(textHeight * scale);
		int scaledTextWidthSystem = Math.round(textWidthSystem * scale);
		int padding = 3;

		// Position for Player Text (Bottom-Right)
		int xPlayer = screenWidth - scaledTextWidthPlayer - padding;
		int yPlayer = screenHeight - scaledTextHeight - padding;

		// Position for System Text (Bottom-Left)
		int xSystem = padding;
		int ySystem = screenHeight - scaledTextHeight - padding;

		context.getMatrices().push();

		// Player Background
		context.getMatrices().translate(xPlayer, yPlayer, 0);
		context.getMatrices().scale(scale, scale, 1);
		RenderUtils.renderRoundedQuad(
				context.getMatrices(),
				new Color(20, 20, 20, 150),
				-2,
				-1,
				scaledTextWidthPlayer + 4,
				scaledTextHeight + 2,
				2,
				3
		);

		// Player Text
		TextRenderer.drawString(
				playerText,
				context,
				0,
				0,
				Utils.getMainColor(255, 0).getRGB()
		);

		// Reset translation and scale before rendering System text
		context.getMatrices().pop();
		context.getMatrices().push();

		// System Background (separate from player)
		context.getMatrices().translate(xSystem, ySystem, 0);  // Correctly translate for system text
		context.getMatrices().scale(scale, scale, 1);
		RenderUtils.renderRoundedQuad(
				context.getMatrices(),
				new Color(20, 20, 20, 150),
				-2,
				-1,
				scaledTextWidthSystem + 4,
				scaledTextHeight + 2,
				2,
				3
		);

		// System Text
		TextRenderer.drawString(
				systemText,
				context,
				0,
				0,
				Utils.getMainColor(255, 0).getRGB()
		);

		// Restore projection
		context.getMatrices().pop();
		RenderUtils.unscaledProjection();
	}







	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		if (mc.currentScreen == this) {
			if (system.INSTANCE.previousScreen != null)
				system.INSTANCE.previousScreen.render(context, 0, 0, delta);

			// Background fade transition
			if (currentColor == null)
				currentColor = new Color(0, 0, 0, 0);
			else
				currentColor = new Color(0, 0, 0, currentColor.getAlpha());

			if (currentColor.getAlpha() != (ClickGUI.background.getValue() ? 200 : 0))
				currentColor = ColorUtils.smoothAlphaTransition(0.05F, ClickGUI.background.getValue() ? 200 : 0, currentColor);

			// Draw semi-transparent background
			if (mc.currentScreen instanceof ClickGui)
				context.fill(0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight(), currentColor.getRGB());

			// Background image rendering
			if (ClickGUI.backgroundImage.getValue()) {
				RenderUtils.unscaledProjection();
				int imageWidth = 699;
				int imageHeight = 357;
				int screenWidth = mc.getWindow().getWidth();
				int screenHeight = mc.getWindow().getHeight();
				int imageX = (screenWidth - imageWidth) / 2;
				int imageY = screenHeight - imageHeight;

				context.drawTexture(
						BACKGROUND_IMAGE,
						imageX, imageY,
						0, 0,
						imageWidth, imageHeight,
						imageWidth, imageHeight
				);
				RenderUtils.scaledProjection();
			}

			// Main GUI rendering
			RenderUtils.unscaledProjection();
			mouseX *= (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
			mouseY *= (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
			super.render(context, mouseX, mouseY, delta);

			for (Window window : windows) {
				window.render(context, mouseX, mouseY, delta);
				window.updatePosition(mouseX, mouseY, delta);
			}

			// Render player name LAST (so it appears on top)
			renderPlayerName(context);

			RenderUtils.scaledProjection();
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		for (Window window : windows)
			window.keyPressed(keyCode, scanCode, modifiers);
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		mouseX *= (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
		mouseY *= (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
		for (Window window : windows)
			window.mouseClicked(mouseX, mouseY, button);
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		mouseX *= (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
		mouseY *= (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
		for (Window window : windows)
			window.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		mouseY *= MinecraftClient.getInstance().getWindow().getScaleFactor();
		for (Window window : windows)
			window.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void close() {
		system.INSTANCE.getModuleManager().getModule(ClickGUI.class).setEnabledStatus(false);
		onGuiClose();
	}

	public void onGuiClose() {
		mc.setScreenAndRender(system.INSTANCE.previousScreen);
		currentColor = null;
		for (Window window : windows)
			window.onGuiClose();
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		mouseX *= (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
		mouseY *= (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
		for (Window window : windows)
			window.mouseReleased(mouseX, mouseY, button);
		return super.mouseReleased(mouseX, mouseY, button);
	}
}