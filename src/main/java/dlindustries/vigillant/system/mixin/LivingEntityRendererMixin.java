package dlindustries.vigillant.system.mixin;

import dlindustries.vigillant.system.module.modules.render.NameTags;
import dlindustries.vigillant.system.system;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Redirect(
            method = "hasLabel(Lnet/minecraft/entity/LivingEntity;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;getSquaredDistanceToCamera(Lnet/minecraft/entity/Entity;)D"
            )
    )
    private double adjustDistance(EntityRenderDispatcher instance, Entity entity) {
        NameTags nameTags = system.INSTANCE.getModuleManager().getModule(NameTags.class);
        if (nameTags != null && nameTags.isEnabled() && nameTags.isUnlimitedRange()) {
            return 1.0; // Fake close distance
        }
        return instance.getSquaredDistanceToCamera(entity);
    }

    @Inject(
            method = "hasLabel(Lnet/minecraft/entity/LivingEntity;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MinecraftClient;getInstance()Lnet/minecraft/client/MinecraftClient;"
            ),
            cancellable = true
    )
    private void forcePlayerNametags(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        NameTags nameTags = system.INSTANCE.getModuleManager().getModule(NameTags.class);
        if (nameTags != null && nameTags.isEnabled() && nameTags.shouldForcePlayerNametags()) {
            cir.setReturnValue(true);
        }
    }
}