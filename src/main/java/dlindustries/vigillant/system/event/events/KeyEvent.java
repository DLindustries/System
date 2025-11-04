package dlindustries.vigillant.system.event.events;

import dlindustries.vigillant.system.event.Event;
import dlindustries.vigillant.system.event.Listener;

import java.util.ArrayList;

public interface KeyEvent extends Listener {
    void onKey(int key, int action);

    class Impl extends Event<KeyEvent> {
        private final int key;
        private final int action;

        public Impl(int key, int action) {
            this.key = key;
            this.action = action;
        }

        public int getKey() {
            return key;
        }

        public int getAction() {
            return action;
        }

        @Override
        public void fire(ArrayList<KeyEvent> listeners) {
            listeners.forEach(l -> l.onKey(key, action));
        }

        @Override
        public Class<KeyEvent> getListenerType() {
            return KeyEvent.class;
        }
    }
}
