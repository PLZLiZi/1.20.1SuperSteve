package plz.lizi.supersteve.client.renderer;

import java.util.Set;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.client.model.data.ModelData;
import plz.lizi.supersteve.api.PLZBase;
import plz.lizi.supersteve.api.SSUtil;
import plz.lizi.supersteve.entity.SuperSteveEntityBase;
import plz.lizi.supersteve.entity.SuperSteveEntityBase.State;

public class SSEnterLayer extends SSLayer {
    private static final float BLOCKS_R = 2F;
    private static final Block[] BLOCKS = { Blocks.NETHERITE_BLOCK, Blocks.BEDROCK, Blocks.COMMAND_BLOCK, Blocks.STRUCTURE_BLOCK };
    private final SuperSteveRenderer parent;
    // private static final float HALF_SQRT_3 = (float) (Math.sqrt((double) 3.0F) / (double) 2.0F);

    public SSEnterLayer(SuperSteveRenderer pRenderer) {
        super(pRenderer);
        this.parent = pRenderer;
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
        float currentTick = (float) entity.stateTime() + partialTick;
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        float[] color = SSUtil.getRainbowColor(3);
        SSRenders.renderStrip(poseStack, bufferSource.getBuffer(SSRenders.POSITION_COLOR), 0.2F, 144, 32, color[0], color[1], color[2], 0.5F, packedLight);
        if (currentTick >= SuperSteveEntityBase.ENTER_ACTIVE[2]) {
            float startTick = SuperSteveEntityBase.ENTER_ACTIVE[2];
            float endTick = SuperSteveEntityBase.ENTER_ACTIVE[3];
            float duration = endTick - startTick;
            float rawProgress = duration > 0 ? (currentTick - startTick) / duration : 0F;
            float pgs = (float) Math.pow(PLZBase.progress(rawProgress), 0.4);
            poseStack.pushPose();
            poseStack.translate(0.0F, pgs * 1.8F, 0.0F);
            poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
            poseStack.scale(pgs * 3.0F, pgs * 3.0F, pgs * 3.0F);
            parent.solidWeapons.render(poseStack);
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
        int num = BLOCKS.length;
        float eachTime = (float) (SuperSteveEntityBase.ENTER_ACTIVE[3] - SuperSteveEntityBase.ENTER_ACTIVE[2]) / (float) num;
        for (int i = 0; i < num; i++) {
            float fallS = (float) SuperSteveEntityBase.ENTER_ACTIVE[2] + (i * eachTime);
            float fallPgs = (currentTick - fallS) / (fallS + eachTime - fallS);
            fallPgs = Mth.clamp(fallPgs, 0.0F, 1.0F);
            if (fallPgs > 0) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(i * (360.0F / num)));
                poseStack.translate(0.0F, Mth.lerp(1.0F - (float) Math.pow(1.0F - fallPgs, 3.0), 255.0F, 0.0F), Mth.lerp(Mth.clamp((currentTick - (float) SuperSteveEntityBase.ENTER_ACTIVE[4]) / ((float) SuperSteveEntityBase.ENTER_ACTIVE[5] - (float) SuperSteveEntityBase.ENTER_ACTIVE[4]), 0.0F, 1.0F), BLOCKS_R, 0.0F));
                poseStack.translate(-0.5F, -0.5F, -0.5F);
                blockRenderer.renderSingleBlock(BLOCKS[i].defaultBlockState(), poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
                poseStack.popPose();
            }
        }
        poseStack.popPose();
    }
    // 旧版末影龙出场
    // float $$15 = Math.min(enterpgs > 0.8F ? (enterpgs - 0.8F) / 0.2F : 0.0F, 1.0F);
    // RandomSource $$16 = RandomSource.create(entity.getUUID().getMostSignificantBits());
    // VertexConsumer $$17 = bufferSource.getBuffer(SSRenders.POSITION_COLOR_NC);
    // poseStack.scale(enterpgs * 0.5F, enterpgs * 0.5F, enterpgs * 0.5F);
    // for (int $$18 = 0; (float) $$18 < (enterpgs + enterpgs * enterpgs) / 2.0F * 60.0F; ++$$18) {
    // poseStack.mulPose(Axis.XP.rotationDegrees($$16.nextFloat() * 360.0F));
    // poseStack.mulPose(Axis.YP.rotationDegrees($$16.nextFloat() * 360.0F));
    // poseStack.mulPose(Axis.ZP.rotationDegrees($$16.nextFloat() * 360.0F));
    // poseStack.mulPose(Axis.XP.rotationDegrees($$16.nextFloat() * 360.0F));
    // poseStack.mulPose(Axis.YP.rotationDegrees($$16.nextFloat() * 360.0F));
    // poseStack.mulPose(Axis.ZP.rotationDegrees($$16.nextFloat() * 360.0F + enterpgs * 90.0F));
    // float $$19 = $$16.nextFloat() * 20.0F + 5.0F + $$15 * 10.0F;
    // float $$20 = $$16.nextFloat() * 2.0F + 1.0F + $$15 * 2.0F;
    // Matrix4f $$21 = poseStack.last().pose();
    // int $$22 = (int) (255.0F * (1.0F - $$15));
    // vertex01($$17, $$21, $$22);
    // vertex2($$17, $$21, $$19, $$20);
    // vertex3($$17, $$21, $$19, $$20);
    // vertex01($$17, $$21, $$22);
    // vertex3($$17, $$21, $$19, $$20);
    // vertex4($$17, $$21, $$19, $$20);
    // vertex01($$17, $$21, $$22);
    // vertex4($$17, $$21, $$19, $$20);
    // vertex2($$17, $$21, $$19, $$20);
    // }
    // private static void vertex01(VertexConsumer p_254498_, Matrix4f p_253891_, int p_254278_) {
    // p_254498_.vertex(p_253891_, 0.0F, 0.0F, 0.0F).color(255, 255, 255, p_254278_).endVertex();
    // }
    // private static void vertex2(VertexConsumer p_253956_, Matrix4f p_254053_, float p_253704_, float p_253701_) {
    // float[] color = SSUtil.getRainbowColor(3);
    // p_253956_.vertex(p_254053_, -HALF_SQRT_3 * p_253701_, p_253704_, -0.5F * p_253701_).color(color[0], color[1], color[2], 0).endVertex();
    // }
    // private static void vertex3(VertexConsumer p_253850_, Matrix4f p_254379_, float p_253729_, float p_254030_) {
    // float[] color = SSUtil.getRainbowColor(3);
    // p_253850_.vertex(p_254379_, HALF_SQRT_3 * p_254030_, p_253729_, -0.5F * p_254030_).color(color[0], color[1], color[2], 0).endVertex();
    // }
    // private static void vertex4(VertexConsumer p_254184_, Matrix4f p_254082_, float p_253649_, float p_253694_) {
    // float[] color = SSUtil.getRainbowColor(3);
    // p_254184_.vertex(p_254082_, 0.0F, p_253649_, 1.0F * p_253694_).color(color[0], color[1], color[2], 0).endVertex();
    // }
}
