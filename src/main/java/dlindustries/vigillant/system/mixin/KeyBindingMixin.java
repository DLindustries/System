package dlindustries.vigillant.system.mixin;

import dlindustries.vigillant.system.imixin.IKeyBinding;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import static dlindustries.vigillant.system.system.mc;

@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin implements IKeyBinding {

	@Shadow
	private InputUtil.Key boundKey;

	@Override
	public boolean isActuallyPressed() {
		long handle = mc.getWindow().getHandle();
		int code = boundKey.getCode();
		return InputUtil.isKeyPressed(handle, code);
	}

	@Override
	public void resetPressed() {
		setPressed(isActuallyPressed());
	}

	@Shadow
	public abstract void setPressed(boolean pressed);
}
