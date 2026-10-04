package plz.lizi.supersteve.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class SpriteTexStateShard extends RenderStateShard.EmptyTextureStateShard {
	public SpriteTexStateShard(TextureAtlasSprite sprite) {
		this(sprite, new DynamicTexture[] { SSRenders.getRegion(sprite) });
	}

	private SpriteTexStateShard(TextureAtlasSprite sprite, DynamicTexture[] holder) {
		super(() -> {
			if (holder[0] != null) {
				SSRenders.updateRegion(sprite, holder[0]);
				RenderSystem.setShaderTexture(0, holder[0].getId());
			}
		}, () -> {
		});
	}
}