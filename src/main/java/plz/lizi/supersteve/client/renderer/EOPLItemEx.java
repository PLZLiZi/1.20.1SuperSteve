package plz.lizi.supersteve.client.renderer;

import java.awt.Color;
import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import plz.lizi.supersteve.client.renderer.model.ExtraModel;

public class EOPLItemEx implements IClientItemExtensions {
	private static EOPLItemEx INSTANCE;
	private final ItemRenderer renderer = new ItemRenderer();

	public static EOPLItemEx getInstance() {
		if (INSTANCE == null)
			INSTANCE = new EOPLItemEx();
		return INSTANCE;
	}

	private EOPLItemEx() {
	}

	@Override
	public ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
		if (entityLiving.isUsingItem() && itemStack.getUseAnimation() == UseAnim.BLOCK) {
			return ArmPose.BLOCK;
		}
		return null;
	}

	@Override
	public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess) {
		if (player.isUsingItem() && itemInHand.getUseAnimation() == UseAnim.BLOCK) {
			int horizontal = (arm == HumanoidArm.RIGHT) ? 1 : -1;
			poseStack.translate((float) horizontal * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F);
			poseStack.translate(horizontal * -0.14142136F, 0.08F, 0.14142136F);
			poseStack.mulPose(Axis.XP.rotationDegrees(-102.25F));
			poseStack.mulPose(Axis.YP.rotationDegrees(horizontal * 13.365F));
			poseStack.mulPose(Axis.ZP.rotationDegrees(horizontal * 78.05F));
			float f1 = (float) Math.sin(Math.sqrt(swingProcess) * Math.PI);
			poseStack.mulPose(Axis.YP.rotationDegrees((float) Math.sin(swingProcess * swingProcess * Math.PI) * -20.0F));
			poseStack.mulPose(Axis.ZP.rotationDegrees(f1 * -20.0F));
			poseStack.mulPose(Axis.XP.rotationDegrees(f1 * -80.0F));
			return true;
		}
		return false;
	}

	@Override
	public BlockEntityWithoutLevelRenderer getCustomRenderer() {
		return renderer;
	}

	private static class ItemRenderer extends BlockEntityWithoutLevelRenderer {
		private ExtraModel MODEL;

		public ItemRenderer() {
			super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
		}

		@Override
		public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
			if (MODEL == null)
				MODEL = new ExtraModel().base(Minecraft.getInstance().getItemRenderer().getModel(stack, null, null, 0).getParticleIcon(null)).layer(() -> SSRenders.MC_SHADER, List.of(ResourceLocation.tryParse("supersteve:item/mask"))).outline(()->SSRenders.RAINBOW_OUTLINE_SHADER, 0.01F, () -> Color.WHITE.getRGB());
			poseStack.pushPose();
			MODEL.render(stack, transformType, poseStack, buffer, combinedLight, combinedOverlay);
			poseStack.popPose();
		}
	}
}

