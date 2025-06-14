package dlindustries.vigillant.system.event.events;

import dlindustries.vigillant.system.event.CancellableEvent;
import dlindustries.vigillant.system.event.Listener;

import java.util.ArrayList;

public interface MouseMoveListener extends Listener {
	void onMouseMove(MouseMoveEvent event);

	class MouseMoveEvent extends CancellableEvent<MouseMoveListener> {
		public long windowHandle;
		public double x, y;

		public MouseMoveEvent(long windowHandle, double x, double y) {
			this.windowHandle = windowHandle;
			this.x = x;
			this.y = y;
		}

		@Override
		public void fire(ArrayList<MouseMoveListener> listeners) {
			listeners.forEach(e -> e.onMouseMove(this));
		}

		@Override
		public Class<MouseMoveListener> getListenerType() {
			return MouseMoveListener.class;
		}
	}
}
