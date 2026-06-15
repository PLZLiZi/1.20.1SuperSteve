package plz.lizi.supersteve.client.renderer;

import java.util.Set;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import plz.lizi.supersteve.entity.SuperSteveEntityBase;
import plz.lizi.supersteve.entity.SuperSteveEntityBase.State;

public class SSBackLayer extends SSLayer {
	public static final float ROAT_SPEED = 5;
	public static final float FLOAT = 0.3F;
	public static final float FLOAT_SPEED = 40;
	private final SuperSteveRenderer parent;

	public SSBackLayer(SuperSteveRenderer parent) {
		super(parent);
		this.parent = parent;
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
		poseStack.scale(3F, 3F, 3F);
		parent.solidWeapons.render(poseStack);
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
