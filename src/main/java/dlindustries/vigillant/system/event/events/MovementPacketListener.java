package dlindustries.vigillant.system.event.events;

import dlindustries.vigillant.system.event.Event;
import dlindustries.vigillant.system.event.Listener;

import java.util.ArrayList;

public interface MovementPacketListener extends Listener {
	void onSendMovementPackets();

	class MovementPacketEvent extends Event<MovementPacketListener> {
		@Override
		public void fire(ArrayList<MovementPacketListener> listeners) {
			listeners.forEach(MovementPacketListener::onSendMovementPackets);
		}

		@Override
		public Class<MovementPacketListener> getListenerType() {
			return MovementPacketListener.class;
		}
	}
}
