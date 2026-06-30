package plz.lizi.supersteve.entity;

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
import net.minecraft.util.RandomSource;
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
import net.minecraftforge.event.entity.living.PotionColorCalculationEvent;
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
import plz.lizi.supersteve.power.SSCore;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import net.minecraft.BlockUtil;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.Direction;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class SuperSteveEntity extends SuperSteveEntityBase {
	private ItemStack eopl = ItemStack.EMPTY;
	private ItemStack head32k = ItemStack.EMPTY;
	private ItemStack chest32k = ItemStack.EMPTY;
	private ItemStack legs32k = ItemStack.EMPTY;
	private ItemStack boot32k = ItemStack.EMPTY;
	private ItemStack mainhand32k = ItemStack.EMPTY;
	private ItemStack offhand32k = ItemStack.EMPTY;
	private LivingEntity target;
	private Map<MobEffect, MobEffectInstance> noEffects = new HashMap<>();
	private List<ItemEntity> idrops = new CopyOnWriteArrayList<>();

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
		if (!level.isClientSide) {
			idrops.add(new ItemEntity(level, getX(), getY(), getZ(), mainhand32k, new Random().nextDouble() * 0.2 - 0.1, 0.2, new Random().nextDouble() * 0.2 - 0.1));
			idrops.add(new ItemEntity(level, getX(), getY(), getZ(), offhand32k, new Random().nextDouble() * 0.2 - 0.1, 0.2, new Random().nextDouble() * 0.2 - 0.1));
			idrops.add(new ItemEntity(level, getX(), getY(), getZ(), head32k, new Random().nextDouble() * 0.2 - 0.1, 0.2, new Random().nextDouble() * 0.2 - 0.1));
			idrops.add(new ItemEntity(level, getX(), getY(), getZ(), chest32k, new Random().nextDouble() * 0.2 - 0.1, 0.2, new Random().nextDouble() * 0.2 - 0.1));
			idrops.add(new ItemEntity(level, getX(), getY(), getZ(), legs32k, new Random().nextDouble() * 0.2 - 0.1, 0.2, new Random().nextDouble() * 0.2 - 0.1));
			idrops.add(new ItemEntity(level, getX(), getY(), getZ(), boot32k, new Random().nextDouble() * 0.2 - 0.1, 0.2, new Random().nextDouble() * 0.2 - 0.1));
			idrops.add(new ItemEntity(level, getX(), getY(), getZ(), SSUtil.make32K(new ItemStack(Items.NETHERITE_PICKAXE)), new Random().nextDouble() * 0.2 - 0.1, 0.2, new Random().nextDouble() * 0.2 - 0.1));
			idrops.add(new ItemEntity(level, getX(), getY(), getZ(), SSUtil.make32K(new ItemStack(Items.NETHERITE_HOE)), new Random().nextDouble() * 0.2 - 0.1, 0.2, new Random().nextDouble() * 0.2 - 0.1));
			idrops.add(new ItemEntity(level, getX(), getY(), getZ(), SSUtil.make32K(new ItemStack(Items.NETHERITE_AXE)), new Random().nextDouble() * 0.2 - 0.1, 0.2, new Random().nextDouble() * 0.2 - 0.1));
			idrops.add(new ItemEntity(level, getX(), getY(), getZ(), SSUtil.make32K(new ItemStack(Items.NETHERITE_SHOVEL)), new Random().nextDouble() * 0.2 - 0.1, 0.2, new Random().nextDouble() * 0.2 - 0.1));
			if (new Random().nextInt(2) == 0) {
				idrops.add(new ItemEntity(level, getX(), getY(), getZ(), new ItemStack(SSModItems.SSP_SIGN_SPLINTER.get()), new Random().nextDouble() * 0.2 - 0.1, 0.2, new Random().nextDouble() * 0.2 - 0.1));
			}
		}
		setPersistenceRequired();
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
		if (!level.isClientSide) {
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
		ssTick(false);
		if (level instanceof ServerLevel sl) {
			if (state == State.ENTER) {
				if (stateTime == ENTER_ACTIVE[1])
					sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0, 0);
				else if (stateTime == ENTER_ACTIVE[0])
					sl.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("multiplayer.player.joined", getCustomName()).withStyle(ChatFormatting.YELLOW), false);
			} else if (state == State.EXIT) {
				float fieldSz = openFieldPgs(stateTime, 0) * ssGetAttR(true) * 8f;
				for (var t : sl.getAllEntities()) {
					if (t == null || !(t instanceof LivingEntity livingEntity) || t instanceof SuperSteveEntityBase || distanceTo(t) > fieldSz)
						continue;
					SSUtil.forceHurtEx(livingEntity, level.damageSources().generic(), Math.max(2F, Math.abs(Math.max(livingEntity.getMaxHealth() / 40F, livingEntity.getHealth() / 40F))));
				}
			} else if (state == State.EXIT) {
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
		if (!this.level.isClientSide) {
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
		this.level.getProfiler().push("headTurn");
		f2 = this.tickHeadTurn(f1, f2);
		this.level.getProfiler().pop();
		this.level.getProfiler().push("rangeChecks");
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
		this.level.getProfiler().pop();
		this.animStep += f2;
		if (this.isFallFlying()) {
			++this.fallFlyTicks;
		} else {
			this.fallFlyTicks = 0;
		}
		if (this.isSleeping()) {
			this.setXRot(0.0F);
		}
		if (!this.level.isClientSide) {
			this.tickLeash();
			if (this.tickCount % 5 == 0) {
				this.updateControlFlags();
			}
		}
		if (SSCore.SERVER_TICK_MANAGER.containsKey(this))
			SSCore.SERVER_TICK_MANAGER.put(this, true);
		if (SSCore.CLIENT_TICK_MANAGER.containsKey(this))
			SSCore.CLIENT_TICK_MANAGER.put(this, true);
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
		noEffects.clear();
		if (!noEffects.isEmpty())
			noEffects = new HashMap<>();
		return noEffects.values();
	}

	@Override
	public Map<MobEffect, MobEffectInstance> getActiveEffectsMap() {
		noEffects.clear();
		if (!noEffects.isEmpty())
			noEffects = new HashMap<>();
		return noEffects;
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
		if (isAddedToWorld || level instanceof ServerLevel sl && (sl.getEntity(getUUID()) != this || sl.getEntity(getId()) != this)) {
			idrops.clear();
			SSUtil.killEntity(this);
			return;
		}
		isAddedToWorld = true;
		if (!level.isClientSide) {
			SSUtil.SS_INSTANCES.putIfAbsent(getId(), new EntityInstance<>());
			SSUtil.SS_INSTANCES.get(getId()).put(this);
		} else {
			SSUtil.SS_INSTANCES.putIfAbsent(getId(), new EntityInstance<>());
			SSUtil.SS_INSTANCES.get(getId()).set(this);
		}
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
		this.level.getProfiler().push("sensing");
		this.sensing.tick();
		this.level.getProfiler().pop();
		int i = this.level.getServer().getTickCount() + this.getId();
		if (i % 2 != 0 && this.tickCount > 1) {
			this.level.getProfiler().push("targetSelector");
			this.targetSelector.tickRunningGoals(false);
			this.level.getProfiler().pop();
			this.level.getProfiler().push("goalSelector");
			this.goalSelector.tickRunningGoals(false);
			this.level.getProfiler().pop();
		} else {
			this.level.getProfiler().push("targetSelector");
			this.targetSelector.tick();
			this.level.getProfiler().pop();
			this.level.getProfiler().push("goalSelector");
			this.goalSelector.tick();
			this.level.getProfiler().pop();
		}
		this.level.getProfiler().push("navigation");
		this.navigation.tick();
		this.level.getProfiler().pop();
		this.level.getProfiler().push("mob tick");
		this.customServerAiStep();
		this.level.getProfiler().pop();
		this.level.getProfiler().push("controls");
		this.level.getProfiler().push("move");
		this.moveControl.tick();
		this.level.getProfiler().popPush("look");
		this.lookControl.tick();
		this.level.getProfiler().popPush("jump");
		this.jumpControl.tick();
		this.level.getProfiler().pop();
		this.level.getProfiler().pop();
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
		this.level.getProfiler().push("ai");
		if (this.isImmobile()) {
			this.jumping = false;
			this.xxa = 0.0F;
			this.zza = 0.0F;
		} else if (this.isEffectiveAi()) {
			this.level.getProfiler().push("newAi");
			this.serverAiStep();
			this.level.getProfiler().pop();
		}
		this.level.getProfiler().pop();
		this.level.getProfiler().push("jump");
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
		this.level.getProfiler().pop();
		this.level.getProfiler().push("travel");
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
		this.level.getProfiler().pop();
		this.level.getProfiler().push("freezing");
		if (!this.level.isClientSide && !this.isDeadOrDying()) {
			int i = this.getTicksFrozen();
			if (this.isInPowderSnow && this.canFreeze()) {
				this.setTicksFrozen(Math.min(this.getTicksRequiredToFreeze(), i + 1));
			} else {
				this.setTicksFrozen(Math.max(0, i - 2));
			}
		}
		this.removeFrost();
		this.tryAddFrost();
		if (!this.level.isClientSide && this.tickCount % 40 == 0 && this.isFullyFrozen() && this.canFreeze()) {
			this.hurt(this.damageSources().freeze(), 1.0F);
		}
		this.level.getProfiler().pop();
		this.level.getProfiler().push("push");
		if (this.autoSpinAttackTicks > 0) {
			--this.autoSpinAttackTicks;
			this.checkAutoSpinAttack(aabb, this.getBoundingBox());
		}
		this.pushEntities();
		this.level.getProfiler().pop();
		if (!this.level.isClientSide && this.isSensitiveToWater() && this.isInWaterRainOrBubble()) {
			this.hurt(this.damageSources().drown(), 1.0F);
		}
		this.level.getProfiler().push("looting");
		Vec3i vec3i = this.getPickupReach();
		if (!this.level.isClientSide && this.canPickUpLoot() && this.isAlive() && !this.dead && ForgeEventFactory.getMobGriefingEvent(this.level, this)) {
			for (ItemEntity itementity : this.level.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate((double) vec3i.getX(), (double) vec3i.getY(), (double) vec3i.getZ()))) {
				if (!itementity.isRemoved() && !itementity.getItem().isEmpty() && !itementity.hasPickUpDelay() && this.wantsToPickUp(itementity.getItem())) {
					this.pickUpItem(itementity);
				}
			}
		}
		this.level.getProfiler().pop();
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

	public Entity old = null;

	@Override
	public boolean hurt(DamageSource damagesource, float amount) {
		if (!isAlive() || getState() != State.ALIVE)
			return false;
		if (damagesource.getEntity() != null && damagesource.is(DamageTypes.PLAYER_ATTACK) && damagesource.getEntity() instanceof Player player) {
			if (player.getMainHandItem().equals(new ItemStack(SSModItems.ENDOFPLZ_LITE.get()), false)) {
				SSUtil.killEntity(this);
				return true;
			} else {
				if (distanceTo(player) > ATTACK_RANGE)
					return false;
				if (iInvulnerableTime <= 0) {
					iInvulnerableTime = MAX_INVULNERABLE_TIME;
					{
						// TODO: 彩蛋
						String playerName = player.getGameProfile().getName();
						if (playerName.equals("shugangan") || playerName.equals("shiki214ein")) {
							if (!level.isClientSide) {
								idrops.clear();
								((ServerLevel) level).addFreshEntity(new ItemEntity(level, getX(), getY(), getZ(), new ItemStack(SSModItems.ENDOFPLZ_LITE.get()), new Random().nextDouble() * 0.2 - 0.1, 0.2, new Random().nextDouble() * 0.2 - 0.1));
								player.sendSystemMessage(Component.translatable("entity.supersteve.special_message").withStyle(ChatFormatting.YELLOW));
							}
							setHealth.accept(0F);
							return true;
						}
					}
					float attackPst = Math.min(1F, Math.max(0.1F, (float) (amount / SSUtil.getMaxDamageInBag(player, (SuperSteveEntityBase) this))));
					if (Float.isInfinite(attackPst) || Float.isNaN(attackPst))
						attackPst = 1F;
					boolean plzlizi = ssGetMode() == SuperSteveEntityBase.SSMode.PLZLIZI;
					setHealth.accept(ssGetHealth() - (level.isClientSide ? 0 : (SSUtil.randfloat(plzlizi ? 0.03f : 0.1f, plzlizi ? 0.08f : 0.4f) * attackPst)));
					super.hurt(damagesource, 0F);
					SSUtil.forceHurtEx(player, damageSources().generic(), amount);
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
				for (var entity : level.getEntities(EntityTypeTest.forClass(Entity.class), new AABB(position(), position()).inflate(size), SSUtil.ENTITY_EVERYTHING)) {
					if (distanceTo(entity) > size)
						continue;
					doHurtTarget(entity, threadCall);
				}
				if (getY() < -65) {
					ssSetPos(threadCall, getX(), -65, getZ());
					var movement = getDeltaMovement();
					setDeltaMovement(movement.x, 1, movement.z);
				}
				boolean badPos = false;
				for (double v2test : new double[] { getX(), getY(), getZ() })
					if (!Double.isFinite(v2test))
						badPos = true;
				if (badPos)
					setToSafePos(threadCall);
				else
					ssSetSafePos(position());
				if (!level.isClientSide && getTarget() != null && getTarget().level == level && getTarget().isAlive() && position().distanceTo(getTarget().position()) >= 64.0d) {
					ssSetSafePos(getTarget().position());
					setToSafePos(threadCall);
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
		for (double v2test : new double[] { p_20344_, p_20345_, p_20346_ })
			if (!Double.isFinite(v2test))
				return;
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
			if (!SSUtil.EOPL_OWNERS.containsKey(player.getUUID())) {
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
		if (level.isClientSide)
			return;
		if (ssGetHealth() <= 0F) {
			synchronized (idrops) {
				for (var drop : idrops) {
					drop.setPos(position());
					level.addFreshEntity(drop);
				}
				idrops.clear();
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
		this.level.getProfiler().push("entityBaseTick");
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
		if (this.level.isClientSide) {
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
				this.level.levelEvent((Player) null, 1009, this.blockPosition, 1);
			}
		}
		if (this.isInLava()) {
			this.lavaHurt();
			this.fallDistance *= this.getFluidFallDistanceModifier((FluidType) ForgeMod.LAVA_TYPE.get());
		}
		this.checkBelowWorld();
		if (!this.level.isClientSide) {
			this.setSharedFlagOnFire(this.remainingFireTicks > 0);
		}
		this.firstTick = false;
		this.level.getProfiler().pop();
		this.level.getProfiler().push("livingEntityBaseTick");
		if (this.fireImmune() || this.level.isClientSide) {
			this.clearFire();
		}
		if (this.isAlive()) {
			boolean flag = false;
			if (!this.level.isClientSide) {
				if (this.isInWall()) {
					this.hurt(this.damageSources().inWall(), 1.0F);
				} else if (flag && !this.level.getWorldBorder().isWithinBounds(this.getBoundingBox())) {
					double d0 = this.level.getWorldBorder().getDistanceToBorder(this) + this.level.getWorldBorder().getDamageSafeZone();
					if (d0 < (double) 0.0F) {
						double d1 = this.level.getWorldBorder().getDamagePerBlock();
						if (d1 > (double) 0.0F) {
							this.hurt(this.damageSources().outOfBorder(), (float) Math.max(1, Mth.floor(-d0 * d1)));
						}
					}
				}
			}
			int airSupply = this.getAirSupply();
			ForgeHooks.onLivingBreathe(this, airSupply - this.decreaseAirSupply(airSupply), this.increaseAirSupply(airSupply) - airSupply);
			if (!this.level.isClientSide) {
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
		this.level.getProfiler().pop();
		this.level.getProfiler().push("mobBaseTick");
		if (this.isAlive() && this.random.nextInt(1000) < this.ambientSoundTime++) {
			this.resetAmbientSoundTime();
			this.playAmbientSound();
		}
		this.level.getProfiler().pop();
	}

	@Override
	public void discard() {}

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
		return !isAlive();
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
			idrops.clear();
			idrops.add(new ItemEntity(level, getX(), getY(), getZ(), eopl, new Random().nextDouble() * 0.2 - 0.1, 0.2, new Random().nextDouble() * 0.2 - 0.1));
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

	public void setToSafePos(boolean threadCall) {
		var pos = ssGetSafePos();
		ssSetPos(threadCall, pos.x, pos.y, pos.z);
	}

	public Vec3 ssGetSafePos() {
		return new Vec3(getEntityData().get(SS_SAFE_POS));
	}

	public void ssSetSafePos(Vec3 pos) {
		getEntityData().set(SS_SAFE_POS, new Vector3f((float) pos.x, (float) pos.y, (float) pos.z));
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
		getEntityData().define(SS_SAFE_POS, new Vector3f(0));
	}

	@Override
	public void addAdditionalSaveData(CompoundTag pCompound) {
		super.addAdditionalSaveData(pCompound);
		pCompound.putBoolean("PersistenceRequired", true);
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
			getEntityData().set(SS_TYPE, pCompound.getString("SSM"));
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

	@Override
	public void setOldPosAndRot() {
		double d0 = this.getX();
		double d1 = this.getY();
		double d2 = this.getZ();
		this.xo = d0;
		this.yo = d1;
		this.zo = d2;
		this.xOld = d0;
		this.yOld = d1;
		this.zOld = d2;
		this.yRotO = this.getYRot();
		this.xRotO = this.getXRot();
	}

	@Override
	public int getBlockX() {
		return this.blockPosition.getX();
	}

	@Override
	public double getX() {
		return this.position.x;
	}

	@Override
	public double getX(double pScale) {
		return this.position.x + (double) this.getBbWidth() * pScale;
	}

	@Override
	public double getRandomX(double pScale) {
		return this.getX(((double) 2.0F * this.random.nextDouble() - (double) 1.0F) * pScale);
	}

	@Override
	public int getBlockY() {
		return this.blockPosition.getY();
	}

	@Override
	public double getY() {
		return this.position.y;
	}

	@Override
	public double getY(double pScale) {
		return this.position.y + (double) this.getBbHeight() * pScale;
	}

	@Override
	public double getRandomY() {
		return this.getY(this.random.nextDouble());
	}

	@Override
	public double getEyeY() {
		return this.position.y + (double) this.eyeHeight;
	}

	@Override
	public int getBlockZ() {
		return this.blockPosition.getZ();
	}

	@Override
	public double getZ() {
		return this.position.z;
	}

	@Override
	public double getZ(double pScale) {
		return this.position.z + (double) this.getBbWidth() * pScale;
	}

	@Override
	public double getRandomZ(double pScale) {
		return this.getZ(((double) 2.0F * this.random.nextDouble() - (double) 1.0F) * pScale);
	}

	@Override
	public float tickHeadTurn(float pYRot, float pAnimStep) {
		this.bodyRotationControl.clientTick();
		return pAnimStep;
	}

	@Override
	public Vec3 getDeltaMovement() {
		return this.deltaMovement;
	}

	@Override
	public float maxUpStep() {
		return 10;
	}

	@Override
	public boolean isEffectiveAi() {
		return !this.level.isClientSide;
	}

	@Override
	public void addDeltaMovement(Vec3 pAddend) {
		this.setDeltaMovement(this.getDeltaMovement().add(pAddend));
	}

	@Override
	public BlockPos blockPosition() {
		return this.blockPosition;
	}

	@Override
	public boolean canAddPassenger(Entity pPassenger) {
		return false;
	}

	@Override
	public boolean canAttackType(EntityType<?> pType) {
		return true;
	}

	@Override
	public boolean canBeHitByProjectile() {
		return false;
	}

	@Override
	public boolean canBeLeashed(Player pPlayer) {
		return false;
	}

	@Override
	public boolean canRide(Entity pVehicle) {
		return false;
	}

	@Override
	public AABB getBoundingBox() {
		return this.bb;
	}

	@Override
	@Nullable
	public String getEncodeId() {
		EntityType<?> entitytype = this.getType();
		ResourceLocation resourcelocation = EntityType.getKey(entitytype);
		return entitytype.canSerialize() && resourcelocation != null ? resourcelocation.toString() : null;
	}

	@Override
	public Vec3 getLookAngle() {
		return this.calculateViewVector(this.getXRot(), this.getYRot());
	}

	@Override
	public LookControl getLookControl() {
		return this.lookControl;
	}

	@Override
	public int getMaxAirSupply() {
		return 300;
	}

	@Override
	public int getMaxFallDistance() {
		return 30;
	}

	@Override
	public int getMaxHeadXRot() {
		return 40;
	}

	@Override
	public int getMaxHeadYRot() {
		return 75;
	}

	@Override
	public int getMaxSpawnClusterSize() {
		return 4;
	}

	@Override
	public Vec3 getMeleeAttackReferencePosition() {
		return this.position;
	}

	@Override
	public MoveControl getMoveControl() {
		return this.moveControl;
	}

	@Override
	public MovementEmission getMovementEmission() {
		return MovementEmission.ALL;
	}

	@Override
	public int getHeadRotSpeed() {
		return 10;
	}

	@Override
	public float getJumpBoostPower() {
		return this.hasEffect(MobEffects.JUMP) ? 0.1f * ((float) this.getEffect(MobEffects.JUMP).getAmplifier() + 1.0f) : 0.0f;
	}

	@Override
	public JumpControl getJumpControl() {
		return this.jumpControl;
	}

	@Override
	public float getJumpPower() {
		return 0.42f * this.getBlockJumpFactor() + this.getJumpBoostPower();
	}

	@Override
	public boolean isCurrentlyGlowing() {
		return false;
	}

	@Override
	public boolean isCustomNameVisible() {
		return true;
	}

	@Override
	public boolean isFullyFrozen() {
		return false;
	}

	@Override
	public boolean isLeashed() {
		return this.leashHolder != null;
	}

	@Override
	public void setRemoved(RemovalReason pRemovalReason) {}

	@Override
	public boolean isPersistenceRequired() {
		return true;
	}
	// TODO: override methods

	@Override
	public void absMoveTo(double pX, double pY, double pZ, float pYRot, float pXRot) {
		this.absMoveTo(pX, pY, pZ);
		this.setYRot(pYRot % 360.0f);
		this.setXRot(Mth.clamp(pXRot, -90.0f, 90.0f) % 360.0f);
		this.yRotO = this.getYRot();
		this.xRotO = this.getXRot();
	}

	@Override
	public void actuallyHurt(DamageSource pDamageSource, float pDamageAmount) {
		if (!this.isInvulnerableTo(pDamageSource)) {
			Entity entity;
			if ((pDamageAmount = ForgeHooks.onLivingHurt(this, pDamageSource, pDamageAmount)) <= 0.0f) {
				return;
			}
			pDamageAmount = this.getDamageAfterArmorAbsorb(pDamageSource, pDamageAmount);
			pDamageAmount = this.getDamageAfterMagicAbsorb(pDamageSource, pDamageAmount);
			float f1 = Math.max(pDamageAmount - this.getAbsorptionAmount(), 0.0f);
			this.setAbsorptionAmount(this.getAbsorptionAmount() - (pDamageAmount - f1));
			float f = pDamageAmount - f1;
			if (f > 0.0f && f < 3.4028235E37f && (entity = pDamageSource.getEntity()) instanceof ServerPlayer) {
				ServerPlayer serverplayer = (ServerPlayer) entity;
				serverplayer.awardStat(Stats.DAMAGE_DEALT_ABSORBED, Math.round(f * 10.0f));
			}
			if ((f1 = ForgeHooks.onLivingDamage(this, pDamageSource, f1)) != 0.0f) {
				this.getCombatTracker().recordDamage(pDamageSource, f1);
				this.setHealth(this.getHealth() - f1);
				this.setAbsorptionAmount(this.getAbsorptionAmount() - f1);
				this.gameEvent(GameEvent.ENTITY_DAMAGE);
			}
		}
	}

	@Override
	public void addPassenger(Entity pPassenger) {}

	@Override
	public boolean addTag(String pTag) {
		return this.tags.size() >= 1024 ? false : this.tags.add(pTag);
	}

	@Override
	public void animateHurt(float pYaw) {
		this.hurtTime = this.hurtDuration = 10;
	}

	@Override
	public double applyPistonMovementRestriction(Direction.Axis pAxis, double pDistance) {
		int i = pAxis.ordinal();
		double d0 = Mth.clamp(pDistance + this.pistonDeltas[i], -0.51, 0.51);
		pDistance = d0 - this.pistonDeltas[i];
		this.pistonDeltas[i] = d0;
		return pDistance;
	}

	@Override
	public void blockUsingShield(LivingEntity pAttacker) {
		pAttacker.blockedByShield(this);
	}

	@Override
	public void blockedByShield(LivingEntity pDefender) {
		pDefender.knockback(0.5, pDefender.getX() - this.getX(), pDefender.getZ() - this.getZ());
	}

	@Override
	public boolean broadcastToPlayer(ServerPlayer pPlayer) {
		return true;
	}

	@Override
	public void calculateEntityAnimation(boolean pIncludeHeight) {
		float f = (float) Mth.length(this.getX() - this.xo, pIncludeHeight ? this.getY() - this.yo : 0.0, this.getZ() - this.zo);
		this.updateWalkAnimation(f);
	}

	@Override
	public int calculateFallDamage(float pFallDistance, float pDamageMultiplier) {
		if (this.getType().is(EntityTypeTags.FALL_DAMAGE_IMMUNE)) {
			return 0;
		}
		MobEffectInstance mobeffectinstance = this.getEffect(MobEffects.JUMP);
		float f = mobeffectinstance == null ? 0.0f : (float) (mobeffectinstance.getAmplifier() + 1);
		return Mth.ceil((pFallDistance - 3.0f - f) * pDamageMultiplier);
	}

	@Override
	public Vec3 calculateUpVector(float pXRot, float pYRot) {
		return this.calculateViewVector(pXRot - 90.0f, pYRot);
	}

	@Override
	public Vec3 calculateViewVector(float pXRot, float pYRot) {
		float f = pXRot * ((float) Math.PI / 180);
		float f1 = -pYRot * ((float) Math.PI / 180);
		float f2 = Mth.cos(f1);
		float f3 = Mth.sin(f1);
		float f4 = Mth.cos(f);
		float f5 = Mth.sin(f);
		return new Vec3(f3 * f4, -f5, f2 * f4);
	}

	@Override
	public boolean canBeSeenAsEnemy() {
		return !this.isInvulnerable() && this.canBeSeenByAnyone();
	}

	@Override
	public boolean canBeSeenByAnyone() {
		return true;
	}

	@Override
	public boolean canEnterPose(Pose pPose) {
		return this.level().noCollision(this, this.getBoundingBoxForPose(pPose).deflate(1.0E-7));
	}

	@Override
	public boolean canReplaceCurrentItem(ItemStack pCandidate, ItemStack pExisting) {
		return true;
	}

	@Override
	public boolean canReplaceEqualItem(ItemStack pCandidate, ItemStack pExisting) {
		return false;
	}

	@Override
	public boolean canSpawnSoulSpeedParticle() {
		return this.tickCount % 5 == 0 && this.getDeltaMovement().x != 0.0 && this.getDeltaMovement().z != 0.0 && !this.isSpectator() && EnchantmentHelper.hasSoulSpeed(this) && this.onSoulSpeedBlock();
	}

	@Override
	public boolean canSpawnSprintParticle() {
		return this.isSprinting() && !this.isInWater() && !this.isSpectator() && !this.isCrouching() && !this.isInLava() && this.isAlive() && !this.isInFluidType();
	}

	@Override
	public boolean canSprint() {
		return false;
	}

	@Override
	@Nullable
	public Entity changeDimension(ServerLevel pDestination) {
		return this;
	}

	@Override
	public boolean checkBedExists() {
		return false;
	}

	@Override
	public void checkDespawn() {}

	@Override
	public void checkSlowFallDistance() {
		if (this.getDeltaMovement().y() > -0.5 && this.fallDistance > 1.0f) {
			this.fallDistance = 1.0f;
		}
	}

	@Override
	public boolean checkSpawnObstruction(LevelReader pLevel) {
		return !pLevel.containsAnyLiquid(this.getBoundingBox()) && pLevel.isUnobstructed(this);
	}

	@Override
	public boolean checkSpawnRules(LevelAccessor pLevel, MobSpawnType pSpawnReason) {
		return this.getWalkTargetValue(this.blockPosition(), pLevel) >= 0.0f;
	}

	@Override
	public void checkSupportingBlock(boolean pOnGround, @Nullable Vec3 pMovement) {
		if (pOnGround) {
			AABB aabb = this.getBoundingBox();
			AABB aabb1 = new AABB(aabb.minX, aabb.minY - 1.0E-6, aabb.minZ, aabb.maxX, aabb.minY, aabb.maxZ);
			Optional<BlockPos> optional = this.level.findSupportingBlock(this, aabb1);
			if (!optional.isPresent() && !this.onGroundNoBlocks) {
				if (pMovement != null) {
					AABB aabb2 = aabb1.move(-pMovement.x, 0.0, -pMovement.z);
					optional = this.level.findSupportingBlock(this, aabb2);
					this.mainSupportingBlockPos = optional;
				}
			} else {
				this.mainSupportingBlockPos = optional;
			}
			this.onGroundNoBlocks = optional.isEmpty();
		} else {
			this.onGroundNoBlocks = false;
			if (this.mainSupportingBlockPos.isPresent()) {
				this.mainSupportingBlockPos = Optional.empty();
			}
		}
	}

	@Override
	public ChunkPos chunkPosition() {
		return this.chunkPosition;
	}

	@Override
	public void clearRestriction() {
		this.restrictRadius = -1.0f;
	}

	@Override
	public void clearSleepingPos() {
		this.entityData.set(SLEEPING_POS_ID, Optional.empty());
	}

	@Override
	public void copyPosition(Entity pEntity) {
		this.moveTo(pEntity.getX(), pEntity.getY(), pEntity.getZ(), pEntity.getYRot(), pEntity.getXRot());
	}

	@Override
	@Deprecated
	public boolean couldAcceptPassenger() {
		return true;
	}

	@Override
	public BodyRotationControl createBodyControl() {
		return new BodyRotationControl(this);
	}

	@Override
	public HoverEvent createHoverEvent() {
		return new HoverEvent(HoverEvent.Action.SHOW_ENTITY, new HoverEvent.EntityTooltipInfo(this.getType(), this.getUUID(), this.getName()));
	}

	@Override
	public PathNavigation createNavigation(Level pLevel) {
		return new GroundPathNavigation(this, pLevel);
	}

	@Override
	public int decreaseAirSupply(int pCurrentAir) {
		int i = EnchantmentHelper.getRespiration(this);
		return i > 0 && this.random.nextInt(i + 1) > 0 ? pCurrentAir : pCurrentAir - 1;
	}

	@Override
	public void dismountTo(double pX, double pY, double pZ) {
		this.teleportTo(pX, pY, pZ);
	}

	@Override
	public boolean dismountsUnderwater() {
		return this.getType().is(EntityTypeTags.DISMOUNTS_UNDERWATER);
	}

	@Override
	public boolean displayFireAnimation() {
		return this.isOnFire() && !this.isSpectator();
	}

	@Override
	public float distanceTo(Entity pEntity) {
		float f = (float) (this.getX() - pEntity.getX());
		float f1 = (float) (this.getY() - pEntity.getY());
		float f2 = (float) (this.getZ() - pEntity.getZ());
		return Mth.sqrt(f * f + f1 * f1 + f2 * f2);
	}

	@Override
	public double distanceToSqr(double pX, double pY, double pZ) {
		double d0 = this.getX() - pX;
		double d1 = this.getY() - pY;
		double d2 = this.getZ() - pZ;
		return d0 * d0 + d1 * d1 + d2 * d2;
	}

	@Override
	public void dropExperience() {
		if (this.level() instanceof ServerLevel && !this.wasExperienceConsumed() && (this.isAlwaysExperienceDropper() || this.lastHurtByPlayerTime > 0 && this.shouldDropExperience() && this.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT))) {
			ExperienceOrb.award((ServerLevel) this.level(), this.position(), this.getExperienceReward());
		}
	}

	@Override
	public void dropLeash(boolean pBroadcastPacket, boolean pDropLeash) {
		if (this.leashHolder != null) {
			this.leashHolder = null;
			this.leashInfoTag = null;
			if (!this.level().isClientSide && pDropLeash) {
				this.spawnAtLocation(Items.LEAD);
			}
			if (!this.level().isClientSide && pBroadcastPacket && this.level() instanceof ServerLevel) {
				((ServerLevel) this.level()).getChunkSource().broadcast(this, new ClientboundSetEntityLinkPacket(this, null));
			}
		}
	}

	@Override
	public void ejectPassengers() {
		for (int i = this.passengers.size() - 1; i >= 0; --i) {
			((Entity) this.passengers.get(i)).stopRiding();
		}
	}

	@Override
	public void enchantSpawnedArmor(RandomSource pRandom, float pChanceMultiplier, EquipmentSlot pSlot) {
		ItemStack itemstack = this.getItemBySlot(pSlot);
		if (!itemstack.isEmpty() && pRandom.nextFloat() < 0.5f * pChanceMultiplier) {
			this.setItemSlot(pSlot, EnchantmentHelper.enchantItem(pRandom, itemstack, (int) (5.0f + pChanceMultiplier * (float) pRandom.nextInt(18)), false));
		}
	}

	@Override
	public void enchantSpawnedWeapon(RandomSource pRandom, float pChanceMultiplier) {
		if (!this.getMainHandItem().isEmpty() && pRandom.nextFloat() < 0.25f * pChanceMultiplier) {
			this.setItemSlot(EquipmentSlot.MAINHAND, EnchantmentHelper.enchantItem(pRandom, this.getMainHandItem(), (int) (5.0f + pChanceMultiplier * (float) pRandom.nextInt(18)), false));
		}
	}

	@Override
	public ItemStack equipItemIfPossible(ItemStack pStack) {
		EquipmentSlot equipmentslot = Mob.getEquipmentSlotForItem(pStack);
		ItemStack itemstack = this.getItemBySlot(equipmentslot);
		boolean flag = this.canReplaceCurrentItem(pStack, itemstack);
		if (equipmentslot.isArmor() && !flag) {
			equipmentslot = EquipmentSlot.MAINHAND;
			itemstack = this.getItemBySlot(equipmentslot);
			flag = itemstack.isEmpty();
		}
		if (flag && this.canHoldItem(pStack)) {
			double d0 = this.getEquipmentDropChance(equipmentslot);
			if (!itemstack.isEmpty() && (double) Math.max(this.random.nextFloat() - 0.1f, 0.0f) < d0) {
				this.spawnAtLocation(itemstack);
			}
			if (equipmentslot.isArmor() && pStack.getCount() > 1) {
				ItemStack itemstack1 = pStack.copyWithCount(1);
				this.setItemSlotAndDropWhenKilled(equipmentslot, itemstack1);
				return itemstack1;
			}
			this.setItemSlotAndDropWhenKilled(equipmentslot, pStack);
			return pStack;
		}
		return ItemStack.EMPTY;
	}

	@Override
	public boolean equipmentHasChanged(ItemStack pOldItem, ItemStack pNewItem) {
		return !ItemStack.matches(pNewItem, pOldItem);
	}

	@Override
	public void extinguishFire() {
		if (!this.level().isClientSide && this.wasOnFire) {
			this.playEntityOnFireExtinguishedSound();
		}
		this.clearFire();
	}

	@Override
	@Deprecated
	@Nullable
	@ApiStatus.OverrideOnly
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
		RandomSource randomsource = pLevel.getRandom();
		this.getAttribute(Attributes.FOLLOW_RANGE).addPermanentModifier(new AttributeModifier("Random spawn bonus", randomsource.triangle(0.0, 0.11485000000000001), AttributeModifier.Operation.MULTIPLY_BASE));
		if (randomsource.nextFloat() < 0.05f) {
			this.setLeftHanded(true);
		} else {
			this.setLeftHanded(false);
		}
		this.spawnType = pReason;
		return pSpawnData;
	}

	@Override
	protected double followLeashSpeed() {
		return 1.0;
	}

	@Override
	public float getAbsorptionAmount() {
		return this.absorptionAmount;
	}

	@Override
	public int getAirSupply() {
		return this.entityData.get(DATA_AIR_SUPPLY_ID);
	}

	@Override
	public float getArmorCoverPercentage() {
		Iterable<ItemStack> iterable = this.getArmorSlots();
		int i = 0;
		int j = 0;
		for (ItemStack itemstack : iterable) {
			if (!itemstack.isEmpty()) {
				++j;
			}
			++i;
		}
		return i > 0 ? (float) j / (float) i : 0.0f;
	}

	@Override
	public float getAttackAnim(float pPartialTick) {
		float f = this.attackAnim - this.oAttackAnim;
		if (f < 0.0f) {
			f += 1.0f;
		}
		return this.oAttackAnim + f * pPartialTick;
	}

	@Override
	public float getBlockExplosionResistance(Explosion pExplosion, BlockGetter pLevel, BlockPos pPos, BlockState pBlockState, FluidState pFluidState, float pExplosionPower) {
		return pExplosionPower;
	}

	@Override
	public float getBlockJumpFactor() {
		float f = this.level().getBlockState(this.blockPosition()).getBlock().getJumpFactor();
		float f1 = this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getJumpFactor();
		return (double) f == 1.0 ? f1 : f;
	}

	@Override
	public BlockPos getBlockPosBelowThatAffectsMyMovement() {
		return this.getOnPos(0.500001f);
	}

	@Override
	public float getBlockSpeedFactor() {
		BlockState blockstate = this.level().getBlockState(this.blockPosition());
		float f = blockstate.getBlock().getSpeedFactor();
		if (!blockstate.is(Blocks.WATER) && !blockstate.is(Blocks.BUBBLE_COLUMN)) {
			return (double) f == 1.0 ? this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getSpeedFactor() : f;
		}
		return f;
	}

	@Override
	public BlockState getBlockStateOn() {
		return this.level().getBlockState(this.getOnPos());
	}

	@Override
	@Deprecated
	public BlockState getBlockStateOnLegacy() {
		return this.level().getBlockState(this.getOnPosLegacy());
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		if (this.getItemBySlot(EquipmentSlot.HEAD).is(Items.DRAGON_HEAD)) {
			return this.getBoundingBox().inflate(0.5, 0.5, 0.5);
		}
		return this.getBoundingBox();
	}

	@Override
	public AABB getBoundingBoxForPose(Pose pPose) {
		EntityDimensions entitydimensions = this.getDimensions(pPose);
		float f = entitydimensions.width / 2.0f;
		Vec3 vec3 = new Vec3(this.getX() - (double) f, this.getY(), this.getZ() - (double) f);
		Vec3 vec31 = new Vec3(this.getX() + (double) f, this.getY() + (double) entitydimensions.height, this.getZ() + (double) f);
		return new AABB(vec3, vec31);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
		if (capability == ForgeCapabilities.ITEM_HANDLER && this.isAlive()) {
			if (facing == null) {
				return this.handlers[2].cast();
			}
			if (facing.getAxis().isVertical()) {
				return this.handlers[0].cast();
			}
			if (facing.getAxis().isHorizontal()) {
				return this.handlers[1].cast();
			}
		}
		return super.getCapability(capability, facing);
	}

	@Override
	public CombatTracker getCombatTracker() {
		return this.combatTracker;
	}

	@Override
	public Level getCommandSenderWorld() {
		return this.level;
	}

	@Override
	@Nullable
	public Entity getControlledVehicle() {
		return null;
	}

	@Override
	@Nullable
	public LivingEntity getControllingPassenger() {
		return null;
	}

	@Override
	public int getCurrentSwingDuration() {
		return 6;
	}

	@Override
	public float getDamageAfterArmorAbsorb(DamageSource pDamageSource, float pDamageAmount) {
		if (!pDamageSource.is(DamageTypeTags.BYPASSES_ARMOR)) {
			this.hurtArmor(pDamageSource, pDamageAmount);
			pDamageAmount = CombatRules.getDamageAfterAbsorb(pDamageAmount, this.getArmorValue(), (float) this.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
		}
		return pDamageAmount;
	}

	@Override
	public float getDamageAfterMagicAbsorb(DamageSource pDamageSource, float pDamageAmount) {
		float f2;
		if (pDamageSource.is(DamageTypeTags.BYPASSES_EFFECTS)) {
			return pDamageAmount;
		}
		if (this.hasEffect(MobEffects.DAMAGE_RESISTANCE) && !pDamageSource.is(DamageTypeTags.BYPASSES_RESISTANCE) && (f2 = (pDamageAmount) - (pDamageAmount = Math.max((pDamageAmount * (float) (25 - ((this.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1) * 5))) / 25.0f, 0.0f))) > 0.0f && f2 < 3.4028235E37f) {
			if ((Object) this instanceof ServerPlayer) {
				((ServerPlayer) (Object) this).awardStat(Stats.CUSTOM.get(Stats.DAMAGE_RESISTED), Math.round(f2 * 10.0f));
			} else if (pDamageSource.getEntity() instanceof ServerPlayer) {
				((ServerPlayer) pDamageSource.getEntity()).awardStat(Stats.CUSTOM.get(Stats.DAMAGE_DEALT_RESISTED), Math.round(f2 * 10.0f));
			}
		}
		if (pDamageAmount <= 0.0f) {
			return 0.0f;
		}
		if (pDamageSource.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
			return pDamageAmount;
		}
		int k = EnchantmentHelper.getDamageProtection(this.getArmorSlots(), pDamageSource);
		if (k > 0) {
			pDamageAmount = CombatRules.getDamageAfterMagicAbsorb(pDamageAmount, k);
		}
		return pDamageAmount;
	}

	@Override
	public int getDimensionChangingDelay() {
		return Integer.MAX_VALUE;
	}

	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity pPassenger) {
		return new Vec3(this.getX(), this.getBoundingBox().maxY, this.getZ());
	}

	@Override
	public ImmutableList<Pose> getDismountPoses() {
		return ImmutableList.of(Pose.STANDING);
	}

	@Override
	public Optional<BlockUtil.FoundRectangle> getExitPortal(ServerLevel pDestination, BlockPos pFindFrom, boolean pIsToNether, WorldBorder pWorldBorder) {
		return pDestination.getPortalForcer().findPortalAround(pFindFrom, pIsToNether, pWorldBorder);
	}

	@Override
	public int getExperienceReward() {
		if (this.xpReward > 0) {
			int i = this.xpReward;
			for (int j = 0; j < this.armorItems.size(); ++j) {
				if (this.armorItems.get(j).isEmpty() || !(this.armorDropChances[j] <= 1.0f))
					continue;
				i += 1 + this.random.nextInt(3);
			}
			for (int k = 0; k < this.handItems.size(); ++k) {
				if (this.handItems.get(k).isEmpty() || !(this.handDropChances[k] <= 1.0f))
					continue;
				i += 1 + this.random.nextInt(3);
			}
			return i;
		}
		return this.xpReward;
	}

	@Override
	public float getEyeHeight(Pose pPose, EntityDimensions pSize) {
		return pPose == Pose.SLEEPING ? 0.2f : this.getStandingEyeHeight(pPose, pSize);
	}

	@Override
	public FluidType getEyeInFluidType() {
		return this.forgeFluidTypeOnEyes;
	}

	@Override
	public Vec3 getEyePosition() {
		return new Vec3(this.getX(), this.getEyeY(), this.getZ());
	}

	@Override
	public SoundEvent getFallDamageSound(int pHeight) {
		return pHeight > 4 ? this.getFallSounds().big() : this.getFallSounds().small();
	}

	@Override
	public int getFallFlyingTicks() {
		return this.fallFlyTicks;
	}

	@Override
	public Fallsounds getFallSounds() {
		return new Fallsounds(SoundEvents.GENERIC_SMALL_FALL, SoundEvents.GENERIC_BIG_FALL);
	}

	@Override
	public BlockState getFeetBlockState() {
		if (this.feetBlockState == null) {
			this.feetBlockState = this.level().getBlockState(this.blockPosition());
		}
		return this.feetBlockState;
	}

	@Override
	@Nullable
	public Entity getFirstPassenger() {
		return null;
	}

	@Override
	public Vec3 getFluidFallingAdjustedMovement(double pGravity, boolean pIsFalling, Vec3 pDeltaMovement) {
		if (!this.isNoGravity() && !this.isSprinting()) {
			double d0 = pIsFalling && Math.abs(pDeltaMovement.y - 0.005) >= 0.003 && Math.abs(pDeltaMovement.y - pGravity / 16.0) < 0.003 ? -0.003 : pDeltaMovement.y - pGravity / 16.0;
			return new Vec3(pDeltaMovement.x, d0, pDeltaMovement.z);
		}
		return pDeltaMovement;
	}

	@Override
	public double getFluidJumpThreshold() {
		return (double) this.getEyeHeight() < 0.4 ? 0.0 : 0.4;
	}

	@Override
	public double getFluidTypeHeight(FluidType type) {
		return this.forgeFluidTypeHeight.getDouble(type);
	}

	@Override
	public float getFlyingSpeed() {
		return this.getControllingPassenger() instanceof Player ? this.getSpeed() * 0.1f : 0.02f;
	}

	@Override
	public float getFrictionInfluencedSpeed(float pFriction) {
		return this.onGround() ? this.getSpeed() * (0.21600002f / (pFriction * pFriction * pFriction)) : this.getFlyingSpeed();
	}

	@Override
	public Optional<BlockPos> getLastClimbablePos() {
		return this.lastClimbablePos;
	}

	@Override
	@Nullable
	public LivingEntity getLastHurtByMob() {
		return this.lastHurtByMob;
	}

	@Override
	public int getLastHurtByMobTimestamp() {
		return this.lastHurtByMobTimestamp;
	}

	@Override
	@Nullable
	public Entity getLeashHolder() {
		if (this.leashHolder == null && this.delayedLeashHolderId != 0 && this.level().isClientSide) {
			this.leashHolder = this.level().getEntity(this.delayedLeashHolderId);
		}
		return this.leashHolder;
	}

	@Override
	public Vec3 getLeashOffset(float pPartialTick) {
		return this.getLeashOffset();
	}

	@Override
	@Deprecated
	public float getLightLevelDependentMagicValue() {
		return this.level().hasChunkAt(this.getBlockX(), this.getBlockZ()) ? this.level().getLightLevelDependentMagicValue(BlockPos.containing(this.getX(), this.getEyeY(), this.getZ())) : 0.0f;
	}

	@Override
	public Vec3 getLightProbePosition(float pPartialTicks) {
		return this.getEyePosition(pPartialTicks);
	}

	@Override
	public AABB getLocalBoundsForPose(Pose pPose) {
		EntityDimensions entitydimensions = this.getDimensions(pPose);
		return new AABB(-entitydimensions.width / 2.0f, 0.0, -entitydimensions.width / 2.0f, entitydimensions.width / 2.0f, entitydimensions.height, entitydimensions.width / 2.0f);
	}

	@Override
	public float getNameTagOffsetY() {
		return this.getBbHeight() + 0.5f;
	}

	@Override
	public PathNavigation getNavigation() {
		return this.navigation;
	}

	@Override
	public BlockPos getOnPos() {
		return this.getOnPos(1.0E-5f);
	}

	@Override
	@Deprecated
	public BlockPos getOnPosLegacy() {
		return this.getOnPos(0.2f);
	}

	@Override
	public List<Entity> getPassengers() {
		return this.passengers;
	}

	@Override
	public Stream<Entity> getPassengersAndSelf() {
		return Stream.of(this);
	}

	@Override
	public double getPassengersRidingOffset() {
		return (double) this.dimensions.height * 0.75;
	}

	@Override
	public float getPathfindingMalus(BlockPathTypes pNodeType) {
		Mob mob1;
		Entity entity = this.getControlledVehicle();
		Mob mob = entity instanceof Mob && (mob1 = (Mob) entity).shouldPassengersInheritMalus() ? mob1 : this;
		Float f = mob.pathfindingMalus.get(pNodeType);
		return f == null ? pNodeType.getMalus() : f.floatValue();
	}

	@Override
	public double getPerceivedTargetDistanceSquareForMeleeAttack(LivingEntity pEntity) {
		return Math.max(this.distanceToSqr(pEntity.getMeleeAttackReferencePosition()), this.distanceToSqr(pEntity.position()));
	}

	@Override
	public float getPercentFrozen() {
		return 0;
	}

	@Override
	public int getPermissionLevel() {
		return Integer.MAX_VALUE;
	}

	@Override
	public CompoundTag getPersistentData() {
		if (this.persistentData == null) {
			this.persistentData = new CompoundTag();
		}
		return this.persistentData;
	}

	@Override
	public float getPickRadius() {
		return 0.0f;
	}

	@Override
	public PushReaction getPistonPushReaction() {
		return PushReaction.NORMAL;
	}

	@Override
	public int getPortalWaitTime() {
		return 0;
	}

	@Override
	public Pose getPose() {
		return this.entityData.get(DATA_POSE);
	}

	@Override
	public Vec3 getPosition(float pPartialTicks) {
		double d0 = Mth.lerp((double) pPartialTicks, this.xo, this.getX());
		double d1 = Mth.lerp((double) pPartialTicks, this.yo, this.getY());
		double d2 = Mth.lerp((double) pPartialTicks, this.zo, this.getZ());
		return new Vec3(d0, d1, d2);
	}

	@Override
	public VecDeltaCodec getPositionCodec() {
		return this.packetPositionCodec;
	}

	@Override
	public BlockPos getPrimaryStepSoundBlockPos(BlockPos pPos) {
		BlockPos blockpos = pPos.above();
		BlockState blockstate = this.level().getBlockState(blockpos);
		return !blockstate.is(BlockTags.INSIDE_STEP_SOUND_BLOCKS) && !blockstate.is(BlockTags.COMBINATION_STEP_SOUND_BLOCKS) ? pPos : blockpos;
	}

	@Override
	public Vec3 getRelativePortalPosition(Direction.Axis pAxis, BlockUtil.FoundRectangle pPortal) {
		return PortalShape.getRelativePosition(pPortal, pAxis, this.position(), this.getDimensions(this.getPose()));
	}

	@Override
	public BlockPos getRestrictCenter() {
		return this.restrictCenter;
	}

	@Override
	public float getRestrictRadius() {
		return this.restrictRadius;
	}

	@Override
	public float getRiddenSpeed(Player pPlayer) {
		return this.getSpeed();
	}

	@Override
	public Entity getRootVehicle() {
		return this;
	}

	@Override
	public Vec3 getRopeHoldPosition(float pPartialTicks) {
		return this.getPosition(pPartialTicks).add(0.0, (double) this.eyeHeight * 0.7, 0.0);
	}

	@Override
	public Vec2 getRotationVector() {
		return new Vec2(this.getXRot(), this.getYRot());
	}

	@Override
	public Stream<Entity> getSelfAndPassengers() {
		return Stream.of(this);
	}

	@Override
	public Optional<BlockPos> getSleepingPos() {
		return this.entityData.get(SLEEPING_POS_ID);
	}

	@Override
	@Nullable
	public MobSpawnType getSpawnType() {
		return this.spawnType;
	}

	@Override
	public float getStandingEyeHeight(Pose pPose, EntityDimensions pDimensions) {
		return pDimensions.height * 0.85f;
	}

	@Override
	public float getSwimAmount(float pPartialTicks) {
		return Mth.lerp(pPartialTicks, this.swimAmountO, this.swimAmount);
	}

	@Override
	public SoundEvent getSwimHighSpeedSplashSound() {
		return SoundEvents.GENERIC_SPLASH;
	}

	@Override
	public SoundEvent getSwimSound() {
		return SoundEvents.GENERIC_SWIM;
	}

	@Override
	public SoundEvent getSwimSplashSound() {
		return SoundEvents.GENERIC_SPLASH;
	}

	@Override
	public Set<String> getTags() {
		return this.tags;
	}

	@Override
	public int getTicksRequiredToFreeze() {
		return Integer.MAX_VALUE;
	}

	@Override
	public Component getTypeName() {
		return this.getType().getDescription();
	}

	@Override
	public Vec3 getUpVector(float pPartialTicks) {
		return this.calculateUpVector(this.getViewXRot(pPartialTicks), this.getViewYRot(pPartialTicks));
	}

	@Override
	@Nullable
	public Entity getVehicle() {
		return null;
	}

	@Override
	public Vec3 getViewVector(float pPartialTicks) {
		return this.calculateViewVector(this.getViewXRot(pPartialTicks), this.getViewYRot(pPartialTicks));
	}

	@Override
	public float getViewXRot(float pPartialTicks) {
		return pPartialTicks == 1.0f ? this.getXRot() : Mth.lerp(pPartialTicks, this.xRotO, this.getXRot());
	}

	@Override
	public float getViewYRot(float pPartialTicks) {
		return pPartialTicks == 1.0f ? this.yHeadRot : Mth.lerp(pPartialTicks, this.yHeadRotO, this.yHeadRot);
	}

	@Override
	public double getVisibilityPercent(@Nullable Entity pLookingEntity) {
		double d0 = 1.0;
		if (this.isDiscrete()) {
			d0 *= 0.8;
		}
		if (this.isInvisible()) {
			float f = this.getArmorCoverPercentage();
			if (f < 0.1f) {
				f = 0.1f;
			}
			d0 *= 0.7 * (double) f;
		}
		if (pLookingEntity != null) {
			ItemStack itemstack = this.getItemBySlot(EquipmentSlot.HEAD);
			EntityType<?> entitytype = pLookingEntity.getType();
			if (entitytype == EntityType.SKELETON && itemstack.is(Items.SKELETON_SKULL) || entitytype == EntityType.ZOMBIE && itemstack.is(Items.ZOMBIE_HEAD) || entitytype == EntityType.PIGLIN && itemstack.is(Items.PIGLIN_HEAD) || entitytype == EntityType.PIGLIN_BRUTE && itemstack.is(Items.PIGLIN_HEAD) || entitytype == EntityType.CREEPER && itemstack.is(Items.CREEPER_HEAD)) {
				d0 *= 0.5;
			}
		}
		return d0;
	}

	@Override
	public float getVisualRotationYInDegrees() {
		return this.yBodyRot;
	}

	@Override
	public float getVoicePitch() {
		return this.isBaby() ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.5f : (this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f;
	}

	@Override
	public float getWalkTargetValue(BlockPos pPos) {
		return this.getWalkTargetValue(pPos, this.level());
	}

	@Override
	public float getXRot() {
		return this.xRot;
	}

	@Override
	public float getYHeadRot() {
		return this.yHeadRot;
	}

	@Override
	public float getYRot() {
		return this.yRot;
	}

	@Override
	public void handleEntityEvent(byte pId) {
		if (pId == 20) {
			this.spawnAnim();
		} else {
			switch (pId) {
				case 3: {
					SoundEvent soundevent = this.getDeathSound();
					if (soundevent != null) {
						this.playSound(soundevent, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f);
					}
					if ((Object) this instanceof Player)
						break;
					this.setHealth(0.0f);
					this.die(this.damageSources().generic());
					break;
				}
				case 29: {
					this.playSound(SoundEvents.SHIELD_BLOCK, 1.0f, 0.8f + this.level().random.nextFloat() * 0.4f);
					break;
				}
				case 30: {
					this.playSound(SoundEvents.SHIELD_BREAK, 0.8f, 0.8f + this.level().random.nextFloat() * 0.4f);
					break;
				}
				case 46: {
					for (int j = 0; j < 128; ++j) {
						double d0 = (double) j / 127.0;
						float f = (this.random.nextFloat() - 0.5f) * 0.2f;
						float f1 = (this.random.nextFloat() - 0.5f) * 0.2f;
						float f2 = (this.random.nextFloat() - 0.5f) * 0.2f;
						double d1 = Mth.lerp(d0, this.xo, this.getX()) + (this.random.nextDouble() - 0.5) * (double) this.getBbWidth() * 2.0;
						double d2 = Mth.lerp(d0, this.yo, this.getY()) + this.random.nextDouble() * (double) this.getBbHeight();
						double d3 = Mth.lerp(d0, this.zo, this.getZ()) + (this.random.nextDouble() - 0.5) * (double) this.getBbWidth() * 2.0;
						this.level().addParticle(ParticleTypes.PORTAL, d1, d2, d3, f, f1, f2);
					}
					break;
				}
				case 47: {
					this.breakItem(this.getItemBySlot(EquipmentSlot.MAINHAND));
					break;
				}
				case 48: {
					this.breakItem(this.getItemBySlot(EquipmentSlot.OFFHAND));
					break;
				}
				case 49: {
					this.breakItem(this.getItemBySlot(EquipmentSlot.HEAD));
					break;
				}
				case 50: {
					this.breakItem(this.getItemBySlot(EquipmentSlot.CHEST));
					break;
				}
				case 51: {
					this.breakItem(this.getItemBySlot(EquipmentSlot.LEGS));
					break;
				}
				case 52: {
					this.breakItem(this.getItemBySlot(EquipmentSlot.FEET));
					break;
				}
				case 54: {
					HoneyBlock.showJumpParticles(this);
					break;
				}
				case 55: {
					this.swapHandItems();
					break;
				}
				case 60: {
					this.makePoofParticles();
					break;
				}
				default: {
					switch (pId) {
						case 53: {
							HoneyBlock.showSlideParticles(this);
						}
					}
				}
			}
		}
	}

	@Override
	public Vec3 handleOnClimbable(Vec3 pDeltaMovement) {
		if (this.onClimbable()) {
			this.resetFallDistance();
			double d0 = Mth.clamp(pDeltaMovement.x, (double) -0.15f, (double) 0.15f);
			double d1 = Mth.clamp(pDeltaMovement.z, (double) -0.15f, (double) 0.15f);
			double d2 = Math.max(pDeltaMovement.y, (double) -0.15f);
			if (d2 < 0.0 && !this.getFeetBlockState().isScaffolding(this) && this.isSuppressingSlidingDownLadder() && (Object) this instanceof Player) {
				d2 = 0.0;
			}
			pDeltaMovement = new Vec3(d0, d2, d1);
		}
		return pDeltaMovement;
	}

	@Override
	public Vec3 handleRelativeFrictionAndCalculateMovement(Vec3 pDeltaMovement, float pFriction) {
		this.moveRelative(this.getFrictionInfluencedSpeed(pFriction), pDeltaMovement);
		this.setDeltaMovement(this.handleOnClimbable(this.getDeltaMovement()));
		this.move(MoverType.SELF, this.getDeltaMovement());
		Vec3 vec3 = this.getDeltaMovement();
		if ((this.horizontalCollision || this.jumping) && (this.onClimbable() || this.getFeetBlockState().is(Blocks.POWDER_SNOW) && PowderSnowBlock.canEntityWalkOnPowderSnow(this))) {
			vec3 = new Vec3(vec3.x, 0.2, vec3.z);
		}
		return vec3;
	}

	@Override
	public boolean hasControllingPassenger() {
		return false;
	}

	@Override
	public boolean hasGlowingTag() {
		return this.hasGlowingTag;
	}

	@Override
	public boolean hasIndirectPassenger(Entity pEntity) {
		return false;
	}

	@Override
	public boolean hasPassenger(Entity pEntity) {
		return false;
	}

	@Override
	public boolean hasPose(Pose pPose) {
		return this.getPose() == pPose;
	}

	@Override
	public boolean hasRestriction() {
		return this.restrictRadius != -1.0f;
	}

	@Override
	public void hurtCurrentlyUsedShield(float pDamageAmount) {}

	@Override
	public int increaseAirSupply(int pCurrentAir) {
		return Math.min(pCurrentAir + 4, this.getMaxAirSupply());
	}

	@Override
	public boolean isAddedToWorld() {
		return this.isAddedToWorld;
	}

	@Override
	public boolean isAffectedByFluids() {
		return true;
	}

	@Override
	public boolean isAffectedByPotions() {
		return false;
	}

	@Override
	public boolean isAlwaysExperienceDropper() {
		return false;
	}

	@Override
	public boolean isBaby() {
		return false;
	}

	@Override
	public boolean isBlocking() {
		if (this.isUsingItem() && !this.useItem.isEmpty()) {
			Item item = this.useItem.getItem();
			if (!this.useItem.canPerformAction(ToolActions.SHIELD_BLOCK)) {
				return false;
			}
			return item.getUseDuration(this.useItem) - this.useItemRemaining >= 5;
		}
		return false;
	}

	@Override
	public boolean isFallFlying() {
		return this.getSharedFlag(7);
	}

	@Override
	public boolean isFlapping() {
		return false;
	}

	@Override
	public boolean isHorizontalCollisionMinor(Vec3 pDeltaMovement) {
		return false;
	}

	@Override
	public boolean isIgnoringBlockTriggers() {
		return true;
	}

	@Override
	public boolean isMaxGroupSizeReached(int pSize) {
		return false;
	}

	@Override
	public boolean isPassenger() {
		return false;
	}

	@Override
	public boolean isPassengerOfSameVehicle(Entity pEntity) {
		return false;
	}

	@Override
	public boolean isPathFinding() {
		return !this.getNavigation().isDone();
	}

	@Override
	@Deprecated
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	public boolean isRemoved() {
		return false;
	}

	@Override
	public boolean isShiftKeyDown() {
		return this.getSharedFlag(1);
	}

	@Override
	@ApiStatus.Internal
	public boolean isSpawnCancelled() {
		return this.spawnCancelled;
	}

	@Override
	public boolean isSprinting() {
		return this.getSharedFlag(3);
	}

	@Override
	public boolean isStateClimbable(BlockState pState) {
		return pState.is(BlockTags.CLIMBABLE) || pState.is(Blocks.POWDER_SNOW);
	}

	@Override
	public boolean isSteppingCarefully() {
		return this.isShiftKeyDown();
	}

	@Override
	public boolean isSupportedBy(BlockPos pPos) {
		return this.mainSupportingBlockPos.isPresent() && this.mainSupportingBlockPos.get().equals(pPos);
	}

	@Override
	public boolean isSwimming() {
		// TODO: ?
		return false;// this.getSharedFlag(4);
	}

	@Override
	public boolean isVehicle() {
		return false;
	}

	@Override
	public boolean isVisuallySwimming() {
		return this.hasPose(Pose.SWIMMING);
	}

	@Override
	public boolean isWithinRestriction() {
		return this.isWithinRestriction(this.blockPosition());
	}

	@Override
	public void jumpFromGround() {
		Vec3 vec3 = this.getDeltaMovement();
		this.setDeltaMovement(vec3.x, this.getJumpPower(), vec3.z);
		if (this.isSprinting()) {
			float f = this.getYRot() * ((float) Math.PI / 180);
			this.setDeltaMovement(this.getDeltaMovement().add(-Mth.sin(f) * 0.2f, 0.0, Mth.cos(f) * 0.2f));
		}
		this.hasImpulse = true;
	}

	@Override
	public void jumpInLiquidInternal(Runnable onSuper) {
		if (this.getNavigation().canFloat()) {
			onSuper.run();
		} else {
			this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.3, 0.0));
		}
	}

	@Override
	public boolean killedEntity(ServerLevel pLevel, LivingEntity pEntity) {
		return true;
	}

	@Override
	public void lerpHeadTo(float pYaw, int pPitch) {
		this.lyHeadRot = pYaw;
		this.lerpHeadSteps = pPitch;
	}

	@Override
	public void lerpMotion(double pX, double pY, double pZ) {
		this.setDeltaMovement(pX, pY, pZ);
	}

	@Override
	public void lerpTo(double pX, double pY, double pZ, float pYaw, float pPitch, int pPosRotationIncrements, boolean pTeleport) {
		this.lerpX = pX;
		this.lerpY = pY;
		this.lerpZ = pZ;
		this.lerpYRot = pYaw;
		this.lerpXRot = pPitch;
		this.lerpSteps = pPosRotationIncrements;
	}

	@Override
	public Level level() {
		return this.level;
	}

	@Override
	public Vec3 limitPistonMovement(Vec3 pPos) {
		if (pPos.lengthSqr() <= 1.0E-7) {
			return pPos;
		}
		long i = this.level().getGameTime();
		if (i != this.pistonDeltasGameTime) {
			Arrays.fill(this.pistonDeltas, 0.0);
			this.pistonDeltasGameTime = i;
		}
		if (pPos.x != 0.0) {
			double d2 = this.applyPistonMovementRestriction(Direction.Axis.X, pPos.x);
			return Math.abs(d2) <= (double) 1.0E-5f ? Vec3.ZERO : new Vec3(d2, 0.0, 0.0);
		}
		if (pPos.y != 0.0) {
			double d1 = this.applyPistonMovementRestriction(Direction.Axis.Y, pPos.y);
			return Math.abs(d1) <= (double) 1.0E-5f ? Vec3.ZERO : new Vec3(0.0, d1, 0.0);
		}
		if (pPos.z != 0.0) {
			double d0 = this.applyPistonMovementRestriction(Direction.Axis.Z, pPos.z);
			return Math.abs(d0) <= (double) 1.0E-5f ? Vec3.ZERO : new Vec3(0.0, 0.0, d0);
		}
		return Vec3.ZERO;
	}

	@Override
	public void lookAt(Entity pEntity, float pMaxYRotIncrease, float pMaxXRotIncrease) {
		double d1;
		double d0 = pEntity.getX() - this.getX();
		double d2 = pEntity.getZ() - this.getZ();
		if (pEntity instanceof LivingEntity) {
			LivingEntity livingentity = (LivingEntity) pEntity;
			d1 = livingentity.getEyeY() - this.getEyeY();
		} else {
			d1 = (pEntity.getBoundingBox().minY + pEntity.getBoundingBox().maxY) / 2.0 - this.getEyeY();
		}
		double d3 = Math.sqrt(d0 * d0 + d2 * d2);
		float f = (float) (Mth.atan2(d2, d0) * 57.2957763671875) - 90.0f;
		float f1 = (float) (-(Mth.atan2(d1, d3) * 57.2957763671875));
		this.setXRot(this.rotlerp(this.getXRot(), f1, pMaxXRotIncrease));
		this.setYRot(this.rotlerp(this.getYRot(), f, pMaxYRotIncrease));
	}

	@Override
	public AABB makeBoundingBox() {
		return this.dimensions.makeBoundingBox(this.position);
	}

	@Override
	public void makeStuckInBlock(BlockState pState, Vec3 pMotionMultiplier) {
		this.resetFallDistance();
		this.stuckSpeedMultiplier = pMotionMultiplier;
	}

	@Override
	public void markHurt() {
		this.hurtMarked = true;
	}

	@Override
	public boolean mayInteract(Level pLevel, BlockPos pPos) {
		return true;
	}

	@Override
	public Vec3 maybeBackOffFromEdge(Vec3 pVec, MoverType pMover) {
		return pVec;
	}

	@Override
	public void maybeDisableShield(Player pPlayer, ItemStack pMobItemStack, ItemStack pPlayerItemStack) {
		if (!pMobItemStack.isEmpty() && !pPlayerItemStack.isEmpty() && pMobItemStack.getItem() instanceof AxeItem && pPlayerItemStack.is(Items.SHIELD)) {
			float f = 0.25f + (float) EnchantmentHelper.getBlockEfficiency(this) * 0.05f;
			if (this.random.nextFloat() < f) {
				pPlayer.getCooldowns().addCooldown(Items.SHIELD, 100);
				this.level().broadcastEntityEvent(pPlayer, (byte) 30);
			}
		}
	}

	@Override
	public void moveRelative(float pAmount, Vec3 pRelative) {
		Vec3 vec3 = Entity.getInputVector(pRelative, pAmount, this.getYRot());
		this.setDeltaMovement(this.getDeltaMovement().add(vec3));
	}

	@Override
	public void moveTo(Vec3 pVec) {
		this.moveTo(pVec.x, pVec.y, pVec.z);
	}

	@Override
	public ListTag newFloatList(float... pNumbers) {
		ListTag listtag = new ListTag();
		for (float f : pNumbers) {
			listtag.add(FloatTag.valueOf(f));
		}
		return listtag;
	}

	@Override
	public float nextStep() {
		return (int) this.moveDist + 1;
	}

	@Override
	public void onEffectAdded(MobEffectInstance pEffectInstance, @Nullable Entity pEntity) {}

	@Override
	public void onEffectRemoved(MobEffectInstance pEffectInstance) {}

	@Override
	public void onFlap() {}

	@Override
	public void onInsideBlock(BlockState pState) {}

	@Override
	protected void onLeashDistance(float pDistance) {}

	@Override
	public void onOffspringSpawnedFromEgg(Player pPlayer, Mob pChild) {}

	@Override
	public void onPassengerTurned(Entity pEntityToUpdate) {}

	@Override
	public void onPathfindingDone() {}

	@Override
	public void onPathfindingStart() {}

	@Override
	public boolean onlyOpCanSetNbt() {
		return true;
	}

	@Override
	public void playAmbientSound() {
		SoundEvent soundevent = this.getAmbientSound();
		if (soundevent != null) {
			this.playSound(soundevent, this.getSoundVolume(), this.getVoicePitch());
		}
	}

	@Override
	public void playAmethystStepSound() {
		this.crystalSoundIntensity *= (float) Math.pow(0.997, this.tickCount - this.lastCrystalSoundPlayTick);
		this.crystalSoundIntensity = Math.min(1.0f, this.crystalSoundIntensity + 0.07f);
		float f = 0.5f + this.crystalSoundIntensity * this.random.nextFloat() * 1.2f;
		float f1 = 0.1f + this.crystalSoundIntensity * 1.2f;
		this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, f1, f);
		this.lastCrystalSoundPlayTick = this.tickCount;
	}

	@Override
	public void playBlockFallSound() {
		if (!this.isSilent()) {
			int i = Mth.floor(this.getX());
			int j = Mth.floor(this.getY() - (double) 0.2f);
			int k = Mth.floor(this.getZ());
			BlockPos pos = new BlockPos(i, j, k);
			BlockState blockstate = this.level().getBlockState(pos);
			if (!blockstate.isAir()) {
				SoundType soundtype = blockstate.getSoundType(this.level(), pos, this);
				this.playSound(soundtype.getFallSound(), soundtype.getVolume() * 0.5f, soundtype.getPitch() * 0.75f);
			}
		}
	}

	@Override
	public void playCombinationStepSounds(BlockState p_277472_, BlockState p_277630_, BlockPos primaryPos, BlockPos secondaryPos) {
		SoundType soundtype = p_277472_.getSoundType(this.level, primaryPos, this);
		this.playSound(soundtype.getStepSound(), soundtype.getVolume() * 0.15f, soundtype.getPitch());
		this.playMuffledStepSound(p_277630_, secondaryPos);
	}

	@Override
	public void playEntityOnFireExtinguishedSound() {
		this.playSound(SoundEvents.GENERIC_EXTINGUISH_FIRE, 0.7f, 1.6f + (this.random.nextFloat() - this.random.nextFloat()) * 0.4f);
	}

	@Override
	public void playHurtSound(DamageSource pSource) {
		this.resetAmbientSoundTime();
		SoundEvent soundevent = this.getHurtSound(pSource);
		if (soundevent != null) {
			this.playSound(soundevent, this.getSoundVolume(), this.getVoicePitch());
		}
	}

	@Override
	public void playMuffledStepSound(BlockState p_283110_, BlockPos pos) {
		SoundType soundtype = p_283110_.getSoundType(this.level, pos, this);
		this.playSound(soundtype.getStepSound(), soundtype.getVolume() * 0.05f, soundtype.getPitch() * 0.8f);
	}

	@Override
	public boolean isSilent() {
		return false;
	}

	@Override
	public boolean isSleeping() {
		return false;
	}

	@Override
	public void playSound(SoundEvent pSound, float pVolume, float pPitch) {
		if (!this.isSilent()) {
			this.level().playSound(null, this.getX(), this.getY(), this.getZ(), pSound, this.getSoundSource(), pVolume, pPitch);
		}
	}

	@Override
	public void playStepSound(BlockPos pPos, BlockState pState) {
		SoundType soundtype = pState.getSoundType(this.level, pPos, this);
		this.playSound(soundtype.getStepSound(), soundtype.getVolume() * 0.15f, soundtype.getPitch());
	}

	@Override
	public void playSwimSound(float pVolume) {
		this.playSound(this.getSwimSound(), pVolume, 1.0f + (this.random.nextFloat() - this.random.nextFloat()) * 0.4f);
	}

	@Override
	public void playerTouch(Player pPlayer) {}

	@Override
	public Vec3 position() {
		return this.position;
	}

	@Override
	public void reapplyPosition() {
		this.setPos(this.position.x, this.position.y, this.position.z);
	}

	@Override
	public void recreateFromPacket(ClientboundAddEntityPacket pPacket) {
		double d0 = pPacket.getX();
		double d1 = pPacket.getY();
		double d2 = pPacket.getZ();
		float f = pPacket.getYRot();
		float f1 = pPacket.getXRot();
		this.syncPacketPositionCodec(d0, d1, d2);
		this.yBodyRot = pPacket.getYHeadRot();
		this.yHeadRot = pPacket.getYHeadRot();
		this.yBodyRotO = this.yBodyRot;
		this.yHeadRotO = this.yHeadRot;
		this.setId(pPacket.getId());
		this.setUUID(pPacket.getUUID());
		this.absMoveTo(d0, d1, d2, f, f1);
		this.setDeltaMovement(pPacket.getXa(), pPacket.getYa(), pPacket.getZa());
	}

	@Override
	public boolean removeEffect(MobEffect pEffect) {
		return false;
	}

	@Override
	@Nullable
	public MobEffectInstance removeEffectNoUpdate(@Nullable MobEffect pEffect) {
		return null;
	}

	@Override
	public void removeEffectParticles() {
		this.entityData.set(DATA_EFFECT_AMBIENCE_ID, false);
		this.entityData.set(DATA_EFFECT_COLOR_ID, 0);
	}

	@Override
	public void removeFrost() {
		AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
		if (attributeinstance != null && attributeinstance.getModifier(SPEED_MODIFIER_POWDER_SNOW_UUID) != null) {
			attributeinstance.removeModifier(SPEED_MODIFIER_POWDER_SNOW_UUID);
		}
	}

	@Override
	public void removeSoulSpeed() {
		AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
		if (attributeinstance != null && attributeinstance.getModifier(SPEED_MODIFIER_SOUL_SPEED_UUID) != null) {
			attributeinstance.removeModifier(SPEED_MODIFIER_SOUL_SPEED_UUID);
		}
	}

	@Override
	public boolean removeTag(String pTag) {
		return this.tags.remove(pTag);
	}

	@Override
	public boolean repositionEntityAfterLoad() {
		return true;
	}

	@Override
	public void resetFallDistance() {
		this.fallDistance = 0.0f;
	}

	@Override
	public void restrictTo(BlockPos pPos, int pDistance) {
		this.restrictCenter = pPos;
		this.restrictRadius = pDistance;
	}

	@Override
	public float rotlerp(float pAngle, float pTargetAngle, float pMaxIncrease) {
		float f = Mth.wrapDegrees(pTargetAngle - pAngle);
		if (f > pMaxIncrease) {
			f = pMaxIncrease;
		}
		if (f < -pMaxIncrease) {
			f = -pMaxIncrease;
		}
		return pAngle + f;
	}

	@Override
	public void sendDebugPackets() {
		DebugPackets.sendGoalSelector(this.level(), this, this.goalSelector);
	}

	@Override
	public void sendEffectToPassengers(MobEffectInstance pEffectInstance) {}

	@Override
	public void sendSystemMessage(Component pComponent) {}

	@Override
	public void setAbsorptionAmount(float pAbsorptionAmount) {
		if (pAbsorptionAmount < 0.0f) {
			pAbsorptionAmount = 0.0f;
		}
		this.absorptionAmount = pAbsorptionAmount;
	}

	@Override
	public void setAggressive(boolean pAggressive) {
		byte b0 = this.entityData.get(DATA_MOB_FLAGS_ID);
		this.entityData.set(DATA_MOB_FLAGS_ID, pAggressive ? (byte) (b0 | 4) : (byte) (b0 & 0xFFFFFFFB));
	}

	@Override
	public void setAirSupply(int pAir) {
		this.entityData.set(DATA_AIR_SUPPLY_ID, pAir);
	}

	@Override
	public void setBaby(boolean pBaby) {}

	@Override
	public void setBoundingBox(AABB pBb) {
		this.bb = pBb;
	}

	@Override
	public void setCustomNameVisible(boolean pAlwaysRenderNameTag) {
		this.entityData.set(DATA_CUSTOM_NAME_VISIBLE, pAlwaysRenderNameTag);
	}

	@Override
	public void setDelayedLeashHolderId(int pLeashHolderID) {
		this.delayedLeashHolderId = pLeashHolderID;
		this.dropLeash(false, false);
	}

	@Override
	public void setDeltaMovement(Vec3 pDeltaMovement) {
		if (pDeltaMovement.horizontalDistanceSqr() >= 101)
			// TODO: ?????
			return;
		this.deltaMovement = pDeltaMovement;
	}

	@Override
	public void setFluidTypeHeight(FluidType type, double height) {
		this.forgeFluidTypeHeight.put(type, height);
	}

	@Override
	public void setGlowingTag(boolean pHasGlowingTag) {
		this.hasGlowingTag = false;
		this.setSharedFlag(6, false);
	}

	@Override
	public void setInvisible(boolean pInvisible) {
		this.setSharedFlag(5, false);
	}

	@Override
	public void setJumping(boolean pJumping) {
		this.jumping = pJumping;
	}

	@Override
	public void setLastHurtByMob(@Nullable LivingEntity pLivingEntity) {}

	@Override
	public void setLastHurtByPlayer(@Nullable Player pPlayer) {}

	@Override
	public void setLeashedTo(Entity pLeashHolder, boolean pBroadcastPacket) {
		this.leashHolder = pLeashHolder;
		this.leashInfoTag = null;
		if (!this.level().isClientSide && pBroadcastPacket && this.level() instanceof ServerLevel) {
			((ServerLevel) this.level()).getChunkSource().broadcast(this, new ClientboundSetEntityLinkPacket(this, this.leashHolder));
		}
		if (this.isPassenger()) {
			this.stopRiding();
		}
	}

	@Override
	public void setLevel(Level pLevel) {
		this.level = pLevel;
	}

	@Override
	public void setLivingEntityFlag(int pKey, boolean pValue) {
		int i = this.entityData.get(DATA_LIVING_ENTITY_FLAGS).byteValue();
		i = pValue ? (i |= pKey) : (i &= ~pKey);
		this.entityData.set(DATA_LIVING_ENTITY_FLAGS, (byte) i);
	}

	@Override
	public void setMaxUpStep(float pMaxUpStep) {
		this.maxUpStep = pMaxUpStep;
	}

	@Override
	public void setNoGravity(boolean pNoGravity) {}

	@Override
	public void setOnGroundWithKnownMovement(boolean pOnGround, Vec3 pMovement) {
		this.onGround = pOnGround;
		this.checkSupportingBlock(pOnGround, pMovement);
	}

	@Override
	public void setPathfindingMalus(BlockPathTypes pNodeType, float pMalus) {
		this.pathfindingMalus.put(pNodeType, Float.valueOf(pMalus));
	}

	@Override
	public void setPosRaw(double pX, double pY, double pZ) {
		ssSetPos(false, pX, pY, pZ);
	}

	@Override
	public void setPosToBed(BlockPos p_21081_) {
		this.setPos((double) p_21081_.getX() + 0.5, (double) p_21081_.getY() + 0.6875, (double) p_21081_.getZ() + 0.5);
	}

	@Override
	public void setPose(Pose pPose) {
		this.entityData.set(DATA_POSE, pPose);
	}

	@Override
	public void setRecordPlayingNearby(BlockPos pJukebox, boolean pPartyParrot) {}

	@Override
	public void setShiftKeyDown(boolean pKeyDown) {
		this.setSharedFlag(1, pKeyDown);
	}

	@Override
	public void setSleepingPos(BlockPos pPos) {
		this.entityData.set(SLEEPING_POS_ID, Optional.of(pPos));
	}

	@Override
	@ApiStatus.Internal
	public void setSpawnCancelled(boolean cancel) {
		if (this.isAddedToWorld()) {
			throw new UnsupportedOperationException("Late invocations of Mob#setSpawnCancelled are not permitted.");
		}
		this.spawnCancelled = cancel;
	}

	@Override
	public void setSpeed(float pSpeed) {
		this.speed = pSpeed;
		this.setZza(pSpeed);
	}

	@Override
	public void setSprinting(boolean pSprinting) {
		this.setSharedFlag(3, pSprinting);
		AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
		if (attributeinstance.getModifier(SPEED_MODIFIER_SPRINTING_UUID) != null) {
			attributeinstance.removeModifier(SPEED_MODIFIER_SPRINTING);
		}
		if (pSprinting) {
			attributeinstance.addTransientModifier(SPEED_MODIFIER_SPRINTING);
		}
	}

	@Override
	public void setSwimming(boolean pSwimming) {
		this.setSharedFlag(4, pSwimming);
	}

	@Override
	public void setXxa(float pAmount) {
		this.xxa = pAmount;
	}

	@Override
	public void setYya(float pAmount) {
		this.yya = pAmount;
	}

	@Override
	public void setZza(float pAmount) {
		this.zza = pAmount;
	}

	@Override
	public boolean shouldBeSaved() {
		return true;
	}

	@Override
	public boolean shouldBlockExplode(Explosion pExplosion, BlockGetter pLevel, BlockPos pPos, BlockState pBlockState, float pExplosionPower) {
		return true;
	}

	@Override
	public boolean shouldDespawnInPeaceful() {
		return false;
	}

	@Override
	public boolean shouldDropExperience() {
		return true;
	}

	@Override
	public boolean shouldPassengersInheritMalus() {
		return false;
	}

	@Override
	public boolean shouldPlayAmethystStepSound(BlockState pState) {
		return pState.is(BlockTags.CRYSTAL_SOUND_BLOCKS) && this.tickCount >= this.lastCrystalSoundPlayTick + 20;
	}

	@Override
	public boolean shouldRemoveSoulSpeed(BlockState pState) {
		return !pState.isAir() || this.isFallFlying();
	}

	@Override
	protected boolean shouldStayCloseToLeashHolder() {
		return true;
	}

	@Override
	public boolean showVehicleHealth() {
		return false;
	}

	@Override
	public void skipDropExperience() {
		this.skipDropExperience = true;
	}

	@Override
	public void spawnAnim() {}

	@Override
	@Nullable
	public ItemEntity spawnAtLocation(ItemLike pItem) {
		return this.spawnAtLocation(pItem, 0);
	}

	@Override
	public void spawnItemParticles(ItemStack pStack, int pAmount) {}

	@Override
	public void spawnSoulSpeedParticle() {}

	@Override
	public void spawnSprintParticle() {}

	@Override
	public boolean startRiding(Entity pEntity, boolean pForce) {
		return false;
	}

	@Override
	public void stopRiding() {
		Entity entity = this.getVehicle();
		this.removeVehicle();
		if (entity != null && entity != this.getVehicle() && !this.level().isClientSide) {
			this.dismountVehicle(entity);
		}
	}

	@Override
	public void syncPacketPositionCodec(double pX, double pY, double pZ) {
		for (double v2test : new double[] { pX, pY, pZ })
			if (!Double.isFinite(v2test))
				return;
		this.packetPositionCodec.setBase(new Vec3(pX, pY, pZ));
	}

	@Override
	public void teleportToWithTicket(double pX, double pY, double pZ) {
		// TODO: ?
	}

	@Override
	public Vec3 trackingPosition() {
		return this.position();
	}

	@Override
	public void tryAddFrost() {}

	@Override
	public void tryCheckInsideBlocks() {
		try {
			this.checkInsideBlocks();
		} catch (Throwable throwable) {
			CrashReport crashreport = CrashReport.forThrowable(throwable, "Checking entity block collision");
			CrashReportCategory crashreportcategory = crashreport.addCategory("Entity being checked for collision");
			this.fillCrashReportCategory(crashreportcategory);
			throw new ReportedException(crashreport);
		}
	}

	@Override
	public void turn(double pYRot, double pXRot) {
		float f = (float) pXRot * 0.15f;
		float f1 = (float) pYRot * 0.15f;
		this.setXRot(this.getXRot() + f);
		this.setYRot(this.getYRot() + f1);
		this.setXRot(Mth.clamp(this.getXRot(), -90.0f, 90.0f));
		this.xRotO += f;
		this.yRotO += f1;
		this.xRotO = Mth.clamp(this.xRotO, -90.0f, 90.0f);
		if (this.vehicle != null) {
			this.vehicle.onPassengerTurned(this);
		}
	}

	@Override
	public void unsetRemoved() {
		this.removalReason = null;
	}

	@Override
	public void updateControlFlags() {
		boolean flag = !(this.getControllingPassenger() instanceof Mob);
		boolean flag1 = !(this.getVehicle() instanceof Boat);
		this.goalSelector.setControlFlag(Goal.Flag.MOVE, flag);
		this.goalSelector.setControlFlag(Goal.Flag.JUMP, flag && flag1);
		this.goalSelector.setControlFlag(Goal.Flag.LOOK, flag);
	}

	@Override
	public void updateDynamicGameEventListener(BiConsumer<DynamicGameEventListener<?>, ServerLevel> pListenerConsumer) {}

	@Override
	public void updateEffectVisibility() {
		this.effectsDirty = true;
	}

	@Override
	public void updateFallFlying() {
		ItemStack itemstack;
		boolean flag = this.getSharedFlag(7);
		flag = flag && !this.onGround() && !this.isPassenger() && !this.hasEffect(MobEffects.LEVITATION) ? (itemstack = this.getItemBySlot(EquipmentSlot.CHEST)).canElytraFly(this) && itemstack.elytraFlightTick(this, this.fallFlyTicks) : false;
		if (!this.level().isClientSide) {
			this.setSharedFlag(7, flag);
		}
	}

	@Override
	public void updateInvisibilityStatus() {
		if (this.activeEffects.isEmpty()) {
			this.removeEffectParticles();
			this.setInvisible(false);
		} else {
			Collection<MobEffectInstance> collection = this.activeEffects.values();
			PotionColorCalculationEvent event = new PotionColorCalculationEvent(this, PotionUtils.getColor(collection), LivingEntity.areAllEffectsAmbient(collection), collection);
			MinecraftForge.EVENT_BUS.post((Event) event);
			this.entityData.set(DATA_EFFECT_AMBIENCE_ID, event.areParticlesHidden());
			this.entityData.set(DATA_EFFECT_COLOR_ID, event.getColor());
			this.setInvisible(this.hasEffect(MobEffects.INVISIBILITY));
		}
	}

	@Override
	public void updateSwimAmount() {
		this.swimAmountO = this.swimAmount;
		this.swimAmount = this.isVisuallySwimming() ? Math.min(1.0f, this.swimAmount + 0.09f) : Math.max(0.0f, this.swimAmount - 0.09f);
	}

	@Override
	public void updateSwingTime() {
		int i = this.getCurrentSwingDuration();
		if (this.swinging) {
			++this.swingTime;
			if (this.swingTime >= i) {
				this.swingTime = 0;
				this.swinging = false;
			}
		} else {
			this.swingTime = 0;
		}
		this.attackAnim = (float) this.swingTime / (float) i;
	}

	@Override
	public void updateWalkAnimation(float pPartialTick) {
		float f = Math.min(pPartialTick * 4.0f, 1.0f);
		this.walkAnimation.update(f, 0.4f);
	}

	@Override
	public void verifyEquippedItem(ItemStack pStack) {
		CompoundTag compoundtag = pStack.getTag();
		if (compoundtag != null) {
			pStack.getItem().verifyTagAfterLoad(compoundtag);
		}
	}

	@Override
	public boolean vibrationAndSoundEffectsFromBlock(BlockPos pPos, BlockState pState, boolean pPlayStepSound, boolean pBroadcastGameEvent, Vec3 p_286448_) {
		if (pState.isAir()) {
			return false;
		}
		boolean flag = this.isStateClimbable(pState);
		if ((this.onGround() || flag || this.isCrouching() && p_286448_.y == 0.0 || this.isOnRails()) && !this.isSwimming()) {
			if (pPlayStepSound) {
				this.walkingStepSound(pPos, pState);
			}
			if (pBroadcastGameEvent) {
				this.level().gameEvent(GameEvent.STEP, this.position(), GameEvent.Context.of(this, pState));
			}
			return true;
		}
		return false;
	}

	@Override
	public void walkingStepSound(BlockPos pPos, BlockState pState) {
		this.playStepSound(pPos, pState);
		if (this.shouldPlayAmethystStepSound(pState)) {
			this.playAmethystStepSound();
		}
	}

	@Override
	public boolean wasExperienceConsumed() {
		return this.skipDropExperience;
	}

	@Override
	public void waterSwimSound() {
		Entity entity = this.isVehicle() && this.getControllingPassenger() != null ? this.getControllingPassenger() : this;
		float f = entity == this ? 0.35f : 0.4f;
		Vec3 vec3 = entity.getDeltaMovement();
		float f1 = Math.min(1.0f, (float) Math.sqrt(vec3.x * vec3.x * (double) 0.2f + vec3.y * vec3.y + vec3.z * vec3.z * (double) 0.2f) * f);
		this.playSwimSound(f1);
	}
}
