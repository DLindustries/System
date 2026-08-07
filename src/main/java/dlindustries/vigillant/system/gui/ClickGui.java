package dlindustries.vigillant.system.gui;

import dlindustries.vigillant.system.VigillantSystem;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.modules.client.ClickGUI;
import dlindustries.vigillant.system.utils.ColorUtils;
import dlindustries.vigillant.system.utils.RenderUtils;
import dlindustries.vigillant.system.utils.TextRenderer;
import dlindustries.vigillant.system.utils.Utils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static dlindustries.vigillant.system.VigillantSystem.mc;

public final class ClickGui extends Screen {
	public List<Window> windows = new ArrayList<>();
	public Color currentColor;
	private TextFieldWidget searchField;
	private static final int SEARCH_BAR_WIDTH  = 120;
	private static final int SEARCH_BAR_HEIGHT = 14;
	private static final int SEARCH_BAR_Y      = 6;
	private static final Identifier BACKGROUND_IMAGE = Identifier.of("system", "images/ren.png");
	private static final int WINDOW_START_X      = 50;
	private static final int WINDOW_OFFSET_X     = 250;
	private static final int WINDOW_Y            = 50;
	private static final int WINDOW_WIDTH        = 230;
	private static final int WINDOW_TITLE_HEIGHT = 30;

	public ClickGui() {
		super(Text.empty());
		int offsetX = WINDOW_START_X;
		for (Category category : Category.values()) {
			windows.add(new Window(offsetX, WINDOW_Y, WINDOW_WIDTH, WINDOW_TITLE_HEIGHT, category, this));
			offsetX += WINDOW_OFFSET_X;
		}
	}
	@Override
	public void init() {
		int bx = (mc.getWindow().getScaledWidth() - SEARCH_BAR_WIDTH) / 2;

		searchField = new TextFieldWidget(
				mc.textRenderer,
				bx, SEARCH_BAR_Y,
				SEARCH_BAR_WIDTH, SEARCH_BAR_HEIGHT,
				Text.literal("Search modules...")
		);
		searchField.setMaxLength(50);
		searchField.setDrawsBackground(false);
		searchField.setPlaceholder(Text.literal("Search modules..."));
		addSelectableChild(searchField);
	}
	public String getSearchQuery() {
		return searchField != null ? searchField.getText() : "";
	}
	public boolean isDraggingAlready() {
		for (Window window : windows)
			if (window.dragging)
				return true;
		return false;
	}
	private void renderSearchBar(DrawContext context, int mouseX, int mouseY) {
		if (searchField == null) return;

		int bx      = searchField.getX();
		int by      = searchField.getY();
		int bw      = searchField.getWidth();
		int bh      = searchField.getHeight();
		boolean focused = searchField.isFocused();
		Color bg = focused
				? new Color(30, 30, 30, 210)
				: new Color(18, 18, 18, 180);
		Color border = focused
				? Utils.getMainColor(220, 0)
				: new Color(75, 75, 75, 160);
		RenderUtils.renderRoundedQuad(
				context.getMatrices(), bg,
				bx, by, bx + bw, by + bh,
				3, 50
		);
		RenderUtils.renderRoundedOutline(
				context, border,
				bx, by, bx + bw, by + bh,
				3, 3, 3, 3, 1.4, 30
		);
		if (focused) {
			context.fillGradient(
					bx + 4, by + bh - 2,
					bx + bw - 4, by + bh,
					Utils.getMainColor(200, 0).getRGB(),
					Utils.getMainColor(200, 3).getRGB()
			);
		}
		searchField.render(context, mouseX, mouseY, 0);
	}
	private void renderPlayerName(DrawContext context) {
		if (mc.player == null || mc.getWindow() == null) return;
		String playerName = mc.player.getName().getString();
		dlindustries.vigillant.system.module.modules.client.NameProtect nameProtect =
				VigillantSystem.INSTANCE.getModuleManager().getModule(
						dlindustries.vigillant.system.module.modules.client.NameProtect.class);
		if (nameProtect != null)
			playerName = nameProtect.replaceName(playerName);
		String playerText = "Player | " + playerName;
		String systemText = "Credits:Dsy3, DL-industries, Claude";
		int screenWidth  = mc.getWindow().getWidth();
		int screenHeight = mc.getWindow().getHeight();
		int textWidthPlayer = TextRenderer.getWidth(playerText);
		int textWidthSystem = TextRenderer.getWidth(systemText);
		int textHeight      = mc.textRenderer.fontHeight;
		float scale               = 1.0f;
		int scaledTextWidthPlayer = Math.round(textWidthPlayer * scale);
		int scaledTextHeight      = Math.round(textHeight * scale);
		int scaledTextWidthSystem = Math.round(textWidthSystem * scale);
		int padding = 3;
		int xPlayer = screenWidth  - scaledTextWidthPlayer - padding;
		int yPlayer = screenHeight - scaledTextHeight      - padding - 3;
		int xSystem = padding;
		int ySystem = screenHeight - scaledTextHeight      - padding - 3;
		int fgColor = Utils.getMainColor(255, 0).getRGB();
		context.getMatrices().pushMatrix();
		context.getMatrices().translate(xPlayer, yPlayer);
		context.getMatrices().scale(scale, scale);
		RenderUtils.renderRoundedQuad(context.getMatrices(), new Color(20, 20, 20, 150),
				-2, -1, scaledTextWidthPlayer + 4, scaledTextHeight + 2, 2, 3);
		TextRenderer.drawString(playerText, context, 0, 0, fgColor);
		context.getMatrices().popMatrix();
		context.getMatrices().pushMatrix();
		context.getMatrices().translate(xSystem, ySystem);
		context.getMatrices().scale(scale, scale);
		RenderUtils.renderRoundedQuad(context.getMatrices(), new Color(20, 20, 20, 150),
				-2, -1, scaledTextWidthSystem + 4, scaledTextHeight + 2, 2, 3);
		TextRenderer.drawString(systemText, context, 0, 0, fgColor);
		context.getMatrices().popMatrix();
	}
	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		if (mc.currentScreen != this) return;
		if (ClickGUI.blur.getValue()) mc.gameRenderer.renderBlur();
		if (VigillantSystem.INSTANCE.previousScreen != null)
			VigillantSystem.INSTANCE.previousScreen.render(context, 0, 0, delta);
		if (currentColor == null)
			currentColor = new Color(0, 0, 0, 0);
		else
			currentColor = new Color(0, 0, 0, currentColor.getAlpha());
		int targetAlpha = ClickGUI.background.getValue() ? ClickGUI.backgroundAlpha.getValueInt() : 0;
		if (currentColor.getAlpha() != targetAlpha)
			currentColor = ColorUtils.smoothAlphaTransition(0.05F, targetAlpha, currentColor);
		float scaleFactor = (float) mc.getWindow().getScaleFactor();
		float invScale    = 1.0f / scaleFactor;
		context.getMatrices().pushMatrix();
		context.getMatrices().scale(invScale, invScale);
		if (mc.currentScreen instanceof ClickGui)
			context.fill(0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight(), currentColor.getRGB());
		if (ClickGUI.backgroundImage.getValue()) {
			int imageWidth   = 699;
			int imageHeight  = 357;
			int screenWidth  = mc.getWindow().getWidth();
			int screenHeight = mc.getWindow().getHeight();
			context.drawTexture(RenderPipelines.GUI_TEXTURED, BACKGROUND_IMAGE,
					(screenWidth - imageWidth) / 2, screenHeight - imageHeight,
					0.0f, 0.0f, imageWidth, imageHeight, imageWidth, imageHeight);
		}
		int pixelMouseX = (int) (mouseX * scaleFactor);
		int pixelMouseY = (int) (mouseY * scaleFactor);
		for (Window window : windows) {
			window.updatePosition(pixelMouseX, pixelMouseY, delta);
			window.render(context, pixelMouseX, pixelMouseY, delta);
		}
		renderPlayerName(context);
		context.getMatrices().popMatrix();
		renderSearchBar(context, mouseX, mouseY);
	}
	@Override
	public boolean keyPressed(KeyInput keyInput) {
		if (searchField != null && searchField.isFocused()) {
			if (keyInput.key() == 256) {
				searchField.setText("");
				searchField.setFocused(false);
				setFocused(null);
				return true;
			}
			if (searchField.keyPressed(keyInput))
				return true;
		}
		for (Window window : windows)
			window.keyPressed(keyInput.key(), keyInput.scancode(), keyInput.modifiers());
		return super.keyPressed(keyInput);
	}
	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (searchField != null) {
			boolean overField = click.x() >= searchField.getX()
					&& click.x() <= searchField.getX() + searchField.getWidth()
					&& click.y() >= searchField.getY()
					&& click.y() <= searchField.getY() + searchField.getHeight();

			if (overField) {
				setFocused(searchField);
				searchField.setFocused(true);
				searchField.mouseClicked(click, doubled);
				return true;
			} else {
				setFocused(null);
				searchField.setFocused(false);
			}
		}
		double scaleFactor = mc.getWindow().getScaleFactor();
		int pixelX = (int) (click.x() * scaleFactor);
		int pixelY = (int) (click.y() * scaleFactor);
		for (Window window : windows)
			window.mouseClicked(pixelX, pixelY, click.button());
		return super.mouseClicked(click, doubled);
	}
	@Override
	public boolean mouseDragged(Click click, double deltaX, double deltaY) {
		double scaleFactor = mc.getWindow().getScaleFactor();
		int pixelX = (int) (click.x() * scaleFactor);
		int pixelY = (int) (click.y() * scaleFactor);
		for (Window window : windows)
			window.mouseDragged(pixelX, pixelY, click.button(), deltaX, deltaY);
		return super.mouseDragged(click, deltaX, deltaY);
	}
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		double scaleFactor = mc.getWindow().getScaleFactor();
		int pixelX = (int) (mouseX * scaleFactor);
		int pixelY = (int) (mouseY * scaleFactor);
		for (Window window : windows) {
			if (window.isContentHovered(pixelX, pixelY)) {
				window.mouseScrolled(pixelX, pixelY, horizontalAmount, verticalAmount);
				return true;
			}
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}
	@Override
	public boolean mouseReleased(Click click) {
		double scaleFactor = mc.getWindow().getScaleFactor();
		int pixelX = (int) (click.x() * scaleFactor);
		int pixelY = (int) (click.y() * scaleFactor);
		for (Window window : windows)
			window.mouseReleased(pixelX, pixelY, click.button());
		return super.mouseReleased(click);
	}
	@Override
	public boolean shouldPause() { return false; }
	@Override
	public void close() {
		ClickGUI clickGuiModule = VigillantSystem.INSTANCE.getModuleManager().getModule(ClickGUI.class);
		if (clickGuiModule != null && clickGuiModule.isEnabled()) {
			clickGuiModule.setEnabled(false);
			return;
		}
		onGuiClose();
	}
	public void onGuiClose() {
		mc.setScreenAndRender(VigillantSystem.INSTANCE.previousScreen);
		currentColor = null;
		if (searchField != null) {
			searchField.setText("");
			searchField.setFocused(false);
		}
		for (Window window : windows)
			window.onGuiClose();
	}
}