package plz.lizi.supersteve.event;

import java.awt.Color;
import org.joml.Matrix4f;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import plz.lizi.supersteve.SuperSteveMod;
import plz.lizi.supersteve.api.PLZBase;
import plz.lizi.supersteve.api.SSUtil;
import plz.lizi.supersteve.client.renderer.SuperSteveRenderer;
import plz.lizi.supersteve.entity.SuperSteveEntityBase;
import plz.lizi.supersteve.entity.SuperSteveEntityBase.State;

public class SSEvents {
	public static ResourceLocation SS_BAR = ResourceLocation.tryBuild(SuperSteveMod.MODID, "textures/gui/ssbar.png");

	@SubscribeEvent
	public static void bossEventProgress(CustomizeGuiOverlayEvent.BossEventProgress event) {
		LerpingBossEvent bossEvent = event.getBossEvent();
		SuperSteveEntityBase fss = null;
		for (var ssi : SuperSteveMod.SS_INSTANCES.values())
			if (ssi != null && ssi.clientInstance != null && ssi.clientInstance.getUUID().equals(bossEvent.getId()))
				fss = ssi.clientInstance;
		if (fss != null) {
			SuperSteveEntityBase ss = fss;
			event.setCanceled(true);
			Minecraft mc = Minecraft.getInstance();
			Window window = mc.getWindow();
			RenderSystem.enableBlend();
			GuiGraphics guiGraphics = event.getGuiGraphics();
			float x = window.getGuiScaledWidth() / 2;
			float y = event.getY();
			float barWidth = 180;
			float barHeight = 22;
			float inPgs = bossEvent.getProgress();
			PoseStack poseStack = guiGraphics.pose();
			poseStack.pushPose();
			poseStack.translate(x, y, 0);
			poseStack.scale(1.5F, 1.5F, 1F);
			var sidePgs = 0F;
			var dsidePgs = 1f;
			if (ss.getState() == State.ENTER) {
				inPgs = ((float) ss.stateTime() + event.getPartialTick()) / (float) SuperSteveEntityBase.ENTER_ACTIVE[0];
			} else if (ss.getState() == State.EXIT) {
				sidePgs = ((float) ss.stateTime() + event.getPartialTick()) / (float) SuperSteveEntityBase.DEATH_ACTIVE[0];
				dsidePgs = 1f - sidePgs;
				inPgs = 0;
			}
			sidePgs = PLZBase.progress(sidePgs);
			dsidePgs = PLZBase.progress(dsidePgs);
			inPgs = PLZBase.progress(inPgs);
			Matrix4f pose = poseStack.last().pose();
			BufferBuilder builder = Tesselator.getInstance().getBuilder();
			builder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
			builder.vertex(pose, -barWidth / 2F * dsidePgs, -barHeight / 2F, 0).uv(sidePgs * 0.5f, 0).endVertex();
			builder.vertex(pose, -barWidth / 2F * dsidePgs, +barHeight / 2F, 0).uv(sidePgs * 0.5f, 22F / 27F).endVertex();
			builder.vertex(pose, +barWidth / 2F * dsidePgs, +barHeight / 2F, 0).uv((dsidePgs + 1f) * 0.5f, 22F / 27F).endVertex();
			builder.vertex(pose, +barWidth / 2F * dsidePgs, -barHeight / 2F, 0).uv((dsidePgs + 1f) * 0.5f, 0).endVertex();
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderTexture(0, SS_BAR);
			RenderStateShard.TRANSLUCENT_TRANSPARENCY.setupRenderState();
			BufferUploader.drawWithShader(builder.end());
			builder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
			float xStart = -barWidth / 2F + 10F;
			float yStart = -barHeight / 2F + 9F;
			float hWidth = 165;
			float hHeight = 5;
			builder.vertex(pose, xStart, yStart, 0).uv(0, 22F / 27F).endVertex();
			builder.vertex(pose, xStart, yStart + hHeight, 0).uv(0, 1).endVertex();
			builder.vertex(pose, xStart + (inPgs * hWidth), yStart + hHeight, 0).uv(165F / 180F * inPgs, 1).endVertex();
			builder.vertex(pose, xStart + (inPgs * hWidth), yStart, 0).uv(165F / 180F * inPgs, 22F / 27F).endVertex();
			BufferUploader.drawWithShader(builder.end());
			RenderStateShard.TRANSLUCENT_TRANSPARENCY.clearRenderState();
			poseStack.popPose();
			guiGraphics.drawCenteredString(mc.font, Component.literal(fss.getCustomName().getString() + " " + ss.ssGetHealth() + "/20.0").withStyle(Style.EMPTY.withBold(true).withItalic(true)), (int) x, (int) (y - barHeight / 2F), Color.HSBtoRGB(SSUtil.getRainbowHue(3000), 1, 1));
			RenderSystem.disableBlend();
		}
	}

	@SubscribeEvent
	public static void renderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
			Minecraft mc = Minecraft.getInstance();
			var partialTick = event.getPartialTick();
			var poseStack = event.getPoseStack();
			var cam = event.getCamera();
			Vec3 cam3 = cam.getPosition();
			double c0 = cam3.x();
			double c1 = cam3.y();
			double c2 = cam3.z();
			var frustum = event.getFrustum();
			var zhis = event.getLevelRenderer();
			MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
			for (var istc : SuperSteveMod.SS_INSTANCES.values()) {
				SuperSteveEntityBase entity = istc.clientInstance;
				if (entity == null)
					continue;
				if (mc.entityRenderDispatcher.shouldRender(entity, frustum, c0, c1, c2) || entity.hasIndirectPassenger(mc.player)) {
					BlockPos blockpos = entity.blockPosition();
					if ((zhis.level.isOutsideBuildHeight(blockpos.getY()) || zhis.isChunkCompiled(blockpos)) && (entity != cam.getEntity() || cam.isDetached() || cam.getEntity() instanceof LivingEntity && ((LivingEntity) cam.getEntity()).isSleeping()))
						continue;
				}
				if (entity.tickCount == 0) {
					entity.xOld = entity.getX();
					entity.yOld = entity.getY();
					entity.zOld = entity.getZ();
				}
				SuperSteveRenderer entityrenderer = (SuperSteveRenderer) mc.getEntityRenderDispatcher().getRenderer(entity);
				double lr0 = Mth.lerp((double) partialTick, entity.xOld, entity.getX());
				double lr1 = Mth.lerp((double) partialTick, entity.yOld, entity.getY());
				double lr2 = Mth.lerp((double) partialTick, entity.zOld, entity.getZ());
				float f = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
				Vec3 vec3 = entityrenderer.getRenderOffset(entity, partialTick);
				double d2 = lr0 - c0 + vec3.x();
				double d3 = lr1 - c1 + vec3.y();
				double d0 = lr2 - c2 + vec3.z();
				poseStack.pushPose();
				poseStack.translate(d2, d3, d0);
				entityrenderer.render(entity, f, partialTick, poseStack, buffer, mc.getEntityRenderDispatcher().getPackedLightCoords(entity, partialTick));
				poseStack.popPose();
			}
			buffer.endLastBatch();
			buffer.endBatch(RenderType.entitySolid(InventoryMenu.BLOCK_ATLAS));
			buffer.endBatch(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
			buffer.endBatch(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
			buffer.endBatch(RenderType.entitySmoothCutout(InventoryMenu.BLOCK_ATLAS));
		}
	}
}
