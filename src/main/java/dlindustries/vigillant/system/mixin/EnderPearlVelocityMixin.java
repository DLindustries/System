package dlindustries.vigillant.system.mixin;

import dlindustries.vigillant.system.module.modules.crystal.KeyPearl.PearlBoostAccessor;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderPearlEntity.class)
public class EnderPearlVelocityMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickBoost(CallbackInfo ci) {
        EnderPearlEntity pearl = (EnderPearlEntity) (Object) this;

        PearlBoostAccessor boost = PearlBoostAccessor.INSTANCE;
        if (!boost.enabled) return;

        // Only first tick if configured
        if (boost.firstTickOnly && pearl.age > 1) return;

        double multiplier = boost.multiplier;
        if (multiplier <= 1.0) return;

        Vec3d vel = pearl.getVelocity();
        if (vel == null) return;

        pearl.setVelocity(vel.multiply(multiplier));
    }
}
