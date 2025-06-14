package dlindustries.vigillant.system.event.events;

import dlindustries.vigillant.system.event.CancellableEvent;
import dlindustries.vigillant.system.event.Listener;

import java.util.ArrayList;

public interface AttackListener extends Listener {
	void onAttack(AttackEvent event);

	class AttackEvent extends CancellableEvent<AttackListener> {

		@Override
		public void fire(ArrayList<AttackListener> listeners) {
			listeners.forEach(e -> e.onAttack(this));
		}

		@Override
		public Class<AttackListener> getListenerType() {
			return AttackListener.class;
		}
	}
}
