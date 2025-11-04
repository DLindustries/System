package dlindustries.vigillant.system.mixin;

import dlindustries.vigillant.system.module.modules.optimizer.ShieldOptimizer;
import dlindustries.vigillant.system.system;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @ModifyConstant(
            method = "isBlocking",
            constant = @Constant(intValue = 5)
    )
    private int modifyShieldDelay(int original) {
        return system.INSTANCE.getModuleManager().getModule(ShieldOptimizer.class).isEnabled() ? 0 : original;
    }
}