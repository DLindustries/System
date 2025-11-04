package dlindustries.vigillant.system.event.events;

import dlindustries.vigillant.system.event.Event;
import dlindustries.vigillant.system.event.Listener;

import java.util.ArrayList;

public interface ChunkMarkClosedEvent extends Listener {
    void onChunkMarkClosed();

    class Impl extends Event<ChunkMarkClosedEvent> {
        @Override
        public void fire(ArrayList<ChunkMarkClosedEvent> listeners) {
            listeners.forEach(ChunkMarkClosedEvent::onChunkMarkClosed);
        }

        @Override
        public Class<ChunkMarkClosedEvent> getListenerType() {
            return ChunkMarkClosedEvent.class;
        }
    }
}
