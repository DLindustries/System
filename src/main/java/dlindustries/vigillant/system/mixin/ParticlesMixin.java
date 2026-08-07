package dlindustries.vigillant.system.mixin;

import dlindustries.vigillant.system.module.modules.optimizer.CameraOptimizer;
import dlindustries.vigillant.system.VigillantSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class ParticlesMixin {

    @Mixin(ParticleManager.class)
    public static class ParticleManagerMixin {

        @Inject(
                method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
                at = @At("HEAD"),
                cancellable = true
        )
        private void onAddParticle(ParticleEffect effect, double x, double y, double z,
                                   double vx, double vy, double vz, CallbackInfoReturnable<Particle> ci) {
            CameraOptimizer module = VigillantSystem.INSTANCE.getModuleManager().getModule(CameraOptimizer.class);
            if (module != null && module.isNoParticlesEnabled()) {
                ci.setReturnValue(null);
            }
        }
    }
    @Mixin(ClientWorld.class)
    public static class ClientWorldMixin {

        @Inject(method = "addBlockBreakParticles", at = @At("HEAD"), cancellable = true)
        private void onAddBlockBreakParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
            CameraOptimizer module = VigillantSystem.INSTANCE.getModuleManager().getModule(CameraOptimizer.class);
            if (module != null && module.isNoBreakParticlesEnabled()) {
                ci.cancel();
            }
        }
    }
    @Mixin(InGameOverlayRenderer.class)
    public static abstract class InGameOverlayRendererMixin {

        @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
        private static void onFireOverlay(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Sprite sprite, CallbackInfo ci) {
            CameraOptimizer module = VigillantSystem.INSTANCE.getModuleManager().getModule(CameraOptimizer.class);
            if (module != null && module.isNoParticlesEnabled()) ci.cancel();
        }
    }
    @Mixin(InGameHud.class)
    public static class InGameHudMixin {

        @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
        private void onPortalOverlay(DrawContext context, float nauseaStrength, CallbackInfo ci) {
            CameraOptimizer module = VigillantSystem.INSTANCE.getModuleManager().getModule(CameraOptimizer.class);
            if (module != null && module.isNoOverlayEnabled()) ci.cancel();
        }
    }
}