package dlindustries.vigillant.system.utils;

import net.minecraft.client.gui.DrawContext;
import static dlindustries.vigillant.system.VigillantSystem.mc;

public final class TextRenderer {
	public static void drawString(CharSequence string, DrawContext context, int x, int y, int color) {
		drawMinecraftText(string, context, x, y, color);
	}
	public static int getWidth(CharSequence string) {
		String s = string == null ? "" : string.toString();
		return mc.textRenderer.getWidth(s) * 2;
	}
	public static void drawCenteredString(CharSequence string, DrawContext context, int x, int y, int color) {
		drawCenteredMinecraftText(string, context, x, y, color);
	}
	public static void drawLargeString(CharSequence string, DrawContext context, int x, int y, int color) {
		drawLargerMinecraftText(string, context, x, y, color);
	}
	public static void drawMinecraftText(CharSequence string, DrawContext context, int x, int y, int color) {
		var matrices = context.getMatrices();
		matrices.pushMatrix();
		matrices.scale(2f, 2f);
		context.drawText(mc.textRenderer, string == null ? "" : string.toString(), x / 2, y / 2, color, false);
		matrices.popMatrix();
	}
	public static void drawLargerMinecraftText(CharSequence string, DrawContext context, int x, int y, int color) {
		var matrices = context.getMatrices();
		matrices.pushMatrix();
		matrices.scale(3f, 3f);
		context.drawText(mc.textRenderer, string == null ? "" : string.toString(), x / 3, y / 3, color, false);
		matrices.popMatrix();
	}
	public static void drawCenteredMinecraftText(CharSequence string, DrawContext context, int x, int y, int color) {
		var matrices = context.getMatrices();
		matrices.pushMatrix();
		matrices.scale(2f, 2f);
		String s = string == null ? "" : string.toString();
		context.drawText(mc.textRenderer, s, (x / 2) - (mc.textRenderer.getWidth(s) / 2), y / 2, color, false);
		matrices.popMatrix();
	}
}