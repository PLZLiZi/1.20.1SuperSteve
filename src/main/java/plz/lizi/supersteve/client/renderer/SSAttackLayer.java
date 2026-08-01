package plz.lizi.supersteve.client.renderer;

import java.util.Set;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import plz.lizi.supersteve.entity.SuperSteveEntityBase;
import plz.lizi.supersteve.entity.SuperSteveEntityBase.State;

public class SSAttackLayer extends SSLayer {
    public SSAttackLayer(SuperSteveRenderer p_117346_) {
        super(p_117346_);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, SuperSteveEntityBase entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.attacks == null || entity.attacks.isEmpty())
            return;
        poseStack.pushPose();
        poseStack.translate(0, -entity.getBbHeight() / 3f, 0);
        var myPos = entity.getPosition(partialTick);
        var vc = buffer.getBuffer(SSRenders.POSITION_COLOR);
        for (var attack : entity.attacks) {
            poseStack.pushPose();
            poseStack.translate(attack.pos.x - myPos.x, attack.pos.y - myPos.y, attack.pos.z - myPos.z);
            poseStack.mulPose(Axis.XP.rotationDegrees((float) attack.rot.x));
            poseStack.mulPose(Axis.YP.rotationDegrees((float) attack.rot.y));
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) attack.rot.z));
            poseStack.scale(attack.size.x, attack.size.y, attack.size.x);
            SSRenders.renderAttack(vc, poseStack, ((float) (attack.tick + partialTick) / (float) attack.life), 0, 0, 0, 1);
            poseStack.scale(-1.2f, 1.2f, 1.2f);
            SSRenders.renderAttack(vc, poseStack, ((float) (attack.tick + partialTick) / (float) attack.life), 1, 1, 1, 1);
            poseStack.popPose();
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
        return Set.of(State.values());
    }
}
