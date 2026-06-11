package plz.lizi.supersteve.client.renderer;

import java.util.Set;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import plz.lizi.supersteve.client.renderer.model.SolidImgModel;
import plz.lizi.supersteve.client.renderer.model.SuperSteveModel;
import plz.lizi.supersteve.entity.SuperSteveEntityBase;
import plz.lizi.supersteve.entity.SuperSteveEntityBase.State;

public class SSBackLayer extends SSLayer {
	private static final ResourceLocation WEAPON_CIRCLE = new ResourceLocation("supersteve:textures/entities/ss_weapon_circle.png");
	private static final float ROAT_SPEED = 5;
	private static final float FLOAT = 0.3F;
	private static final float FLOAT_SPEED = 40;
	private final SolidImgModel solidImgModel;

	public SSBackLayer(RenderLayerParent<SuperSteveEntityBase, SuperSteveModel> parent) {
		super(parent);
		solidImgModel = new SolidImgModel(WEAPON_CIRCLE, 1F / 16F);
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, SuperSteveEntityBase entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
		poseStack.pushPose();
		float tick = entity.ssGetTick() + partialTicks;
		double time = tick / FLOAT_SPEED;
		double yOffset = Math.sin(time * 2 * Math.PI) * FLOAT;
		poseStack.translate(0.0D, -0.5D + yOffset, 0.5D);
		poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
		poseStack.mulPose(Axis.ZP.rotationDegrees(tick * ROAT_SPEED));
		float size = 3F;
		poseStack.scale(size, size, size);
		solidImgModel.render(poseStack);
		//VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(WEAPON_CIRCLE));
		//PoseStack.Pose pose = poseStack.last();
		//packedLight = 0xF000F0;
		//vertexConsumer.vertex(pose.pose(), -1.0F, -1.0F, 0.0F).color(255, 255, 255, 255).uv(0.0F, 1.0F).overlayCoords(0, 10).uv2(packedLight).normal(pose.normal(), 0.0F, 0.0F, 1.0F).endVertex();
		//vertexConsumer.vertex(pose.pose(), 1.0F, -1.0F, 0.0F).color(255, 255, 255, 255).uv(1.0F, 1.0F).overlayCoords(0, 10).uv2(packedLight).normal(pose.normal(), 0.0F, 0.0F, 1.0F).endVertex();
		//vertexConsumer.vertex(pose.pose(), 1.0F, 1.0F, 0.0F).color(255, 255, 255, 255).uv(1.0F, 0.0F).overlayCoords(0, 10).uv2(packedLight).normal(pose.normal(), 0.0F, 0.0F, 1.0F).endVertex();
		//vertexConsumer.vertex(pose.pose(), -1.0F, 1.0F, 0.0F).color(255, 255, 255, 255).uv(0.0F, 0.0F).overlayCoords(0, 10).uv2(packedLight).normal(pose.normal(), 0.0F, 0.0F, 1.0F).endVertex();
		//poseStack.popPose();
		//poseStack.pushPose();
		//poseStack.translate(0.0F, 0.5F, 0.0F);
		poseStack.popPose();
	}

	@Override
	public boolean deathReduce() {
		return true;
	}

	@Override
	public boolean isStatic() {
		return false;
	}

	@Override
	public Set<State> activeAt() {
		return Set.of(State.ALIVE, State.EXIT);
	}
}
