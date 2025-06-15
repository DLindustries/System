package dlindustries.vigillant.system.module.modules.optimizer;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.utils.EncryptedString;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import org.lwjgl.glfw.GLFW;

public final class NoJumpDelay extends Module implements TickListener {
	private boolean wasJumping = false;

	public NoJumpDelay() {
		super(EncryptedString.of("No Jump Delay"),
				EncryptedString.of("Lets you jump faster."),
				-1,
				Category.optimizer);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.currentScreen != null || mc.player == null)
			return;

		FluidState fluidState = mc.player.getWorld().getFluidState(mc.player.getBlockPos());
		if (fluidState.isIn(FluidTags.WATER) || fluidState.isIn(FluidTags.LAVA)) {
			wasJumping = false;
			return;
		}

		if (!mc.player.isOnGround()) {
			wasJumping = false;
			return;
		}

		boolean jumping = GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;

		if (jumping && !wasJumping) {
			mc.options.jumpKey.setPressed(false);
			mc.player.jump();
		}

		wasJumping = jumping;
	}
}