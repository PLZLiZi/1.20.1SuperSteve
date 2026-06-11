package plz.lizi.supersteve.client.renderer;

import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

public class SSFont {
	public static FontTest FONT_TEST = new FontTest((p_284586_) -> {
		var manager = Minecraft.getInstance().fontManager;
		return (FontSet) manager.fontSets.getOrDefault(manager.getActualId(p_284586_), manager.missingFontSet);
	}, false);

	public static class FontTest extends Font {

		public FontTest(Function<ResourceLocation, FontSet> p_243253_, boolean p_243245_) {
			super(p_243253_, p_243245_);
		}

		public int drawInBatch(@NotNull FormattedCharSequence formattedCharSequence, float x, float y, int rgb, boolean b1, @NotNull Matrix4f matrix4f, @NotNull MultiBufferSource multiBufferSource, @NotNull Font.DisplayMode mode, int i, int i1) { // multiBufferSource.get
			super.drawInBatch(formattedCharSequence, x, y, rgb, b1, matrix4f, multiBufferSource, mode, i, i1);
			return 0;
		}

		@Override
		public void renderChar(BakedGlyph p_254105_, boolean p_254001_, boolean p_254262_, float p_254256_, float p_253753_, float p_253629_, Matrix4f p_254014_, VertexConsumer p_253852_, float p_254317_, float p_253809_, float p_253870_, float p_254287_, int p_253905_) {
			p_254105_.render(p_254262_, p_253753_, p_253629_, p_254014_, p_253852_, p_254317_, p_253809_, p_253870_, p_254287_, p_253905_);
			if (p_254001_) {
				p_254105_.render(p_254262_, p_253753_ + p_254256_, p_253629_, p_254014_, p_253852_, p_254317_, p_253809_, p_253870_, p_254287_, p_253905_);
			}
		}
	}
}
