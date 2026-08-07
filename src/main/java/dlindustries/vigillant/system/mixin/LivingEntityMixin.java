package dlindustries.vigillant.system.mixin;

import dlindustries.vigillant.system.module.modules.optimizer.ShieldOptimizer;
import dlindustries.vigillant.system.VigillantSystem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow public abstract boolean isUsingItem();
    @Shadow public abstract ItemStack getActiveItem();
    @Shadow public abstract int getItemUseTime();

    @Inject(method = "isBlocking", at = @At("RETURN"), cancellable = true)
    private void bypassShieldWarmup(CallbackInfoReturnable<Boolean> cir) {
        ShieldOptimizer shieldOptimizer = VigillantSystem.INSTANCE.getModuleManager().getModule(ShieldOptimizer.class);
        if (shieldOptimizer == null || !shieldOptimizer.isEnabled() || cir.getReturnValueZ()) {
            return;
        }

        if (isUsingItem() && getActiveItem().getItem() instanceof ShieldItem && getItemUseTime() < 5) {
            cir.setReturnValue(true);
        }
    }
}
