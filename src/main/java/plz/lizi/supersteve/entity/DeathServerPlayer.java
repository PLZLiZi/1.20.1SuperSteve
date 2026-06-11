package plz.lizi.supersteve.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public class DeathServerPlayer extends ServerPlayer {
	public DeathServerPlayer(MinecraftServer p_254143_, ServerLevel p_254435_, GameProfile p_253651_) {
		super(p_254143_, p_254435_, p_253651_);
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