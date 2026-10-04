package plz.lizi.supersteve.client.renderer.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.MatrixUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.RenderTypeGroup;
import net.minecraftforge.client.model.data.ModelData;
import plz.lizi.supersteve.client.renderer.SSRenders;

public class ExtraModel implements BakedModel {
	private final Minecraft mc = Minecraft.getInstance();
	private BakedModel base = mc.getModelManager().getMissingModel();
	private final Map<ResourceLocation, LinkedList<float[]>> masks = new HashMap<>();
	private Supplier<ShaderInstance> layer = null;
	private Supplier<ShaderInstance> outline = null;
	private int outlineSize = 0;
	private OutlineModel outlineModule = null;

	public ExtraModel() {}

	public ExtraModel base(TextureAtlasSprite sprite) {
		SimpleBakedModel.Builder builder = new SimpleBakedModel.Builder(false, false, false, ItemTransforms.NO_TRANSFORMS, ItemOverrides.EMPTY).particle(sprite);
		for (BakedQuad q : SSRenders.bakeQuads(sprite))
			builder.addUnculledFace(q);
		return base(builder.build(RenderTypeGroup.EMPTY));
	}

	public ExtraModel base(BakedModel base) {
		this.base = base == null ? Minecraft.getInstance().getModelManager().getMissingModel() : base;
		return this;
	}

	public ExtraModel layer(Supplier<ShaderInstance> shader, List<ResourceLocation> masks) {
		this.layer = shader;
		for (ResourceLocation mask : masks)
			this.masks.put(mask, new LinkedList<>());
		return this;
	}

	public ExtraModel outline(Supplier<ShaderInstance> outline, int size, Supplier<Integer> color) {
		this.outline = outline;
		this.outlineSize = size;
		return this;
	}

	public void render(ItemStack p_108830_, ItemDisplayContext p_270899_, PoseStack p_108832_, MultiBufferSource p_108833_, int p_108834_, int p_108835_) {
		BufferSource buffersource = mc.renderBuffers.bufferSource;
		buffersource.endBatch();
		renderOutline(p_108830_, p_270899_, p_108832_, buffersource, p_108834_, p_108835_);
		buffersource.endBatch();
		renderBase(p_108830_, p_270899_, p_108832_, buffersource, p_108834_, p_108835_);
		buffersource.endBatch();
		renderLayer(p_108832_, buffersource, p_108830_, p_108834_, p_108835_);
		buffersource.endBatch();
	}

	public void renderOutline(ItemStack p_108830_, ItemDisplayContext p_270899_, PoseStack p_108832_, MultiBufferSource p_108833_, int p_108834_, int p_108835_) {
		if (outline == null || outlineSize <= 0)
			return;
		if (outlineModule == null) {
			OutlineModel result = SSRenders.bakeOutlineModel(base.getParticleIcon(null), Math.max(1, 6 / outlineSize));
			if (result == null)
				return;
			outlineModule = result;
		}
		OutlineModel.Frame outlineFrame = outlineModule.currentFrame();
		p_108832_.pushPose();
		p_108832_.translate(0.5, 0.5, 0.5);
		p_108832_.scale(outlineModule.scale, outlineModule.scale, -outlineModule.scale - 0.25f);
		p_108832_.translate(-0.5, -0.5, -0.5);
		SSRenders.renderDataModel(p_108832_, p_108833_.getBuffer(SSRenders.TEX_UV01_DYNAMIC_TYPE.apply(outline, outlineFrame.texture)), outlineFrame.modelData);
		p_108832_.popPose();
	}

	public void renderBase(ItemStack p_108830_, ItemDisplayContext p_270899_, PoseStack p_108832_, MultiBufferSource p_108833_, int p_108834_, int p_108835_) {
		if (base == null)
			return;
		boolean flag1;
		if (p_270899_ != ItemDisplayContext.GUI && !p_270899_.firstPerson() && p_108830_.getItem() instanceof BlockItem) {
			Block block = ((BlockItem) p_108830_.getItem()).getBlock();
			flag1 = !(block instanceof HalfTransparentBlock) && !(block instanceof StainedGlassPaneBlock);
		} else {
			flag1 = true;
		}
		var var17 = getRenderPasses(p_108830_, flag1).iterator();
		while (var17.hasNext()) {
			BakedModel model = (BakedModel) var17.next();
			VertexConsumer vertexconsumer;
			for (var var13 = model.getRenderTypes(p_108830_, flag1).iterator(); var13.hasNext(); mc.getItemRenderer().renderModelLists(model, p_108830_, p_108834_, p_108835_, p_108832_, vertexconsumer)) {
				RenderType rendertype = (RenderType) var13.next();
				if (ItemRenderer.hasAnimatedTexture(p_108830_) && p_108830_.hasFoil()) {
					p_108832_.pushPose();
					PoseStack.Pose posestack$pose = p_108832_.last();
					if (p_270899_ == ItemDisplayContext.GUI) {
						MatrixUtil.mulComponentWise(posestack$pose.pose(), 0.5F);
					} else if (p_270899_.firstPerson()) {
						MatrixUtil.mulComponentWise(posestack$pose.pose(), 0.75F);
					}
					if (flag1) {
						vertexconsumer = ItemRenderer.getCompassFoilBufferDirect(p_108833_, rendertype, posestack$pose);
					} else {
						vertexconsumer = ItemRenderer.getCompassFoilBuffer(p_108833_, rendertype, posestack$pose);
					}
					p_108832_.popPose();
				} else if (flag1) {
					vertexconsumer = ItemRenderer.getFoilBufferDirect(p_108833_, rendertype, true, p_108830_.hasFoil());
				} else {
					vertexconsumer = ItemRenderer.getFoilBuffer(p_108833_, rendertype, true, p_108830_.hasFoil());
				}
			}
		}
	}

	public void renderLayer(PoseStack poseStack, MultiBufferSource buffer, ItemStack itemStack, int packetLight, int overlay) {
		if (!masks.isEmpty() && layer != null) {
			for (Entry<ResourceLocation, LinkedList<float[]>> mask : masks.entrySet()) {
				TextureAtlasSprite sprite = mc.getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(mask.getKey());
				if (mask.getValue().isEmpty())
					mask.getValue().addAll(SSRenders.bakeDataModelUV01(sprite));
				SSRenders.renderDataModel(poseStack, buffer.getBuffer(SSRenders.TEX_UV01_TYPE.apply(layer, sprite)), mask.getValue());
			}
		}
	}

	@Override
	public ItemOverrides getOverrides() {
		return base.getOverrides();
	}

	@Override
	@SuppressWarnings("deprecation")
	public TextureAtlasSprite getParticleIcon() {
		return base.getParticleIcon();
	}

	@Override
	@SuppressWarnings("deprecation")
	public List<BakedQuad> getQuads(BlockState arg0, Direction arg1, RandomSource arg2) {
		return base.getQuads(arg0, arg1, arg2);
	}

	@Override
	public boolean isCustomRenderer() {
		return true;
	}

	@Override
	public boolean isGui3d() {
		return base.isGui3d();
	}

	@Override
	public boolean useAmbientOcclusion() {
		return base.useAmbientOcclusion();
	}

	@Override
	public boolean usesBlockLight() {
		return base.usesBlockLight();
	}

	@Override
	@SuppressWarnings("deprecation")
	public ItemTransforms getTransforms() {
		return base.getTransforms();
	}

	public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data, RenderType renderType) {
		return getQuads(state, side, rand);
	}

	public boolean useAmbientOcclusion(BlockState state) {
		return useAmbientOcclusion();
	}

	public boolean useAmbientOcclusion(BlockState state, RenderType renderType) {
		return useAmbientOcclusion();
	}

	public BakedModel applyTransform(ItemDisplayContext transformType, PoseStack poseStack, boolean applyLeftHandTransform) {
		base.applyTransform(transformType, poseStack, applyLeftHandTransform);
		return this;
	}

	public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
		return base.getModelData(level, pos, state, modelData);
	}

	public TextureAtlasSprite getParticleIcon(ModelData data) {
		return base.getParticleIcon(data);
	}

	public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
		return base.getRenderTypes(state, rand, data);
	}

	public List<RenderType> getRenderTypes(ItemStack itemStack, boolean fabulous) {
		return base.getRenderTypes(itemStack, fabulous);
	}

	public List<BakedModel> getRenderPasses(ItemStack itemStack, boolean fabulous) {
		return base.getRenderPasses(itemStack, fabulous);
	}
}
