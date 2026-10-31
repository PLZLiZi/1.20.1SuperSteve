package plz.lizi.supersteve.level;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import plz.lizi.supersteve.SuperSteveMod;

public class SSSaveData extends SavedData {
	public static final String DATA_NAME = SuperSteveMod.MODID;
	private ListTag ssList = new ListTag();

	public SSSaveData() {}

	public static SSSaveData load(CompoundTag nbt) {
		SSSaveData data = new SSSaveData();
		data.ssList = nbt.getList("ssList", 10);
		return data;
	}

	public static SSSaveData get(ServerLevel level) {
		return level.getServer().overworld().getDataStorage().computeIfAbsent(SSSaveData::load, SSSaveData::new, DATA_NAME);
	}

	@Override
	public CompoundTag save(CompoundTag nbt) {
		nbt.put("ssList", ssList);
		return nbt;
	}

	public ListTag getSSList() {
		return ssList;
	}

	public void setSSList(ListTag list) {
		this.ssList = list == null ? new ListTag() : list;
		this.setDirty();
	}
}