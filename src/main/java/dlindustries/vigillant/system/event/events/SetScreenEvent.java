package dlindustries.vigillant.system.event.events;

import dlindustries.vigillant.system.event.Event;
import dlindustries.vigillant.system.event.Listener;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;

public interface SetScreenEvent extends Listener {
    void onSetScreen(Screen screen);

    class Impl extends Event<SetScreenEvent> {
        private final Screen screen;

        public Impl(Screen screen) {
            this.screen = screen;
        }

        public Screen getScreen() {
            return screen;
        }

        @Override
        public void fire(ArrayList<SetScreenEvent> listeners) {
            listeners.forEach(l -> l.onSetScreen(screen));
        }

        @Override
        public Class<SetScreenEvent> getListenerType() {
            return SetScreenEvent.class;
        }
    }
}
