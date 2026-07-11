package plz.lizi.supersteve.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import plz.lizi.supersteve.SuperSteveMod;
import plz.lizi.supersteve.api.PLZBase;
import plz.lizi.supersteve.api.SSUtil;
import plz.lizi.supersteve.client.renderer.EOPLItemEx;
import plz.lizi.supersteve.client.sound.SSMusic;

public class EndOfPLZLite extends Item {
	public EndOfPLZLite() {
		super(new Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC));
	}

	@Override
	public UseAnim getUseAnimation(ItemStack itemstack) {
		return UseAnim.BLOCK;
	}

	@Override
	public boolean hasCraftingRemainingItem() {
		return true;
	}

	@Override
	public ItemStack getCraftingRemainingItem(ItemStack itemstack) {
		return new ItemStack(this);
	}

	@Override
	public int getEnchantmentValue() {
		return 0;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isFoil(ItemStack itemstack) {
		return false;
	}

	@Override
	public int getUseDuration(ItemStack itemstack) {
		return 128000;
	}

	@Override
	public float getDestroySpeed(ItemStack p_41425_, BlockState state) {
		return -1.0F;
	}

	@Override
	public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
		return true;
	}

	@Override
	public boolean isCorrectToolForDrops(BlockState state) {
		return true;
	}

	@Override
	public boolean isBarVisible(ItemStack p_150899_) {
		return true;
	}

	@Override
	public int getBarColor(ItemStack p_150901_) {
		return java.awt.Color.HSBtoRGB(SSUtil.getRainbowHue(5000), 1.0F, 1.0F);
	}

	@Override
	public int getBarWidth(ItemStack p_150900_) {
		return 13;
	}

	@Override
	public Component getName(ItemStack p_41458_) {
		return Component.literal(PLZBase.lowCaseFlowString(SuperSteveMod.SAFEMODE ? "- E N D  O F  T W D R -" : "- E N D  O F  P L Z * L I T E -", 3, 0.04)).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(java.awt.Color.HSBtoRGB(SSUtil.getRainbowHue(5000), 1.0F, 1.0F))).withBold(true));
	}

	@Override
	public Component getHighlightTip(ItemStack item, Component displayName) {
		return Component.literal(PLZBase.lowCaseFlowString(SuperSteveMod.SAFEMODE ? "- END OF TWDR -" : "- END OF PLZ * LITE -", 3, 0.04)).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(java.awt.Color.HSBtoRGB(SSUtil.getRainbowHue(5000), 1.0F, 1.0F))).withBold(true));
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		entity.startUsingItem(hand);
		return super.use(world, entity, hand);
	}

	public static void killAndDrop(Entity entityIn, Player player) {
		if (entityIn instanceof LivingEntity le)
			SSUtil.forceHurt(le, player.level.damageSources.playerAttack(player), 0);
		SSUtil.killEntity(entityIn);
	}

	@Override
	public void releaseUsing(ItemStack itemstack, Level world, LivingEntity entity, int time) {
		if (!(entity instanceof Player))
			return;
		Player player = (Player) entity;
		if (!world.isClientSide) {
			ServerLevel serverLevel = (ServerLevel) world;
			try {
				for (var entityIn : new ArrayList<>(StreamSupport.stream(serverLevel.getAllEntities().spliterator(), false).collect(Collectors.toList()))) {
					killAndDrop(entityIn, player);
				}
			} catch (Throwable throwable) {
				throwable.printStackTrace();
			}
			try {
				ObjectIterator<EntitySection<Entity>> iterator = serverLevel.entityManager.sectionStorage.sections.values().iterator();
				while (iterator.hasNext()) {
					EntitySection<Entity> section = iterator.next();
					for (Entity entityIn : new ArrayList<>(section.storage.allInstances)) {
						killAndDrop(entityIn, player);
					}
				}
			} catch (Throwable throwable) {
				throwable.printStackTrace();
			}
			if (serverLevel.getDragonFight() != null && serverLevel.getEntity(serverLevel.getDragonFight().getDragonUUID()) instanceof EnderDragon dragon)
				serverLevel.getDragonFight().setDragonKilled(dragon);
		} else {
			ClientLevel clientLevel = (ClientLevel) world;
			try {
				clientLevel.minecraft.gui.getBossOverlay().reset();
				for (var entityIn : new ArrayList<>(StreamSupport.stream(clientLevel.entitiesForRendering().spliterator(), false).collect(Collectors.toList()))) {
					killAndDrop(entityIn, player);
				}
			} catch (Throwable throwable) {
				throwable.printStackTrace();
			}
			try {
				ObjectIterator<EntitySection<Entity>> iterator = clientLevel.entityStorage.sectionStorage.sections.values().iterator();
				while (iterator.hasNext()) {
					EntitySection<Entity> section = iterator.next();
					for (Entity entityIn : new ArrayList<>(section.storage.allInstances)) {
						killAndDrop(entityIn, player);
					}
				}
			} catch (Throwable throwable) {
				try {
					Object values = PLZBase.getField(clientLevel.entityStorage.sectionStorage.sections, false, "value");
					for (Object obj : (Object[]) ((values instanceof EntitySection[]) ? values : new Object[0])) {
						if (obj instanceof EntitySection section_) {
							@SuppressWarnings("unchecked")
							EntitySection<Entity> section = section_;
							for (Entity entityIn : new ArrayList<>(section.storage.allInstances)) {
								killAndDrop(entityIn, player);
							}
						}
					}
				} catch (Throwable e) {
					throwable.printStackTrace();
					e.printStackTrace();
				}
			}
			SSMusic.endAllBgm();
			Minecraft.getInstance().getSoundManager().stop();
		}
		for (var ss_instance : SSUtil.SS_INSTANCES.values()) {
			SSUtil.killEntity(ss_instance.serverInstance);
			SSUtil.killEntity(ss_instance.clientInstance);
		}
		SSUtil.SS_INSTANCES.clear();
	}

	@Override
	public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
		return true;
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity entity, LivingEntity sourceentity) {
		SSUtil.killEntity(entity);
		return false;
	}

	@Override
	public boolean onEntitySwing(ItemStack itemstack, LivingEntity entity) {
		if (!entity.level.isClientSide && entity instanceof ServerPlayer player) {
			Vec3 lookVec = player.getLookAngle();
			Level level = player.level;
			BlockPos blockPos = player.level.clip(new ClipContext(player.getEyePosition(1f), player.getEyePosition(1f).add(player.getViewVector(1f).scale(player.getBlockReach())), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)).getBlockPos();
			for (int i = 0; i < 20; i++) {
				Vec3 targetVec = player.getEyePosition(1.0F).add(lookVec.scale(i));
				for (Entity entityIn : level.getEntitiesOfClass(LivingEntity.class, new AABB(targetVec.x() - 0.5, targetVec.y() - 0.5, targetVec.z() - 0.5, targetVec.x() + 0.5, targetVec.y() + 0.5, targetVec.z() + 0.5))) {
					killAndDrop(entityIn, player);
				}
			}
			var drops = Block.getDrops(level.getBlockState(blockPos), player.serverLevel(), blockPos, null);
			if (drops.isEmpty()) {
				player.getInventory().add(new ItemStack(level.getBlockState(blockPos).getBlock()));
			} else {
				for (var itemStack : drops) {
					player.getInventory().add(itemStack);
				}
			}
			level.destroyBlock(blockPos, false);
		} else {
		}
		return false;
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		try {
			if (entity instanceof Player player) {
				SSUtil.checkEOPLOwner(player);
				SSUtil.safeEntity(player);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onDroppedByPlayer(ItemStack itemstack, Player player) {
		SSUtil.removeEOPLOwner(player);
		return true;
	}

	@Override
	public boolean mineBlock(ItemStack p_41416_, Level p_41417_, BlockState p_41418_, BlockPos p_41419_, LivingEntity p_41420_) {
		return false;
	}

	@Override
	public boolean canAttackBlock(BlockState p_41441_, Level p_41442_, BlockPos p_41443_, Player p_41444_) {
		return false;
	}

	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(EOPLItemEx.INSTACNE);
	}
}
