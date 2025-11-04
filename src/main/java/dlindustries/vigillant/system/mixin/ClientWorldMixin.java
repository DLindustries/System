package dlindustries.vigillant.system.mixin;

import dlindustries.vigillant.system.module.modules.render.RenderBarrier;
import dlindustries.vigillant.system.system;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Items;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public class ClientWorldMixin
{
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(at = @At("HEAD"),
            method = "getBlockParticle()Lnet/minecraft/block/Block;",
            cancellable = true)
    private void onGetBlockParticle(CallbackInfoReturnable<Block> cir)
    {
        if(!system.INSTANCE.getModuleManager().getModule(RenderBarrier.class).isEnabled())
            return;
        if(client.interactionManager.getCurrentGameMode() == GameMode.CREATIVE
                && client.player.getMainHandStack().getItem() == Items.LIGHT)
            return;

        cir.setReturnValue(Blocks.BARRIER);
    }
}