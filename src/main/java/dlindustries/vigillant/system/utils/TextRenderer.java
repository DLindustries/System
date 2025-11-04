package dlindustries.vigillant.system.utils;

import dlindustries.vigillant.system.font.Fonts;
import dlindustries.vigillant.system.module.modules.client.ClickGUI;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import static dlindustries.vigillant.system.system.mc;
public final class TextRenderer {

	public static void drawString(CharSequence string, DrawContext context, int x, int y, int color) {
		boolean custom = ClickGUI.customFont.getValue();
		if (custom)
			Fonts.QUICKSAND.drawString(context.getMatrices(), string, x, y - 8, color);
		else drawMinecraftText(string, context, x, y, color);
	}

	public static int getWidth(CharSequence string) {
		boolean custom = ClickGUI.customFont.getValue();
		if (custom)
			return Fonts.QUICKSAND.getStringWidth(string);
		else return mc.textRenderer.getWidth(string == null ? "" : string.toString()) * 2;
	}

	public static void drawCenteredString(CharSequence string, DrawContext context, int x, int y, int color) {
		boolean custom = ClickGUI.customFont.getValue();
		if (custom)
			Fonts.QUICKSAND.drawString(context.getMatrices(), string, (x - (Fonts.QUICKSAND.getStringWidth(string) / 2)), y - 8, color);
		else drawCenteredMinecraftText(string, context, x, y, color);
	}

	public static void drawLargeString(CharSequence string, DrawContext context, int x, int y, int color) {
		boolean custom = ClickGUI.customFont.getValue();
		if (custom) {
			MatrixStack matrices = context.getMatrices();
			matrices.push();

			matrices.scale(1.4f, 1.4f, 1.4f);
			Fonts.QUICKSAND.drawString(context.getMatrices(), string, x, y - 8, color);
			matrices.pop();
		} else
			drawLargerMinecraftText(string, context, x, y, color);
	}

	public static void drawMinecraftText(CharSequence string, DrawContext context, int x, int y, int color) {
		MatrixStack matrices = context.getMatrices();
		matrices.push();

		matrices.scale(2f, 2f, 2f);
		context.drawText(mc.textRenderer, string == null ? "" : string.toString(), (x) / 2, (y) / 2, color, false);
		matrices.pop();
	}

	public static void drawLargerMinecraftText(CharSequence string, DrawContext context, int x, int y, int color) {
		MatrixStack matrices = context.getMatrices();
		matrices.push();

		matrices.scale(3f, 3f, 3f);
		context.drawText(mc.textRenderer, string == null ? "" : string.toString(), (x) / 3, (y) / 3, color, false);
		matrices.pop();
	}

	public static void drawCenteredMinecraftText(CharSequence string, DrawContext context, int x, int y, int color) {
		MatrixStack matrices = context.getMatrices();
		matrices.push();

		matrices.scale(2f, 2f, 2f);
		String s = string == null ? "" : string.toString();
		context.drawText(mc.textRenderer, s, (x / 2) - (mc.textRenderer.getWidth(s) / 2), (y) / 2, color, false);
		matrices.pop();
	}
}
