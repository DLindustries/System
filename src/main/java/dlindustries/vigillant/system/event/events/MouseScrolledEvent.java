package dlindustries.vigillant.system.event.events;

import dlindustries.vigillant.system.event.Event;
import dlindustries.vigillant.system.event.Listener;

import java.util.ArrayList;

public interface MouseScrolledEvent extends Listener {
    void onMouseScrolled(double amount);

    class Impl extends Event<MouseScrolledEvent> {
        private final double amount;

        public Impl(double amount) {
            this.amount = amount;
        }

        public double getAmount() {
            return amount;
        }

        @Override
        public void fire(ArrayList<MouseScrolledEvent> listeners) {
            listeners.forEach(l -> l.onMouseScrolled(amount));
        }

        @Override
        public Class<MouseScrolledEvent> getListenerType() {
            return MouseScrolledEvent.class;
        }
    }
}
