package plz.lizi.supersteve.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import plz.lizi.supersteve.SuperSteveMod;
import plz.lizi.supersteve.client.renderer.model.SolidImgModel;
import plz.lizi.supersteve.client.renderer.model.SuperSteveModel;
import plz.lizi.supersteve.entity.SuperSteveEntityBase;
import plz.lizi.supersteve.entity.SuperSteveEntityBase.State;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelLayers;
import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

public class SuperSteveRenderer extends HumanoidMobRenderer<SuperSteveEntityBase, SuperSteveModel> {
	private static final ResourceLocation TEXTURE = ResourceLocation.tryBuild(SuperSteveMod.MODID, "textures/entities/steve.png");
	private static final ResourceLocation WEAPON_CIRCLE = new ResourceLocation("supersteve:textures/entities/ss_weapon_circle.png");
	private static final float SCALE = 0.95F;
	private final HumanoidArmorLayer<SuperSteveEntityBase, SuperSteveModel, SuperSteveModel> armorLayer;
	private final ItemInHandLayer<SuperSteveEntityBase, SuperSteveModel> itemInHandLayer;
	private final List<SSLayer> moveLayers = new ArrayList<>();
	private final List<SSLayer> staticLayers = new ArrayList<>();
	public final SolidImgModel solidWeapons;

	public SuperSteveRenderer(EntityRendererProvider.Context context) {
		super(context, new SuperSteveModel(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
		solidWeapons = new SolidImgModel(WEAPON_CIRCLE, 1F / 16F);
		itemInHandLayer = new ItemInHandLayer<>(this, context.getItemInHandRenderer());
		armorLayer = (new HumanoidArmorLayer<>(this, new SuperSteveModel(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), new SuperSteveModel(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
		addSSLayer(new SSEnterLayer(this));
		addSSLayer(new SSBackLayer(this));
		addSSLayer(new SSGeoLayer(this));
		addSSLayer(new SSStripeLayer(this));
		addSSLayer(new SSWeaponLayer(this));
		addSSLayer(new SSSheildLayer(this));
		addSSLayer(new SSFieldLayer(this));
		addSSLayer(new SSAttackLayer(this));
	}

	public void addSSLayer(SSLayer sl) {
		if (sl.isStatic())
			staticLayers.add(sl);
		else
			moveLayers.add(sl);
	}

	public static <T extends LivingEntity> void fixRot(PoseStack poseStack, T entity, float partialTick) {
		poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(partialTick, entity.yBodyRotO, entity.yBodyRot)));
	}

	public static void unscale(PoseStack poseStack) {
		float us = 1 / SCALE;
		poseStack.scale(us, us, us);
	}

	public static void deathReduce(PoseStack poseStack, SuperSteveEntityBase entity, float partialTick) {
		if (entity.getState() == State.EXIT) {
			float pgs = Math.max(0, Math.min(1, (entity.stateTime() + partialTick - SuperSteveEntityBase.DEATH_ACTIVE[1]) / (SuperSteveEntityBase.DEATH_ACTIVE[2] - SuperSteveEntityBase.DEATH_ACTIVE[1]) * 2));
			pgs = 1 - pgs;
			poseStack.scale(pgs, pgs, pgs);
		}
	}

	@Override
	public ResourceLocation getTextureLocation(SuperSteveEntityBase entity) {
		return TEXTURE;
	}

	@Override
	public void render(SuperSteveEntityBase entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		State animation = entity.getState();
		poseStack.pushPose();
		poseStack.translate(0, entity.getBbHeight() / 3f, 0);
		for (var sl : staticLayers) {
			if (!sl.activeAt().contains(animation))
				continue;
			poseStack.pushPose();
			if (sl.deathReduce())
				deathReduce(poseStack, entity, partialTick);
			sl.render(poseStack, bufferSource, packedLight, entity, 0, 0, partialTick, 0, 0, 0);
			poseStack.popPose();
		}
		poseStack.popPose();
		poseStack.pushPose();
		poseStack.pushPose();
		poseStack.scale(SCALE, SCALE, SCALE);
		this.model.attackTime = this.getAttackAnim(entity, partialTick);
		boolean shouldSit = entity.isPassenger() && entity.getVehicle() != null && entity.getVehicle().shouldRiderSit();
		this.model.riding = shouldSit;
		this.model.young = entity.isBaby();
		float f = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
		float f1 = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
		float f2 = f1 - f;
		float f7;
		if (shouldSit && entity.getVehicle() instanceof LivingEntity) {
			LivingEntity livingentity = (LivingEntity) entity.getVehicle();
			f = Mth.rotLerp(partialTick, livingentity.yBodyRotO, livingentity.yBodyRot);
			f2 = f1 - f;
			f7 = Mth.wrapDegrees(f2);
			if (f7 < -85.0F) {
				f7 = -85.0F;
			}
			if (f7 >= 85.0F) {
				f7 = 85.0F;
			}
			f = f1 - f7;
			if (f7 * f7 > 2500.0F) {
				f += f7 * 0.2F;
			}
			f2 = f1 - f;
		}
		float f6 = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
		if (isEntityUpsideDown(entity)) {
			f6 *= -1.0F;
			f2 *= -1.0F;
		}
		float f8;
		if (entity.hasPose(Pose.SLEEPING)) {
			Direction direction = entity.getBedOrientation();
			if (direction != null) {
				f8 = entity.getEyeHeight(Pose.STANDING) - 0.1F;
				poseStack.translate((float) (-direction.getStepX()) * f8, 0.0F, (float) (-direction.getStepZ()) * f8);
			}
		}
		f7 = this.getBob(entity, partialTick);
		this.setupRotations(entity, poseStack, f7, f, partialTick);
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		this.scale(entity, poseStack, partialTick);
		poseStack.translate(0.0F, -1.501F, 0.0F);
		f8 = 0.0F;
		float f5 = 0.0F;
		if (!shouldSit && entity.isAlive()) {
			f8 = entity.walkAnimation.speed(partialTick);
			f5 = entity.walkAnimation.position(partialTick);
			if (entity.isBaby()) {
				f5 *= 3.0F;
			}
			if (f8 > 1.0F) {
				f8 = 1.0F;
			}
		}
		this.model.prepareMobModel(entity, f5, f8, partialTick);
		this.model.setupAnim(entity, f5, f8, f7, f2, f6);
		if (!entity.isInvisible()) {
			RenderType rendertype = model.renderType(getTextureLocation(entity));
			if (rendertype != null) {
				VertexConsumer vertexconsumer = bufferSource.getBuffer(rendertype);
				int i = getOverlayCoords(entity, this.getWhiteOverlayProgress(entity, partialTick));
				this.model.renderToBuffer(poseStack, vertexconsumer, packedLight, i, 1.0F, 1.0F, 1.0F, 1.0F);
			}
			armorLayer.render(poseStack, bufferSource, packedLight, entity, f5, f8, partialTick, f7, f2, f6);
			itemInHandLayer.render(poseStack, bufferSource, packedLight, entity, f5, f8, partialTick, f7, f2, f6);
		}
		poseStack.pushPose();
		unscale(poseStack);
		for (var ml : moveLayers) {
			if (!ml.activeAt().contains(animation))
				continue;
			poseStack.pushPose();
			if (ml.deathReduce())
				deathReduce(poseStack, entity, partialTick);
			ml.render(poseStack, bufferSource, packedLight, entity, f5, f8, partialTick, f7, f2, f6);
			poseStack.popPose();
		}
		poseStack.popPose();
		poseStack.popPose();
		if (entity.getState() == State.ALIVE)
			this.renderNameTag(entity, entity.getDisplayName(), poseStack, bufferSource, packedLight);
		Entity $$6 = entity.getLeashHolder();
		if ($$6 != null) {
			this.renderLeash(entity, partialTick, poseStack, bufferSource, $$6);
		}
		poseStack.popPose();
	}

	@Override
	public boolean shouldRender(SuperSteveEntityBase p_115468_, Frustum p_115469_, double p_115470_, double p_115471_, double p_115472_) {
		return true;
	}

	@Override
	public void setupRotations(SuperSteveEntityBase p_115317_, PoseStack p_115318_, float p_115319_, float p_115320_, float p_115321_) {
		if (this.isShaking(p_115317_)) {
			p_115320_ += (float) (Math.cos((double) p_115317_.tickCount * 3.25) * Math.PI * 0.4000000059604645);
		}
		if (!p_115317_.hasPose(Pose.SLEEPING)) {
			p_115318_.mulPose(Axis.YP.rotationDegrees(180.0F - p_115320_));
		}
		if (p_115317_.getState() != State.ALIVE) {
		} else if (p_115317_.isAutoSpinAttack()) {
			p_115318_.mulPose(Axis.XP.rotationDegrees(-90.0F - p_115317_.getXRot()));
			p_115318_.mulPose(Axis.YP.rotationDegrees(((float) p_115317_.tickCount + p_115321_) * -75.0F));
		} else if (p_115317_.hasPose(Pose.SLEEPING)) {
			Direction direction = p_115317_.getBedOrientation();
			float f1 = direction != null ? sleepDirectionToRotation(direction) : p_115320_;
			p_115318_.mulPose(Axis.YP.rotationDegrees(f1));
			p_115318_.mulPose(Axis.ZP.rotationDegrees(this.getFlipDegrees(p_115317_)));
			p_115318_.mulPose(Axis.YP.rotationDegrees(270.0F));
		} else if (isEntityUpsideDown(p_115317_)) {
			p_115318_.translate(0.0F, p_115317_.getBbHeight() + 0.1F, 0.0F);
			p_115318_.mulPose(Axis.ZP.rotationDegrees(180.0F));
		}
	}
}
