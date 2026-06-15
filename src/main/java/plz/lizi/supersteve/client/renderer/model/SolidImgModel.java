package plz.lizi.supersteve.client.renderer.model;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.joml.Matrix4f;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import plz.lizi.supersteve.api.PLZBase;
import plz.lizi.supersteve.api.SSUtil;
import plz.lizi.supersteve.api.SSUtil.Edge;

public class SolidImgModel {
	private ResourceLocation res;
	private BufferedImage img;
	private List<SSUtil.Edge> edges = new ArrayList<>();
	private float hthickness;

	public SolidImgModel(ResourceLocation res, float thickness) {
		try (InputStream is = Minecraft.getInstance().getResourceManager().getResourceOrThrow(res).open()) {
			this.res = res;
			hthickness = thickness / 8F;
			img = ImageIO.read(is);
			float w = img.getWidth();
			float h = img.getHeight();
			for (Edge edge : SSUtil.findImgEdges(img)) {
				edges.add(new Edge((edge.x1 / w) - 0.5F, (edge.y1 / h) - 0.5F, (edge.x2 / w) - 0.5F, (edge.y2 / h) - 0.5F, edge.color));
			}
		} catch (Throwable e) {
			PLZBase.throwEx(e);
		}
	}

	public void render(PoseStack poseStack) {
		var builder = Tesselator.getInstance().getBuilder();
		builder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.disableCull();
		RenderSystem.enablePolygonOffset();
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		Matrix4f pose = poseStack.last().pose();
		for (Edge edge : edges) {
			int r = edge.color.getRed();
			int g = edge.color.getGreen();
			int b = edge.color.getBlue();
			int a = edge.color.getAlpha();
			builder.vertex(pose, edge.x1, edge.y1, hthickness).color(r, g, b, a).endVertex();
			builder.vertex(pose, edge.x2, edge.y2, hthickness).color(r, g, b, a).endVertex();
			builder.vertex(pose, edge.x2, edge.y2, -hthickness).color(r, g, b, a).endVertex();
			builder.vertex(pose, edge.x1, edge.y1, -hthickness).color(r, g, b, a).endVertex();
		}
		BufferUploader.drawWithShader(builder.end());
		builder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, res);
		builder.vertex(pose, -0.5F, 0.5F, hthickness).uv(0, 1).endVertex();
		builder.vertex(pose, -0.5F, -0.5F, hthickness).uv(0, 0).endVertex();
		builder.vertex(pose, 0.5F, -0.5F, hthickness).uv(1, 0).endVertex();
		builder.vertex(pose, 0.5F, 0.5F, hthickness).uv(1, 1).endVertex();
		builder.vertex(pose, 0.5F, 0.5F, -hthickness).uv(1, 1).endVertex();
		builder.vertex(pose, 0.5F, -0.5F, -hthickness).uv(1, 0).endVertex();
		builder.vertex(pose, -0.5F, -0.5F, -hthickness).uv(0, 0).endVertex();
		builder.vertex(pose, -0.5F, 0.5F, -hthickness).uv(0, 1).endVertex();
		BufferUploader.drawWithShader(builder.end());
	}
}
