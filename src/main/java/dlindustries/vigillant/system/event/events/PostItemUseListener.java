package dlindustries.vigillant.system.event.events;

import dlindustries.vigillant.system.event.CancellableEvent;
import dlindustries.vigillant.system.event.Listener;

import java.util.ArrayList;

public interface PostItemUseListener extends Listener {
    void onPostItemUse(PostItemUseEvent event);

    class PostItemUseEvent extends CancellableEvent {
        @Override
        public void fire(ArrayList listeners) {
            for (Object obj : listeners) {
                ((PostItemUseListener) obj).onPostItemUse(this);
            }
        }

        @Override
        public Class<? extends Listener> getListenerType() {
            return PostItemUseListener.class;
        }
    }
}
