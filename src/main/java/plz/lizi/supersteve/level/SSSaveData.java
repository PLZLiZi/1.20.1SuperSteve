package plz.lizi.supersteve.level;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public class SSSaveData extends SavedData {
    private int someValue = 0;

    public SSSaveData() {}

    public static SSSaveData load(CompoundTag nbt) {
        SSSaveData data = new SSSaveData();
        data.someValue = nbt.getInt("someValue");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        nbt.putInt("someValue", someValue);
        return nbt;
    }

    public void setSomeValue(int val) {
        this.someValue = val;
        this.setDirty();
    }

    public int getSomeValue() {
        return someValue;
    }
}
