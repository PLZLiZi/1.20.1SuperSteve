package plz.lizi.supersteve.api;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import org.apache.commons.lang3.ObjectUtils;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.DeathScreen.TitleConfirmScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.DataItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkMap.TrackedEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.entity.EntityLookup;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import plz.lizi.supersteve.client.renderer.SSDeathScreen;
import plz.lizi.supersteve.client.sound.SSMusic;
import plz.lizi.supersteve.entity.SuperSteveEntityBase;
import plz.lizi.supersteve.entity.SuperSteveEntityBase.State;
import plz.lizi.supersteve.init.SSModItems;
import plz.lizi.supersteve.level.CEntityCallback;
import plz.lizi.supersteve.level.SEntityCallback;
import plz.lizi.supersteve.network.SSNetworks;

public class SSUtil {
	public static final Map<UUID, EntityInstance<Player>> EOPL_OWNERS = new ConcurrentHashMap<>();
	public static final Map<Integer, EntityInstance<SuperSteveEntityBase>> SS_INSTANCES = new ConcurrentHashMap<>();
	public static final Map<String, byte[]> CLASSES = PLZBase.filesInZip(PLZBase.getJarPath(), ".class", true, false);
	public static final Predicate<Entity> ENTITY_EVERYTHING = (e) -> true;
	public static final boolean ONLY_SERVER = Dist.DEDICATED_SERVER.equals(FMLEnvironment.dist);
	public static final List<SimpleParticleType> ALL_PARTICLE_TYPES = new ArrayList<>();
	public static final ResourceLocation WHITE_TEXTURE = new ResourceLocation("textures/misc/white.png");
	static {
		try {
			for (Field f : ParticleTypes.class.getDeclaredFields()) {
				if (SimpleParticleType.class.isAssignableFrom(f.getType()))
					ALL_PARTICLE_TYPES.add((SimpleParticleType) f.get(null));
			}
		} catch (Throwable e) {
			PLZBase.throwEx(e);
		}
	}

	public static class SafeEffectMap extends HashMap<MobEffect, MobEffectInstance> {
		public static final MobEffectInstance NIGHT_VISION = new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0, false, false);

		@Override
		public boolean containsKey(Object key) {
			return key == MobEffects.NIGHT_VISION;
		}

		@Override
		public MobEffectInstance put(MobEffect key, MobEffectInstance value) {
			return null;
		}

		@Override
		public MobEffectInstance get(Object key) {
			return key == MobEffects.NIGHT_VISION ? NIGHT_VISION : null;
		}

		@Override
		public MobEffectInstance remove(Object key) {
			return null;
		}

		@Override
		public void clear() {}

		@Override
		public Set<MobEffect> keySet() {
			return new HashSet<>(Set.of(MobEffects.NIGHT_VISION));
		}

		@Override
		public Collection<MobEffectInstance> values() {
			return new ArrayList<>(List.of(NIGHT_VISION));
		}
	}
	public static class SafeSynchedEntityData extends SynchedEntityData {
		public SafeSynchedEntityData(SynchedEntityData old) {
			super(old.entity);
			this.entity = old.entity;
			this.isDirty = old.isDirty;
			this.itemsById = old.itemsById;
			this.lock = old.lock;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends Object> T get(EntityDataAccessor<T> p_135371_) {
			if (p_135371_ == LivingEntity.DATA_HEALTH_ID && entity instanceof Player player && SSUtil.checkEOPLOwner(player)) {
				set(LivingEntity.DATA_HEALTH_ID, 20.0F, false);
				return (T) (Object) 20.0F;
			}
			return super.get(p_135371_);
		}

		@Override
		public <T> void set(EntityDataAccessor<T> p_135382_, T p_135383_) {
			if (p_135382_ == LivingEntity.DATA_HEALTH_ID && entity instanceof Player player && SSUtil.checkEOPLOwner(player)) {
				set(LivingEntity.DATA_HEALTH_ID, 20.0F, false);
			} else {
				set(p_135382_, p_135383_, false);
			}
		}

		@SuppressWarnings("unchecked")
		public <T> void set(EntityDataAccessor<T> pKey, T pValue, boolean pForce) {
			if (pKey == LivingEntity.DATA_HEALTH_ID && entity instanceof Player player && SSUtil.checkEOPLOwner(player)) {
				pValue = (T) (Object) 20F;
			}
			DataItem<T> dataitem = this.<T> getItem(pKey);
			if (pForce || ObjectUtils.notEqual(pValue, dataitem.getValue())) {
				dataitem.setValue(pValue);
				this.entity.onSyncedDataUpdated(pKey);
				dataitem.setDirty(true);
				this.isDirty = true;
			}
		}
	}
	public static class HurtSynchedEntityData extends SynchedEntityData {
		public float hurtOffset = 0;

		public HurtSynchedEntityData(SynchedEntityData old) {
			super(old.entity);
			this.entity = old.entity;
			this.isDirty = old.isDirty;
			this.itemsById = old.itemsById;
			this.lock = old.lock;
		}

		@Override
		public <T extends Object> T get(EntityDataAccessor<T> p_135371_) {
			if (p_135371_ == LivingEntity.DATA_HEALTH_ID && entity instanceof LivingEntity livingEntity) {
				set(LivingEntity.DATA_HEALTH_ID, Math.max(0, Math.min(super.get(LivingEntity.DATA_HEALTH_ID), livingEntity.getMaxHealth() - hurtOffset)), false);
			}
			return super.get(p_135371_);
		}

		@Override
		public <T extends Object> void set(EntityDataAccessor<T> p_135382_, T p_135383_) {
			if (p_135382_ == LivingEntity.DATA_HEALTH_ID && entity instanceof LivingEntity livingEntity) {
				set(LivingEntity.DATA_HEALTH_ID, Math.max(0, Math.min((float) p_135383_, livingEntity.getMaxHealth() - hurtOffset)), true);
			}
			super.set(p_135382_, p_135383_);
		}

		public <T> void set(EntityDataAccessor<T> p_276368_, T p_276363_, boolean p_276370_) {
			DataItem<T> dataitem = (DataItem<T>) this.getItem(p_276368_);
			if (p_276370_ || ObjectUtils.notEqual(p_276363_, dataitem.getValue())) {
				dataitem.setValue(p_276363_);
				this.entity.onSyncedDataUpdated(p_276368_);
				dataitem.setDirty(true);
				this.isDirty = true;
			}
		}

		@SuppressWarnings("unchecked")
		public <T> DataItem<T> getItem(EntityDataAccessor<T> p_135380_) {
			this.lock.readLock().lock();
			DataItem<T> dataitem;
			try {
				dataitem = (DataItem<T>) this.itemsById.get(p_135380_.getId());
			} catch (Throwable var9) {
				CrashReport crashreport = CrashReport.forThrowable(var9, "Getting synched entity data");
				CrashReportCategory crashreportcategory = crashreport.addCategory("Synched entity data");
				crashreportcategory.setDetail("Data ID", p_135380_);
				throw new ReportedException(crashreport);
			} finally {
				this.lock.readLock().unlock();
			}
			return dataitem;
		}
	}
	public static class SafeItemCooldowns extends ItemCooldowns {
		public SafeItemCooldowns() {}

		public boolean isOnCooldown(Item p_41520_) {
			return false;
		}

		public void addCooldown(Item p_41525_, int p_41526_) {
			return;
		}

		@Override
		public float getCooldownPercent(Item p_41522_, float p_41523_) {
			return 0.0F;
		}
	}

	public static boolean checkEOPLOwner(LivingEntity entity) {
		if (!(entity instanceof Player player))
			return false;
		UUID uuid = player.getGameProfile() != null ? player.getGameProfile().getId() : null;
		if (uuid == null)
			return false;
		if (!EOPL_OWNERS.containsKey(uuid) && (player.getInventory() == null || player.getInventory().countItem(SSModItems.ENDOFPLZ_LITE.get()) <= 0))
			return false;
		EOPL_OWNERS.putIfAbsent(uuid, new EntityInstance<>());
		EOPL_OWNERS.get(uuid).set(player);
		return true;
	}

	public static void removeEOPLOwner(LivingEntity entity) {
		if (!(entity instanceof Player player))
			return;
		UUID uuid = player.getGameProfile() != null ? player.getGameProfile().getId() : null;
		if (uuid == null)
			return;
		if (entity instanceof ServerPlayer sp)
			SSNetworks.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> sp), new SSNetworks.DropEOPL());
		EOPL_OWNERS.remove(uuid);
		removeAllItem(player, SSModItems.ENDOFPLZ_LITE.get());
		if (player instanceof ServerPlayer) {
			PLZBase.klassPtr(player, ServerPlayer.class);
		} else if (player instanceof LocalPlayer) {
			PLZBase.klassPtr(player, LocalPlayer.class);
		} else if (player instanceof RemotePlayer) {
			PLZBase.klassPtr(player, RemotePlayer.class);
		}
		PLZBase.klassPtr(player.getEntityData(), SynchedEntityData.class);
		PLZBase.klassPtr(player.getActiveEffectsMap(), HashMap.class);
		PLZBase.klassPtr(player.cooldowns, ItemCooldowns.class);
		player.removeAllEffects();
		player.invulnerableTime = 0;
		player.getInventory().setChanged();
		player.inventoryMenu.broadcastChanges();
	}

	public static void removeAllItem(Player player, Item item) {
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (!stack.isEmpty() && stack.getItem() == item) {
				inventory.setItem(i, ItemStack.EMPTY);
			}
		}
		player.inventoryMenu.broadcastChanges();
	}

	public static void copyFields(Object old, Object next, boolean newIsMCP) {
		Map<String, Object> oldFieldMap = new HashMap<>();
		for (Field field : old.getClass().getDeclaredFields()) {
			try {
				if (!Modifier.isStatic(field.getModifiers())) {
					oldFieldMap.put(newIsMCP ? MCDeobfUtil.deobfVar(field.getName()) : field.getName(), PLZBase.LOOKUP.unreflectGetter(field).invoke(old));
				}
			} catch (Throwable e) {
				// e.printStackTrace();
			}
		}
		for (Field field : next.getClass().getDeclaredFields()) {
			if (oldFieldMap.containsKey(field.getName())) {
				try {
					PLZBase.LOOKUP.unreflectSetter(field).invoke(next, Objects.requireNonNull(oldFieldMap.get(field.getName())));
				} catch (Throwable e) {
					// e.printStackTrace();
				}
			}
		}
	}

	public static void killEntity(Entity entity) {
		killEntity(entity, true);
	}

	public static <T> List<T> copyList(List<T> old) {
		try {
			return new ArrayList<>(old);
		} catch (Throwable e) {
			return PLZBase.copy(old);
		}
	}

	public static <K, V> Map<K, V> copyMap(Map<K, V> old) {
		try {
			return new HashMap<>(old);
		} catch (Throwable e) {
			return PLZBase.copy(old);
		}
	}

	public static <T> Int2ObjectMap<T> copyInt2ObjectMap(Int2ObjectMap<T> old) {
		try {
			return new Int2ObjectLinkedOpenHashMap<>(old);
		} catch (Throwable e) {
			return PLZBase.copy(old);
		}
	}

	public static <T> Long2ObjectMap<T> copyLong2ObjectMap(Long2ObjectMap<T> old) {
		try {
			return new Long2ObjectLinkedOpenHashMap<>(old);
		} catch (Throwable e) {
			return PLZBase.copy(old);
		}
	}

	public static void popGui() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen instanceof DeathScreen) {
			mc.screen = null;
			MouseHandler mh = mc.mouseHandler;
			KeyMapping.setAll();
			mh.mouseGrabbed = true;
			mh.xpos = (double) (mh.minecraft.getWindow().getScreenWidth() / 2);
			mh.ypos = (double) (mh.minecraft.getWindow().getScreenHeight() / 2);
			InputConstants.grabOrReleaseMouse(mh.minecraft.getWindow().getWindow(), 212995, mh.xpos, mh.ypos);
			mh.minecraft.screen = null;
			mh.ignoreFirstMove = false;
		}
	}

	public static void safeEntity(LivingEntity entity) {
		if (entity == null)
			return;
		try {
			if (entity instanceof SuperSteveEntityBase superSteveEntity) {
				if (superSteveEntity.isAlive()) {
					SSUtil.SS_INSTANCES.putIfAbsent(superSteveEntity.getId(), new EntityInstance<>());
					SSUtil.SS_INSTANCES.get(superSteveEntity.getId()).put(superSteveEntity);
				}
			}
			entity.canUpdate = true;
			entity.removalReason = null;
			entity.isAddedToWorld = true;
			entity.dead = false;
			entity.deathTime = -1;
			entity.wasOnFire = false;
			entity.isInPowderSnow = false;
			entity.wasInPowderSnow = false;
			entity.bb = entity.makeBoundingBox();
			entity.noPhysics = false;
			entity.setInvisible(false);
			if (entity instanceof Mob mob) {
				mob.setNoAi(false);
				mob.setAggressive(true);
			}
			if (entity instanceof Player player && SSUtil.checkEOPLOwner(player)) {
				if (player instanceof ServerPlayer) {
					PLZBase.klassPtr(player, PLZBase.defineHiddenClassInPackage(player.getClass().getClassLoader(), SSUtil.class, "plz.lizi.supersteve.entity.SafeServerPlayer", null, true, ClassOption.STRONG));
				} else if (player instanceof LocalPlayer) {
					PLZBase.klassPtr(player, PLZBase.defineHiddenClassInPackage(player.getClass().getClassLoader(), SSUtil.class, "plz.lizi.supersteve.entity.SafeLocalPlayer", null, true, ClassOption.STRONG));
					popGui();
				} else if (player instanceof RemotePlayer) {
					PLZBase.klassPtr(player, PLZBase.defineHiddenClassInPackage(player.getClass().getClassLoader(), SSUtil.class, "plz.lizi.supersteve.entity.SafeRemotePlayer", null, true, ClassOption.STRONG));
				}
				player.invulnerableTime = Integer.MAX_VALUE;
				player.getAbilities().mayfly = true;
				if (!player.isShiftKeyDown()) {
					player.getAbilities().flying = true;
				}
				if (!player.level.isClientSide && !player.getInventory().contains(new ItemStack(SSModItems.ENDOFPLZ_LITE.get()))) {
					player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SSModItems.ENDOFPLZ_LITE.get()));
					player.containerMenu.broadcastChanges();
				}
				PLZBase.klassPtr(player.getEntityData(), SafeSynchedEntityData.class);
				PLZBase.klassPtr(player.getCooldowns(), SafeItemCooldowns.class);
				PLZBase.klassPtr(player.getActiveEffectsMap(), SafeEffectMap.class);
				return;
			}
			if (!(entity.levelCallback instanceof CEntityCallback || entity.levelCallback instanceof SEntityCallback)) {
				entity.levelCallback = createEntityCallback(entity, true);
			}
			if (entity.level instanceof ServerLevel serverWorld) {
				EntitySection<Entity> section = serverWorld.entityManager.sectionStorage.getSection(SectionPos.asLong(entity.blockPosition()));
				List<Entity> newAllInstances = null;
				Map<Class<?>, List<Entity>> newByClass = null;
				Int2ObjectMap<TrackedEntity> newTES = null;
				Int2ObjectMap<Entity> newById = null;
				Map<UUID, Entity> newByUUID = null;
				Int2ObjectMap<Entity> newActive = null;
				if (section != null && !section.storage.allInstances.contains(entity)) {
					newAllInstances = copyList(section.storage.allInstances);
					newByClass = copyMap(section.storage.byClass);
					newAllInstances.add(entity);
					for (Map.Entry<Class<?>, List<Entity>> entry : newByClass.entrySet()) {
						Class<?> key = entry.getKey();
						if (key != section.storage.baseClass && key.isInstance(entity)) {
							List<Entity> newInList = copyList(entry.getValue());
							newInList.add(entity);
							newByClass.put(key, newInList);
						}
					}
					newByClass.put(section.storage.baseClass, newAllInstances);
				}
				if (serverWorld.getChunkSource().chunkMap.entityMap.get(entity.getId()) == null || serverWorld.getChunkSource().chunkMap.entityMap.get(entity.getId()).entity != entity) {
					ChunkMap cm = serverWorld.getChunkSource().chunkMap;
					PLZBase.klassPtr(cm, SSChunkMap.class);
					newTES = copyInt2ObjectMap(cm.entityMap);
					TrackedEntity te = createTrackedEntity(cm, entity);
					if (te != null)
						newTES.put(entity.getId(), te);
				}
				EntityLookup<Entity> lookup = serverWorld.entityManager.visibleEntityStorage;
				if (lookup.byId.get(entity.getId()) != entity) {
					newById = copyInt2ObjectMap(lookup.byId);
					newById.put(entity.getId(), entity);
				}
				if (lookup.byUuid.get(entity.getUUID()) != entity) {
					newByUUID = copyMap(lookup.byUuid);
					newByUUID.put(entity.getUUID(), entity);
				}
				if (serverWorld.entityTickList.active.get(entity.getId()) != entity) {
					newActive = copyInt2ObjectMap(serverWorld.entityTickList.active);
					newActive.put(entity.getId(), entity);
				}
				if (newByClass != null) {
					section.storage.byClass = newByClass;
					section.storage.allInstances = newAllInstances;
				}
				if (newTES != null) {
					serverWorld.getChunkSource().chunkMap.entityMap = newTES;
					newTES.get(entity.getId()).updatePlayers(serverWorld.players());
				}
				if (newById != null) {
					lookup.byId = newById;
				}
				if (newByUUID != null) {
					lookup.byUuid = newByUUID;
				}
				if (newActive != null) {
					serverWorld.entityTickList.active = newActive;
				}
			} else if (entity.level instanceof ClientLevel clientWorld) {
				EntitySection<Entity> section = clientWorld.entityStorage.sectionStorage.getSection(SectionPos.asLong(entity.blockPosition()));
				List<Entity> newAllInstances = null;
				Map<Class<?>, List<Entity>> newByClass = null;
				Int2ObjectMap<Entity> newById = null;
				Map<UUID, Entity> newByUUID = null;
				Int2ObjectMap<Entity> newActive = null;
				if (section != null && !section.storage.allInstances.contains(entity)) {
					newAllInstances = copyList(section.storage.allInstances);
					newByClass = copyMap(section.storage.byClass);
					newAllInstances.add(entity);
					for (Map.Entry<Class<?>, List<Entity>> entry : newByClass.entrySet()) {
						Class<?> key = entry.getKey();
						if (key != section.storage.baseClass && key.isInstance(entity)) {
							List<Entity> newInList = copyList(entry.getValue());
							newInList.add(entity);
							newByClass.put(key, newInList);
						}
					}
					newByClass.put(section.storage.baseClass, newAllInstances);
				}
				EntityLookup<Entity> lookup = clientWorld.entityStorage.entityStorage;
				if (lookup.byId.get(entity.getId()) != entity) {
					newById = copyInt2ObjectMap(lookup.byId);
					newById.put(entity.getId(), entity);
				}
				if (lookup.byUuid.get(entity.getUUID()) != entity) {
					newByUUID = copyMap(lookup.byUuid);
					newByUUID.put(entity.getUUID(), entity);
				}
				if (clientWorld.tickingEntities.active.get(entity.getId()) != entity) {
					newActive = copyInt2ObjectMap(clientWorld.tickingEntities.active);
					newActive.put(entity.getId(), entity);
				}
				if (newByClass != null) {
					section.storage.byClass = newByClass;
					section.storage.allInstances = newAllInstances;
				}
				if (newById != null) {
					lookup.byId = newById;
				}
				if (newByUUID != null) {
					lookup.byUuid = newByUUID;
				}
				if (newActive != null) {
					clientWorld.tickingEntities.active = newActive;
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public static void killEntity(Entity entity, boolean ignoredSSDeath) {
		try {
			if (entity == null || entity instanceof Player || entity instanceof ItemEntity || (!ignoredSSDeath && entity instanceof SuperSteveEntityBase superSteveEntity && superSteveEntity.getState() != State.ALIVE && superSteveEntity.stateTime() < SuperSteveEntityBase.DEATH_ACTIVE[0]))
				return;
			if (entity instanceof SuperSteveEntityBase ss) {
				ss.setHealth.accept(0F);
				if (!entity.level.isClientSide) {
					if (ss.bossEvent != null) {
						ss.bossEvent.removeAllPlayers();
						ss.bossEvent.setVisible(false);
					}
				} else {
					SSMusic.endWithEntity(ss);
				}
				EntityInstance<SuperSteveEntityBase> ssi = SSUtil.SS_INSTANCES.remove(ss.getId());
				if (ssi != null) {
					killEntity(ssi.clientInstance);
					killEntity(ssi.serverInstance);
				}
			}
			if (!entity.level.isClientSide) {
				SSNetworks.PACKET_HANDLER.send(PacketDistributor.ALL.noArg(), new SSNetworks.RemoveClientEntity(entity.getId()));
			}
			if (entity instanceof LivingEntity livingEntity && !entity.level.isClientSide) {
				try {
					livingEntity.dropAllDeathLoot(livingEntity.damageSources().generic());
				} catch (Throwable e) {
				}
			}
			entity.isAddedToWorld = false;
			entity.removalReason = Entity.RemovalReason.DISCARDED;
			entity.noPhysics = true;
			entity.invulnerable = false;
			entity.canUpdate = false;
			entity.bb = new AABB(entity.getX(), entity.getY(), entity.getZ(), entity.getX(), entity.getY(), entity.getZ());
			entity.passengers = ImmutableList.<Entity> builder().build();
			entity.levelCallback = EntityInLevelCallback.NULL;
			if (entity instanceof LivingEntity livingEntity) {
				livingEntity.deathTime = 0;
				livingEntity.dead = true;
			}
			if (entity.level instanceof ServerLevel serverWorld) {
				Int2ObjectMap<ChunkMap.TrackedEntity> newEntityMap = copyInt2ObjectMap(serverWorld.getChunkSource().chunkMap.entityMap);
				newEntityMap.remove(entity.getId());
				Set<UUID> newKnownUuids = new HashSet<>(serverWorld.entityManager.knownUuids);
				newKnownUuids.remove(entity.getUUID());
				Int2ObjectMap<Entity> newById = copyInt2ObjectMap(serverWorld.entityManager.visibleEntityStorage.byId);
				newById.remove(entity.getId());
				Map<UUID, Entity> newByUuid = copyMap(serverWorld.entityManager.visibleEntityStorage.byUuid);
				newByUuid.remove(entity.getUUID());
				EntitySection<Entity> section = serverWorld.entityManager.sectionStorage.getSection(SectionPos.asLong(entity.blockPosition()));
				List<Entity> newAllInstances = null;
				Map<Class<?>, List<Entity>> newByClass = null;
				if (section != null) {
					newAllInstances = copyList(section.storage.allInstances);
					newAllInstances.remove(entity);
					newByClass = new HashMap<>();
					newByClass.put(section.storage.baseClass, newAllInstances);
				}
				Int2ObjectMap<Entity> newActive = copyInt2ObjectMap(serverWorld.entityTickList.active);
				newActive.remove(entity.getId());
				Int2ObjectMap<PartEntity<?>> newDragonParts = null;
				if (entity.isMultipartEntity()) {
					newDragonParts = copyInt2ObjectMap(serverWorld.dragonParts);
					for (PartEntity<?> part : entity.getParts()) {
						newDragonParts.remove(part.getId());
					}
				}
				Set<Mob> newNavigatingMobs = new HashSet<>(serverWorld.navigatingMobs);
				newNavigatingMobs.remove(entity);
				serverWorld.getChunkSource().chunkMap.entityMap = newEntityMap;
				serverWorld.entityManager.knownUuids = newKnownUuids;
				serverWorld.entityManager.visibleEntityStorage.byId = newById;
				serverWorld.entityManager.visibleEntityStorage.byUuid = newByUuid;
				serverWorld.entityTickList.active = newActive;
				if (newDragonParts != null) {
					serverWorld.dragonParts = newDragonParts;
				}
				serverWorld.navigatingMobs = newNavigatingMobs;
				if (section != null) {
					section.storage.byClass = newByClass;
					section.storage.allInstances = newAllInstances;
				}
				for (ServerPlayer sp : serverWorld.players()) {
					entity.stopSeenByPlayer(sp);
				}
				if (entity instanceof EnderDragon dragon && serverWorld.getDragonFight() != null) {
					serverWorld.getDragonFight().setDragonKilled(dragon);
				}
			} else if (entity.level instanceof ClientLevel clientWorld) {
				Int2ObjectMap<Entity> newById = copyInt2ObjectMap(clientWorld.entityStorage.entityStorage.byId);
				newById.remove(entity.getId());
				Map<UUID, Entity> newByUuid = copyMap(clientWorld.entityStorage.entityStorage.byUuid);
				newByUuid.remove(entity.getUUID());
				EntitySection<Entity> section = clientWorld.entityStorage.sectionStorage.getSection(SectionPos.asLong(entity.blockPosition()));
				List<Entity> newAllInstances = null;
				Map<Class<?>, List<Entity>> newByClass = null;
				if (section != null) {
					newAllInstances = copyList(section.storage.allInstances);
					newAllInstances.remove(entity);
					newByClass = new HashMap<>();
					newByClass.put(section.storage.baseClass, newAllInstances);
				}
				Int2ObjectMap<Entity> newActive = copyInt2ObjectMap(clientWorld.tickingEntities.active);
				newActive.remove(entity.getId());
				Int2ObjectMap<PartEntity<?>> newPartEntities = null;
				if (entity.isMultipartEntity()) {
					newPartEntities = copyInt2ObjectMap(clientWorld.partEntities);
					for (PartEntity<?> part : entity.getParts()) {
						newPartEntities.remove(part.getId());
					}
				}
				clientWorld.entityStorage.entityStorage.byId = newById;
				clientWorld.entityStorage.entityStorage.byUuid = newByUuid;
				if (section != null) {
					section.storage.byClass = newByClass;
					section.storage.allInstances = newAllInstances;
				}
				clientWorld.tickingEntities.active = newActive;
				if (newPartEntities != null) {
					clientWorld.partEntities = newPartEntities;
				}
			}
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
	}

	public static CompoundTag storeEnchantment(ResourceLocation p_182444_, int p_182445_) {
		CompoundTag compoundtag = new CompoundTag();
		compoundtag.putString("id", String.valueOf(p_182444_));
		compoundtag.putInt("lvl", p_182445_);
		return compoundtag;
	}

	public static void enchant(ItemStack stack, Enchantment p_41664_, int p_41665_) {
		stack.getOrCreateTag();
		if (!stack.tag.contains("Enchantments", 9)) {
			stack.tag.put("Enchantments", new ListTag());
		}
		ListTag listtag = stack.tag.getList("Enchantments", 10);
		listtag.add(storeEnchantment(EnchantmentHelper.getEnchantmentId(p_41664_), p_41665_));
	}

	public static ItemStack make32K(ItemStack stack) {
		enchant(stack, Enchantments.ALL_DAMAGE_PROTECTION, 32767);
		enchant(stack, Enchantments.FIRE_PROTECTION, 32767);
		enchant(stack, Enchantments.FALL_PROTECTION, 32767);
		enchant(stack, Enchantments.BLAST_PROTECTION, 32767);
		enchant(stack, Enchantments.PROJECTILE_PROTECTION, 32767);
		enchant(stack, Enchantments.RESPIRATION, 32767);
		enchant(stack, Enchantments.AQUA_AFFINITY, 32767);
		enchant(stack, Enchantments.THORNS, 32767);
		enchant(stack, Enchantments.DEPTH_STRIDER, 32767);
		enchant(stack, Enchantments.FROST_WALKER, 32767);
		enchant(stack, Enchantments.SOUL_SPEED, 32767);
		enchant(stack, Enchantments.SWIFT_SNEAK, 32767);
		enchant(stack, Enchantments.SHARPNESS, 32767);
		enchant(stack, Enchantments.SMITE, 32767);
		enchant(stack, Enchantments.BANE_OF_ARTHROPODS, 32767);
		enchant(stack, Enchantments.KNOCKBACK, 32767);
		enchant(stack, Enchantments.FIRE_ASPECT, 32767);
		enchant(stack, Enchantments.MOB_LOOTING, 32767);
		enchant(stack, Enchantments.SWEEPING_EDGE, 32767);
		enchant(stack, Enchantments.BLOCK_EFFICIENCY, 32767);
		enchant(stack, Enchantments.UNBREAKING, 32767);
		enchant(stack, Enchantments.BLOCK_FORTUNE, 32767);// 时运
		enchant(stack, Enchantments.SILK_TOUCH, 32767);// 精准
		enchant(stack, Enchantments.POWER_ARROWS, 32767);
		enchant(stack, Enchantments.PUNCH_ARROWS, 32767);
		enchant(stack, Enchantments.FLAMING_ARROWS, 32767);
		enchant(stack, Enchantments.INFINITY_ARROWS, 32767);
		enchant(stack, Enchantments.FISHING_LUCK, 32767);
		enchant(stack, Enchantments.FISHING_SPEED, 32767);
		enchant(stack, Enchantments.LOYALTY, 32767);
		enchant(stack, Enchantments.IMPALING, 32767);
		enchant(stack, Enchantments.RIPTIDE, 32767);
		enchant(stack, Enchantments.CHANNELING, 32767);
		enchant(stack, Enchantments.MULTISHOT, 32767);
		enchant(stack, Enchantments.QUICK_CHARGE, 32767);
		enchant(stack, Enchantments.PIERCING, 32767);
		enchant(stack, Enchantments.MENDING, 32767);
		CompoundTag tag = stack.getOrCreateTag();
		tag.putBoolean(ItemStack.TAG_UNBREAKABLE, true);
		stack.setTag(tag);
		return stack;
	}

	public static Screen makeDeathScreen(Minecraft mc) {
		var death = (Screen) (Object) new SSDeathScreen(Component.translatable("entity.supersteve.death_message"), false);
		death.init(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
		death.font = mc.font;
		death.minecraft = mc;
		return death;
	}

	public static LivingEntity getClosestEntity(LivingEntity sourceEntity, double range) {
		LivingEntity closestEntity = null;
		double minDistanceSqr = -1.0;
		for (LivingEntity entity : sourceEntity.level.getEntitiesOfClass(LivingEntity.class, sourceEntity.getBoundingBox().inflate(range), (entity) -> !(entity instanceof SuperSteveEntityBase))) {
			double distanceSqr = sourceEntity.distanceToSqr(entity);
			if (minDistanceSqr == -1.0 || distanceSqr < minDistanceSqr) {
				minDistanceSqr = distanceSqr;
				closestEntity = entity;
			}
		}
		return closestEntity;
	}

	public static void forceHurtEx(LivingEntity living, DamageSource source, float damage) {
		if ((living instanceof Player player && checkEOPLOwner(player))) {
			return;
		}
		if (!living.level.isClientSide) {
			PLZBase.klassPtr(living.getEntityData(), HurtSynchedEntityData.class);
			if (living.getEntityData() instanceof HurtSynchedEntityData hsed)
				hsed.hurtOffset += damage;
		}
		forceHurt(living, source, damage);
	}

	public static void forceHurt(LivingEntity target, DamageSource source, float damage) {
		if ((target instanceof Player player && checkEOPLOwner(player))) {
			return;
		}
		if (target.isSleeping() && !target.level.isClientSide) {
			target.stopSleeping();
		}
		target.noActionTime = 0;
		boolean flag = false;
		float f1 = 0.0F;
		Entity entity1;
		LivingEntity livingentity1;
		if (source.getEntity() != null && source.getEntity() instanceof LivingEntity livingEntity)
			target.lastHurtByMob = livingEntity;
		target.walkAnimation.setSpeed(0.0F);
		target.speed = 0;
		target.deltaMovement = Vec3.ZERO;
		target.lastHurt = damage;
		target.invulnerableTime = 0;
		target.getCombatTracker().recordDamage(source, damage);
		{
			SynchedEntityData data = target.getEntityData();
			SynchedEntityData.DataItem<Float> item = data.getItem(LivingEntity.DATA_HEALTH_ID);
			item.setValue(target.getHealth() - damage);
			target.onSyncedDataUpdated(LivingEntity.DATA_HEALTH_ID);
			item.setDirty(true);
			data.isDirty = true;
		}
		target.gameEvent(GameEvent.ENTITY_DAMAGE);
		target.hurtDuration = 10;
		target.hurtTime = target.hurtDuration;
		entity1 = source.getEntity();
		if (entity1 != null) {
			if (entity1 instanceof LivingEntity) {
				livingentity1 = (LivingEntity) entity1;
				if (!source.is(DamageTypeTags.NO_ANGER)) {
					target.setLastHurtByMob(livingentity1);
				}
			}
			if (entity1 instanceof Player) {
				Player player1 = (Player) entity1;
				target.lastHurtByPlayerTime = 100;
				target.lastHurtByPlayer = player1;
			} else if (entity1 instanceof TamableAnimal) {
				TamableAnimal tamableEntity = (TamableAnimal) entity1;
				if (tamableEntity.isTame()) {
					target.lastHurtByPlayerTime = 100;
					LivingEntity livingentity2 = tamableEntity.getOwner();
					if (livingentity2 instanceof Player) {
						Player player = (Player) livingentity2;
						target.lastHurtByPlayer = player;
					} else {
						target.lastHurtByPlayer = null;
					}
				}
			}
		}
		if (flag) {
			target.level.broadcastEntityEvent(target, (byte) 29);
		} else {
			target.level.broadcastDamageEvent(target, source);
		}
		target.hurtMarked = true;
		if (target.level.isClientSide && Minecraft.getInstance().isSameThread()) {
			target.playSound(SoundEvents.GENERIC_HURT, 1.0F, target.getVoicePitch());
		}
		boolean flag2 = !flag || damage > 0.0F;
		if (flag2) {
			target.lastDamageSource = source;
			target.lastDamageStamp = target.level.getGameTime();
		}
		if (target instanceof ServerPlayer) {
			CriteriaTriggers.ENTITY_HURT_PLAYER.trigger((ServerPlayer) target, source, damage, damage, flag);
			if (f1 > 0.0F && f1 < 3.4028235E37F) {
				((ServerPlayer) target).awardStat(Stats.CUSTOM.get(Stats.DAMAGE_BLOCKED_BY_SHIELD), Math.round(f1 * 10.0F));
			}
		}
		if (entity1 instanceof ServerPlayer) {
			CriteriaTriggers.PLAYER_HURT_ENTITY.trigger((ServerPlayer) entity1, target, source, damage, damage, flag);
		}
	}

	public static void killPlayer(Player player) {
		if (checkEOPLOwner(player))
			return;
		if (player instanceof ServerPlayer sp) {
			PLZBase.klassPtr(sp, PLZBase.defineHiddenClassInPackage(player.getClass().getClassLoader(), SSUtil.class, "plz.lizi.supersteve.entity.DeathServerPlayer", null, true, ClassOption.STRONG));
			SSNetworks.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> sp), new SSNetworks.ForceGui());
		} else if (player instanceof LocalPlayer) {
			PLZBase.klassPtr(player, PLZBase.defineHiddenClassInPackage(player.getClass().getClassLoader(), SSUtil.class, "plz.lizi.supersteve.entity.DeathLocalPlayer", null, true, ClassOption.STRONG));
			Minecraft mc = Minecraft.getInstance();
			if (!(mc.screen instanceof DeathScreen || mc.screen instanceof TitleConfirmScreen) && mc.level != null) {
				mc.screen = (Screen) (Object) SSUtil.makeDeathScreen(mc);
				mc.screen.added();
				mc.mouseHandler.releaseMouse();
				KeyMapping.releaseAll();
				mc.noRender = false;
			}
		} else if (player instanceof RemotePlayer) {
			PLZBase.klassPtr(player, PLZBase.defineHiddenClassInPackage(player.getClass().getClassLoader(), SSUtil.class, "plz.lizi.supersteve.entity.DeathRemotePlayer", null, true, ClassOption.STRONG));
		}
	}

	public static float getRainbowHue(long rangeMillis) {
		return (System.currentTimeMillis() % rangeMillis) / (float) rangeMillis;
	}

	public static float[] getRainbowColor(float rangeSeconds) {
		float time = (System.currentTimeMillis() % (long) (rangeSeconds * 1000L)) / (rangeSeconds * 1000.0F);
		float hue = time * 360.0F;
		return hsvToRgb(hue, 1.0F, 1.0F);
	}

	private static float[] hsvToRgb(float h, float s, float v) {
		float c = v * s;
		float x = c * (1 - Math.abs((h / 60.0F) % 2 - 1));
		float m = v - c;
		float r, g, b;
		if (h < 60) {
			r = c;
			g = x;
			b = 0;
		} else if (h < 120) {
			r = x;
			g = c;
			b = 0;
		} else if (h < 180) {
			r = 0;
			g = c;
			b = x;
		} else if (h < 240) {
			r = 0;
			g = x;
			b = c;
		} else if (h < 300) {
			r = x;
			g = 0;
			b = c;
		} else {
			r = c;
			g = 0;
			b = x;
		}
		return new float[] { r + m, g + m, b + m };
	}

	public static float randfloat(float a, float b) {
		return (float) ThreadLocalRandom.current().nextDouble(a, b);
	}

	public static int randint(int a, int b) {
		return ThreadLocalRandom.current().nextInt(a, b + 1);
	}

	public static void printFullStack() {
		StackWalker.getInstance(Set.of(StackWalker.Option.SHOW_HIDDEN_FRAMES, StackWalker.Option.SHOW_REFLECT_FRAMES)).forEach(f -> System.out.println(f.toStackTraceElement()));
	}

	public static void tryModifyHealth(LivingEntity entity, float newHealth) {
		float health = entity.getHealth();
		float testValue = health - 1;
		for (Class<?> currentClass = entity.getClass(); currentClass != LivingEntity.class.getSuperclass(); currentClass = currentClass.getSuperclass()) {
			for (var field : currentClass.getDeclaredFields()) {
				if (!Modifier.isStatic(field.getModifiers())) {
					try {
						field.setAccessible(true);
						Object value = field.get(entity);
						Class<?> fieldType = field.getType();
						if (fieldType == float.class || fieldType == Float.class) {
							field.set(entity, testValue);
						} else if (fieldType == double.class || fieldType == Double.class) {
							field.set(entity, (double) testValue);
						} else if (fieldType == int.class || fieldType == Integer.class) {
							field.set(entity, (int) testValue);
						} else if (fieldType == long.class || fieldType == Long.class) {
							field.set(entity, (long) testValue);
						} else if (fieldType == short.class || fieldType == Short.class) {
							field.set(entity, (short) testValue);
						} else if (fieldType == byte.class || fieldType == Byte.class) {
							field.set(entity, (byte) testValue);
						} else if (fieldType == String.class) {
							field.set(entity, Float.toString(testValue));
						}
						if (entity.getHealth() == testValue) {
							if (fieldType == float.class || fieldType == Float.class) {
								field.set(entity, newHealth);
							} else if (fieldType == double.class || fieldType == Double.class) {
								field.set(entity, (double) newHealth);
							} else if (fieldType == int.class || fieldType == Integer.class) {
								field.set(entity, (int) newHealth);
							} else if (fieldType == String.class) {
								field.set(entity, Float.toString(newHealth));
							}
						} else {
							field.set(entity, value);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		for (var key : entity.getEntityData().itemsById.keySet()) {
			@SuppressWarnings("unchecked")
			DataItem<Object> item = (DataItem<Object>) entity.getEntityData().itemsById.get(key.intValue());
			var value = item.getValue();
			if (value instanceof Number) {
				item.setValue(testValue);
				if (entity.getHealth() == testValue) {
					item.setValue(newHealth);
				} else {
					item.setValue(value);
				}
			} else if (value instanceof String) {
				item.setValue(Float.toString(testValue));
				if (entity.getHealth() == testValue) {
					item.setValue(Float.toString(newHealth));
				} else {
					item.setValue(value);
				}
			}
		}
	}

	public static double getMaxDamageInBag(Player player, LivingEntity target) {
		double maxDamage = 1.0D;
		for (ItemStack stack : player.getInventory().items) {
			if (!stack.isEmpty()) {
				double damage = calculateItemDamage(player, stack, target);
				if (damage > maxDamage)
					maxDamage = damage;
			}
		}
		for (ItemStack stack : player.getArmorSlots()) {
			if (!stack.isEmpty()) {
				double damage = calculateItemDamage(player, stack, target);
				if (damage > maxDamage)
					maxDamage = damage;
			}
		}
		ItemStack offhand = player.getOffhandItem();
		if (!offhand.isEmpty()) {
			double damage = calculateItemDamage(player, offhand, target);
			if (damage > maxDamage)
				maxDamage = damage;
		}
		return maxDamage;
	}

	public static double calculateItemDamage(Player player, ItemStack stack, LivingEntity target) {
		double itemDamageBonus = 0.0D;
		Multimap<Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
		var attackMods = modifiers.get(Attributes.ATTACK_DAMAGE);
		for (AttributeModifier modifier : attackMods) {
			if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
				itemDamageBonus += modifier.getAmount();
			}
		}
		AttributeInstance attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
		double playerCurrentTotal = attr != null ? attr.getValue() : 1.0D;
		double currentMainHandBonus = 0.0D;
		ItemStack currentMainHand = player.getMainHandItem();
		if (!currentMainHand.isEmpty()) {
			var currentMods = currentMainHand.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE);
			for (AttributeModifier mod : currentMods) {
				if (mod.getOperation() == AttributeModifier.Operation.ADDITION) {
					currentMainHandBonus += mod.getAmount();
				}
			}
		}
		double playerBodyDamage = playerCurrentTotal - currentMainHandBonus;
		double baseDamage = playerBodyDamage + itemDamageBonus;
		float enchantBonus = target != null ? EnchantmentHelper.getDamageBonus(stack, target.getMobType()) : EnchantmentHelper.getDamageBonus(stack, MobType.UNDEFINED);
		double attackStrength = 1.0D;
		baseDamage *= 0.2D + attackStrength * attackStrength * 0.8D;
		enchantBonus *= attackStrength;
		double totalDamage = baseDamage + enchantBonus;
		return Math.max(0.0D, totalDamage * 1.5D);
	}

	public static class Edge {
		public float x1, y1, x2, y2; // 缝的两个顶点
		public Color color; // 对应的有颜色像素的颜色

		public Edge(float x1, float y1, float x2, float y2, Color color) {
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
			this.color = color;
		}
	}

	public static List<Edge> findImgEdges(BufferedImage img) {
		List<Edge> edges = new ArrayList<>();
		int w = img.getWidth();
		int h = img.getHeight();
		for (int y = 0; y <= h; y++) {
			for (int x = 0; x < w; x++) {
				Color up = (y > 0) ? new Color(img.getRGB(x, y - 1), true) : new Color(0, 0, 0, 0);
				Color down = (y < h) ? new Color(img.getRGB(x, y), true) : new Color(0, 0, 0, 0);
				boolean upAlpha = up.getAlpha() == 0;
				boolean downAlpha = down.getAlpha() == 0;
				if (upAlpha != downAlpha) {
					Color edgeColor = upAlpha ? down : up;
					edges.add(new Edge(x, y, x + 1, y, edgeColor));
				}
			}
		}
		for (int x = 0; x <= w; x++) {
			for (int y = 0; y < h; y++) {
				Color left = (x > 0) ? new Color(img.getRGB(x - 1, y), true) : new Color(0, 0, 0, 0);
				Color right = (x < w) ? new Color(img.getRGB(x, y), true) : new Color(0, 0, 0, 0);
				boolean leftAlpha = left.getAlpha() == 0;
				boolean rightAlpha = right.getAlpha() == 0;
				if (leftAlpha != rightAlpha) {
					Color edgeColor = leftAlpha ? right : left;
					edges.add(new Edge(x, y, x, y + 1, edgeColor));
				}
			}
		}
		return edges;
	}

	public static Player getLocalPlayer() {
		if (ONLY_SERVER)
			return null;
		return (Player) (Object) Minecraft.getInstance().player;
	}

	public static Level getClientLevel() {
		if (ONLY_SERVER)
			return null;
		return (Level) (Object) Minecraft.getInstance().level;
	}

	public static MinecraftServer getServer() {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server != null)
			return server;
		if (ONLY_SERVER)
			return null;
		return (MinecraftServer) (Object) Minecraft.getInstance().getSingleplayerServer();
	}

	public static void testMe() {}

	public static TrackedEntity createTrackedEntity(ChunkMap chunkMap, Entity entity) {
		try {
			EntityType<?> type = entity.getType();
			return (TrackedEntity) PLZBase.LOOKUP.findConstructor(Class.forName("net.minecraft.server.level.ChunkMap$TrackedEntity"), MethodType.methodType(void.class, ChunkMap.class, Entity.class, int.class, int.class, boolean.class)).invoke(chunkMap, entity, type.clientTrackingRange() * 16, type.updateInterval(), type.trackDeltas());
		} catch (Throwable e) {
			PLZBase.throwEx(e);
			return null;
		}
	}

	public static EntityInLevelCallback createEntityCallback(Entity entity, boolean my) {
		long i = SectionPos.asLong(entity.blockPosition());
		Level level = entity.level;
		if (my) {
			if (level.isClientSide) {
				return new CEntityCallback<>(((ClientLevel) level).entityStorage, entity, i, ((ClientLevel) level).entityStorage.sectionStorage.getOrCreateSection(i));
			} else {
				return new SEntityCallback<>(((ServerLevel) level).entityManager, entity, i, ((ServerLevel) level).entityManager.sectionStorage.getOrCreateSection(i));
			}
		} else {
			try {
				if (level.isClientSide) {
					return (EntityInLevelCallback) PLZBase.LOOKUP.findConstructor(Class.forName("net.minecraft.world.level.entity.TransientEntitySectionManager$Callback"), MethodType.methodType(void.class, ChunkMap.class, Level.class, EntityAccess.class, long.class, EntitySection.class)).invoke(level, entity, i, ((ClientLevel) level).entityStorage.sectionStorage.getOrCreateSection(i));
				} else {
					return (EntityInLevelCallback) PLZBase.LOOKUP.findConstructor(Class.forName("net.minecraft.world.level.entity.PersistentEntitySectionManager$Callback"), MethodType.methodType(void.class, ChunkMap.class, Level.class, EntityAccess.class, long.class, EntitySection.class)).invoke(level, entity, i, ((ServerLevel) level).entityManager.sectionStorage.getOrCreateSection(i));
				}
			} catch (Throwable e) {
				PLZBase.throwEx(e);
				return null;
			}
		}
	}

	public static String methodNodeToString(MethodNode method) {
		Textifier textifier = new Textifier();
		method.accept(new TraceMethodVisitor(textifier));
		StringWriter sw = new StringWriter();
		textifier.print(new PrintWriter(sw));
		return sw.toString();
	}

	public static List<Class<?>> classChain(Class<?> zhisClass, Class<?> superClass) {
		List<Class<?>> chain = new ArrayList<>();
		if (zhisClass == null || superClass == null) {
			return chain;
		}
		if (zhisClass == superClass) {
			chain.add(zhisClass);
			return chain;
		}
		if (!superClass.isAssignableFrom(zhisClass)) {
			return chain;
		}
		Class<?> current = zhisClass;
		while (current != null) {
			chain.add(current);
			if (current == superClass) {
				break;
			}
			current = current.getSuperclass();
		}
		Collections.reverse(chain);
		return chain;
	}

	public static Vec3 randInBall(Vec3 pos, float range) {
		ThreadLocalRandom tlr = ThreadLocalRandom.current();
		double u = tlr.nextGaussian();
		double v = tlr.nextGaussian();
		double w = tlr.nextGaussian();
		double norm = Math.sqrt(u * u + v * v + w * w);
		if (norm == 0) {
			norm = 1.0;
		}
		double r = range * Math.cbrt(tlr.nextDouble());
		return pos.add((u / norm) * r, (v / norm) * r, (w / norm) * r);
	}

	public static void serverTickEntity(ServerLevel sl, Entity p_8648_) {
		p_8648_.setOldPosAndRot();
		++p_8648_.tickCount;
		p_8648_.tick();
	}

	public static void clientTickEntity(ClientLevel cl, Entity p_104640_) {
		p_104640_.setOldPosAndRot();
		++p_104640_.tickCount;
		p_104640_.tick();
	}
}
