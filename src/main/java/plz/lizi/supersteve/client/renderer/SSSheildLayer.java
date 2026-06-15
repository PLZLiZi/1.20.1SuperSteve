package plz.lizi.supersteve.client.renderer;

import java.util.Set;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import plz.lizi.supersteve.api.SSUtil;
import plz.lizi.supersteve.entity.SuperSteveEntityBase;
import plz.lizi.supersteve.entity.SuperSteveEntityBase.State;

public class SSSheildLayer extends SSLayer {
    public SSSheildLayer(SuperSteveRenderer parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, SuperSteveEntityBase entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.iInvulnerableTime <= 0)
            return;
        poseStack.pushPose();
        poseStack.translate(0, 0.4f, 0);
        poseStack.scale(1, 1.5f, 1);
        float[] color = SSUtil.getRainbowColor(3);
        SSRenders.renderBall(bufferSource.getBuffer(SSRenders.POSITION_COLOR), poseStack, 0.8f, color[0], color[1], color[2], (float) (entity.iInvulnerableTime + partialTick) / (float) SuperSteveEntityBase.MAX_INVULNERABLE_TIME / 2F, packedLight);
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
