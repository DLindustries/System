package dlindustries.vigillant.system.event.events;

import dlindustries.vigillant.system.event.CancellableEvent;
import dlindustries.vigillant.system.event.Listener;

import java.util.ArrayList;

public interface PreItemUseListener extends Listener {
    void onPreItemUse(PreItemUseEvent event);

    class PreItemUseEvent extends CancellableEvent<PreItemUseListener> {
        @Override
        public void fire(ArrayList<PreItemUseListener> listeners) {
            listeners.forEach(l -> l.onPreItemUse(this));
        }

        @Override
        public Class<PreItemUseListener> getListenerType() {
            return PreItemUseListener.class;
        }
    }
}
