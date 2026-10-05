package plz.lizi.supersteve.client.renderer;

import java.util.Set;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Style;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.client.model.data.ModelData;
import plz.lizi.supersteve.api.PLZBase;
import plz.lizi.supersteve.api.SSUtil;
import plz.lizi.supersteve.entity.SuperSteveEntityBase;
import plz.lizi.supersteve.entity.SuperSteveEntityBase.State;

public class SSEnterLayer extends SSLayer {
    private static final float BLOCKS_RADIUS = 3F;
    private static final Block[] BLOCKS = { Blocks.NETHERITE_BLOCK, Blocks.BEDROCK, Blocks.COMMAND_BLOCK, Blocks.STRUCTURE_BLOCK };
    private static final int CODE_ENTRY_COUNT = 200;
    private static final float[][] OUTLINE_DIRS = { { 0.3f, 0 }, { -0.3f, 0 }, { 0, 0.3f }, { 0, -0.3f } };
    private static final float CODE_CENTER_Y = 0.0F;
    private static final float CODE_END = SuperSteveEntityBase.ENTER_ACTIVE[5];
    private static final float START_RADIUS_MIN = 14.0F;
    private static final float START_RADIUS_MAX = 40.0F;
    private static final float ARRIVE_JITTER = 22.0F;
    private static final float CODE_SPHERE_RADIUS = 2.5F;
    private static final float CODE_SPHERE_SHRINK_RADIUS = 0.8F;
    private static final float MIN_CONVERGE_TIME = 12.0F;
    private final float[] circleRotSpeed;
    private final SuperSteveRenderer parent;

    public SSEnterLayer(SuperSteveRenderer pRenderer) {
        super(pRenderer);
        this.parent = pRenderer;
        this.circleRotSpeed = new float[] { SSUtil.randfloat(0, 10), SSUtil.randfloat(0, 10), SSUtil.randfloat(0, 10) };
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
        if (currentTick >= SuperSteveEntityBase.ENTER_ACTIVE[2]) {
            float st = SuperSteveEntityBase.ENTER_ACTIVE[2];
            float wpg = (float) Math.pow(PLZBase.progress((currentTick - st) / ((float) SuperSteveEntityBase.ENTER_ACTIVE[3] - st)), 2);
            poseStack.pushPose();
            poseStack.translate(0.0F, wpg * 1.6F, 0.0F);
            poseStack.mulPose(new Quaternionf().rotateXYZ((circleRotSpeed[0] * wpg * currentTick) % 360F * Mth.DEG_TO_RAD, (circleRotSpeed[1] * wpg * currentTick) % 360F * Mth.DEG_TO_RAD, (circleRotSpeed[2] * wpg * currentTick) % 360F * Mth.DEG_TO_RAD));
            poseStack.scale(wpg * 3.0F, wpg * 3.0F, wpg * 3.0F);
            parent.solidWeapons.render(poseStack);
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
        int blockCount = BLOCKS.length;
        float delayPerBlock = (float) (SuperSteveEntityBase.ENTER_ACTIVE[3] - SuperSteveEntityBase.ENTER_ACTIVE[2]) / (float) blockCount;
        for (int i = 0; i < blockCount; i++) {
            float bpg = (currentTick - ((float) SuperSteveEntityBase.ENTER_ACTIVE[2] + (i * delayPerBlock))) / delayPerBlock;
            bpg = Mth.clamp(bpg, 0.0F, 1.0F);
            if (bpg > 0) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(i * (360.0F / blockCount)));
                if (currentTick >= SuperSteveEntityBase.ENTER_ACTIVE[5])
                    poseStack.translate(SSUtil.randfloat(-0.1F, 0.1F), SSUtil.randfloat(-0.1F, 0.1F), SSUtil.randfloat(-0.1F, 0.1F));
                poseStack.translate(0.0F, 0.0F, Mth.lerp(Mth.clamp((currentTick - (float) SuperSteveEntityBase.ENTER_ACTIVE[4]) / ((float) SuperSteveEntityBase.ENTER_ACTIVE[5] - (float) SuperSteveEntityBase.ENTER_ACTIVE[4]), 0.0F, 1.0F), BLOCKS_RADIUS, 0.0F));
                float scale = 1.0F - (float) Math.pow(1.0F - bpg, 3.0);
                poseStack.scale(scale, scale, scale);
                poseStack.translate(-0.5F, -0.5F, -0.5F);
                blockRenderer.renderSingleBlock(BLOCKS[i].defaultBlockState(), poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
                poseStack.popPose();
            }
        }
        poseStack.popPose();
        if (currentTick < CODE_END)
            this.renderCodeStream(poseStack, bufferSource, entity, currentTick);
    }

    public void renderCodeStream(PoseStack poseStack, MultiBufferSource bufferSource, SuperSteveEntityBase entity, float currentTick) {
        // 由AI制作: GLM-5.3Flash & DeepSeek4.1Flash, 有点难了说是，懒得动脑子
        float global = 1.0F - Mth.clamp((currentTick - (CODE_END - 10F)) / 10.0F, 0.0F, 1.0F);
        if (global <= 0.0F)
            return;
        long seedBase = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
        Font font = Minecraft.getInstance().font;
        FontSet fontSet = font.getFontSet(Style.DEFAULT_FONT);
        SSRenders.ENV_GLYPH_BIND = SSEnvLayer.pageOf(fontSet, fontSet.getGlyph('A'));
        VertexConsumer gvc = bufferSource.getBuffer(SSRenders.ENV_GLYPH);
        float phase1End = SuperSteveEntityBase.ENTER_ACTIVE[4];
        float phase2End = SuperSteveEntityBase.ENTER_ACTIVE[5];
        float arriveMax = Math.max(1.0F, phase2End - MIN_CONVERGE_TIME);
        for (int i = 0; i < CODE_ENTRY_COUNT; i++) {
            RandomSource rs = RandomSource.create(seedBase ^ (i * 0x9E3779B9L));
            float radius0 = Mth.lerp(rs.nextFloat(), START_RADIUS_MIN, START_RADIUS_MAX);
            float yaw0 = i * (360.0F / CODE_ENTRY_COUNT) + (rs.nextFloat() - 0.5F) * 30.0F;
            float pitch = Mth.lerp(rs.nextFloat(), -50.0F, 60.0F);
            int len = (int) Mth.lerp(rs.nextFloat(), 6.0F, 14.0F);
            float scale = Mth.lerp(rs.nextFloat(), 0.048F, 0.065F);
            float hue0 = rs.nextFloat();
            rs.nextFloat();
            rs.nextFloat();
            rs.nextFloat();
            rs.nextFloat();
            rs.nextBoolean();
            float selfSpinSpeed = 8.0F + (i % 8) * 1.5F;
            float arriveOffset = (rs.nextFloat() - 0.5F) * 2.0F * ARRIVE_JITTER;
            float arriveTime = Mth.clamp(phase1End + arriveOffset, 1.0F, arriveMax);
            double radPitch = Math.toRadians(pitch);
            double radYaw0 = Math.toRadians(yaw0);
            float cosP = (float) Math.cos(radPitch);
            float sinP = (float) Math.sin(radPitch);
            float startX = cosP * (float) Math.sin(radYaw0) * radius0;
            float startY = CODE_CENTER_Y + sinP * radius0;
            float startZ = cosP * (float) Math.cos(radYaw0) * radius0;
            float dx = -startX;
            float dy = -startY;
            float dz = -startZ;
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist < 0.001F)
                continue;
            dx /= dist;
            dy /= dist;
            dz /= dist;
            float lineEnd = CODE_SPHERE_RADIUS - (len - 1) / 2.0F * 0.15F;
            float p1 = Mth.clamp(currentTick / arriveTime, 0.0F, 1.0F);
            p1 = p1 * p1 * (3.0F - 2.0F * p1);
            float lineRadius = Mth.lerp(p1, radius0, lineEnd);
            float mergeP = Mth.clamp((currentTick - (float) SuperSteveEntityBase.ENTER_ACTIVE[4]) / ((float) SuperSteveEntityBase.ENTER_ACTIVE[5] - (float) SuperSteveEntityBase.ENTER_ACTIVE[4]), 0.0F, 1.0F);
            float sphereR = Mth.lerp(mergeP, CODE_SPHERE_RADIUS, CODE_SPHERE_SHRINK_RADIUS);
            float alpha = Mth.clamp(currentTick / 5.0F, 0.0F, 1.0F) * global;
            if (alpha < 0.02F)
                continue;
            String text = randomAscii(seedBase, i, (int) currentTick, len);
            float[] arcOff = new float[len];
            float arcTotal = 0.0F;
            for (int c = 0; c < len; c++) {
                float fracC = (float) c / (float) (len - 1);
                float csC = Mth.lerp(fracC * fracC, 1.0F, 0.4F);
                arcOff[c] = arcTotal;
                arcTotal += (font.width(text.substring(c, c + 1)) + 1) * csC * scale;
            }
            for (int c = 0; c < len; c++) {
                float frac = (float) c / (float) (len - 1);
                float cs = Mth.lerp(frac * frac, 1.0F, 0.4F);
                float charOffset = (c - (len - 1) / 2.0F) * 0.15F;
                float pLand = Mth.clamp((radius0 + charOffset - CODE_SPHERE_RADIUS) / (radius0 - lineEnd), 0.0F, 1.0F);
                float tLand = arriveTime * (0.5F - (float) Math.sin(Math.asin(1.0F - 2.0F * pLand) / 3.0F));
                float px, py, pz;
                Quaternionf orient;
                if (currentTick < tLand) {
                    float r = lineRadius + charOffset;
                    px = dx * r;
                    py = dy * r + CODE_CENTER_Y;
                    pz = dz * r;
                    orient = new Quaternionf().rotationY((float) Math.toRadians(currentTick * selfSpinSpeed + c * 15.0F));
                } else {
                    if (sphereR < 0.15F)
                        continue;
                    float u = currentTick - tLand;
                    float glide = 1.0F - (float) Math.exp(-u * 0.25F);
                    float latR = Math.max(0.05F, cosP * sphereR);
                    float thetaC = yaw0 + (float) Math.toDegrees((arcOff[c] - arcTotal * 0.5F) / latR) * glide + u * (2.0F + 3.0F * mergeP);
                    double radYawC = Math.toRadians(thetaC);
                    px = cosP * (float) Math.sin(radYawC) * sphereR;
                    py = CODE_CENTER_Y + sinP * sphereR;
                    pz = cosP * (float) Math.cos(radYawC) * sphereR;
                    orient = new Quaternionf().rotationY((float) Math.toRadians(thetaC)).rotateZ((float) Math.toRadians(-pitch));
                }
                poseStack.pushPose();
                poseStack.translate(px, py, pz);
                poseStack.mulPose(orient);
                poseStack.scale(scale * cs, scale * cs, scale);
                Matrix4f matrix = poseStack.last().pose();
                float hue = (hue0 + c * 0.045F + currentTick * 0.004F) % 1.0F;
                float[] rgb = hsvColor(hue * 360.0F);
                BakedGlyph glyph = fontSet.getGlyph(text.charAt(c));
                float gx = -(glyph.left + glyph.right) / 2.0F;
                float gy = 3.0F - (glyph.up + glyph.down) / 2.0F;
                glyph.render(false, gx, gy, matrix, gvc, rgb[0], rgb[1], rgb[2], alpha, 0xF000F0);
                for (float[] d : OUTLINE_DIRS)
                    glyph.render(false, gx + d[0], gy + d[1], matrix, gvc, 1.0F, 1.0F, 1.0F, alpha, 0xF000F0);
                glyph.render(false, gx, gy, matrix, gvc, rgb[0], rgb[1], rgb[2], alpha, 0xF000F0);
                poseStack.popPose();
            }
        }
    }

    private static String randomAscii(long seedBase, int i, int tick, int len) {
        RandomSource rs = RandomSource.create(seedBase ^ (i * 7919L) + tick / 2);
        StringBuilder sb = new StringBuilder(len);
        for (int c = 0; c < len; c++)
            sb.append((char) (0x21 + rs.nextInt(0x5E)));
        return sb.toString();
    }

    private static float[] hsvColor(float hue) {
        float x = 1.0F - Math.abs((hue / 60.0F) % 2.0F - 1.0F);
        float r, g, b;
        if (hue < 60) {
            r = 1;
            g = x;
            b = 0;
        } else if (hue < 120) {
            r = x;
            g = 1;
            b = 0;
        } else if (hue < 180) {
            r = 0;
            g = 1;
            b = x;
        } else if (hue < 240) {
            r = 0;
            g = x;
            b = 1;
        } else if (hue < 300) {
            r = x;
            g = 0;
            b = 1;
        } else {
            r = 1;
            g = 0;
            b = x;
        }
        return new float[] { r, g, b };
    }
}
