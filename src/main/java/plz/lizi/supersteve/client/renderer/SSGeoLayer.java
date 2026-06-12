package plz.lizi.supersteve.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import org.joml.Vector3f;
import plz.lizi.supersteve.api.SSUtil;
import plz.lizi.supersteve.client.renderer.model.SuperSteveModel;
import plz.lizi.supersteve.entity.SuperSteveEntityBase;
import plz.lizi.supersteve.entity.SuperSteveEntityBase.State;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SSGeoLayer extends SSLayer {
	private final Vector3f ROAT = new Vector3f(SSUtil.randfloat(-5F, 5F), SSUtil.randfloat(-5F, 5F), SSUtil.randfloat(-5F, 5F));
	private static final List<Vector3f> VERTICES = new ArrayList<>();
	private static final List<int[]> EDGES = new ArrayList<>();

	public SSGeoLayer(RenderLayerParent<SuperSteveEntityBase, SuperSteveModel> parent) {
		super(parent);
		if (VERTICES.isEmpty()) {
			initFootballGeometry();
		}
	}

	private static void initFootballGeometry() {
		float phi = (1.0F + (float) Math.sqrt(5.0F)) / 2.0F;
		addPermutations(0, 1, 3 * phi);
		addPermutations(2, 1 + 2 * phi, phi);
		addPermutations(1, 2 + phi, 2 * phi);
		float edgeDistSq = (2.0F * 2.0F) * 1.05f;
		for (int i = 0; i < VERTICES.size(); i++) {
			for (int j = i + 1; j < VERTICES.size(); j++) {
				if (VERTICES.get(i).distanceSquared(VERTICES.get(j)) < edgeDistSq) {
					EDGES.add(new int[] { i, j });
				}
			}
		}
	}

	private static void addPermutations(float x, float y, float z) {
		for (int i = 0; i < 8; i++) {
			float sx = ((i & 1) == 0 ? 1 : -1) * x;
			float sy = ((i & 2) == 0 ? 1 : -1) * y;
			float sz = ((i & 4) == 0 ? 1 : -1) * z;
			addUniqueVertex(new Vector3f(sx, sy, sz));
			addUniqueVertex(new Vector3f(sy, sz, sx));
			addUniqueVertex(new Vector3f(sz, sx, sy));
		}
	}

	private static void addUniqueVertex(Vector3f v) {
		for (Vector3f existing : VERTICES) {
			if (existing.distanceSquared(v) < 0.01f)
				return;
		}
		VERTICES.add(v);
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, SuperSteveEntityBase entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
		poseStack.pushPose();
		VertexConsumer vc = buffer.getBuffer(SSRenders.CUSTOM_LINE.apply(5D));
		float[] rgb = SSUtil.getRainbowColor(3);
		float time = (float) entity.ssGetTick() + partialTick;
		poseStack.mulPose(Axis.XP.rotationDegrees(time * ROAT.x));
		poseStack.mulPose(Axis.YP.rotationDegrees(time * ROAT.y));
		poseStack.mulPose(Axis.ZP.rotationDegrees(time * ROAT.z));
		float radius = (float) entity.ssGetAttR(true);
		PoseStack.Pose lastPose = poseStack.last();
		line(vc, lastPose, -radius, -radius, -radius, radius, -radius, -radius, rgb[0], rgb[1], rgb[2], 1.0F);
		line(vc, lastPose, radius, -radius, -radius, radius, -radius, radius, rgb[0], rgb[1], rgb[2], 1.0F);
		line(vc, lastPose, radius, -radius, radius, -radius, -radius, radius, rgb[0], rgb[1], rgb[2], 1.0F);
		line(vc, lastPose, -radius, -radius, radius, -radius, -radius, -radius, rgb[0], rgb[1], rgb[2], 1.0F);
		line(vc, lastPose, -radius, radius, -radius, radius, radius, -radius, rgb[0], rgb[1], rgb[2], 1.0F);
		line(vc, lastPose, radius, radius, -radius, radius, radius, radius, rgb[0], rgb[1], rgb[2], 1.0F);
		line(vc, lastPose, radius, radius, radius, -radius, radius, radius, rgb[0], rgb[1], rgb[2], 1.0F);
		line(vc, lastPose, -radius, radius, radius, -radius, radius, -radius, rgb[0], rgb[1], rgb[2], 1.0F);
		line(vc, lastPose, -radius, -radius, -radius, -radius, radius, -radius, rgb[0], rgb[1], rgb[2], 1.0F);
		line(vc, lastPose, radius, -radius, -radius, radius, radius, -radius, rgb[0], rgb[1], rgb[2], 1.0F);
		line(vc, lastPose, radius, -radius, radius, radius, radius, radius, rgb[0], rgb[1], rgb[2], 1.0F);
		line(vc, lastPose, -radius, -radius, radius, -radius, radius, radius, rgb[0], rgb[1], rgb[2], 1.0F);
		poseStack.popPose();
	}

	public static void renderFootball(PoseStack poseStack, VertexConsumer vc, float scale, float r, float g, float b, float a) {
		PoseStack.Pose pose = poseStack.last();
		for (int[] edge : EDGES) {
			Vector3f v1 = VERTICES.get(edge[0]);
			Vector3f v2 = VERTICES.get(edge[1]);
			line(vc, pose, v1.x * scale, v1.y * scale, v1.z * scale, v2.x * scale, v2.y * scale, v2.z * scale, r, g, b, a);
		}
	}

	private static void line(VertexConsumer vc, PoseStack.Pose pose, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
		vc.vertex(pose.pose(), x1, y1, z1).color(r, g, b, a).normal(pose.normal(), 0, 1, 0).endVertex();
		vc.vertex(pose.pose(), x2, y2, z2).color(r, g, b, a).normal(pose.normal(), 0, 1, 0).endVertex();
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
