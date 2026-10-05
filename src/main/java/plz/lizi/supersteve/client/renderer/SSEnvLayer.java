package plz.lizi.supersteve.client.renderer;

import java.util.Set;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.FontTexture;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;
import plz.lizi.supersteve.entity.SuperSteveEntityBase;
import plz.lizi.supersteve.entity.SuperSteveEntityBase.State;

public class SSEnvLayer extends SSLayer {
    public static final float RADIUS = 128.0F;
    public static final float ALPHA = 1F;
    public static final float RED = 1F;
    public static final float GREEN = 1F;
    public static final float BLUE = 1F;
    public static final int FLOAT_COUNT = 50;
    public static final float FLOAT_MIN_DIST = 3.0F;
    public static final float FLOAT_MAX_DIST = 10.0F;
    public static final float FLOAT_BAND = 18.0F;
    public static final float FLOAT_BOTTOM = -5.0F;
    public static final float FLOAT_RISE_SPEED = 0.15F;
    public static final float FLOAT_SCALE_MIN = 0.35F;
    public static final float FLOAT_SCALE_MAX = 0.7F;

    public SSEnvLayer(SuperSteveRenderer pRenderer) {
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
        return Set.of(State.ENTER, State.ALIVE, State.EXIT);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, SuperSteveEntityBase entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cam = camera.getPosition();
        Vec3 pos = entity.getPosition(partialTick);
        State st = entity.getState();
        float currentTick = (float) entity.stateTime() + partialTick;
        float maskA = ALPHA;
        if (st == State.ENTER) {
            maskA = ALPHA * Mth.clamp((currentTick - (float) SuperSteveEntityBase.ENTER_ACTIVE[6]) / ((float) SuperSteveEntityBase.ENTER_ACTIVE[7] - (float) SuperSteveEntityBase.ENTER_ACTIVE[6]), 0.0F, 1.0F);
        } else if (st == State.EXIT) {
            maskA = ALPHA * (1.0F - Mth.clamp((currentTick - (float) SuperSteveEntityBase.DEATH_ACTIVE[6]) / ((float) SuperSteveEntityBase.DEATH_ACTIVE[7] - (float) SuperSteveEntityBase.DEATH_ACTIVE[6]), 0.0F, 1.0F));
        }
        poseStack.pushPose();
        poseStack.translate((float) (cam.x - pos.x), (float) (cam.y - pos.y - entity.getBbHeight() / 3f), (float) (cam.z - pos.z));
        if (maskA > 0.01F) {
            Matrix4f matrix = poseStack.last().pose();
            VertexConsumer vc = bufferSource.getBuffer(SSRenders.ENV_MASK);
            int seg = 24;
            int ring = 8;
            float maxPitch = (float) (Math.PI / 2.0);
            for (int la = 0; la < ring; la++) {
                float p0 = maxPitch * la / ring;
                float p1 = maxPitch * (la + 1) / ring;
                boolean topRing = (la == ring - 1);
                for (int lo = 0; lo < seg; lo++) {
                    float a0 = (float) (Math.PI * 2.0 * lo / seg);
                    float a1 = (float) (Math.PI * 2.0 * (lo + 1) / seg);
                    if (topRing) {
                        triangleToPole(vc, matrix, p0, a0, a1, maskA);
                    } else {
                        quad(vc, matrix, p0, p1, a0, a1, maskA);
                    }
                }
            }
        }
        if (st == State.ALIVE) {
            lastAliveClock = (float) entity.stateTime() + partialTick;
            renderFloatBlocks(poseStack, bufferSource, entity, lastAliveClock, 0.0F, partialTick);
        } else if (st == State.EXIT) {
            renderFloatBlocks(poseStack, bufferSource, entity, lastAliveClock + currentTick, currentTick, partialTick);
        }
        poseStack.popPose();
    }

    private static float lastAliveClock;

    private static void quad(VertexConsumer vc, Matrix4f m, float p0, float p1, float a0, float a1, float alpha) {
        vertex(vc, m, p0, a0, alpha);
        vertex(vc, m, p0, a1, alpha);
        vertex(vc, m, p1, a1, alpha);
        vertex(vc, m, p1, a0, alpha);
    }

    private static void triangleToPole(VertexConsumer vc, Matrix4f m, float pitch, float a0, float a1, float alpha) {
        vertex(vc, m, pitch, a0, alpha);
        vertex(vc, m, pitch, a1, alpha);
        pole(vc, m, alpha);
        pole(vc, m, alpha);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, float pitch, float yaw, float alpha) {
        float cp = Mth.cos(pitch);
        float sp = Mth.sin(pitch);
        vc.vertex(m, cp * Mth.sin(yaw) * RADIUS, sp * RADIUS, cp * Mth.cos(yaw) * RADIUS).color(RED, GREEN, BLUE, alpha).endVertex();
    }

    private static void pole(VertexConsumer vc, Matrix4f m, float alpha) {
        vc.vertex(m, 0.0F, RADIUS, 0.0F).color(RED, GREEN, BLUE, alpha).endVertex();
    }

    public static FontTexture pageOf(FontSet fontSet, BakedGlyph glyph) {
        for (FontTexture ft : fontSet.textures)
            if (ft.renderTypes == glyph.renderTypes)
                return ft;
        return fontSet.textures.isEmpty() ? null : fontSet.textures.get(0);
    }

    private static void renderFloatBlocks(PoseStack poseStack, MultiBufferSource bufferSource, SuperSteveEntityBase entity, float aliveClock, float exitClock, float partialTick) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        long seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
        float tk = entity.tickCount + partialTick;
        float exitShrink = 1.0F - Mth.clamp(exitClock / (float) SuperSteveEntityBase.DEATH_ACTIVE[8], 0.0F, 1.0F);
        if (exitShrink <= 0.0F)
            return;
        @SuppressWarnings("deprecation")
        var blocks = BuiltInRegistries.BLOCK;
        int blockN = blocks.size();
        for (int i = 0; i < FLOAT_COUNT; i++) {
            RandomSource rs = RandomSource.create(seed ^ (i * 0x9E3779B9L));
            float agl = rs.nextFloat() * Mth.TWO_PI;
            float dist = FLOAT_MIN_DIST + (FLOAT_MAX_DIST - FLOAT_MIN_DIST) * (float) Math.sqrt(rs.nextFloat());
            float ox = Mth.sin(agl) * dist;
            float oz = Mth.cos(agl) * dist;
            float vMul = Mth.lerp(rs.nextFloat(), 0.7F, 1.3F);
            float entryDelay = rs.nextFloat() * (float) SuperSteveEntityBase.ALIVE_ACTIVE[2];
            float scale = Mth.lerp(rs.nextFloat(), FLOAT_SCALE_MIN, FLOAT_SCALE_MAX);
            float swAm = Mth.lerp(rs.nextFloat(), 0.1F, 0.5F);
            float swF = Mth.lerp(rs.nextFloat(), 0.05F, 0.15F);
            float swP = rs.nextFloat() * Mth.TWO_PI;
            float rSX = (rs.nextFloat() - 0.5F) * 3.0F;
            float rSY = (rs.nextFloat() - 0.5F) * 3.0F;
            float rSZ = (rs.nextFloat() - 0.5F) * 3.0F;
            float rPX = rs.nextFloat() * 360.0F;
            float rPY = rs.nextFloat() * 360.0F;
            float rPZ = rs.nextFloat() * 360.0F;
            Block block = blocks.byId(1 + rs.nextInt(Math.max(1, blockN - 1)));
            float te = aliveClock - entryDelay;
            if (te <= 0.0F)
                continue;
            float y = FLOAT_BOTTOM + (te * FLOAT_RISE_SPEED * vMul) % FLOAT_BAND;
            float edge = Mth.clamp((y - FLOAT_BOTTOM) / 1.5F, 0.0F, 1.0F) * Mth.clamp((FLOAT_BOTTOM + FLOAT_BAND - y) / 1.5F, 0.0F, 1.0F);
            float sway = Mth.sin(tk * swF + swP) * swAm;
            float px = ox + sway;
            float pz = oz + Mth.cos(tk * swF + swP) * swAm * 0.6F;
            float s = scale * edge * exitShrink;
            if (s < 0.01F)
                continue;
            poseStack.pushPose();
            poseStack.translate(px, y, pz);
            poseStack.mulPose(new Quaternionf().rotateXYZ(
                    (rPX + tk * rSX) * Mth.DEG_TO_RAD,
                    (rPY + tk * rSY) * Mth.DEG_TO_RAD,
                    (rPZ + tk * rSZ) * Mth.DEG_TO_RAD));
            poseStack.scale(s, s, s);
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            dispatcher.renderSingleBlock(block.defaultBlockState(), poseStack, bufferSource, 0xF000F0, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
            poseStack.popPose();
        }
    }
}
