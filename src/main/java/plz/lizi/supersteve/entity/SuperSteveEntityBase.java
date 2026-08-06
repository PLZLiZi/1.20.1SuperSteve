package plz.lizi.supersteve.entity;

import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3f;
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
import plz.lizi.supersteve.api.PLZBase;
import plz.lizi.supersteve.api.SSUtil;
import plz.lizi.supersteve.level.SSBossEvent;

public abstract class SuperSteveEntityBase extends PathfinderMob {
	public static final float ATTACK_RANGE = 4F;
	public static final int MAX_INVULNERABLE_TICK = 40;
	public static final float MAX_HEALTH = 20F;
	public static final int[] ENTER_ACTIVE = { 110/* 入场时长 */, 108/* 爆炸产生 */, 0/* 方块下落开始 / 环出现 / 多边形出现 */, 80/* 方块下落结束 / 环最大 / 多边形大 */, 80/* 方块合并开始 */, 100/* 方块合并结束 */ };
	public static final int[] DEATH_ACTIVE = { 750/* 死亡时长 */, 0/* 落剑开始 */, 80/* 落剑结束 */, 0/* 领域展开 */, 730/* 领域收回 */, 20/* 声音开始播放 */ };
	public static final EntityDataAccessor<String> SS_HEALTH = SynchedEntityData.defineId(SuperSteveEntityBase.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<String> SS_TYPE = SynchedEntityData.defineId(SuperSteveEntityBase.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<Integer> SS_TICK = SynchedEntityData.defineId(SuperSteveEntityBase.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<String> SS_STATE = SynchedEntityData.defineId(SuperSteveEntityBase.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<Integer> SS_LSTATE = SynchedEntityData.defineId(SuperSteveEntityBase.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Vector3f> SS_SAFE_POS = SynchedEntityData.defineId(SuperSteveEntityBase.class, EntityDataSerializers.VECTOR3);
	public final byte[] key = new byte[SSUtil.randint(10, 99)];
	public final List<Attack> attacks =  new ArrayList<>();
	public Operator health = o -> MAX_HEALTH;
	public long[/* 0: tick, 1: llmax - time */] hurtData = { 0, Long.MAX_VALUE - System.currentTimeMillis() };
	public SSBossEvent bossEvent;

	protected SuperSteveEntityBase(EntityType<? extends PathfinderMob> p_21683_, Level p_21684_) {
		super(p_21683_, p_21684_);
		PLZBase.setField(this, false, "health", (Operator) o -> {
			if (o.length == 2 && o[0] instanceof Float fhealth && o[1] == (Integer) key.length) {
				try {
					getEntityData().set(SS_HEALTH, "SSH" + (Integer.rotateLeft(Float.floatToRawIntBits(fhealth) ^ (int) getUUID().getMostSignificantBits(), 13) ^ (int) getUUID().getMostSignificantBits()));
					// int x = Integer.rotateLeft(Float.floatToRawIntBits(fhealth) ^ 0x114514, 13) ^ 0x917813;
					// getEntityData().set(SS_HEALTH, "SSH=" + Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[] { (byte) (x >> 24), (byte) (x >> 16), (byte) (x >> 8), (byte) x }));
					// getEntityData().set(SS_HEALTH, "SSH=" + String.format("%08X", Float.floatToRawIntBits(Math.max(0, fhealth)) ^ 0xF917813F));
				} catch (Throwable e) {
				}
			} else if (o.length == 0) {
				try {
					String ssh = getEntityData().get(SuperSteveEntityBase.SS_HEALTH);
					if (ssh.startsWith("SSH")) {
						return Float.intBitsToFloat(Integer.rotateRight(Integer.parseInt(ssh.substring(3, ssh.length())) ^ (int) getUUID().getMostSignificantBits(), 13) ^ (int) getUUID().getMostSignificantBits());
						// byte[] data = Base64.getUrlDecoder().decode(ssh.substring(4, ssh.length()));
						// return Float.intBitsToFloat(Integer.rotateRight((((data[0] & 0xFF) << 24) | ((data[1] & 0xFF) << 16) | ((data[2] & 0xFF) << 8) | (data[3] & 0xFF)) ^ 0x917813, 13) ^ 0x114514);
						// return Float.intBitsToFloat((int) Long.parseLong(ssh.substring(4, ssh.length()), 16) ^ 0xF917813F);
					}
				} catch (Throwable e) {
				}
				health.operate(MAX_HEALTH, null);
				return MAX_HEALTH;
			}
			return null;
		});
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.5);
		builder = builder.add(Attributes.MAX_HEALTH, MAX_HEALTH);
		builder = builder.add(Attributes.ARMOR, 32767);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 32767);
		builder = builder.add(Attributes.FOLLOW_RANGE, 2048);
		builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 32767);
		builder = builder.add(Attributes.ATTACK_KNOCKBACK, 0);
		return builder;
	}

	public abstract void ssTick(boolean threadCall);

	public abstract SSMode ssGetMode();

	public abstract void ssSetMode(SSMode mode);

	public abstract int ssGetTick();

	public abstract float ssGetAttR(boolean noDeathReduce);

	public State getState() {
		try {
			State state = State.valueOf(getEntityData().get(SS_STATE));
			State newState = state;
			int stateTime = stateTime();
			if (state == State.ENTER && stateTime > ENTER_ACTIVE[0])
				newState = State.ALIVE;
			else if (state == State.ALIVE && (float) health.operate() <= 0F)
				newState = State.EXIT;
			if (state != newState)
				setState(newState);
			return newState;
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
	public static interface Operator {
		Object operate(Object... o);
	}
}
