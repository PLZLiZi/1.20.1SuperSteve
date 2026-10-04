package plz.lizi.supersteve.client.renderer.model;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SpriteContents;

public class OutlineModel implements AutoCloseable {
	private final Map<Integer, Frame> frameModels;
	private final List<Frame> uniqueModels;
	private final Frame defaultModel;
	private final SpriteContents sourceContents;
	private final SpriteContents.Ticker sourceTicker;
	public final float scale;

	public OutlineModel(Map<Integer, Frame> frameModels, List<Frame> uniqueModels, Frame defaultModel, SpriteContents sourceContents, SpriteContents.Ticker sourceTicker, float scale) {
		this.frameModels = frameModels;
		this.uniqueModels = uniqueModels;
		this.defaultModel = defaultModel;
		this.sourceContents = sourceContents;
		this.sourceTicker = sourceTicker;
		this.scale = scale;
	}

	public Frame currentFrame() {
		if (sourceTicker != null && sourceContents.animatedTexture != null && !sourceContents.animatedTexture.frames.isEmpty()) {
			int sequenceIndex = Math.floorMod(sourceTicker.frame, sourceContents.animatedTexture.frames.size());
			int imageFrame = sourceContents.animatedTexture.frames.get(sequenceIndex).index;
			return frameModels.getOrDefault(imageFrame, defaultModel);
		}
		return defaultModel;
	}

	@Override
	public void close() {
		for (Frame model : uniqueModels)
			model.close();
	}

	public static class Frame implements AutoCloseable {
		public final LinkedList<float[]> modelData;
		public final DynamicTexture texture;

		public Frame(LinkedList<float[]> modelData, DynamicTexture texture) {
			this.modelData = modelData;
			this.texture = texture;
		}

		@Override
		public void close() {
			texture.close();
		}
	}
}
