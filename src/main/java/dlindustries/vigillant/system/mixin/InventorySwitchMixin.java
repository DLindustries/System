package dlindustries.vigillant.system.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dlindustries.vigillant.system.module.modules.crystal.DhandMod;
import dlindustries.vigillant.system.system;

@Mixin(MinecraftClient.class)
public class InventorySwitchMixin {
    @Inject(method = "handleInputEvents", at = @At("HEAD"), cancellable = true)
    private void onInventoryKeyPress(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        KeyBinding inventoryKey = client.options.inventoryKey;

        if (!inventoryKey.wasPressed() || client.currentScreen != null) {
            return; // Ignore if not pressing inventory key or screen is open
        }

        DhandMod module = system.INSTANCE.getModuleManager().getModule(DhandMod.class);
        if (module != null && module.isEnabled()) {
            // Module is active → handle custom logic
            DhandMod.handleInventoryKey();
            inventoryKey.setPressed(false);
            ci.cancel(); // Block vanilla handling
        } else {
            // Module is disabled → open inventory normally
            client.setScreen(new InventoryScreen(client.player));
        }
    }
}