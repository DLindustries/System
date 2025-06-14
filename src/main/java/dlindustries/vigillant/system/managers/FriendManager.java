package dlindustries.vigillant.system.managers;


import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;

import java.util.HashSet;
import java.util.Set;

import static dlindustries.vigillant.system.system.mc;

public final class  FriendManager {
    private final Set<String> friends;

    public FriendManager() {
        this.friends = new HashSet<>();
    }

    public void addFriend(PlayerEntity player) {
        friends.add(player.getName().getString());
    }

    public void removeFriend(PlayerEntity player) {
        friends.remove(player.getName().getString());
    }

    public boolean isFriend(PlayerEntity player) {
        return friends.contains(player.getName().getString());
    }

    public boolean isAimingOverFriend() {
        if(mc.crosshairTarget instanceof EntityHitResult hitResult) {
            Entity entity = hitResult.getEntity();

            if(entity instanceof PlayerEntity player) {
                return isFriend(player);
            }
        }

        return false;
    }
}
