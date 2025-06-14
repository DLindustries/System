package dlindustries.vigillant.system.mixin;

import dlindustries.vigillant.system.event.EventManager;
import dlindustries.vigillant.system.event.events.CameraUpdateListener;
import dlindustries.vigillant.system.module.modules.optimizer.CameraOptimizer;
import dlindustries.vigillant.system.system;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V"))
	private void update(Args args) {
		CameraUpdateListener.CameraUpdateEvent event = new CameraUpdateListener.CameraUpdateEvent(args.get(0), args.get(1), args.get(2));
		EventManager.fire(event);

		args.set(0, event.getX());
		args.set(1, event.getY());
		args.set(2, event.getZ());
	}

	@Inject(at = @At("HEAD"), method = "clipToSpace(F)F", cancellable = true)
	private void onClipToSpace(float desiredCameraDistance, CallbackInfoReturnable<Float> cir) {
		CameraOptimizer optimizer = system.INSTANCE.getModuleManager().getModule(CameraOptimizer.class);
		if (optimizer != null && optimizer.isNoClipEnabled()) {
			cir.setReturnValue(desiredCameraDistance);
		}
	}

	@Inject(at = @At("HEAD"),
			method = "getSubmersionType()Lnet/minecraft/block/enums/CameraSubmersionType;",
			cancellable = true)
	private void onGetSubmersionType(CallbackInfoReturnable<CameraSubmersionType> cir) {
		CameraOptimizer optimizer = system.INSTANCE.getModuleManager().getModule(CameraOptimizer.class);
		if (optimizer != null && optimizer.isNoOverlayEnabled()) {
			cir.setReturnValue(CameraSubmersionType.NONE);
		}
	}
}