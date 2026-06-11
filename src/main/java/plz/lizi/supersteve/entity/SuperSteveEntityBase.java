package plz.lizi.supersteve.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import plz.lizi.supersteve.api.SSUtil;
import plz.lizi.supersteve.level.SSBossEvent;

public abstract class SuperSteveEntityBase extends PathfinderMob {
	public static final float ATTACK_RANGE = 4F;
	public static final int MAX_INVULNERABLE_TIME = 40;
	public static final int[] ENTER_ACTIVE = new int[] { 100/* 入场时长 */, 95/* 爆炸产生 */ };
	public static final int[] DEATH_ACTIVE = new int[] { 750/* 死亡时长 */, 0/* 落剑开始 */, 80/* 落剑结束 */, 0/* 领域展开 */, 730/* 领域收回 */, 20/* 声音开始播放 */ };
	public static final EntityDataAccessor<String> SS_HEALTH = SynchedEntityData.defineId(SuperSteveEntityBase.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<String> SS_TYPE = SynchedEntityData.defineId(SuperSteveEntityBase.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<Integer> SS_TICK = SynchedEntityData.defineId(SuperSteveEntityBase.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<String> SS_STATE = SynchedEntityData.defineId(SuperSteveEntityBase.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<Integer> SS_LSTATE = SynchedEntityData.defineId(SuperSteveEntityBase.class, EntityDataSerializers.INT);
	public final List<Attack> attacks = new ArrayList<>();
	public final Consumer<Object> setHealth = health -> {
		if (health instanceof Float fhealth)
			getEntityData().set(SS_HEALTH, "SSH=" + String.format("%08X", Float.floatToRawIntBits(fhealth) ^ 0xF917813F));
	};
	public final Supplier<Object> getHealth = () -> {
		try {
			String ssh = getEntityData().get(SuperSteveEntityBase.SS_HEALTH);
			if (ssh.startsWith("SSH="))
				return Float.intBitsToFloat((int) Long.parseLong(ssh.substring(4, ssh.length()), 16) ^ 0xF917813F);
		} catch (Throwable e) {
		}
		setHealth.accept(20F);
		return 20F;
	};
	public int iInvulnerableTime = 0;
	public SSBossEvent bossEvent;

	protected SuperSteveEntityBase(EntityType<? extends PathfinderMob> p_21683_, Level p_21684_) {
		super(p_21683_, p_21684_);
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.5);
		builder = builder.add(Attributes.MAX_HEALTH, 20);
		builder = builder.add(Attributes.ARMOR, 32767);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 32767);
		builder = builder.add(Attributes.FOLLOW_RANGE, 2048);
		builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 32767);
		builder = builder.add(Attributes.ATTACK_KNOCKBACK, 0);
		return builder;
	}

	public abstract void ssTick(boolean threadCall);

	public abstract float ssGetHealth();

	public abstract SSMode ssGetMode();

	public abstract void ssSetMode(SSMode mode);

	public abstract int ssGetTick();

	public abstract float ssGetAttR(boolean noDeathReduce);

	public State getState() {
		try {
			return State.valueOf(getEntityData().get(SS_STATE));
		} catch (Throwable e) {
			setState(State.ALIVE);
			return State.ALIVE;
		}
	}

	public void setState(State state) {
		getEntityData().set(SS_STATE, state.name());
		getEntityData().set(SS_LSTATE, ssGetTick());
	}

	public int stateTime() {
		return ssGetTick() - getEntityData().get(SS_LSTATE);
	}

	protected static void registerGoals(PathfinderMob zhis) {
		zhis.goalSelector.addGoal(1, new MeleeAttackGoal(zhis, 1.5, true) {
			@Override
			public double getAttackReachSqr(LivingEntity entity) {
				return (double) ATTACK_RANGE * (double) ATTACK_RANGE;
			}
		});
		zhis.targetSelector.addGoal(2, new HurtByTargetGoal(zhis));
		zhis.goalSelector.addGoal(3, new RandomStrollGoal(zhis, 1.5));
		zhis.goalSelector.addGoal(4, new RandomLookAroundGoal(zhis));
		zhis.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(zhis, LivingEntity.class, false, false));
	}

	public static float openFieldPgs(int stateTime, float partialTick) {
		return (float) Math.pow(Math.max(0F, Math.min(1f, (stateTime + partialTick) < SuperSteveEntityBase.DEATH_ACTIVE[4] ? ((float) (stateTime + partialTick - SuperSteveEntityBase.DEATH_ACTIVE[3]) / (float) SuperSteveEntityBase.DEATH_ACTIVE[0] * 5F) : (1f - (((float) stateTime + partialTick - SuperSteveEntityBase.DEATH_ACTIVE[4]) / ((float) SuperSteveEntityBase.DEATH_ACTIVE[0] - SuperSteveEntityBase.DEATH_ACTIVE[4]))))), 4);
	}

	public static enum SSMode {
		NORMAL, PLZLIZI
	}
	public static class Attack {
		public int tick = 0;
		public final int life;
		public final Vec3 rot;
		public final Vec3 pos;
		public final Vec2 size;

		public Attack(int life, Vec3 rot, Vec3 pos, Vec2 size) {
			this.life = life;
			this.rot = rot == null ? new Vec3(SSUtil.randfloat(0, 360), SSUtil.randfloat(0, 360), SSUtil.randfloat(0, 360)) : rot;
			this.pos = pos;
			this.size = size;
		}
	}
	public static enum State {
		ENTER, ALIVE, EXIT;
	}
}
