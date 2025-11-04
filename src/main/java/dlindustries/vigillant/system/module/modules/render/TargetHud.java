package dlindustries.vigillant.system.module.modules.render;

import dlindustries.vigillant.system.event.events.HudListener;
import dlindustries.vigillant.system.event.events.PacketSendListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

public final class TargetHud extends Module implements HudListener, PacketSendListener {
	private final NumberSetting xCoord = new NumberSetting(EncryptedString.of("X"), 0, 1920, 500, 1);
	private final NumberSetting yCoord = new NumberSetting(EncryptedString.of("Y"), 0, 1080, 500, 1);
	private final BooleanSetting hudTimeout = new BooleanSetting(EncryptedString.of("Timeout"), true)
			.setDescription(EncryptedString.of("Target hud will disappear after 10 seconds"));
	private static final Color PANEL_COLOR = new Color(10, 10, 20, 220);
	private static final Color MAIN_COLOR = new Color(102, 0, 255, 255);
	private static final Color GLOW_COLOR = new Color(64, 0, 255, 100);
	private static final Color ACCENT_COLOR = new Color(7, 0, 200, 255);
	private static final int PANEL_WIDTH = 340;
	private static final int PANEL_HEIGHT = 200;
	private static final int BORDER_RADIUS = 8;

	private long lastAttackTime = 0;
	public static float animation;
	private static final long timeout = 10000;

	public TargetHud() {
		super(EncryptedString.of("Target HUD"),
				EncryptedString.of("Gives you information about the enemy player"),
				-1,
				Category.RENDER);
		addSettings(xCoord, yCoord, hudTimeout);
	}

	@Override
	public void onEnable() {
		eventManager.add(HudListener.class, this);
		eventManager.add(PacketSendListener.class, this);
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(HudListener.class, this);
		eventManager.remove(PacketSendListener.class, this);
		super.onDisable();
	}

	@Override
	public void onRenderHud(HudEvent event) {
		DrawContext context = event.context;
		int x = xCoord.getValueInt();
		int y = yCoord.getValueInt();

		RenderUtils.unscaledProjection();
		if ((!hudTimeout.getValue() || (System.currentTimeMillis() - lastAttackTime <= timeout)) &&
				mc.player.getAttacking() != null && mc.player.getAttacking() instanceof PlayerEntity player && player.isAlive()) {
			animation = RenderUtils.fast(animation, player.isAlive() ? 0 : 1, 15f);

			PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
			float tx = (float) x;
			float ty = (float) y;
			MatrixStack matrixStack = context.getMatrices();
			float thetaRotation = 90 * animation;
			matrixStack.push();
			matrixStack.translate(tx, ty, 0);
			matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(thetaRotation));
			matrixStack.translate(-tx, -ty, 0);
			Color themeColor = Utils.getMainColor(255, 0);
			Color glowColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 100);
			RenderUtils.renderRoundedQuad(matrixStack, PANEL_COLOR, x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, BORDER_RADIUS, BORDER_RADIUS, BORDER_RADIUS, BORDER_RADIUS, 10);

			for (int i = 0; i < 3; i++) {
				RenderUtils.renderRoundedOutline(context, glowColor,
						x - i, y - i, x + PANEL_WIDTH + i, y + PANEL_HEIGHT + i,
						BORDER_RADIUS, BORDER_RADIUS, BORDER_RADIUS, BORDER_RADIUS, 1, 10);
			}

			RenderUtils.renderRoundedOutline(context, themeColor,
					x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT,
					BORDER_RADIUS, BORDER_RADIUS, BORDER_RADIUS, BORDER_RADIUS, 1, 10);
			RenderUtils.renderRoundedQuad(matrixStack, themeColor,
					x, y + 27, x + PANEL_WIDTH, y + 29,
					0, 0, 0, 0, 10);
			dlindustries.vigillant.system.module.modules.client.NameProtect nameProtect =
					dlindustries.vigillant.system.system.INSTANCE.getModuleManager().getModule(
							dlindustries.vigillant.system.module.modules.client.NameProtect.class);
			String displayName = player.getName().getString();
			if (nameProtect != null) displayName = nameProtect.replaceName(displayName);

			TextRenderer.drawString(displayName + " §8| §b" + MathUtils.roundToDecimal(player.distanceTo(mc.player), 0.5) + " blocks", context, x + 28, y + 8, Color.WHITE.getRGB());
			if (entry != null) {
				PlayerSkinDrawer.draw(context, entry.getSkinTextures().texture(), x + 5, y + 5, 20);
			}
			int infoY = y + 35;
			int lineHeight = 25;

			TextRenderer.drawString("§7Type: " + (entry == null ? "§cBot" : "§aPlayer"), context, x + 5, infoY, Color.WHITE.getRGB());
			infoY += lineHeight;

			float health = player.getHealth() + player.getAbsorptionAmount();
			TextRenderer.drawString("§7Health: §a" + String.format("%.1f❤", health), context, x + 5, infoY, Color.WHITE.getRGB());
			infoY += lineHeight;

			TextRenderer.drawString("§7Invisible: " + (player.isInvisible() ? "§cYes" : "§aNo"), context, x + 5, infoY, Color.WHITE.getRGB());
			infoY += lineHeight;

			if (entry != null) {
				int ping = entry.getLatency();
				Color pingColor = ping < 100 ? Color.GREEN : ping < 200 ? Color.YELLOW : Color.RED;
				TextRenderer.drawString("§7Ping: " + ping + "ms", context, x + 5, infoY, pingColor.getRGB());
				infoY += lineHeight;
			}
			int healthHeight = Math.min(Math.round(health * 5), 171);
			int barX = x + PANEL_WIDTH - 8;
			context.fill(barX, y + 200, barX + 4, y + 200 - 171, new Color(30, 30, 40, 200).getRGB());
			context.fill(barX, y + 200, barX + 4, (y + 200) - healthHeight,
					getHealthColor(health, themeColor).getRGB());
			RenderUtils.renderRoundedOutline(context, glowColor,
					barX - 1, (y + 200) - healthHeight - 1, barX + 5, y + 201,
					0, 0, 0, 0, 1, 10);
			if (player.hurtTime != 0) {
				TextRenderer.drawString("§7Damage Tick: " + player.hurtTime, context, x + 125, y + 35, Color.WHITE.getRGB());

				context.fill(x + 125, y + 55, (x + 125) + (player.hurtTime * 15), y + 58,
						getDamageTickColor(player.hurtTime, themeColor).getRGB());

				RenderUtils.renderRoundedOutline(context, glowColor,
						x + 124, y + 54, x + 125 + (player.hurtTime * 15) + 1, y + 59,
						0, 0, 0, 0, 1, 10);
			}

			matrixStack.pop();
		} else {
			animation = RenderUtils.fast(animation, 1, 15f);
		}
		RenderUtils.scaledProjection();
	}
	private Color getHealthColor(float health, Color themeColor) {
		if (health > 15f) return themeColor;
		if (health > 10f) {
			return new Color(
					(themeColor.getRed() + 255) / 2,
					(themeColor.getGreen() + 255) / 2,
					(themeColor.getBlue()) / 2
			);
		}
		if (health > 5f) return new Color(255, 165, 0);
		return new Color(255, 0, 0);
	}

	private Color getDamageTickColor(int hurtTime, Color themeColor) {
		float progress = Math.min(1f, hurtTime / 10f);
		return new Color(
				(int) (themeColor.getRed() * (1 - progress) + 255 * progress),
				(int) (themeColor.getGreen() * (1 - progress)),
				(int) (themeColor.getBlue() * (1 - progress))
		);
	}




	@Override
	public void onPacketSend(PacketSendEvent event) {
		if (event.packet instanceof PlayerInteractEntityC2SPacket packet) {
			packet.handle(new PlayerInteractEntityC2SPacket.Handler() {
				@Override
				public void interact(Hand hand) {}

				@Override
				public void interactAt(Hand hand, Vec3d pos) {}

				@Override
				public void attack() {
					if (mc.targetedEntity instanceof PlayerEntity) {
						lastAttackTime = System.currentTimeMillis();
					}
				}
			});
		}
	}
}