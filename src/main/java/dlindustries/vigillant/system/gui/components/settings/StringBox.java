package dlindustries.vigillant.system.gui.components.settings;

import dlindustries.vigillant.system.VigillantSystem;
import dlindustries.vigillant.system.gui.components.ModuleButton;
import dlindustries.vigillant.system.module.modules.client.ClickGUI;
import dlindustries.vigillant.system.module.setting.Setting;
import dlindustries.vigillant.system.module.setting.StringSetting;
import dlindustries.vigillant.system.utils.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public final class StringBox extends RenderableSetting {
    private final StringSetting setting;
    private Color currentAlpha;

    public StringBox(ModuleButton parent, Setting<?> setting, int offset) {
        super(parent, setting, offset);
        this.setting = (StringSetting) setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        TextRenderer.drawString(
                setting.getName() + ": " + (setting.getValue().length() <= 9 ? setting.getValue() : (setting.getValue().substring(0, 9) + "...")),
                context,
                parentX() + 9,
                (parentY() + parentOffset() + offset) + 9,
                new Color(245, 245, 245, 255).getRGB()
        );
        if (!parent.parent.dragging) {
            int toHoverAlpha = isHovered(mouseX, mouseY) ? 15 : 0;

            if (currentAlpha == null)
                currentAlpha = new Color(255, 255, 255, toHoverAlpha);
            else
                currentAlpha = new Color(255, 255, 255, currentAlpha.getAlpha());

            if (currentAlpha.getAlpha() != toHoverAlpha)
                currentAlpha = ColorUtils.smoothAlphaTransition(0.05F, toHoverAlpha, currentAlpha);

            context.fill(
                    parentX(),
                    parentY() + parentOffset() + offset,
                    parentX() + parentWidth(),
                    parentY() + parentOffset() + offset + parentHeight(),
                    currentAlpha.getRGB()
            );
        }
    }
    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY) && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            mc.setScreen(new Screen(Text.empty()) {
                private String content = setting.getValue();
                @Override
                public void render(DrawContext context, int mouseX, int mouseY, float delta) {
                    if (ClickGUI.blur.getValue()) mc.gameRenderer.renderBlur();
                    float scaleFactor = (float) mc.getWindow().getScaleFactor();
                    float invScale = 1.0f / scaleFactor;
                    context.getMatrices().pushMatrix();
                    context.getMatrices().scale(invScale, invScale);
                    context.fill(
                            0,
                            0,
                            mc.getWindow().getWidth(),
                            mc.getWindow().getHeight(),
                            new Color(0, 0, 0, ClickGUI.background.getValue() ? 200 : 0).getRGB()
                    );
                    int screenMidX = mc.getWindow().getWidth() / 2;
                    int screenMidY = mc.getWindow().getHeight() / 2;
                    int contentWidth = Math.max(TextRenderer.getWidth(content), 600);
                    int width = contentWidth + 30;
                    int startX = screenMidX - (width / 2);
                    int startY = screenMidY - 30;
                    RenderUtils.renderRoundedQuad(
                            context.getMatrices(),
                            new Color(0, 0, 0, ClickGUI.alphaWindow.getValueInt()),
                            startX,
                            startY,
                            startX + width,
                            screenMidY + 30,
                            5,
                            5,
                            0,
                            0,
                            20
                    );
                    TextRenderer.drawCenteredString(
                            setting.getName(),
                            context,
                            screenMidX,
                            startY + 10,
                            new Color(245, 245, 245, 255).getRGB()
                    );
                    context.fill(
                            startX,
                            screenMidY,
                            startX + width,
                            screenMidY + 30,
                            new Color(0, 0, 0, 120).getRGB()
                    );
                    RenderUtils.renderRoundedOutline(
                            context,
                            new Color(50, 50, 50, 255),
                            startX + 10,
                            screenMidY + 5,
                            startX + (width - 10),
                            screenMidY + 25,
                            5,
                            5,
                            5,
                            5,
                            2,
                            20
                    );
                    TextRenderer.drawString(
                            content,
                            context,
                            startX + 15,
                            screenMidY + 8,
                            new Color(245, 245, 245, 255).getRGB()
                    );
                    context.fill(
                            startX,
                            screenMidY,
                            startX + width,
                            screenMidY + 1,
                            Utils.getMainColor(255, 1).getRGB()
                    );
                    context.getMatrices().popMatrix();
                }
                @Override
                public boolean keyPressed(KeyInput keyInput) {
                    int keyCode = keyInput.key();
                    if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                        setting.setValue(content.strip());
                        mc.setScreen(VigillantSystem.INSTANCE.clickGui);
                        return true;
                    }
                    if (keyInput.isPaste()) {
                        content += mc.keyboard.getClipboard();
                        return true;
                    }
                    if (keyInput.isCopy()) {
                        GLFW.glfwSetClipboardString(mc.getWindow().getHandle(), content);
                        return true;
                    }
                    if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                        if (!content.isEmpty()) {
                            content = content.substring(0, content.length() - 1);
                        }
                        return true;
                    }
                    return super.keyPressed(keyInput);
                }
                @Override
                public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
                }
                @Override
                public boolean charTyped(CharInput charInput) {
                    content += (char) charInput.codepoint();
                    return true;
                }
                @Override
                public boolean shouldCloseOnEsc() {
                    return false;
                }
            });
        }
        super.mouseClicked(mouseX, mouseY, button);
    }
}