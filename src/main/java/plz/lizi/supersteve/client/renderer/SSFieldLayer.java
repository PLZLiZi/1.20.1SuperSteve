package plz.lizi.supersteve.client.renderer;

import java.util.Set;
import com.google.common.base.Objects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import plz.lizi.supersteve.entity.SuperSteveEntityBase;
import plz.lizi.supersteve.entity.SuperSteveEntityBase.State;

public class SSFieldLayer extends SSLayer {
    public SSFieldLayer(SuperSteveRenderer p_117346_) {
        super(p_117346_);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, SuperSteveEntityBase entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        poseStack.pushPose();
        int stateTime = entity.stateTime();
		if (stateTime > SuperSteveEntityBase.DEATH_ACTIVE[1]) {
			poseStack.pushPose();
			float pgs = Math.max(0, Math.min(1, (stateTime + partialTick - SuperSteveEntityBase.DEATH_ACTIVE[1]) / SuperSteveEntityBase.DEATH_ACTIVE[2]));
			poseStack.translate(0, (float) Math.cos(Math.PI / 2F * pgs) * SuperSteveEntityBase.DEATH_ACTIVE[0] + 10, 0);
			poseStack.scale(25, 25, 25);
			poseStack.mulPose(Axis.YP.rotationDegrees(45F));
			poseStack.mulPose(Axis.ZN.rotationDegrees(135F));
			Minecraft.getInstance().getItemRenderer().renderStatic(entity.getItemInHand(InteractionHand.MAIN_HAND), ItemDisplayContext.NONE, LightTexture.pack(15, 15), OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level, 0);
			poseStack.popPose();
		}
        float openFieldPgs = SuperSteveEntityBase.openFieldPgs(stateTime, partialTick);
        float fieldSz = openFieldPgs * entity.ssGetAttR(true) * 8f;
        SSRenders.renderBall(buffer.getBuffer(SSRenders.POSITION_COLOR_H), poseStack, fieldSz, 0/* 黑色到红色领域用这个153f / 255f * openFieldPgs */, 0, 0, openFieldPgs, packedLight);
        //if (openFieldPgs >= 1)
        //    SSRenders.renderBall(buffer.getBuffer(SSRenders.POSITION_COLOR_NC), poseStack, fieldSz + 0.05F, 0, 0, 0, openFieldPgs, packedLight);
        poseStack.translate(0, -entity.getBbHeight() / 3, 0);
        var mc = Minecraft.getInstance();
        var erd = mc.getEntityRenderDispatcher();
        var myPos = entity.getPosition(partialTick);
        for (var t : ((ClientLevel) entity.level).entitiesForRendering()) {
            if (t == null || !(t instanceof LivingEntity) || t instanceof SuperSteveEntityBase || entity.distanceTo(t) > fieldSz)
                continue;
            if (Objects.equal(t, mc.gameRenderer.getMainCamera().getEntity()) && mc.options.getCameraType().isFirstPerson())
                continue;
            var offset = t.getPosition(partialTick).add(-myPos.x, -myPos.y, -myPos.z);
            erd.render(t, offset.x, offset.y, offset.z, Mth.lerp(partialTick, t.yRotO, t.getYRot()), partialTick, poseStack, buffer, packedLight);
        }
        poseStack.popPose();
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
        return Set.of(State.EXIT);
    }
}
