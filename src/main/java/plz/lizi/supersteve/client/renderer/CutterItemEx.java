package plz.lizi.supersteve.client.renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraftforge.client.RenderTypeGroup;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class CutterItemEx implements IClientItemExtensions {
    private static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();
    private static final FaceBakery FACE_BAKERY = new FaceBakery();
    public static final CutterItemEx INSTANCE = new CutterItemEx();
    private static BlockEntityWithoutLevelRenderer RENDERER = null;

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (RENDERER == null)
            RENDERER = new CutterRenderer();
        return RENDERER;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess) {
        if (swingProcess > 0) {
            float progress = (float) Math.pow(swingProcess, 0.5);
            poseStack.translate(0.5, -0.4, -0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            poseStack.mulPose(Axis.ZP.rotationDegrees(90));
            poseStack.mulPose(Axis.YP.rotationDegrees(-5));
            poseStack.mulPose(Axis.XP.rotationDegrees(-180 * progress + 80));
            poseStack.translate(0, 0.5, 0.5);
            return true;
        }
        return false;
    }

    public static class CutterRenderer extends BlockEntityWithoutLevelRenderer {
        private final Minecraft mc = Minecraft.getInstance();
        private BakedModel imodel = null;

        public CutterRenderer() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        }

        @Override
        public void renderByItem(ItemStack pStack, ItemDisplayContext pDisplayContext, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
            renderBase(pStack, pDisplayContext, pPoseStack, pBuffer, pPackedLight, pPackedOverlay);
        }

        public void renderBase(ItemStack p_108830_, ItemDisplayContext p_270899_, PoseStack p_108832_, MultiBufferSource p_108833_, int p_108834_, int p_108835_) {
            var base = mc.getItemRenderer().getModel(p_108830_, null, null, 0);
            if (base == null)
                return;
            if (imodel == null) {
                TextureAtlasSprite sprite = base.getParticleIcon(null);
                List<BakedQuad> quads = new ArrayList<>();
                for (BlockElement elem : ITEM_MODEL_GENERATOR.processFrames(0, "layer0", sprite.contents())) {
                    for (Map.Entry<Direction, BlockElementFace> entry : elem.faces.entrySet()) {
                        quads.add(FACE_BAKERY.bakeQuad(elem.from, elem.to, entry.getValue(), sprite, entry.getKey(), BlockModelRotation.X0_Y0, elem.rotation, elem.shade, null));
                    }
                }
                SimpleBakedModel.Builder builder = new SimpleBakedModel.Builder(false, false, false, ItemTransforms.NO_TRANSFORMS, ItemOverrides.EMPTY).particle(sprite);
                for (BakedQuad q : quads)
                    builder.addUnculledFace(q);
                imodel = builder.build(RenderTypeGroup.EMPTY);
            }
            boolean flag1;
            if (p_270899_ != ItemDisplayContext.GUI && !p_270899_.firstPerson() && p_108830_.getItem() instanceof BlockItem) {
                Block block = ((BlockItem) p_108830_.getItem()).getBlock();
                flag1 = !(block instanceof HalfTransparentBlock) && !(block instanceof StainedGlassPaneBlock);
            } else {
                flag1 = true;
            }
            var var17 = imodel.getRenderPasses(p_108830_, flag1).iterator();
            while (var17.hasNext()) {
                BakedModel model = (BakedModel) var17.next();
                VertexConsumer vertexconsumer;
                for (var var13 = model.getRenderTypes(p_108830_, flag1).iterator(); var13.hasNext(); mc.getItemRenderer().renderModelLists(model, p_108830_, p_108834_, p_108835_, p_108832_, vertexconsumer)) {
                    RenderType rendertype = (RenderType) var13.next();
                    if (flag1) {
                        vertexconsumer = ItemRenderer.getFoilBufferDirect(p_108833_, rendertype, true, p_108830_.hasFoil());
                    } else {
                        vertexconsumer = ItemRenderer.getFoilBuffer(p_108833_, rendertype, true, p_108830_.hasFoil());
                    }
                }
            }
        }
    }
}
