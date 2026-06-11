package plz.lizi.supersteve.entity;

import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import plz.lizi.supersteve.api.SSUtil;

public class SafeLocalPlayer extends LocalPlayer {
	public SafeLocalPlayer(Minecraft p_108621_, ClientLevel p_108622_, ClientPacketListener p_108623_, StatsCounter p_108624_, ClientRecipeBook p_108625_, boolean p_108626_, boolean p_108627_) {
		super(p_108621_, p_108622_, p_108623_, p_108624_, p_108625_, p_108626_, p_108627_);
	}

	@Override
	public float getHealth() {
		return 20.0F;
	}

	@Override
	public float getMaxHealth() {
		return 20.0F;
	}

	@Override
	public boolean isAlive() {
		return true;
	}

	@Override
	public boolean isDeadOrDying() {
		return false;
	}

	@Override
	public double getAttributeValue(Attribute p_21134_) {
		if (p_21134_ == Attributes.MAX_HEALTH) {
			return 20.0d;
		}
		return super.getAttributeValue(p_21134_);
	}

	@Override
	public boolean hurt(DamageSource p_108662_, float p_108663_) {
		return false;
	}

	@Override
	public void actuallyHurt(DamageSource p_108729_, float p_108730_) {}

	@Override
	public void kill() {}

	@Override
	public boolean addEffect(MobEffectInstance p_21165_) {
		return false;
	}

	@Override
	public void forceAddEffect(MobEffectInstance p_147216_, Entity p_147217_) {}

	@Override
	public boolean addEffect(MobEffectInstance p_147208_, Entity p_147209_) {
		return false;
	}

	@Override
	public boolean hasEffect(MobEffect p_21024_) {
		return p_21024_ == MobEffects.NIGHT_VISION;
	}

	@Override
	public MobEffectInstance getEffect(MobEffect p_21125_) {
		if (p_21125_ == MobEffects.NIGHT_VISION) {
			return SSUtil.SafeEffectMap.NIGHT_VISION;
		}
		return super.getEffect(p_21125_);
	}

	@Override
	public void knockback(double p_147241_, double p_147242_, double p_147243_) {}
}
