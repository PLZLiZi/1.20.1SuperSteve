package plz.lizi.supersteve.client.renderer.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import plz.lizi.supersteve.entity.SuperSteveEntityBase;

public class SuperSteveModel extends HumanoidModel<SuperSteveEntityBase> {
    public SuperSteveModel(ModelPart p_170677_) {
        super(p_170677_);
    }

    @Override
    public RenderType renderType(ResourceLocation pLocation) {
        return RenderType.entityCutoutNoCull(pLocation);
    }

    private static void renderModelPart(ModelPart zhis, PoseStack p_104307_, VertexConsumer p_104308_, int p_104309_, int p_104310_, float p_104311_, float p_104312_, float p_104313_, float p_104314_) {
        if (!zhis.cubes.isEmpty() || !zhis.children.isEmpty()) {
            p_104307_.pushPose();
            zhis.translateAndRotate(p_104307_);
            zhis.compile(p_104307_.last(), p_104308_, p_104309_, p_104310_, p_104311_, p_104312_, p_104313_, p_104314_);
            for (ModelPart $$8 : zhis.children.values()) {
                $$8.render(p_104307_, p_104308_, p_104309_, p_104310_, p_104311_, p_104312_, p_104313_, p_104314_);
            }
            p_104307_.popPose();
        }
    }

    @Override
    public void renderToBuffer(PoseStack p_102034_, VertexConsumer p_102035_, int p_102036_, int p_102037_, float p_102038_, float p_102039_, float p_102040_, float p_102041_) {
        this.headParts().forEach((p_102061_) -> renderModelPart(p_102061_, p_102034_, p_102035_, p_102036_, p_102037_, p_102038_, p_102039_, p_102040_, p_102041_));
        this.bodyParts().forEach((p_102051_) -> renderModelPart(p_102051_, p_102034_, p_102035_, p_102036_, p_102037_, p_102038_, p_102039_, p_102040_, p_102041_));
    }
}
