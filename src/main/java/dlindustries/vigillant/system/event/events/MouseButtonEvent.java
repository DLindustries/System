package dlindustries.vigillant.system.event.events;

import dlindustries.vigillant.system.event.Event;
import dlindustries.vigillant.system.event.Listener;

import java.util.ArrayList;

public interface MouseButtonEvent extends Listener {
    void onMouseButton(int button, int action);

    class Impl extends Event<MouseButtonEvent> {
        private final int button;
        private final int action;

        public Impl(int button, int action) {
            this.button = button;
            this.action = action;
        }

        public int getButton() {
            return button;
        }

        public int getAction() {
            return action;
        }

        @Override
        public void fire(ArrayList<MouseButtonEvent> listeners) {
            listeners.forEach(l -> l.onMouseButton(button, action));
        }

        @Override
        public Class<MouseButtonEvent> getListenerType() {
            return MouseButtonEvent.class;
        }
    }
}
