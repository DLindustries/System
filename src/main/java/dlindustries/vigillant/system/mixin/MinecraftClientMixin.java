package dlindustries.vigillant.system.mixin;

import dlindustries.vigillant.system.event.EventManager;
import dlindustries.vigillant.system.event.events.*;
import dlindustries.vigillant.system.module.modules.optimizer.PlacementOptimizer;
import dlindustries.vigillant.system.system;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
	@Shadow @Nullable public ClientWorld world;
	@Shadow @Final private Window window;
	@Shadow public ClientPlayerEntity player;
	@Shadow private int itemUseCooldown;

	@Inject(method = "tick", at = @At("HEAD"))
	private void onTick(CallbackInfo ci) {
		if (world != null) {
			TickListener.TickEvent event = new TickListener.TickEvent();
			EventManager.fire(event);
			PlacementOptimizer optimizer = system.INSTANCE.getModuleManager().getModule(PlacementOptimizer.class);
			if (optimizer != null && optimizer.isEnabled()) {
				Item held = player.getMainHandStack().getItem();
				boolean excludeAnchors = optimizer.shouldExcludeAnchors();
				if (excludeAnchors && (held == Items.RESPAWN_ANCHOR || held == Items.GLOWSTONE)) {
					return;
				}
				int desiredDelay = -1; // -1 means no adjustment
				if (held instanceof BlockItem) {
					desiredDelay = optimizer.getBlockDelay();
				} else if (held == Items.END_CRYSTAL) {
					desiredDelay = optimizer.getCrystalDelay();
				}
				if (desiredDelay >= 0) {
					if (desiredDelay == 0) {
						this.itemUseCooldown = 0;
					} else if (this.itemUseCooldown > desiredDelay) {
						this.itemUseCooldown = desiredDelay - 1;
					}
				}
			}
		}
	}

	@Inject(method = "onResolutionChanged", at = @At("HEAD"))
	private void onResolutionChanged(CallbackInfo ci) {
		EventManager.fire(new ResolutionListener.ResolutionEvent(this.window));
	}

	@Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
	private void onItemUse(CallbackInfo ci) {
		ItemUseListener.ItemUseEvent event = new ItemUseListener.ItemUseEvent();
		EventManager.fire(event);
		if (event.isCancelled()) ci.cancel();
	}

	@Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
	private void onAttack(CallbackInfoReturnable<Boolean> cir) {
		AttackListener.AttackEvent event = new AttackListener.AttackEvent();
		EventManager.fire(event);
		if (event.isCancelled()) cir.setReturnValue(false);
	}

	@Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
	private void onBlockBreaking(boolean breaking, CallbackInfo ci) {
		BlockBreakingListener.BlockBreakingEvent event = new BlockBreakingListener.BlockBreakingEvent();
		EventManager.fire(event);
		if (event.isCancelled()) ci.cancel();
	}

	@Inject(method = "stop", at = @At("HEAD"))
	private void onClose(CallbackInfo ci) {
		system.INSTANCE.getProfileManager().saveProfile();
	}
}