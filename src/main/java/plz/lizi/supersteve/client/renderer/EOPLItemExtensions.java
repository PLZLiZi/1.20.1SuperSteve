package plz.lizi.supersteve.client.renderer;

import org.jetbrains.annotations.Nullable;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class EOPLItemExtensions implements IClientItemExtensions {
	public static EOPLItemExtensions INSTACNE = new EOPLItemExtensions();

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
			/*int hor = (arm == HumanoidArm.RIGHT) ? 1 : -1;
			poseStack.translate(hor * 0.2F, 0.34F, -0.1F);
			poseStack.mulPose(Axis.XP.rotationDegrees(-90.00F));// 
			poseStack.mulPose(Axis.YP.rotationDegrees(hor * 12.00F));
			poseStack.mulPose(Axis.ZP.rotationDegrees(hor * 85.00F));*/
			return true;
		}
		return false;
	}

	@Override
	public @Nullable Font getFont(ItemStack stack, FontContext context) {
		return SSFont.FONT_TEST;
	}
}
