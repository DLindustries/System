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
	// Settings
	private final NumberSetting xCoord = new NumberSetting(EncryptedString.of("X"), 0, 1920, 500, 1);
	private final NumberSetting yCoord = new NumberSetting(EncryptedString.of("Y"), 0, 1080, 500, 1);
	private final BooleanSetting hudTimeout = new BooleanSetting(EncryptedString.of("Timeout"), true)
			.setDescription(EncryptedString.of("Target hud will disappear after 10 seconds"));

	// Visual properties
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
			animation = RenderUtils.fast(animation, mc.player.getAttacking() instanceof PlayerEntity player1 && player1.isAlive() ? 0 : 1, 15f);

			PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
			float tx = (float) x;
			float ty = (float) y;
			MatrixStack matrixStack = context.getMatrices();
			float thetaRotation = 90 * animation;
			matrixStack.push();
			matrixStack.translate(tx, ty, 0);
			matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(thetaRotation));
			matrixStack.translate(-tx, -ty, 0);

			// Draw the main panel with glowing border
			RenderUtils.renderRoundedQuad(matrixStack, PANEL_COLOR, x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, BORDER_RADIUS, BORDER_RADIUS, BORDER_RADIUS, BORDER_RADIUS, 10);

			// Glow effect around the panel
			for (int i = 0; i < 3; i++) {
				RenderUtils.renderRoundedOutline(context, GLOW_COLOR,
						x - i, y - i, x + PANEL_WIDTH + i, y + PANEL_HEIGHT + i,
						BORDER_RADIUS, BORDER_RADIUS, BORDER_RADIUS, BORDER_RADIUS, 1, 10);
			}

			// Main border
			RenderUtils.renderRoundedOutline(context, MAIN_COLOR,
					x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT,
					BORDER_RADIUS, BORDER_RADIUS, BORDER_RADIUS, BORDER_RADIUS, 1, 10);

			// Header separator
			RenderUtils.renderRoundedQuad(matrixStack, MAIN_COLOR,
					x, y + 27, x + PANEL_WIDTH, y + 29,
					0, 0, 0, 0, 10);

			// Draw header with player name and distance
			// In the onRenderHud method, change this line:
			TextRenderer.drawString(player.getName().getString() + " §8| §b" + MathUtils.roundToDecimal(player.distanceTo(mc.player), 0.5) + " blocks", context, x + 28, y + 8, Color.WHITE.getRGB());

			// Player skin
			if (entry != null) {
				PlayerSkinDrawer.draw(context, entry.getSkinTextures().texture(), x + 5, y + 5, 20);
			}

			// Draw player information
			int infoY = y + 35;
			int lineHeight = 25;

			// Player type
			TextRenderer.drawString("§7Type: " + (entry == null ? "§cBot" : "§aPlayer"), context, x + 5, infoY, Color.WHITE.getRGB());
			infoY += lineHeight;

			// Health
			float health = player.getHealth() + player.getAbsorptionAmount();
			TextRenderer.drawString("§7Health: §a" + String.format("%.1f❤", health), context, x + 5, infoY, Color.WHITE.getRGB());
			infoY += lineHeight;

			// Invisible status
			TextRenderer.drawString("§7Invisible: " + (player.isInvisible() ? "§cYes" : "§aNo"), context, x + 5, infoY, Color.WHITE.getRGB());
			infoY += lineHeight;

			// Ping (if player)
			if (entry != null) {
				int ping = entry.getLatency();
				Color pingColor = ping < 100 ? Color.GREEN : ping < 200 ? Color.YELLOW : Color.RED;
				TextRenderer.drawString("§7Ping: " + ping + "ms", context, x + 5, infoY, pingColor.getRGB());
				infoY += lineHeight;
			}

			// Health bar
			int healthHeight = Math.min(Math.round(health * 5), 171);
			int barX = x + PANEL_WIDTH - 8;

			// Health bar background
			context.fill(barX, y + 200, barX + 4, y + 200 - 171, new Color(30, 30, 40, 200).getRGB());

			// Health bar
			context.fill(barX, y + 200, barX + 4, (y + 200) - healthHeight,
					getHealthColor(health).getRGB());

			// Health bar glow
			RenderUtils.renderRoundedOutline(context, GLOW_COLOR,
					barX - 1, (y + 200) - healthHeight - 1, barX + 5, y + 201,
					0, 0, 0, 0, 1, 10);

			// Damage tick
			if (player.hurtTime != 0) {
				TextRenderer.drawString("§7Damage Tick: " + player.hurtTime, context, x + 125, y + 35, Color.WHITE.getRGB());

				// Damage tick progress bar
				context.fill(x + 125, y + 55, (x + 125) + (player.hurtTime * 15), y + 58,
						getDamageTickColor(player.hurtTime).getRGB());

				// Glow effect for damage tick
				RenderUtils.renderRoundedOutline(context, GLOW_COLOR,
						x + 124, y + 54, x + 125 + (player.hurtTime * 15) + 1, y + 59,
						0, 0, 0, 0, 1, 10);
			}

			matrixStack.pop();
		} else {
			animation = RenderUtils.fast(animation, 1, 15f);
		}
		RenderUtils.scaledProjection();
	}

	private Color getHealthColor(float health) {
		if (health > 15) return new Color(0, 255, 0);
		if (health > 10) return new Color(255, 255, 0);
		if (health > 5) return new Color(255, 165, 0);
		return new Color(255, 0, 0);
	}

	private Color getDamageTickColor(int hurtTime) {
		float progress = hurtTime / 10f;
		return new Color(
				(int) (255 * (1 - progress)),
				(int) (255 * progress),
				0
		);
	}

	@Override
	public void onPacketSend(PacketSendListener.PacketSendEvent event) {
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