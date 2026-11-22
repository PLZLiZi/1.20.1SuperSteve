package plz.lizi.supersteve.entity;

import net.minecraft.nbt.CompoundTag;

public interface INBTEntity {
    void addNeededData(CompoundTag tag);

    void readNeededData(CompoundTag tag);
}
