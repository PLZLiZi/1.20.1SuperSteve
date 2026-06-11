package plz.lizi.supersteve.client.renderer;

import java.util.Set;
import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.util.RandomSource;
import plz.lizi.supersteve.api.SSUtil;
import plz.lizi.supersteve.client.renderer.model.SuperSteveModel;
import plz.lizi.supersteve.entity.SuperSteveEntityBase;
import plz.lizi.supersteve.entity.SuperSteveEntityBase.State;

public class SSEnterLayer extends SSLayer {
	private static final float HALF_SQRT_3 = (float) (Math.sqrt((double) 3.0F) / (double) 2.0F);

    public SSEnterLayer(RenderLayerParent<SuperSteveEntityBase, SuperSteveModel> pRenderer) {
        super(pRenderer);
    }

    @Override
    public boolean deathReduce() {
        return false;
    }

    @Override
    public boolean isStatic() {
        return true;
    }

    @Override
    public Set<State> activeAt() {
        return Set.of(State.ENTER);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, SuperSteveEntityBase entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        float stateTime = entity.stateTime();
        float enterpgs = (stateTime + partialTick) / SuperSteveEntityBase.ENTER_ACTIVE[0];
        float $$15 = Math.min(enterpgs > 0.8F ? (enterpgs - 0.8F) / 0.2F : 0.0F, 1.0F);
        RandomSource $$16 = RandomSource.create(entity.getUUID().getMostSignificantBits());
        VertexConsumer $$17 = bufferSource.getBuffer(SSRenders.POSITION_COLOR_NC);
        poseStack.pushPose();
        poseStack.scale(enterpgs * 0.5F, enterpgs * 0.5F, enterpgs * 0.5F);
        for (int $$18 = 0; (float) $$18 < (enterpgs + enterpgs * enterpgs) / 2.0F * 60.0F; ++$$18) {
            poseStack.mulPose(Axis.XP.rotationDegrees($$16.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees($$16.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees($$16.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees($$16.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees($$16.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees($$16.nextFloat() * 360.0F + enterpgs * 90.0F));
            float $$19 = $$16.nextFloat() * 20.0F + 5.0F + $$15 * 10.0F;
            float $$20 = $$16.nextFloat() * 2.0F + 1.0F + $$15 * 2.0F;
            Matrix4f $$21 = poseStack.last().pose();
            int $$22 = (int) (255.0F * (1.0F - $$15));
            vertex01($$17, $$21, $$22);
            vertex2($$17, $$21, $$19, $$20);
            vertex3($$17, $$21, $$19, $$20);
            vertex01($$17, $$21, $$22);
            vertex3($$17, $$21, $$19, $$20);
            vertex4($$17, $$21, $$19, $$20);
            vertex01($$17, $$21, $$22);
            vertex4($$17, $$21, $$19, $$20);
            vertex2($$17, $$21, $$19, $$20);
        }
        poseStack.popPose();
    }

    private static void vertex01(VertexConsumer p_254498_, Matrix4f p_253891_, int p_254278_) {
        p_254498_.vertex(p_253891_, 0.0F, 0.0F, 0.0F).color(255, 255, 255, p_254278_).endVertex();
    }

    private static void vertex2(VertexConsumer p_253956_, Matrix4f p_254053_, float p_253704_, float p_253701_) {
        float[] color = SSUtil.getRainbowColor(3);
        p_253956_.vertex(p_254053_, -HALF_SQRT_3 * p_253701_, p_253704_, -0.5F * p_253701_).color(color[0], color[1], color[2], 0).endVertex();
    }

    private static void vertex3(VertexConsumer p_253850_, Matrix4f p_254379_, float p_253729_, float p_254030_) {
        float[] color = SSUtil.getRainbowColor(3);
        p_253850_.vertex(p_254379_, HALF_SQRT_3 * p_254030_, p_253729_, -0.5F * p_254030_).color(color[0], color[1], color[2], 0).endVertex();
    }

    private static void vertex4(VertexConsumer p_254184_, Matrix4f p_254082_, float p_253649_, float p_253694_) {
        float[] color = SSUtil.getRainbowColor(3);
        p_254184_.vertex(p_254082_, 0.0F, p_253649_, 1.0F * p_253694_).color(color[0], color[1], color[2], 0).endVertex();
    }
}
