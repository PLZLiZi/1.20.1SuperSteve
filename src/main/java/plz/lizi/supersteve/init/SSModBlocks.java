package plz.lizi.supersteve.init;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import plz.lizi.supersteve.SuperSteveMod;

public class SSModBlocks {
    public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCKS, SuperSteveMod.MODID);
    //public static final RegistryObject<Block> TEST = REGISTRY.register("test", () -> new TestBlock());
}
