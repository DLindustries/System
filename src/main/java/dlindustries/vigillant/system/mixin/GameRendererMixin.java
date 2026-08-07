package dlindustries.vigillant.system.mixin;

import dlindustries.vigillant.system.event.EventManager;
import dlindustries.vigillant.system.event.events.GameRenderListener;
import dlindustries.vigillant.system.module.modules.optimizer.CameraOptimizer;
import dlindustries.vigillant.system.VigillantSystem;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Shadow public abstract Matrix4f getBasicProjectionMatrix(float fov);
	@Shadow protected abstract float getFov(Camera camera, float tickProgress, boolean changingFov);
	@Shadow @Final private Camera camera;

	@Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", ordinal = 1))
	private void onWorldRender(RenderTickCounter tickCounter, CallbackInfo ci) {
		float tickDelta = tickCounter.getDynamicDeltaTicks();
		MatrixStack matrixStack = new MatrixStack();

		// Build camera-space world matrix for custom world render modules (ESP, tracers, etc.).
		if (camera != null) {
			Vec3d cameraPos = camera.getFocusedEntity() != null
					? new Vec3d(camera.getFocusedEntity().getX(), camera.getFocusedEntity().getY(), camera.getFocusedEntity().getZ())
					: Vec3d.ZERO;
			matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
			matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
			matrixStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
		}

		EventManager.fire(new GameRenderListener.GameRenderEvent(matrixStack, tickDelta));
	}

	@Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
	private void onShouldRenderBlockOutline(CallbackInfoReturnable<Boolean> cir) {
		CameraOptimizer optimizer = VigillantSystem.INSTANCE.getModuleManager().getModule(CameraOptimizer.class);
		if (optimizer != null && optimizer.isEnabled() && optimizer.isToggleKeyPressed()) {
			cir.setReturnValue(false);
		}
	}
}