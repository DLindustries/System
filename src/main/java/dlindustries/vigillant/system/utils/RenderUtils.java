package dlindustries.vigillant.system.utils;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import dlindustries.vigillant.system.module.modules.client.ClickGUI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import java.awt.*;
import java.util.function.Consumer;

import static dlindustries.vigillant.system.VigillantSystem.mc;
public final class RenderUtils {
	public static boolean rendering3D = true;
	public static Vec3d getCameraPos() {
		return mc.gameRenderer.getCamera().getFocusedEntity() != null
				? new Vec3d(mc.gameRenderer.getCamera().getFocusedEntity().getX(), mc.gameRenderer.getCamera().getFocusedEntity().getY(), mc.gameRenderer.getCamera().getFocusedEntity().getZ())
				: Vec3d.ZERO;
	}
	public static double deltaTime() {
		return mc.getCurrentFps() > 0 ? (1.0000 / mc.getCurrentFps()) : 1;
	}
	public static float fast(float end, float start, float multiple) {
		return (1 - MathHelper.clamp((float) (deltaTime() * multiple), 0, 1)) * end + MathHelper.clamp((float) (deltaTime() * multiple), 0, 1) * start;
	}
	public static Vec3d getPlayerLookVec(PlayerEntity player) {
		float f = 0.017453292F;
		float pi = 3.1415927F;
		float f1 = MathHelper.cos(-player.getYaw() * f - pi);
		float f2 = MathHelper.sin(-player.getYaw() * f - pi);
		float f3 = -MathHelper.cos(-player.getPitch() * f);
		float f4 = MathHelper.sin(-player.getPitch() * f);
		return (new Vec3d((f2 * f3), f4, (f1 * f3))).normalize();
	}
	public static void unscaledProjection() {
		rendering3D = false;
	}
	public static void scaledProjection() {
		rendering3D = true;
	}
	public static void renderRoundedQuad(MatrixStack matrices, Color c, double x, double y, double x2, double y2, double corner1, double corner2, double corner3, double corner4, double samples) {
		int color = c.getRGB();
		Matrix4f matrix = matrices.peek().getPositionMatrix();
		float f = (float) (color >> 24 & 255) / 255.0F;
		float g = (float) (color >> 16 & 255) / 255.0F;
		float h = (float) (color >> 8 & 255) / 255.0F;
		float k = (float) (color & 255) / 255.0F;
		GlStateManager._enableBlend();
		renderRoundedQuadInternal(matrix, g, h, k, f, x, y, x2, y2, corner1, corner2, corner3, corner4, samples);
		GlStateManager._enableCull();
		GlStateManager._disableBlend();
	}
	private static void setup() {
		GlStateManager._enableBlend();
	}
	private static void cleanup() {
		GlStateManager._enableCull();
		GlStateManager._disableBlend();
	}
	public static void renderRoundedQuad(MatrixStack matrices, Color c, double x, double y, double x1, double y1, double rad, double samples) {
		renderRoundedQuad(matrices, c, x, y, x1, y1, rad, rad, rad, rad, samples);
	}
	public static void renderRoundedQuad(Matrix3x2fStack matrices, Color c, double x, double y, double x2, double y2, double corner1, double corner2, double corner3, double corner4, double samples) {
		renderRoundedQuad(toMatrixStack(matrices), c, x, y, x2, y2, corner1, corner2, corner3, corner4, samples);
	}
	public static void renderRoundedQuad(Matrix3x2fStack matrices, Color c, double x, double y, double x1, double y1, double rad, double samples) {
		renderRoundedQuad(toMatrixStack(matrices), c, x, y, x1, y1, rad, samples);
	}
	public static void renderRoundedOutlineInternal(Matrix4f matrix, float cr, float cg, float cb, float ca, double fromX, double fromY, double toX, double toY, double radC1, double radC2, double radC3, double radC4, double width, double samples) {
		BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

		double[][] map = new double[][]{new double[]{toX - radC4, toY - radC4, radC4}, new double[]{toX - radC2, fromY + radC2, radC2},
				new double[]{fromX + radC1, fromY + radC1, radC1}, new double[]{fromX + radC3, toY - radC3, radC3}};
		for (int i = 0; i < 4; i++) {
			double[] current = map[i];
			double rad = current[2];
			for (double r = i * 90d; r < (360 / 4d + i * 90d); r += (90 / samples)) {
				float rad1 = (float) Math.toRadians(r);
				double sin1 = Math.sin(rad1);
				float sin = (float) (sin1 * rad);
				double cos1 = Math.cos(rad1);
				float cos = (float) (cos1 * rad);
				bufferBuilder.vertex(matrix, (float) current[0] + sin, (float) current[1] + cos, 0.0F).color(cr, cg, cb, ca);
				bufferBuilder.vertex(matrix, (float) (current[0] + sin + sin1 * width), (float) (current[1] + cos + cos1 * width), 0.0F).color(cr, cg, cb, ca);
			}
			float rad1 = (float) Math.toRadians((360 / 4d + i * 90d));
			double sin1 = Math.sin(rad1);
			float sin = (float) (sin1 * rad);
			double cos1 = Math.cos(rad1);
			float cos = (float) (cos1 * rad);
			bufferBuilder.vertex(matrix, (float) current[0] + sin, (float) current[1] + cos, 0.0F).color(cr, cg, cb, ca);
			bufferBuilder.vertex(matrix, (float) (current[0] + sin + sin1 * width), (float) (current[1] + cos + cos1 * width), 0.0F).color(cr, cg, cb, ca);
		}
		int i = 0;
		double[] current = map[i];
		double rad = current[2];
		float cos = (float) (rad);
		bufferBuilder.vertex(matrix, (float) current[0], (float) current[1] + cos, 0.0F).color(cr, cg, cb, ca);
		bufferBuilder.vertex(matrix, (float) (current[0]), (float) (current[1] + cos + width), 0.0F).color(cr, cg, cb, ca);
		BuiltBuffer built = bufferBuilder.end();
		RenderLayers.debugFilledBox().draw(built);
	}
	public static void renderFilledBox(MatrixStack matrices, Box box, Color color) {
		renderFilledBox(matrices,
				(float) box.minX, (float) box.minY, (float) box.minZ,
				(float) box.maxX, (float) box.maxY, (float) box.maxZ,
				color);
	}
	public static void renderFilledBox(MatrixStack matrices, float minX, float minY, float minZ,
									   float maxX, float maxY, float maxZ, Color color) {
		GlStateManager._disableDepthTest();
		GlStateManager._depthFunc(GL11.GL_ALWAYS);
		GlStateManager._enableBlend();

		float r = color.getRed() / 255f;
		float g = color.getGreen() / 255f;
		float b = color.getBlue() / 255f;
		float a = color.getAlpha() / 255f;

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

		MatrixStack.Entry entry = matrices.peek();
		Matrix4f matrix = entry.getPositionMatrix();
		buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
		buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
		buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
		buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
		buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
		buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
		buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
		buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
		buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
		buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
		buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
		buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
		buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
		buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
		buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
		buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
		buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
		buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
		buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
		buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
		buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
		buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
		buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
		buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);

		BuiltBuffer built = buffer.end();
		RenderLayers.debugFilledBox().draw(built);

		GlStateManager._depthFunc(GL11.GL_LEQUAL);
		GlStateManager._enableDepthTest();
		GlStateManager._disableBlend();
	}

	public static void renderOutlinedBox(MatrixStack matrices, Box box, Color color) {
		renderOutlinedBox(matrices,
				(float) box.minX, (float) box.minY, (float) box.minZ,
				(float) box.maxX, (float) box.maxY, (float) box.maxZ,
				color);
	}

	public static void renderOutlinedBox(MatrixStack matrices, float minX, float minY, float minZ,
										 float maxX, float maxY, float maxZ, Color color) {
		GlStateManager._disableDepthTest();
		GlStateManager._depthFunc(GL11.GL_ALWAYS);
		GlStateManager._enableBlend();

		float r = color.getRed() / 255f;
		float g = color.getGreen() / 255f;
		float b = color.getBlue() / 255f;
		float a = color.getAlpha() / 255f;

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR_NORMAL);

		MatrixStack.Entry entry = matrices.peek();
		Matrix4f matrix = entry.getPositionMatrix();
		buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).normal(0, 1, 0);
		buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).normal(0, 1, 0);
		buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).normal(0, 1, 0);
		buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).normal(0, 1, 0);
		buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).normal(0, 1, 0);
		buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).normal(0, 1, 0);
		buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).normal(0, 1, 0);
		buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).normal(0, 1, 0);
		buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).normal(0, 1, 0);
		buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).normal(0, 1, 0);
		buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).normal(0, 1, 0);
		buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).normal(0, 1, 0);
		buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).normal(0, 1, 0);
		buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).normal(0, 1, 0);
		buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).normal(0, 1, 0);
		buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).normal(0, 1, 0);
		buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).normal(0, 1, 0);
		buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).normal(0, 1, 0);
		buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).normal(0, 1, 0);
		buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).normal(0, 1, 0);
		buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).normal(0, 1, 0);
		buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).normal(0, 1, 0);
		buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).normal(0, 1, 0);
		buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).normal(0, 1, 0);

		BuiltBuffer built = buffer.end();
		RenderLayers.lines().draw(built);

		GlStateManager._depthFunc(GL11.GL_LEQUAL);
		GlStateManager._enableDepthTest();
		GlStateManager._disableBlend();
	}

	public static void renderLine(MatrixStack matrices, Vec3d start, Vec3d end, Color color) {
		GlStateManager._disableDepthTest();
		GlStateManager._depthFunc(GL11.GL_ALWAYS);
		GlStateManager._enableBlend();

		float r = color.getRed() / 255f;
		float g = color.getGreen() / 255f;
		float b = color.getBlue() / 255f;
		float a = color.getAlpha() / 255f;

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR_NORMAL);

		MatrixStack.Entry entry = matrices.peek();
		Matrix4f matrix = entry.getPositionMatrix();

		buffer.vertex(matrix, (float) start.x, (float) start.y, (float) start.z).color(r, g, b, a).normal(0, 1, 0);
		buffer.vertex(matrix, (float) end.x, (float) end.y, (float) end.z).color(r, g, b, a).normal(0, 1, 0);

		BuiltBuffer built = buffer.end();
		RenderLayers.lines().draw(built);

		GlStateManager._depthFunc(GL11.GL_LEQUAL);
		GlStateManager._enableDepthTest();
		GlStateManager._disableBlend();
	}

	public static void renderCircle(MatrixStack matrices, Color c, double originX, double originY, double rad, int segments) {
		int segments1 = MathHelper.clamp(segments, 4, 360);
		int color = c.getRGB();

		Matrix4f matrix = matrices.peek().getPositionMatrix();
		float f = (float) (color >> 24 & 255) / 255.0F;
		float g = (float) (color >> 16 & 255) / 255.0F;
		float h = (float) (color >> 8 & 255) / 255.0F;
		float k = (float) (color & 255) / 255.0F;
		setup();
		BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
		for (int i = 0; i < 360; i += Math.min(360 / segments1, 360 - i)) {
			double radians = Math.toRadians(i);
			double sin = Math.sin(radians) * rad;
			double cos = Math.cos(radians) * rad;
			bufferBuilder.vertex(matrix, (float) (originX + sin), (float) (originY + cos), 0).color(g, h, k, f);
		}

		// FIX: actually draw the built buffer instead of discarding it
		BuiltBuffer built = bufferBuilder.end();
		RenderLayers.debugFilledBox().draw(built);
		cleanup();
	}

	public static void renderCircle(Matrix3x2fStack matrices, Color c, double originX, double originY, double rad, int segments) {
		renderCircle(toMatrixStack(matrices), c, originX, originY, rad, segments);
	}

	private static MatrixStack toMatrixStack(Matrix3x2fStack matrices) {
		Matrix3x2f m = new Matrix3x2f(matrices);
		MatrixStack stack = new MatrixStack();
		Matrix4f matrix = stack.peek().getPositionMatrix();
		matrix.set(
				m.m00(), m.m10(), 0.0f, 0.0f,
				m.m01(), m.m11(), 0.0f, 0.0f,
				0.0f, 0.0f, 1.0f, 0.0f,
				m.m20(), m.m21(), 0.0f, 1.0f
		);
		return stack;
	}

	public static void renderRoundedOutline(DrawContext poses, Color c, double fromX, double fromY, double toX, double toY, double rad1, double rad2, double rad3, double rad4, double width, double samples) {
		int color = c.getRGB();
		Matrix4f matrix = toMatrixStack(poses.getMatrices()).peek().getPositionMatrix();
		float f = (float) (color >> 24 & 255) / 255.0F;
		float g = (float) (color >> 16 & 255) / 255.0F;
		float h = (float) (color >> 8 & 255) / 255.0F;
		float k = (float) (color & 255) / 255.0F;
		setup();
		renderRoundedOutlineInternal(matrix, g, h, k, f, fromX, fromY, toX, toY, rad1, rad2, rad3, rad4, width, samples);
		cleanup();
	}

	public static void renderRoundedQuadInternal(Matrix4f matrix, float cr, float cg, float cb, float ca, double fromX, double fromY, double toX, double toY, double corner1, double corner2, double corner3, double corner4, double samples) {
		BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

		double[][] map = new double[][]{new double[]{toX - corner4, toY - corner4, corner4}, new double[]{toX - corner2, fromY + corner2, corner2},
				new double[]{fromX + corner1, fromY + corner1, corner1}, new double[]{fromX + corner3, toY - corner3, corner3}};
		for (int i = 0; i < 4; i++) {
			double[] current = map[i];
			double rad = current[2];
			for (double r = i * 90d; r < (360 / 4d + i * 90d); r += (90 / samples)) {
				float rad1 = (float) Math.toRadians(r);
				float sin = (float) (Math.sin(rad1) * rad);
				float cos = (float) (Math.cos(rad1) * rad);
				bufferBuilder.vertex(matrix, (float) current[0] + sin, (float) current[1] + cos, 0.0F).color(cr, cg, cb, ca);
			}
			float rad1 = (float) Math.toRadians((360 / 4d + i * 90d));
			float sin = (float) (Math.sin(rad1) * rad);
			float cos = (float) (Math.cos(rad1) * rad);
			bufferBuilder.vertex(matrix, (float) current[0] + sin, (float) current[1] + cos, 0.0F).color(cr, cg, cb, ca);
		}

		// FIX: actually draw the built buffer instead of discarding it
		BuiltBuffer built = bufferBuilder.end();
		RenderLayers.debugFilledBox().draw(built);
	}

	interface RenderAction {
		void run(BufferBuilder buffer, float x, float y, float z, float x1, float y1, float z1, float red, float green, float blue, float alpha, Matrix4f matrix);
	}

	public static void renderLine(MatrixStack matrices, Color color, Vec3d start, Vec3d end) {
		matrices.push();
		Matrix4f s = matrices.peek().getPositionMatrix();
		if (ClickGUI.antiAliasing.getValue()) {
			GL11.glEnable(GL13.GL_MULTISAMPLE);
			GL11.glEnable(GL11.GL_LINE_SMOOTH);
			GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
		}
		GL11.glDepthFunc(GL11.GL_ALWAYS);
		GlStateManager._enableBlend();

		genericAABBRender(
				VertexFormat.DrawMode.DEBUG_LINES,
				VertexFormats.POSITION_COLOR,
				null,
				s,
				start,
				end.subtract(start),
				color,
				(buffer, x, y, z, x1, y1, z1, red, green, blue, alpha, matrix) -> {
					buffer.vertex(matrix, x, y, z).color(red, green, blue, alpha);
					buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha);
				}
		);

		GL11.glDepthFunc(GL11.GL_LEQUAL);
		GlStateManager._disableBlend();
		if (ClickGUI.antiAliasing.getValue()) {
			GL11.glDisable(GL11.GL_LINE_SMOOTH);
			GL11.glDisable(GL13.GL_MULTISAMPLE);
		}
		matrices.pop();
	}

	private static void genericAABBRender(VertexFormat.DrawMode mode, VertexFormat format, Object shader, Matrix4f stack, Vec3d start, Vec3d dimensions, Color color, RenderAction action) {
		float red = color.getRed() / 255f;
		float green = color.getGreen() / 255f;
		float blue = color.getBlue() / 255f;
		float alpha = color.getAlpha() / 255f;
		Vec3d end = start.add(dimensions);
		float x1 = (float) start.x;
		float y1 = (float) start.y;
		float z1 = (float) start.z;
		float x2 = (float) end.x;
		float y2 = (float) end.y;
		float z2 = (float) end.z;
		useBuffer(mode, format, shader, bufferBuilder -> action.run(bufferBuilder, x1, y1, z1, x2, y2, z2, red, green, blue, alpha, stack));
	}

	private static void useBuffer(VertexFormat.DrawMode mode, VertexFormat format, Object shader, Consumer<BufferBuilder> runner) {
		Tessellator t = Tessellator.getInstance();
		BufferBuilder bb = t.begin(mode, format);

		runner.accept(bb);

		setup();
		BuiltBuffer built = bb.end();
		if (mode == VertexFormat.DrawMode.DEBUG_LINES || mode == VertexFormat.DrawMode.LINES) {
			RenderLayers.lines().draw(built);
		} else {
			RenderLayers.debugFilledBox().draw(built);
		}
		cleanup();
	}
}