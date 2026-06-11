package plz.lizi.supersteve.client.renderer;

import java.util.Set;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import plz.lizi.supersteve.client.renderer.model.SuperSteveModel;
import plz.lizi.supersteve.entity.SuperSteveEntityBase;
import plz.lizi.supersteve.entity.SuperSteveEntityBase.State;

public class SSWeaponLayer extends SSLayer {
	private static final float TIME = 100;
	private static final int COUNT = 30;

	public SSWeaponLayer(RenderLayerParent<SuperSteveEntityBase, SuperSteveModel> parent) {
		super(parent);
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, SuperSteveEntityBase entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
		float totalTime = (float) entity.ssGetTick() + partialTick;
		float timeFactor = (float) (totalTime * 2 * Math.PI / TIME);
		poseStack.pushPose();
		poseStack.scale(1, -1, -1);
		poseStack.translate(0, -0.3, 0);
		int interval = 60;
		float pgs = (totalTime % interval) / (float) interval;
		for (int i = 0; i < COUNT; i++) {
			poseStack.pushPose();
			float angle = timeFactor + (float) (i * 2 * Math.PI / COUNT);
			float sin = (float) Math.sin(pgs * Math.PI);
			float pgs2 = (float) Mth.lerp(sin, /* 0.4 */entity.ssGetAttR(true) * 0.8F, entity.ssGetAttR(true));
			float x = (float) (pgs2 * Math.cos(angle));
			float y = (float) (0.2F * Math.sin(totalTime * Math.PI * 0.05F));
			float z = (float) (pgs2 * Math.sin(angle));
			poseStack.translate(x, y, z);
			poseStack.mulPose(Axis.YP.rotation(-angle));
			poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
			poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
			Minecraft.getInstance().getItemRenderer().renderStatic(entity.getItemInHand(InteractionHand.MAIN_HAND), ItemDisplayContext.GROUND, LightTexture.pack(15, 15), OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), 0);
			poseStack.popPose();
		}
		poseStack.popPose();
	}

	public static void renderLineCube(PoseStack poseStack, VertexConsumer vc, float h, float r, float g, float b, float a) {
		PoseStack.Pose pose = poseStack.last();
		float x1 = -h, x2 = h, y1 = -h, y2 = h, z1 = -h, z2 = h;
		line(vc, pose, x1, y1, z1, x2, y1, z1, r, g, b, a);
		line(vc, pose, x2, y1, z1, x2, y2, z1, r, g, b, a);
		line(vc, pose, x2, y2, z1, x1, y2, z1, r, g, b, a);
		line(vc, pose, x1, y2, z1, x1, y1, z1, r, g, b, a);
		line(vc, pose, x1, y1, z2, x2, y1, z2, r, g, b, a);
		line(vc, pose, x2, y1, z2, x2, y2, z2, r, g, b, a);
		line(vc, pose, x2, y2, z2, x1, y2, z2, r, g, b, a);
		line(vc, pose, x1, y2, z2, x1, y1, z2, r, g, b, a);
		line(vc, pose, x1, y1, z1, x1, y1, z2, r, g, b, a);
		line(vc, pose, x2, y1, z1, x2, y1, z2, r, g, b, a);
		line(vc, pose, x2, y2, z1, x2, y2, z2, r, g, b, a);
		line(vc, pose, x1, y2, z1, x1, y2, z2, r, g, b, a);
	}

	private static void line(VertexConsumer vc, PoseStack.Pose pose, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
		vc.vertex(pose.pose(), x1, y1, z1).overlayCoords(0, 10).color(r, g, b, a).normal(pose.normal(), 0, 1, 0).endVertex();
		vc.vertex(pose.pose(), x2, y2, z2).overlayCoords(0, 10).color(r, g, b, a).normal(pose.normal(), 0, 1, 0).endVertex();
	}

	@Override
	public boolean deathReduce() {
		return true;
	}

	@Override
	public boolean isStatic() {
		return true;
	}

	@Override
	public Set<State> activeAt() {
		return Set.of(State.ALIVE, State.EXIT);
	}
}
