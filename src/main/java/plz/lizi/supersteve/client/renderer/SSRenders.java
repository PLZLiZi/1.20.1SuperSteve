package plz.lizi.supersteve.client.renderer;

import java.util.OptionalDouble;
import java.util.function.Function;
import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.RenderStateShard.DepthTestStateShard;
import net.minecraft.client.renderer.RenderStateShard.MultiTextureStateShard;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

public class SSRenders {
    public static final Function<Double, RenderType> CUSTOM_LINE = Util.memoize(size -> RenderType.create("supersteve:lines", DefaultVertexFormat.POSITION_COLOR_NORMAL, Mode.LINES, 256, RenderType.CompositeState.builder().setShaderState(ShaderStateShard.RENDERTYPE_LINES_SHADER).setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(size))).setLayeringState(ShaderStateShard.VIEW_OFFSET_Z_LAYERING).setTransparencyState(ShaderStateShard.TRANSLUCENT_TRANSPARENCY).setOutputState(ShaderStateShard.ITEM_ENTITY_TARGET).setWriteMaskState(ShaderStateShard.COLOR_DEPTH_WRITE).setCullState(ShaderStateShard.NO_CULL).createCompositeState(false)));
    public static final RenderType END_PORTAL_NC = RenderType.create("supersteve:end_portal_nc", DefaultVertexFormat.POSITION, Mode.QUADS, 256, false, false, RenderType.CompositeState.builder().setShaderState(RenderStateShard.RENDERTYPE_END_PORTAL_SHADER).setTextureState(MultiTextureStateShard.builder().add(TheEndPortalRenderer.END_SKY_LOCATION, false, false).add(TheEndPortalRenderer.END_PORTAL_LOCATION, false, false).build()).setCullState(ShaderStateShard.NO_CULL).createCompositeState(false));
    public static final RenderType END_PORTAL_H = RenderType.create("supersteve:end_portal_h", DefaultVertexFormat.POSITION, Mode.QUADS, 256, false, false, RenderType.CompositeState.builder().setShaderState(RenderStateShard.RENDERTYPE_END_PORTAL_SHADER).setTextureState(MultiTextureStateShard.builder().add(TheEndPortalRenderer.END_SKY_LOCATION, false, false).add(TheEndPortalRenderer.END_PORTAL_LOCATION, false, false).build()).setCullState(ShaderStateShard.CULL).setDepthTestState(DepthTestStateShard.NO_DEPTH_TEST).createCompositeState(false));
    public static final RenderType POSITION_COLOR_NC = RenderType.create("supersteve:position_color_nc", DefaultVertexFormat.POSITION_COLOR, Mode.QUADS, 256, RenderType.CompositeState.builder().setShaderState(RenderStateShard.POSITION_COLOR_SHADER).setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY).setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST).setCullState(RenderStateShard.NO_CULL).createCompositeState(false));
    public static final RenderType POSITION_COLOR_H = RenderType.create("supersteve:position_color_h", DefaultVertexFormat.POSITION_COLOR, Mode.QUADS, 256, RenderType.CompositeState.builder().setShaderState(RenderStateShard.POSITION_COLOR_SHADER).setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY).setCullState(RenderStateShard.CULL).setDepthTestState(DepthTestStateShard.NO_DEPTH_TEST).createCompositeState(false));
    public static final RenderType POSITION_COLOR = RenderType.create("supersteve:position_color", DefaultVertexFormat.POSITION_COLOR, Mode.QUADS, 256, RenderType.CompositeState.builder().setShaderState(RenderStateShard.POSITION_COLOR_SHADER).setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY).setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST).setCullState(RenderStateShard.CULL).createCompositeState(false));

    public static void renderAttack(VertexConsumer vc, PoseStack poseStack, float progress, float r, float g, float b, float a) {
        poseStack.pushPose();
        float topPgs = Math.max(0F, Math.min(1f, progress * 2f));
        float topDPgs = Math.max(0F, Math.min(1f, topPgs * 2f));
        float topUPgs = Math.max(0F, Math.min(1f, (topPgs - 0.5f) * 2f));
        float endPgs = Math.max(0F, Math.min(1f, (progress - 0.5f) * 2f));
        float endDPgs = Math.max(0F, Math.min(1f, endPgs * 2f));
        float endUPgs = Math.max(0F, Math.min(1f, (endPgs - 0.5f) * 2f));
        float H = 4f;
        float hfH = H / 2f;
        float W = 0.2f;
        float hfW = W / 2f;
        for (int i = 0; i < 4; i++) {
            Matrix4f pose = poseStack.last().pose();
            vc.vertex(pose, -hfW * endDPgs, -hfH * (1f - endDPgs), hfW * endDPgs).color(r, g, b, a).endVertex();
            vc.vertex(pose, hfW * endDPgs, -hfH * (1f - endDPgs), hfW * endDPgs).color(r, g, b, a).endVertex();
            vc.vertex(pose, hfW * topDPgs, -hfH * (1f - topDPgs), hfW * topDPgs).color(r, g, b, a).endVertex();
            vc.vertex(pose, -hfW * topDPgs, -hfH * (1f - topDPgs), hfW * topDPgs).color(r, g, b, a).endVertex();
            //
            vc.vertex(pose, -hfW * (1f - endUPgs), hfH * endUPgs, hfW * (1f - endUPgs)).color(r, g, b, a).endVertex();
            vc.vertex(pose, hfW * (1f - endUPgs), hfH * endUPgs, hfW * (1f - endUPgs)).color(r, g, b, a).endVertex();
            vc.vertex(pose, hfW * (1f - topUPgs), hfH * topUPgs, hfW * (1f - topUPgs)).color(r, g, b, a).endVertex();
            vc.vertex(pose, -hfW * (1f - topUPgs), hfH * topUPgs, hfW * (1f - topUPgs)).color(r, g, b, a).endVertex();
            poseStack.mulPose(Axis.YP.rotationDegrees(90F));
        }
        float topFcPgs = -2f * Math.abs(topPgs - 0.5f) + 1f;
        float endFcPgs = -2f * Math.abs(endPgs - 0.5f) + 1f;
        float topYPgs = 2f * topPgs - 1f;
        float endYPgs = 2f * endPgs - 1f;
        vc.vertex(poseStack.last().pose(), hfW * topFcPgs, hfH * topYPgs, hfW * topFcPgs).color(r, g, b, a).endVertex();
        vc.vertex(poseStack.last().pose(), -hfW * topFcPgs, hfH * topYPgs, hfW * topFcPgs).color(r, g, b, a).endVertex();
        vc.vertex(poseStack.last().pose(), -hfW * topFcPgs, hfH * topYPgs, -hfW * topFcPgs).color(r, g, b, a).endVertex();
        vc.vertex(poseStack.last().pose(), hfW * topFcPgs, hfH * topYPgs, -hfW * topFcPgs).color(r, g, b, a).endVertex();
        //
        vc.vertex(poseStack.last().pose(), hfW * endFcPgs, hfH * endYPgs, hfW * endFcPgs).color(r, g, b, a).endVertex();
        vc.vertex(poseStack.last().pose(), hfW * endFcPgs, hfH * endYPgs, -hfW * endFcPgs).color(r, g, b, a).endVertex();
        vc.vertex(poseStack.last().pose(), -hfW * endFcPgs, hfH * endYPgs, -hfW * endFcPgs).color(r, g, b, a).endVertex();
        vc.vertex(poseStack.last().pose(), -hfW * endFcPgs, hfH * endYPgs, hfW * endFcPgs).color(r, g, b, a).endVertex();
        poseStack.popPose();
    }

    public static void renderStrip(PoseStack poseStack, VertexConsumer vc, float range, float halfH, int seg, float r, float g, float b, float a, int packedLight) {
        PoseStack.Pose m = poseStack.last();
        for (int i = 0; i < seg; i++) {
            float a0 = (float) (2 * Math.PI * (i / (float) seg));
            float a1 = (float) (2 * Math.PI * ((i + 1F) / seg));
            float x0 = Mth.cos(a0) * range;
            float z0 = Mth.sin(a0) * range;
            float x1 = Mth.cos(a1) * range;
            float z1 = Mth.sin(a1) * range;
            float U = halfH;
            float D = -halfH;
            vc.vertex(m.pose(), x0, U, z0).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(packedLight).normal(m.normal(), x0, 0, z0).endVertex();
            vc.vertex(m.pose(), x0, D, z0).color(r, g, b, a).uv(0, 1).overlayCoords(0, 10).uv2(packedLight).normal(m.normal(), x0, 0, z0).endVertex();
            vc.vertex(m.pose(), x1, D, z1).color(r, g, b, a).uv(1, 1).overlayCoords(0, 10).uv2(packedLight).normal(m.normal(), x1, 0, z1).endVertex();
            vc.vertex(m.pose(), x1, U, z1).color(r, g, b, a).uv(1, 0).overlayCoords(0, 10).uv2(packedLight).normal(m.normal(), x1, 0, z1).endVertex();
        }
    }

    public static void renderBall(VertexConsumer vc, PoseStack poseStack, float radius, float r, float g, float b, float a, int packedLight) {
        int latitudeSegments = 24;
        int longitudeSegments = 24;
        PoseStack.Pose lastPose = poseStack.last();
        for (int i = 0; i < latitudeSegments; i++) {
            float theta1 = (float) Math.PI * (float) i / latitudeSegments;
            float theta2 = (float) Math.PI * (float) (i + 1) / latitudeSegments;
            for (int j = 0; j < longitudeSegments; j++) {
                float phi1 = (float) (2.0 * Math.PI * (double) j / longitudeSegments);
                float phi2 = (float) (2.0 * Math.PI * (double) (j + 1) / longitudeSegments);
                float x00 = radius * Mth.sin(theta1) * Mth.cos(phi1);
                float y00 = radius * Mth.cos(theta1);
                float z00 = radius * Mth.sin(theta1) * Mth.sin(phi1);
                float x10 = radius * Mth.sin(theta2) * Mth.cos(phi1);
                float y10 = radius * Mth.cos(theta2);
                float z10 = radius * Mth.sin(theta2) * Mth.sin(phi1);
                float x11 = radius * Mth.sin(theta2) * Mth.cos(phi2);
                float y11 = radius * Mth.cos(theta2);
                float z11 = radius * Mth.sin(theta2) * Mth.sin(phi2);
                float x01 = radius * Mth.sin(theta1) * Mth.cos(phi2);
                float y01 = radius * Mth.cos(theta1);
                float z01 = radius * Mth.sin(theta1) * Mth.sin(phi2);
                float nx00 = Mth.sin(theta1) * Mth.cos(phi1);
                float ny00 = Mth.cos(theta1);
                float nz00 = Mth.sin(theta1) * Mth.sin(phi1);
                float nx10 = Mth.sin(theta2) * Mth.cos(phi1);
                float ny10 = Mth.cos(theta2);
                float nz10 = Mth.sin(theta2) * Mth.sin(phi1);
                float nx11 = Mth.sin(theta2) * Mth.cos(phi2);
                float ny11 = Mth.cos(theta2);
                float nz11 = Mth.sin(theta2) * Mth.sin(phi2);
                float nx01 = Mth.sin(theta1) * Mth.cos(phi2);
                float ny01 = Mth.cos(theta1);
                float nz01 = Mth.sin(theta1) * Mth.sin(phi2);
                vc.vertex(lastPose.pose(), x00, y00, z00).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(lastPose.normal(), nx00, ny00, nz00).endVertex();
                vc.vertex(lastPose.pose(), x10, y10, z10).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(lastPose.normal(), nx10, ny10, nz10).endVertex();
                vc.vertex(lastPose.pose(), x11, y11, z11).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(lastPose.normal(), nx11, ny11, nz11).endVertex();
                vc.vertex(lastPose.pose(), x01, y01, z01).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(lastPose.normal(), nx01, ny01, nz01).endVertex();
            }
        }
    }
}
