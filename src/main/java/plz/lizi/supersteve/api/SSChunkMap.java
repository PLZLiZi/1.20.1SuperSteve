package plz.lizi.supersteve.api;

import java.util.concurrent.Executor;
import java.util.function.Supplier;
import com.mojang.datafixers.DataFixer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;

public class SSChunkMap extends ChunkMap {
	public SSChunkMap(ServerLevel arg0, LevelStorageAccess arg1, DataFixer arg2, StructureTemplateManager arg3, Executor arg4, BlockableEventLoop<Runnable> arg5, LightChunkGetter arg6, ChunkGenerator arg7, ChunkProgressListener arg8, ChunkStatusUpdateListener arg9, Supplier<DimensionDataStorage> arg10, int arg11, boolean arg12) {
		super(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12);
	}
	
	@Override
	public void addEntity(Entity arg0) {
		try {
			super.addEntity(arg0);
		} catch (Throwable e) {
			//e.printStackTrace();
		}
	}
}
