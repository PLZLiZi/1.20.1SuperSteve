package plz.lizi.supersteve.client.renderer;

import java.util.Arrays;
import net.minecraft.client.renderer.texture.SpriteContents;

class AlphaMask {
	final int width;
	final int height;
	private final long[] pixels;

	public AlphaMask(int width, int height, long[] pixels) {
		this.width = width;
		this.height = height;
		this.pixels = pixels;
	}

	public static AlphaMask create(SpriteContents contents, int frame, int width, int height) {
		long[] pixels = new long[(width * height + Long.SIZE - 1) / Long.SIZE];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int bit = y * width + x;
				if (!contents.isTransparent(frame, x, y))
					pixels[bit / Long.SIZE] |= 1L << (bit % Long.SIZE);
			}
		}
		return new AlphaMask(width, height, pixels);
	}

	public boolean get(int x, int y) {
		int bit = y * width + x;
		return (pixels[bit / Long.SIZE] & 1L << (bit % Long.SIZE)) != 0;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof AlphaMask other && width == other.width && height == other.height && Arrays.equals(pixels, other.pixels);
	}

	@Override
	public int hashCode() {
		int result = 31 * width + height;
		return 31 * result + Arrays.hashCode(pixels);
	}
}