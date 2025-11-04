package dlindustries.vigillant.system.mixin;

import dlindustries.vigillant.system.module.modules.client.NameProtect;
import dlindustries.vigillant.system.system;
import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(TextVisitFactory.class)
public class TextVisitFactoryMixin {

    @ModifyArg(
            method = "visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/text/TextVisitFactory;visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
                    ordinal = 0
            ),
            index = 0
    )
    private static String adjustText(String text) {
        NameProtect nameProtect = system.INSTANCE.getModuleManager().getModule(NameProtect.class);
        if (nameProtect != null && nameProtect.isEnabled()) {
            return nameProtect.replaceName(text);
        }
        return text;
    }
}