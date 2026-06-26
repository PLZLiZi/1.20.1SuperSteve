package plz.lizi.supersteve.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public class DeathRemotePlayer extends RemotePlayer {
    public DeathRemotePlayer(ClientLevel pClientLevel, GameProfile pGameProfile) {
        super(pClientLevel, pGameProfile);
    }

    @Override
	public float getHealth() {
		return 0.0F;
	}

	@Override
	public boolean isAlive() {
		return false;
	}

	@Override
	public boolean isDeadOrDying() {
		return true;
	}

	@Override
	public ItemEntity drop(ItemStack p_36177_, boolean p_36178_) {
		return new ItemEntity(level(), getX(), getY(), getZ(), ItemStack.EMPTY);
	}
}
