package dlindustries.vigillant.system.utils;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class FakeInvScreen extends Screen {

    //  Constructor sets a blank screen title
    public FakeInvScreen(net.minecraft.client.network.ClientPlayerEntity player) {
        super(Text.literal("")); //  Empty screen title
    }

    @Override
    protected void init() {
        //  No GUI elements
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        //  Do not render anything — fully invisible
    }

    @Override
    public boolean shouldPause() {
        return false; //  Game does not pause when this screen is active
    }
}
