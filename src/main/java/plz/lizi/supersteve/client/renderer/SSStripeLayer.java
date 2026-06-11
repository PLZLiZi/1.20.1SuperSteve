package plz.lizi.supersteve.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.joml.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import plz.lizi.supersteve.api.SSUtil;
import plz.lizi.supersteve.client.renderer.model.SuperSteveModel;
import plz.lizi.supersteve.entity.SuperSteveEntityBase;
import plz.lizi.supersteve.entity.SuperSteveEntityBase.State;

public class SSStripeLayer extends SSLayer {
	private static final Map<Integer, Vector3f> SPHERE_SPEEDS = new HashMap<>();
	private static final float COUNT = 5;
	private static final float BASE_HEIGHT = 0.25F;
	private static final int SEGMENTS = 32;

	public SSStripeLayer(RenderLayerParent<SuperSteveEntityBase, SuperSteveModel> parent) {
		super(parent);
		for (int i = 0; i < COUNT; i++) {
			SPHERE_SPEEDS.put(i, new Vector3f(SSUtil.randfloat(-3F, 3F), SSUtil.randfloat(-3F, 3F), SSUtil.randfloat(-3F, 3F)));
		}
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, SuperSteveEntityBase entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
		poseStack.pushPose();
		float ptick = entity.ssGetTick() + partialTick;
		float halfH = BASE_HEIGHT / 2.0F;
		float r = 1.0F, g = 1.0F, b = 1.0F, a = 0.3F;
		var color = SSUtil.getRainbowColor(3);
		r = color[0];
		g = color[1];
		b = color[2];
		VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentEmissive(SSUtil.WHITE_TEXTURE, false));
		for (int i = 0; i < COUNT; i++) {
			poseStack.pushPose();
			poseStack.mulPose(Axis.XP.rotationDegrees(ptick * SPHERE_SPEEDS.get(i).x));
			poseStack.mulPose(Axis.YP.rotationDegrees(ptick * SPHERE_SPEEDS.get(i).y));
			poseStack.mulPose(Axis.ZP.rotationDegrees(ptick * SPHERE_SPEEDS.get(i).z));
			SSRenders.renderStrip(poseStack, vc, entity.ssGetAttR(true) + (i * 0.3F), halfH + (i * 0.04F), SEGMENTS, r, g, b, a, packedLight);
			poseStack.popPose();
		}
		poseStack.popPose();
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
