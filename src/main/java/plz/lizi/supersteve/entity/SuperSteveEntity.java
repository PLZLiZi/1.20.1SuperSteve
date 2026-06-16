package plz.lizi.supersteve.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import com.google.common.collect.ImmutableList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.Brain.Provider;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.registries.ForgeRegistries;
import plz.lizi.supersteve.api.EntityInstance;
import plz.lizi.supersteve.api.SSUtil;
import plz.lizi.supersteve.client.sound.SSMusic;
import plz.lizi.supersteve.init.SSModEntities;
import plz.lizi.supersteve.init.SSModItems;
import plz.lizi.supersteve.init.SSModSounds;
import plz.lizi.supersteve.level.SSBossEvent;

public class SuperSteveEntity extends SuperSteveEntityBase {
	private ItemStack eopl = ItemStack.EMPTY;
	private ItemStack head32k = ItemStack.EMPTY;
	private ItemStack chest32k = ItemStack.EMPTY;
	private ItemStack legs32k = ItemStack.EMPTY;
	private ItemStack boot32k = ItemStack.EMPTY;
	private ItemStack mainhand32k = ItemStack.EMPTY;
	private ItemStack offhand32k = ItemStack.EMPTY;
	private LivingEntity target;
	private Vec3 safePos = Vec3.ZERO;
	private List<ItemEntity> drops = new CopyOnWriteArrayList<>();

	private void init() {
		eyeHeight = 1.62F;
		maxUpStep = 0.6F;
		xpReward = 100;
		setNoAi(false);
		eopl = new ItemStack(SSModItems.ENDOFPLZ_LITE.get());
		head32k = SSUtil.make32K(new ItemStack(Items.NETHERITE_HELMET));
		chest32k = SSUtil.make32K(new ItemStack(Items.NETHERITE_CHESTPLATE));
		legs32k = SSUtil.make32K(new ItemStack(Items.NETHERITE_LEGGINGS));
		boot32k = SSUtil.make32K(new ItemStack(Items.NETHERITE_BOOTS));
		mainhand32k = SSUtil.make32K(new ItemStack(Items.NETHERITE_SWORD));
		offhand32k = ItemStack.EMPTY;
		setItemSlot(EquipmentSlot.MAINHAND, mainhand32k);
		setItemSlot(EquipmentSlot.OFFHAND, offhand32k);
		setItemSlot(EquipmentSlot.HEAD, head32k);
		setItemSlot(EquipmentSlot.CHEST, chest32k);
		setItemSlot(EquipmentSlot.LEGS, legs32k);
		setItemSlot(EquipmentSlot.FEET, boot32k);
		if (!level.isClientSide()) {
			drops.add(new ItemEntity(level, getX(), getY(), getZ(), mainhand32k, new Random().nextDouble() * 0.2 - 0.1, 0.2, new Random().nextDouble() * 0.2 - 0.1));
			drops.add(new ItemEntity(level, getX(), getY(), getZ(), offhand32k, new Random().nextDouble() * 0.2 - 0.1, 0.2, new Random().nextDouble() * 0.2 - 0.1));
			drops.add(new ItemEntity(level, getX(), getY(), getZ(), head32k, new Random().nextDouble() * 0.2 - 0.1, 0.2, new Random().nextDouble() * 0.2 - 0.1));
			drops.add(new ItemEntity(level, getX(), getY(), getZ(), chest32k, new Random().nextDouble() * 0.2 - 0.1, 0.2, new Random().nextDouble() * 0.2 - 0.1));
			drops.add(new ItemEntity(level, getX(), getY(), getZ(), legs32k, new Random().nextDouble() * 0.2 - 0.1, 0.2, new Random().nextDouble() * 0.2 - 0.1));
			drops.add(new ItemEntity(level, getX(), getY(), getZ(), boot32k, new Random().nextDouble() * 0.2 - 0.1, 0.2, new Random().nextDouble() * 0.2 - 0.1));
			drops.add(new ItemEntity(level, getX(), getY(), getZ(), SSUtil.make32K(new ItemStack(Items.NETHERITE_PICKAXE)), new Random().nextDouble() * 0.2 - 0.1, 0.2, new Random().nextDouble() * 0.2 - 0.1));
			drops.add(new ItemEntity(level, getX(), getY(), getZ(), SSUtil.make32K(new ItemStack(Items.NETHERITE_HOE)), new Random().nextDouble() * 0.2 - 0.1, 0.2, new Random().nextDouble() * 0.2 - 0.1));
			drops.add(new ItemEntity(level, getX(), getY(), getZ(), SSUtil.make32K(new ItemStack(Items.NETHERITE_AXE)), new Random().nextDouble() * 0.2 - 0.1, 0.2, new Random().nextDouble() * 0.2 - 0.1));
			drops.add(new ItemEntity(level, getX(), getY(), getZ(), SSUtil.make32K(new ItemStack(Items.NETHERITE_SHOVEL)), new Random().nextDouble() * 0.2 - 0.1, 0.2, new Random().nextDouble() * 0.2 - 0.1));
			if (new Random().nextInt(2) == 0) {
				drops.add(new ItemEntity(level, getX(), getY(), getZ(), new ItemStack(SSModItems.SSP_SIGN_SPLINTER.get()), new Random().nextDouble() * 0.2 - 0.1, 0.2, new Random().nextDouble() * 0.2 - 0.1));
			}
		}
		setPersistenceRequired();
	}

	private void ssInit() {
		if (!level.isClientSide()) {
			SSUtil.SS_INSTANCES.putIfAbsent(getId(), new EntityInstance<>());
			SSUtil.SS_INSTANCES.get(getId()).put(this);
		} else {
			if (SSUtil.ONLY_SERVER)
				SSUtil.SS_INSTANCES.getOrDefault(getId(), new EntityInstance<>()).put(this);
			else {
				SSUtil.SS_INSTANCES.putIfAbsent(getId(), new EntityInstance<>());
				SSUtil.SS_INSTANCES.get(getId()).put(this);
			}
		}
	}

	public SuperSteveEntity(PlayMessages.SpawnEntity packet, Level world) {
		super(SSModEntities.SUPER_STEVE.get(), world);
		setId(packet.getEntityId());
		setUUID(packet.getUuid());
		syncPacketPositionCodec(packet.getPosX(), packet.getPosY(), packet.getPosZ());
		absMoveTo(packet.getPosX(), packet.getPosY(), packet.getPosZ(), (float) (packet.getYaw() * 360) / 256.0F, (float) (packet.getPitch() * 360) / 256.0F);
		setYHeadRot((float) (packet.getHeadYaw() * 360) / 256.0F);
		setYBodyRot((float) (packet.getHeadYaw() * 360) / 256.0F);
		setPos(packet.getPosX(), packet.getPosY(), packet.getPosZ());
		init();
	}

	public SuperSteveEntity(EntityType<SuperSteveEntityBase> type, Level world) {
		super(type, world);
		init();
		if (!level.isClientSide()) {
			bossEvent = new SSBossEvent(getUUID(), getCustomName(), ServerBossEvent.BossBarColor.WHITE, ServerBossEvent.BossBarOverlay.PROGRESS);
		}
	}

	@Override
	public SynchedEntityData getEntityData() {
		return this.entityData;
	}

	@Override
	public void tick() {
		int tick = ssGetTick();
		getEntityData().set(SS_TICK, tick + 1);
		State state = getState();
		int stateTime = stateTime();
		if (state == State.ENTER && tick >= ENTER_ACTIVE[0])
			setState(State.ALIVE);
		else if (state == State.ALIVE && !isAlive())
			setState(State.EXIT);
		ssTick(false);
		if (level instanceof ServerLevel sl) {
			if (state == State.ENTER) {
				if (tick == ENTER_ACTIVE[1])
					sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0, 0);
				else if (tick == ENTER_ACTIVE[0])
					sl.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("multiplayer.player.joined", getCustomName()).withStyle(ChatFormatting.YELLOW), false);
			} else if (state == State.EXIT) {
				float fieldSz = openFieldPgs(stateTime, 0) * ssGetAttR(true) * 8f;
				for (var t : sl.getAllEntities()) {
					if (t == null || !(t instanceof LivingEntity livingEntity) || t instanceof SuperSteveEntityBase || distanceTo(t) > fieldSz)
						continue;
					SSUtil.forceHurtEx(livingEntity, level.damageSources().generic(), Math.max(2F, Math.abs(Math.max(livingEntity.getMaxHealth() / 40F, livingEntity.getHealth() / 40F))));
				}
			}
		} else if (level instanceof ClientLevel cl) {
			var attItr = attacks.iterator();
			while (attItr.hasNext()) {
				Attack att = attItr.next();
				if (att.tick > att.life) {
					attItr.remove();
					continue;
				}
				att.tick++;
			}
			if (state == State.ENTER || state == State.ALIVE) {
				SSMusic.playWithEntity(this, SSModSounds.FUKUMA_MIZUSHI1.get(), true);
			} else if (state == State.EXIT) {
				float fieldSz = openFieldPgs(stateTime, 0) * ssGetAttR(true) * 8f;
				for (var t : cl.entitiesForRendering()) {
					if (t == null || !(t instanceof LivingEntity) || t instanceof SuperSteveEntityBase || distanceTo(t) > fieldSz)
						continue;
					attacks.add(new Attack(SSUtil.randint(5, 10), null, t.position.add(SSUtil.randfloat(-0.5F, 0.5F), t.getBbHeight() / 2d + SSUtil.randfloat(-0.5F, 0.5F), SSUtil.randfloat(-0.5F, 0.5F)), new Vec2(SSUtil.randfloat(0.5f, 1.5f), SSUtil.randfloat(0.5f, 1.5f))));
				}
			}
		}
		iInvulnerableTime = Math.max(0, iInvulnerableTime - 1);
		baseTick();
		this.updatingUsingItem();
		this.updateSwimAmount();
		if (!this.level().isClientSide) {
			int i = this.getArrowCount();
			if (i > 0) {
				if (this.removeArrowTime <= 0) {
					this.removeArrowTime = 20 * (30 - i);
				}
				--this.removeArrowTime;
				if (this.removeArrowTime <= 0) {
					this.setArrowCount(i - 1);
				}
			}
			int j = this.getStingerCount();
			if (j > 0) {
				if (this.removeStingerTime <= 0) {
					this.removeStingerTime = 20 * (30 - j);
				}
				--this.removeStingerTime;
				if (this.removeStingerTime <= 0) {
					this.setStingerCount(j - 1);
				}
			}
			this.detectEquipmentUpdates();
			if (this.tickCount % 20 == 0) {
				this.getCombatTracker().recheckStatus();
			}
			if (this.isSleeping() && !this.checkBedExists()) {
				this.stopSleeping();
			}
		}
		// if (!this.isRemoved()) {
		this.aiStep();
		// }
		double d1 = this.getX() - this.xo;
		double d0 = this.getZ() - this.zo;
		float f = (float) (d1 * d1 + d0 * d0);
		float f1 = this.yBodyRot;
		float f2 = 0.0F;
		this.oRun = this.run;
		float f3 = 0.0F;
		if (f > 0.0025000002F) {
			f3 = 1.0F;
			f2 = (float) Math.sqrt((double) f) * 3.0F;
			float f4 = (float) Mth.atan2(d0, d1) * (180F / (float) Math.PI) - 90.0F;
			float f5 = Mth.abs(Mth.wrapDegrees(this.getYRot()) - f4);
			if (95.0F < f5 && f5 < 265.0F) {
				f1 = f4 - 180.0F;
			} else {
				f1 = f4;
			}
		}
		if (this.attackAnim > 0.0F) {
			f1 = this.getYRot();
		}
		if (!this.onGround()) {
			f3 = 0.0F;
		}
		this.run += (f3 - this.run) * 0.3F;
		this.level().getProfiler().push("headTurn");
		f2 = this.tickHeadTurn(f1, f2);
		this.level().getProfiler().pop();
		this.level().getProfiler().push("rangeChecks");
		while (this.getYRot() - this.yRotO < -180.0F) {
			this.yRotO -= 360.0F;
		}
		while (this.getYRot() - this.yRotO >= 180.0F) {
			this.yRotO += 360.0F;
		}
		while (this.yBodyRot - this.yBodyRotO < -180.0F) {
			this.yBodyRotO -= 360.0F;
		}
		while (this.yBodyRot - this.yBodyRotO >= 180.0F) {
			this.yBodyRotO += 360.0F;
		}
		while (this.getXRot() - this.xRotO < -180.0F) {
			this.xRotO -= 360.0F;
		}
		while (this.getXRot() - this.xRotO >= 180.0F) {
			this.xRotO += 360.0F;
		}
		while (this.yHeadRot - this.yHeadRotO < -180.0F) {
			this.yHeadRotO -= 360.0F;
		}
		while (this.yHeadRot - this.yHeadRotO >= 180.0F) {
			this.yHeadRotO += 360.0F;
		}
		this.level().getProfiler().pop();
		this.animStep += f2;
		if (this.isFallFlying()) {
			++this.fallFlyTicks;
		} else {
			this.fallFlyTicks = 0;
		}
		if (this.isSleeping()) {
			this.setXRot(0.0F);
		}
		if (!this.level().isClientSide) {
			this.tickLeash();
			if (this.tickCount % 5 == 0) {
				this.updateControlFlags();
			}
		}
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		this.bossEvent.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {}

	@Override
	public void customServerAiStep() {
		this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
	}

	@Override
	public boolean hasEffect(MobEffect p_21024_) {
		return false;
	}

	@Override
	public MobEffectInstance getEffect(MobEffect p_21125_) {
		return null;
	}

	@Override
	public boolean addEffect(MobEffectInstance p_21165_) {
		return false;
	}

	@Override
	public Collection<MobEffectInstance> getActiveEffects() {
		return new ArrayList<>();
	}

	@Override
	public Map<MobEffect, MobEffectInstance> getActiveEffectsMap() {
		return new HashMap<>();
	}

	@Override
	public void forceAddEffect(MobEffectInstance p_147216_, Entity p_147217_) {}

	@Override
	public boolean addEffect(MobEffectInstance p_147208_, Entity p_147209_) {
		return false;
	}

	@Override
	public float ssGetHealth() {
		return (float) getHealth.get();
	}

	@Override
	public void onAddedToWorld() {
		isAddedToWorld = true;
		ssInit();
	}

	@Override
	public float getSpeed() {
		return 1.0F;
	}

	@Override
	public EntityType<?> getType() {
		return SSModEntities.SUPER_STEVE.get();
	}

	@Override
	public ItemStack getItemBySlot(EquipmentSlot p_21467_) {
		if (p_21467_ == EquipmentSlot.MAINHAND) {
			return (ssGetMode() == SuperSteveEntityBase.SSMode.PLZLIZI) ? eopl : mainhand32k;
		} else if (p_21467_ == EquipmentSlot.OFFHAND) {
			return (ssGetMode() == SuperSteveEntityBase.SSMode.PLZLIZI) ? ItemStack.EMPTY : offhand32k;
		} else if (p_21467_ == EquipmentSlot.HEAD) {
			return (ssGetMode() == SuperSteveEntityBase.SSMode.PLZLIZI) ? ItemStack.EMPTY : head32k;
		} else if (p_21467_ == EquipmentSlot.CHEST) {
			return (ssGetMode() == SuperSteveEntityBase.SSMode.PLZLIZI) ? ItemStack.EMPTY : chest32k;
		} else if (p_21467_ == EquipmentSlot.LEGS) {
			return (ssGetMode() == SuperSteveEntityBase.SSMode.PLZLIZI) ? ItemStack.EMPTY : legs32k;
		} else if (p_21467_ == EquipmentSlot.FEET) {
			return (ssGetMode() == SuperSteveEntityBase.SSMode.PLZLIZI) ? ItemStack.EMPTY : boot32k;
		} else {
			return ItemStack.EMPTY;
		}
	}

	@Override
	public ItemStack getItemInHand(InteractionHand p_21121_) {
		if (p_21121_ == InteractionHand.MAIN_HAND) {
			return (ssGetMode() == SuperSteveEntityBase.SSMode.PLZLIZI) ? eopl : mainhand32k;
		} else if (p_21121_ == InteractionHand.OFF_HAND) {
			return (ssGetMode() == SuperSteveEntityBase.SSMode.PLZLIZI) ? ItemStack.EMPTY : offhand32k;
		} else {
			return ItemStack.EMPTY;
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		try {
			return NetworkHooks.getEntitySpawningPacket(this);
		} catch (Throwable e) {
			return super.getAddEntityPacket();
		}
	}

	@Override
	public void serverAiStep() {
		++this.noActionTime;
		this.level().getProfiler().push("sensing");
		this.sensing.tick();
		this.level().getProfiler().pop();
		int i = this.level().getServer().getTickCount() + this.getId();
		if (i % 2 != 0 && this.tickCount > 1) {
			this.level().getProfiler().push("targetSelector");
			this.targetSelector.tickRunningGoals(false);
			this.level().getProfiler().pop();
			this.level().getProfiler().push("goalSelector");
			this.goalSelector.tickRunningGoals(false);
			this.level().getProfiler().pop();
		} else {
			this.level().getProfiler().push("targetSelector");
			this.targetSelector.tick();
			this.level().getProfiler().pop();
			this.level().getProfiler().push("goalSelector");
			this.goalSelector.tick();
			this.level().getProfiler().pop();
		}
		this.level().getProfiler().push("navigation");
		this.navigation.tick();
		this.level().getProfiler().pop();
		this.level().getProfiler().push("mob tick");
		this.customServerAiStep();
		this.level().getProfiler().pop();
		this.level().getProfiler().push("controls");
		this.level().getProfiler().push("move");
		this.moveControl.tick();
		this.level().getProfiler().popPush("look");
		this.lookControl.tick();
		this.level().getProfiler().popPush("jump");
		this.jumpControl.tick();
		this.level().getProfiler().pop();
		this.level().getProfiler().pop();
		this.sendDebugPackets();
	}

	@Override
	public void registerGoals() {
		SuperSteveEntityBase.registerGoals(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void aiStep() {
		if (this.noJumpDelay > 0) {
			--this.noJumpDelay;
		}
		if (this.isControlledByLocalInstance()) {
			this.lerpSteps = 0;
			this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());
		}
		if (this.lerpSteps > 0) {
			double d0 = this.getX() + (this.lerpX - this.getX()) / (double) this.lerpSteps;
			double d2 = this.getY() + (this.lerpY - this.getY()) / (double) this.lerpSteps;
			double d4 = this.getZ() + (this.lerpZ - this.getZ()) / (double) this.lerpSteps;
			double d6 = Mth.wrapDegrees(this.lerpYRot - (double) this.getYRot());
			this.setYRot(this.getYRot() + (float) d6 / (float) this.lerpSteps);
			this.setXRot(this.getXRot() + (float) (this.lerpXRot - (double) this.getXRot()) / (float) this.lerpSteps);
			--this.lerpSteps;
			this.setPos(d0, d2, d4);
			this.setRot(this.getYRot(), this.getXRot());
		} else if (!this.isEffectiveAi()) {
			this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
		}
		if (this.lerpHeadSteps > 0) {
			this.yHeadRot += (float) Mth.wrapDegrees(this.lyHeadRot - (double) this.yHeadRot) / (float) this.lerpHeadSteps;
			--this.lerpHeadSteps;
		}
		Vec3 vec31 = this.getDeltaMovement();
		double d1 = vec31.x;
		double d3 = vec31.y;
		double d5 = vec31.z;
		if (Math.abs(vec31.x) < 0.003) {
			d1 = (double) 0.0F;
		}
		if (Math.abs(vec31.y) < 0.003) {
			d3 = (double) 0.0F;
		}
		if (Math.abs(vec31.z) < 0.003) {
			d5 = (double) 0.0F;
		}
		this.setDeltaMovement(d1, d3, d5);
		this.level().getProfiler().push("ai");
		if (this.isImmobile()) {
			this.jumping = false;
			this.xxa = 0.0F;
			this.zza = 0.0F;
		} else if (this.isEffectiveAi()) {
			this.level().getProfiler().push("newAi");
			this.serverAiStep();
			this.level().getProfiler().pop();
		}
		this.level().getProfiler().pop();
		this.level().getProfiler().push("jump");
		if (this.jumping && this.isAffectedByFluids()) {
			FluidType fluidType = this.getMaxHeightFluidType();
			double d7;
			if (!fluidType.isAir()) {
				d7 = this.getFluidTypeHeight(fluidType);
			} else if (this.isInLava()) {
				d7 = this.getFluidHeight(FluidTags.LAVA);
			} else {
				d7 = this.getFluidHeight(FluidTags.WATER);
			}
			boolean flag = this.isInWater() && d7 > (double) 0.0F;
			double d8 = this.getFluidJumpThreshold();
			if (!flag || this.onGround() && !(d7 > d8)) {
				if (!this.isInLava() || this.onGround() && !(d7 > d8)) {
					if (fluidType.isAir() || this.onGround() && !(d7 > d8)) {
						if ((this.onGround() || flag && d7 <= d8) && this.noJumpDelay == 0) {
							this.jumpFromGround();
							this.noJumpDelay = 10;
						}
					} else {
						this.jumpInFluid(fluidType);
					}
				} else {
					this.jumpInFluid((FluidType) ForgeMod.LAVA_TYPE.get());
				}
			} else {
				this.jumpInFluid((FluidType) ForgeMod.WATER_TYPE.get());
			}
		} else {
			this.noJumpDelay = 0;
		}
		this.level().getProfiler().pop();
		this.level().getProfiler().push("travel");
		this.xxa *= 0.98F;
		this.zza *= 0.98F;
		this.updateFallFlying();
		AABB aabb = this.getBoundingBox();
		Vec3 vec3 = new Vec3((double) this.xxa, (double) this.yya, (double) this.zza);
		if (this.hasEffect(MobEffects.SLOW_FALLING) || this.hasEffect(MobEffects.LEVITATION)) {
			this.resetFallDistance();
		}
		label111: {
			LivingEntity livingentity = this.getControllingPassenger();
			if (livingentity instanceof Player player) {
				if (this.isAlive()) {
					this.travelRidden(player, vec3);
					break label111;
				}
			}
			this.travel(vec3);
		}
		this.level().getProfiler().pop();
		this.level().getProfiler().push("freezing");
		if (!this.level().isClientSide && !this.isDeadOrDying()) {
			int i = this.getTicksFrozen();
			if (this.isInPowderSnow && this.canFreeze()) {
				this.setTicksFrozen(Math.min(this.getTicksRequiredToFreeze(), i + 1));
			} else {
				this.setTicksFrozen(Math.max(0, i - 2));
			}
		}
		this.removeFrost();
		this.tryAddFrost();
		if (!this.level().isClientSide && this.tickCount % 40 == 0 && this.isFullyFrozen() && this.canFreeze()) {
			this.hurt(this.damageSources().freeze(), 1.0F);
		}
		this.level().getProfiler().pop();
		this.level().getProfiler().push("push");
		if (this.autoSpinAttackTicks > 0) {
			--this.autoSpinAttackTicks;
			this.checkAutoSpinAttack(aabb, this.getBoundingBox());
		}
		this.pushEntities();
		this.level().getProfiler().pop();
		if (!this.level().isClientSide && this.isSensitiveToWater() && this.isInWaterRainOrBubble()) {
			this.hurt(this.damageSources().drown(), 1.0F);
		}
		this.level().getProfiler().push("looting");
		Vec3i vec3i = this.getPickupReach();
		if (!this.level().isClientSide && this.canPickUpLoot() && this.isAlive() && !this.dead && ForgeEventFactory.getMobGriefingEvent(this.level(), this)) {
			for (ItemEntity itementity : this.level().getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate((double) vec3i.getX(), (double) vec3i.getY(), (double) vec3i.getZ()))) {
				if (!itementity.isRemoved() && !itementity.getItem().isEmpty() && !itementity.hasPickUpDelay() && this.wantsToPickUp(itementity.getItem())) {
					this.pickUpItem(itementity);
				}
			}
		}
		this.level().getProfiler().pop();
		updateSwingTime();
	}

	@Override
	public MobType getMobType() {
		return MobType.UNDEFINED;
	}

	@Override
	public double getMyRidingOffset() {
		return -0.35D;
	}

	@Override
	public float getMaxHealth() {
		return 20.0F;
	}

	@Override
	public boolean hurt(DamageSource damagesource, float amount) {
		if (!isAlive()) {
			return false;
		}
		if (damagesource.getEntity() != null && damagesource.is(DamageTypes.PLAYER_ATTACK) && damagesource.getEntity() instanceof Player player) {
			if (player.getMainHandItem().equals(new ItemStack(SSModItems.ENDOFPLZ_LITE.get()), false)) {
				SSUtil.killEntity(this);
				return true;
			} else {
				if (ssGetTick() < 0 || distanceTo(player) > ATTACK_RANGE)
					return false;
				if (iInvulnerableTime <= 0) {
					{
						// TODO:彩蛋
						String playerName = player.getGameProfile().getName();
						if (playerName.equals("shugangan") || playerName.equals("shiki214ein")) {
							if (!level().isClientSide()) {
								drops.clear();
								((ServerLevel) level()).addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), new ItemStack(SSModItems.ENDOFPLZ_LITE.get()), new Random().nextDouble() * 0.2 - 0.1, 0.2, new Random().nextDouble() * 0.2 - 0.1));
								player.sendSystemMessage(Component.translatable("entity.supersteve.special_message").withStyle(ChatFormatting.YELLOW));
							}
							setHealth.accept(0F);
							return true;
						}
					}
					float attackPst = Math.min(1F, Math.max(0.1F, (float) (amount / SSUtil.getMaxDamageInBag(player, (SuperSteveEntityBase) this))));
					if (Float.isInfinite(attackPst) || Float.isNaN(attackPst)) {
						attackPst = 1F;
					}
					boolean plzlizi = ssGetMode() == SuperSteveEntityBase.SSMode.PLZLIZI;
					setHealth.accept(ssGetHealth() - (level().isClientSide() ? 0 : (new Random().nextFloat(plzlizi ? 0.03f : 0.1f, plzlizi ? 0.04f : 0.2f) * attackPst)));
					// ssSetHealth(ssGetHealth() - SSUtil.randfloat(10, 20));
					super.hurt(damagesource, 0F);
					iInvulnerableTime = MAX_INVULNERABLE_TIME;
					SSUtil.forceHurtEx((Player) damagesource.getEntity(), damageSources().generic(), amount);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean isNoGravity() {
		return getState() != State.ALIVE;
	}

	@Override
	public boolean shouldShowName() {
		return true;
	}

	@Override
	public boolean ignoreExplosion() {
		return true;
	}

	@Override
	public boolean fireImmune() {
		return true;
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	public boolean isPushable() {
		return true;
	}

	@Override
	public void doPush(Entity entityIn) {}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	public void ssTick(boolean threadCall) {
		try {
			if (isAlive()) {
				SSUtil.safeEntity(this);
				if (getCustomName().getString().toLowerCase().equals("plzlizi") && ssGetMode() != SuperSteveEntityBase.SSMode.PLZLIZI) {
					ssSetMode(SuperSteveEntityBase.SSMode.PLZLIZI);
				}
				float size = ssGetAttR(false);
				for (var entity : level().getEntities(EntityTypeTest.forClass(Entity.class), new AABB(position(), position()).inflate(size), SSUtil.ENTITY_EVERYTHING)) {
					if (distanceTo(entity) > size)
						continue;
					doHurtTarget(entity, threadCall);
				}
				if (getY() < -65) {
					ssSetPos(threadCall, getX(), -65, getZ());
					var movement = getDeltaMovement();
					setDeltaMovement(movement.x, 1, movement.z);
				}
				if (Double.isNaN(getX()) || Double.isNaN(getY()) || Double.isNaN(getZ()) || Double.isInfinite(getX()) || Double.isInfinite(getY()) || Double.isInfinite(getZ()))
					ssSetPos(threadCall, safePos.x, safePos.y, safePos.z);
				else
					safePos = position();
				if (!level.isClientSide) {
					if (getTarget() != null && getTarget().level == level && getTarget().isAlive() && position().distanceTo(getTarget().position()) >= 64.0d) {
						safePos = getTarget().position();
						ssSetPos(threadCall, safePos.x, safePos.y, safePos.z);
					}
				}
			} else {
				SSUtil.killEntity(this, false);
			}
		} catch (Throwable e) {
			// e.printStackTrace();
		}
	}

	@Override
	public void handleInsidePortal(BlockPos p_20222_) {}

	@Override
	public boolean canChangeDimensions() {
		return false;
	}

	@Override
	public void setXRot(float p_146927_) {
		if (!Float.isFinite(p_146927_))
			return;
		super.setXRot(p_146927_);
	}

	@Override
	public void setYRot(float p_146923_) {
		if (!Float.isFinite(p_146923_))
			return;
		super.setYRot(p_146923_);
	}

	@Override
	public void setYBodyRot(float p_21309_) {
		if (!Float.isFinite(p_21309_))
			return;
		super.setYBodyRot(p_21309_);
	}

	@Override
	public void setYHeadRot(float p_21306_) {
		if (!Float.isFinite(p_21306_))
			return;
		super.setYHeadRot(p_21306_);
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public void setId(int p_20235_) {
		id = p_20235_;
	}

	@Override
	public UUID getUUID() {
		return uuid;
	}

	@Override
	public void setUUID(UUID p_20085_) {
		uuid = p_20085_;
		stringUUID = uuid.toString();
	}

	@Override
	public void setTarget(LivingEntity p_21544_) {
		target = SSUtil.getClosestEntity(this, 1024.0d);
	}

	@Override
	public LivingEntity getTarget() {
		if (target == null || !target.isAlive() || target instanceof SuperSteveEntityBase) {
			target = SSUtil.getClosestEntity(this, 1024.0d);
		}
		return target;
	}

	public void ssSetPos(boolean otherThread, double p_20344_, double p_20345_, double p_20346_) {
		if (this.position.x != p_20344_ || this.position.y != p_20345_ || this.position.z != p_20346_) {
			this.position = new Vec3(p_20344_, p_20345_, p_20346_);
			int i = Mth.floor(p_20344_);
			int j = Mth.floor(p_20345_);
			int k = Mth.floor(p_20346_);
			if (i != this.blockPosition.getX() || j != this.blockPosition.getY() || k != this.blockPosition.getZ()) {
				this.blockPosition = new BlockPos(i, j, k);
				this.feetBlockState = null;
				if (SectionPos.blockToSectionCoord(i) != this.chunkPosition.x || SectionPos.blockToSectionCoord(k) != this.chunkPosition.z) {
					this.chunkPosition = new ChunkPos(this.blockPosition);
				}
			}
			this.levelCallback.onMove();
		}
		if (!otherThread && this.isAddedToWorld() && !this.level.isClientSide && !this.isRemoved()) {
			this.level.getChunk((int) Math.floor(p_20344_) >> 4, (int) Math.floor(p_20346_) >> 4);
		}
		setBoundingBox(makeBoundingBox());
	}

	@Override
	public void setPos(double p_20344_, double p_20345_, double p_20346_) {
		ssSetPos(false, p_20344_, p_20345_, p_20346_);
	}

	@Override
	public void setHealth(float p_21154_) {
		// ssSetHealth(Math.max(ssGetHealth(), p_21154_));
	}

	@Override
	public float getHealth() {
		return ssGetHealth();
	}

	@Override
	public void teleportTo(double p_19887_, double p_19888_, double p_19889_) {}

	@Override
	public boolean teleportTo(ServerLevel p_265257_, double p_265407_, double p_265727_, double p_265410_, Set<RelativeMovement> p_265083_, float p_265573_, float p_265094_) {
		return false;
	}

	@Override
	public boolean shouldDropLoot() {
		return true;
	}

	@Override
	public void teleportRelative(double p_249341_, double p_252229_, double p_252038_) {}

	public boolean doHurtTarget(Entity entity, boolean threadCall) {
		if (entity == null || entity instanceof SuperSteveEntityBase || !(entity instanceof LivingEntity))
			return false;
		if (!(entity instanceof Player)) {
			SSUtil.killEntity(entity);
		} else {
			Player player = (Player) entity;
			if (!SSUtil.EOPL_PLAYERS.containsKey(player.getUUID())) {
				if (level instanceof ServerLevel serverLevel) {
					float hurtValue = (player.getMaxHealth() / 50.0F);
					if (ssGetMode() == SuperSteveEntityBase.SSMode.PLZLIZI) {
						if (player.getInventory().contains(new ItemStack(SSModItems.SSP_SIGN.get()))) {
							hurtValue = (player.getMaxHealth() / 20.0F);
						} else {
							hurtValue = -1;
						}
					}
					if (threadCall)
						hurtValue = 0;
					if (hurtValue > 0.0F) {
						SSUtil.forceHurtEx(player, player.damageSources().generic(), hurtValue);
						Random rand = new Random();
						double offsetX = (rand.nextDouble() - 0.5) * 1.0;
						double offsetY = (rand.nextDouble() - 0.5) * 1.0;
						double offsetZ = (rand.nextDouble() - 0.5) * 1.0;
						serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + offsetX, player.getY() + player.getBbHeight() / 2 + offsetY, player.getZ() + offsetZ, 0, 0, 0, 0, 0);
					} else if (hurtValue == 0) {
					} else {
						SSUtil.killPlayer(player);
					}
				}
			}
		}
		return true;
	}

	@Override
	public boolean doHurtTarget(Entity entity) {
		swing(InteractionHand.MAIN_HAND);
		return doHurtTarget(entity, false);
	}

	@Override
	public void swing(InteractionHand p_21007_) {
		super.swing(p_21007_);
	}

	@Override
	public void remove(RemovalReason p_276115_) {
		if (ssGetHealth() <= 0.0F) {
			setRemoved(p_276115_);
			brain.clearMemories();
		}
	}

	@Override
	public boolean isAlive() {
		return ssGetHealth() > 0.0F;
	}

	@Override
	public void removeAllGoals(Predicate<Goal> p_262667_) {}

	@Override
	public boolean isInvisible() {
		return getState() == State.ENTER;
	}

	@Override
	public boolean isInvisibleTo(Player p_20178_) {
		return false;
	}

	@Override
	public void tickEffects() {}

	@Override
	public void dropAllDeathLoot(DamageSource p_21192_) {
		if (level.isClientSide())
			return;
		if (ssGetHealth() <= 0.0F) {
			synchronized (drops) {
				for (var drop : drops) {
					drop.setPos(position());
					level.addFreshEntity(drop);
				}
				drops.clear();
			}
		}
	}

	@Override
	public void baseTick() {
		this.oAttackAnim = this.attackAnim;
		if (this.firstTick) {
			// this.getSleepingPos().ifPresent(this::setPosToBed);
		}
		if (this.canSpawnSoulSpeedParticle()) {
			this.spawnSoulSpeedParticle();
		}
		this.level().getProfiler().push("entityBaseTick");
		this.feetBlockState = null;
		if (this.isPassenger() && this.getVehicle().isRemoved()) {
			this.stopRiding();
		}
		if (this.boardingCooldown > 0) {
			--this.boardingCooldown;
		}
		this.walkDistO = this.walkDist;
		this.xRotO = this.getXRot();
		this.yRotO = this.getYRot();
		this.handleNetherPortal();
		if (this.canSpawnSprintParticle()) {
			this.spawnSprintParticle();
		}
		this.wasInPowderSnow = this.isInPowderSnow;
		this.isInPowderSnow = false;
		this.updateInWaterStateAndDoFluidPushing();
		this.updateFluidOnEyes();
		this.updateSwimming();
		if (this.level().isClientSide) {
			this.clearFire();
		} else if (this.remainingFireTicks > 0) {
			if (this.fireImmune()) {
				this.setRemainingFireTicks(this.remainingFireTicks - 4);
				if (this.remainingFireTicks < 0) {
					this.clearFire();
				}
			} else {
				if (this.remainingFireTicks % 20 == 0 && !this.isInLava()) {
					this.hurt(this.damageSources().onFire(), 1.0F);
				}
				this.setRemainingFireTicks(this.remainingFireTicks - 1);
			}
			if (this.getTicksFrozen() > 0) {
				this.setTicksFrozen(0);
				this.level().levelEvent((Player) null, 1009, this.blockPosition, 1);
			}
		}
		if (this.isInLava()) {
			this.lavaHurt();
			this.fallDistance *= this.getFluidFallDistanceModifier((FluidType) ForgeMod.LAVA_TYPE.get());
		}
		this.checkBelowWorld();
		if (!this.level().isClientSide) {
			this.setSharedFlagOnFire(this.remainingFireTicks > 0);
		}
		this.firstTick = false;
		this.level().getProfiler().pop();
		this.level().getProfiler().push("livingEntityBaseTick");
		if (this.fireImmune() || this.level().isClientSide) {
			this.clearFire();
		}
		if (this.isAlive()) {
			boolean flag = false;
			if (!this.level().isClientSide) {
				if (this.isInWall()) {
					this.hurt(this.damageSources().inWall(), 1.0F);
				} else if (flag && !this.level().getWorldBorder().isWithinBounds(this.getBoundingBox())) {
					double d0 = this.level().getWorldBorder().getDistanceToBorder(this) + this.level().getWorldBorder().getDamageSafeZone();
					if (d0 < (double) 0.0F) {
						double d1 = this.level().getWorldBorder().getDamagePerBlock();
						if (d1 > (double) 0.0F) {
							this.hurt(this.damageSources().outOfBorder(), (float) Math.max(1, Mth.floor(-d0 * d1)));
						}
					}
				}
			}
			int airSupply = this.getAirSupply();
			ForgeHooks.onLivingBreathe(this, airSupply - this.decreaseAirSupply(airSupply), this.increaseAirSupply(airSupply) - airSupply);
			if (!this.level().isClientSide) {
				BlockPos blockpos = this.blockPosition();
				if (!Objects.equals(this.lastPos, blockpos)) {
					this.lastPos = blockpos;
					this.onChangedBlock(blockpos);
				}
			}
		}
		this.extinguishFire();
		if (this.hurtTime > 0) {
			--this.hurtTime;
		}
		this.invulnerableTime = 0;
		if (this.isDeadOrDying()) {
			this.tickDeath();
		}
		if (this.lastHurtByPlayerTime > 0) {
			--this.lastHurtByPlayerTime;
		} else {
			this.lastHurtByPlayer = null;
		}
		if (this.lastHurtMob != null && !this.lastHurtMob.isAlive()) {
			this.lastHurtMob = null;
		}
		if (this.lastHurtByMob != null) {
			if (!this.lastHurtByMob.isAlive()) {
				this.setLastHurtByMob((LivingEntity) null);
			} else if (this.tickCount - this.lastHurtByMobTimestamp > 100) {
				this.setLastHurtByMob((LivingEntity) null);
			}
		}
		this.tickEffects();
		this.animStepO = this.animStep;
		this.yBodyRotO = this.yBodyRot;
		this.yHeadRotO = this.yHeadRot;
		this.yRotO = this.getYRot();
		this.xRotO = this.getXRot();
		this.level().getProfiler().pop();
		this.level().getProfiler().push("mobBaseTick");
		if (this.isAlive() && this.random.nextInt(1000) < this.ambientSoundTime++) {
			this.resetAmbientSoundTime();
			this.playAmbientSound();
		}
		this.level().getProfiler().pop();
	}

	@Override
	public void kill() {}

	@Override
	public void invalidateCaps() {}

	@Override
	public void tickDeath() {
		if (!isAlive()) {
			if (!level.isClientSide) {
				if (stateTime() >= DEATH_ACTIVE[0]) {
					SSUtil.killEntity(this);
					((ServerLevel) level).getServer().getPlayerList().broadcastSystemMessage(Component.translatable("multiplayer.player.left", getCustomName()).withStyle(ChatFormatting.YELLOW), false);
				}
			} else {
				if (stateTime() == DEATH_ACTIVE[5]) {
					SSMusic.endWithEntity(this);
					SSMusic.play(SSModSounds.FUKUMA_MIZUSHI2.get());
				}
			}
		}
	}

	@Override
	public void knockback(double p_147241_, double p_147242_, double p_147243_) {}

	@Override
	public void die(DamageSource p_21014_) {}

	@Override
	public void onRemovedFromWorld() {}

	@Override
	public boolean isAlwaysTicking() {
		return true;
	}

	@Override
	public boolean isAttackable() {
		return true;
	}

	@Override
	public boolean isFreezing() {
		return false;
	}

	@Override
	public boolean isDeadOrDying() {
		return ssGetHealth() <= 0.0F;
	}

	@Override
	public boolean canUpdate() {
		return true;
	}

	@Override
	public boolean canAttack(LivingEntity p_21171_) {
		return true;
	}

	@Override
	public boolean canFreeze() {
		return false;
	}

	@Override
	public boolean isNoAi() {
		// return true;
		return getState() != State.ALIVE;
	}

	@Override
	public boolean isAggressive() {
		return true;
	}

	@Override
	public void removeFreeWill() {}

	@Override
	public double getAttributeValue(Attribute p_21134_) {
		if (p_21134_ == Attributes.MAX_HEALTH) {
			return 20.0d;
		} else if (p_21134_ == Attributes.ARMOR) {
			return Double.MAX_VALUE;
		}
		return super.getAttributeValue(p_21134_);
	}

	@Override
	public void setCustomName(Component p_20053_) {
		if (level instanceof ServerLevel sl) {
			boolean playerIn = false;
			for (var sp : sl.players()) {
				if (sp.distanceTo(this) <= 4 && sp.getItemInHand(InteractionHand.MAIN_HAND).is(Items.NAME_TAG)) {
					playerIn = true;
					break;
				}
			}
			if (!playerIn)
				return;
		} else {
			return;
		}
		if (p_20053_.getString().toLowerCase().equals("plzlizi")) {
			ssSetMode(SuperSteveEntityBase.SSMode.PLZLIZI);
			setHealth.accept(20.0F);
			super.setCustomName(Component.literal("PLZLiZi"));
			setItemSlot(EquipmentSlot.MAINHAND, eopl);
			setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
			setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
			setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
			setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
			setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
			drops.clear();
			drops.add(new ItemEntity(level(), getX(), getY(), getZ(), eopl, new Random().nextDouble() * 0.2 - 0.1, 0.2, new Random().nextDouble() * 0.2 - 0.1));
		}
	}

	@Override
	public Component getDisplayName() {
		return getCustomName();
	}

	@Override
	public Component getCustomName() {
		return (ssGetMode() == SuperSteveEntityBase.SSMode.PLZLIZI) ? Component.literal("PLZLiZi") : Component.translatable("entity.supersteve.super_steve");
	}

	@Override
	public Component getName() {
		return getCustomName();
	}

	@Override
	public void onClientRemoval() {}

	@Override
	public SoundEvent getHurtSound(DamageSource ds) {
		return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.hurt"));
	}

	@Override
	public SoundEvent getDeathSound() {
		return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.death"));
	}

	@Override
	public SoundEvent getAmbientSound() {
		return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.stone.step"));
	}

	@Override
	public void setLevelCallback(EntityInLevelCallback p_146849_) {
		if (p_146849_ == EntityInLevelCallback.NULL)
			return;
		levelCallback = p_146849_;
	}

	@Override
	public void canUpdate(boolean value) {}

	@Override
	public Provider<?> brainProvider() {
		return Brain.provider(ImmutableList.of(), ImmutableList.of());
	}

	@Override
	public Brain<?> getBrain() {
		return super.getBrain();
	}

	@Override
	public void handleNetherPortal() {}

	@Override
	public void setPortalCooldown() {}

	@Override
	public void setPortalCooldown(int p_287760_) {}

	@Override
	public int getPortalCooldown() {
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean isOnPortalCooldown() {
		return true;
	}

	@Override
	public void processPortalCooldown() {}

	@Override
	public SSMode ssGetMode() {
		try {
			return SSMode.valueOf(getEntityData().get(SS_TYPE));
		} catch (Throwable e) {
			// TODO: Fix error format
			ssSetMode(SSMode.NORMAL);
			return SSMode.NORMAL;
		}
	}

	@Override
	public void ssSetMode(SSMode mode) {
		getEntityData().set(SS_TYPE, mode.name());
	}

	@Override
	public int ssGetTick() {
		return getEntityData().get(SS_TICK);
	}

	@Override
	public void load(CompoundTag p_20259_) {
		super.load(p_20259_);
		if (bossEvent != null) {
			bossEvent.removeAllPlayers();
			bossEvent.setVisible(false);
		}
		bossEvent = new SSBossEvent(getUUID(), getCustomName(), ServerBossEvent.BossBarColor.WHITE, ServerBossEvent.BossBarOverlay.PROGRESS);
	}

	@Override
	public void defineSynchedData() {
		super.defineSynchedData();
		getEntityData().define(SS_HEALTH, "");
		getEntityData().define(SS_TYPE, SSMode.NORMAL.name());
		getEntityData().define(SS_TICK, 0);
		getEntityData().define(SS_STATE, State.ENTER.name());
		getEntityData().define(SS_LSTATE, 0);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag pCompound) {
		super.addAdditionalSaveData(pCompound);
		pCompound.putString("SSH", getEntityData().get(SS_HEALTH));
		pCompound.putString("SSM", ssGetMode().name());
		pCompound.putString("SSS", getState().name());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag pCompound) {
		super.readAdditionalSaveData(pCompound);
		if (pCompound.contains("SSH"))
			getEntityData().set(SS_HEALTH, pCompound.getString("SSH"));
		if (pCompound.contains("SSM"))
			ssSetMode(SSMode.valueOf(pCompound.getString("SSM")));
		if (pCompound.contains("SSS"))
			getEntityData().set(SS_STATE, pCompound.getString("SSS"));
	}

	@Override
	public float ssGetAttR(boolean noDeathReduce) {
		return (noDeathReduce || getState() == State.ALIVE) ? (ssGetMode() == SuperSteveEntityBase.SSMode.PLZLIZI ? ATTACK_RANGE * 3F : ATTACK_RANGE) : 0;
	}

	@Override
	public boolean shouldRender(double p_20296_, double p_20297_, double p_20298_) {
		return true;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double p_19883_) {
		return true;
	}

	@Override
	public void setTicksFrozen(int p_146918_) {}

	@Override
	public int getTicksFrozen() {
		return 0;
	}

	@Override
	public void move(MoverType p_19973_, Vec3 p_19974_) {
		super.move(p_19973_, p_19974_);
	}
}
