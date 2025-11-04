package dlindustries.vigillant.system.mixin;

import dlindustries.vigillant.system.module.modules.render.NoBounce;
import dlindustries.vigillant.system.system;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static dlindustries.vigillant.system.system.mc;

@Mixin(ItemStack.class)
public class ItemStackMixin {

	@Inject(method = "getBobbingAnimationTime", at = @At("HEAD"), cancellable = true)
	private void removeBounceAnimation(CallbackInfoReturnable<Integer> cir) {
		if (mc.player == null) return;

		NoBounce noBounce = system.INSTANCE.getModuleManager().getModule(NoBounce.class);
		if (system.INSTANCE != null && mc.player != null && noBounce.isEnabled()) {
			ItemStack mainHandStack = mc.player.getMainHandStack();
			if (mainHandStack.isOf(Items.END_CRYSTAL)) {
				cir.setReturnValue(0);
			}
		}
	}
}
