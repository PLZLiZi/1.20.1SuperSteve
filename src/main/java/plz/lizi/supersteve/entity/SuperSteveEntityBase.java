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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import plz.lizi.supersteve.entity.ai.SSFlyingGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import plz.lizi.supersteve.api.PLZBase;
import plz.lizi.supersteve.api.SSUtil;
import plz.lizi.supersteve.level.SSBossEvent;

public abstract class SuperSteveEntityBase extends PathfinderMob {
	public static final float ATTACK_RANGE = 5F;
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
	//private static final Operator HEALTH_MATH = o2 -> {
	//	if (o2.length == 2 && o2[0] instanceof Float f && o2[1] instanceof Integer vrf) {
	//		int bits = Float.floatToRawIntBits(f);
	//		int mixed = Integer.rotateLeft(bits ^ vrf, 13) ^ vrf;
	//		long u = mixed & 0xFFFFFFFFL;
	//		char[] buf = new char[12];
	//		int pos = 10;
	//		do {
	//			buf[--pos] = (char) ('{' + (int) (u % 10));
	//			u /= 10;
	//		} while (u != 0);
	//		int check = Math.floorMod(mixed ^ (vrf * 31), 100);
	//		buf[10] = (char) ('{' + check / 10);
	//		buf[11] = (char) ('{' + check % 10);
	//		int mainLen = 10 - pos;
	//		char[] result = new char[mainLen + 2];
	//		System.arraycopy(buf, pos, result, 0, mainLen);
	//		result[mainLen] = buf[10];
	//		result[mainLen + 1] = buf[11];
	//		return new String(result);
	//	} else if (o2.length == 2 && o2[0] instanceof String s && o2[1] instanceof Integer vrf) {
	//		if (s == null || s.length() < 3)
	//			return null;
	//		int len = s.length();
	//		int c1 = s.charAt(len - 2) - '{';
	//		int c2 = s.charAt(len - 1) - '{';
	//		if (c1 < 0 || c1 > 9 || c2 < 0 || c2 > 9)
	//			return null;
	//		int expectedCheck = c1 * 10 + c2;
	//		long value = 0;
	//		for (int i = 0; i < len - 2; i++) {
	//			int d = s.charAt(i) - '{';
	//			if (d < 0 || d > 9)
	//				return null;
	//			value = value * 10 + d;
	//		}
	//		int mixed = (int) value;
	//		int actualCheck = Math.floorMod(mixed ^ (vrf * 31), 100);
	//		if (actualCheck != expectedCheck)
	//			return null;
	//		int bits = Integer.rotateRight(mixed ^ vrf, 13) ^ vrf;
	//		return Float.valueOf(Float.intBitsToFloat(bits));
	//	}
	//	return null;
	//};
	public final List<Attack> attacks = new ArrayList<>();
	public Operator health = o -> MAX_HEALTH;
	public long[/* 0: tick, 1: llmax - time */] hurtData = { 0, Long.MAX_VALUE - System.currentTimeMillis() };
	public SSBossEvent bossEvent;

	protected SuperSteveEntityBase(EntityType<? extends PathfinderMob> p_21683_, Level p_21684_) {
		super(p_21683_, p_21684_);
		PLZBase.setField(this, false, "health", (Operator) o -> {
			Operator HEALTH_MATH = o2 -> {
				if (o2.length == 2 && o2[0] instanceof Float f && o2[1] instanceof Integer vrf) {
					int bits = Float.floatToRawIntBits(f);
					int mixed = Integer.rotateLeft(bits ^ vrf, 13) ^ vrf;
					long u = mixed & 0xFFFFFFFFL;
					char[] buf = new char[12];
					int pos = 10;
					do {
						buf[--pos] = (char) ('{' + (int) (u % 10));
						u /= 10;
					} while (u != 0);
					int check = Math.floorMod(mixed ^ (vrf * 31), 100);
					buf[10] = (char) ('{' + check / 10);
					buf[11] = (char) ('{' + check % 10);
					int mainLen = 10 - pos;
					char[] result = new char[mainLen + 2];
					System.arraycopy(buf, pos, result, 0, mainLen);
					result[mainLen] = buf[10];
					result[mainLen + 1] = buf[11];
					return new String(result);
				} else if (o2.length == 2 && o2[0] instanceof String s && o2[1] instanceof Integer vrf) {
					if (s == null || s.length() < 3)
						return null;
					int len = s.length();
					int c1 = s.charAt(len - 2) - '{';
					int c2 = s.charAt(len - 1) - '{';
					if (c1 < 0 || c1 > 9 || c2 < 0 || c2 > 9)
						return null;
					int expectedCheck = c1 * 10 + c2;
					long value = 0;
					for (int i = 0; i < len - 2; i++) {
						int d = s.charAt(i) - '{';
						if (d < 0 || d > 9)
							return null;
						value = value * 10 + d;
					}
					int mixed = (int) value;
					int actualCheck = Math.floorMod(mixed ^ (vrf * 31), 100);
					if (actualCheck != expectedCheck)
						return null;
					int bits = Integer.rotateRight(mixed ^ vrf, 13) ^ vrf;
					return Float.valueOf(Float.intBitsToFloat(bits));
				}
				return null;
			};
			if (o.length == 2 && o[0] == health && o[1] instanceof Float fhealth) {
				try {
					int hash = getUUID().hashCode();
					getEntityData().set(SS_HEALTH, "SSH" + hash + (String) HEALTH_MATH.operate(fhealth, hash));
					// getEntityData().set(SS_HEALTH, "SSH" + hash + (Integer.rotateLeft(Float.floatToRawIntBits(fhealth > MAX_HEALTH ? MAX_HEALTH : (fhealth < 0 ? 0 : fhealth)) ^ hash, 13) ^ -hash));
					// int x = Integer.rotateLeft(Float.floatToRawIntBits(fhealth) ^ 0x114514, 13) ^ 0x917813;
					// getEntityData().set(SS_HEALTH, "SSH=" + Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[] { (byte) (x >> 24), (byte) (x >> 16), (byte) (x >> 8), (byte) x }));
					// getEntityData().set(SS_HEALTH, "SSH=" + String.format("%08X", Float.floatToRawIntBits(Math.max(0, fhealth)) ^ 0xF917813F));
				} catch (Throwable e) {
				}
			} else if (o.length == 0) {
				try {
					String ssh = getEntityData().get(SuperSteveEntityBase.SS_HEALTH);
					int hash = getUUID().hashCode();
					String verify = "SSH" + hash;
					if (ssh.startsWith(verify)) {
						// float h = Float.intBitsToFloat(Integer.rotateRight((Integer.parseInt(ssh.substring(verify.length(), ssh.length())) + hash) ^ -hash, 13) ^ hash);
						float h = (float) HEALTH_MATH.operate(ssh.substring(verify.length(), ssh.length()), hash);
						float lh = h > MAX_HEALTH ? MAX_HEALTH : (h < 0 ? 0 : h);
						if (lh != h)
							health.operate(health, lh);
						return lh;
						// byte[] data = Base64.getUrlDecoder().decode(ssh.substring(4, ssh.length()));
						// return Float.intBitsToFloat(Integer.rotateRight((((data[0] & 0xFF) << 24) | ((data[1] & 0xFF) << 16) | ((data[2] & 0xFF) << 8) | (data[3] & 0xFF)) ^ 0x917813, 13) ^ 0x114514);
						// return Float.intBitsToFloat((int) Long.parseLong(ssh.substring(4, ssh.length()), 16) ^ 0xF917813F);
					}
				} catch (Throwable e) {
				}
				health.operate(health, MAX_HEALTH);
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
			Object fh = health.operate();
			if (state == State.ENTER && stateTime > ENTER_ACTIVE[0])
				newState = State.ALIVE;
			else if (state == State.ALIVE && (float) fh <= 0F)
				newState = State.EXIT;
			else if (state == State.EXIT && (float) fh > 0F)
				newState = State.ALIVE;
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
		zhis.goalSelector.addGoal(3, new SSFlyingGoal(zhis, 1.5, 2.0F));
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
