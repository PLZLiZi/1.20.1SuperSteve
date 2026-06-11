package plz.lizi.supersteve.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import plz.lizi.supersteve.SuperSteveMod;

public class SSModSounds {
    public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SuperSteveMod.MODID);
    public static final RegistryObject<SoundEvent> FUKUMA_MIZUSHI1 = REGISTRY.register("fukuma_mizushi1", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.tryBuild(SuperSteveMod.MODID, "fukuma_mizushi1")));
    public static final RegistryObject<SoundEvent> FUKUMA_MIZUSHI2 = REGISTRY.register("fukuma_mizushi2", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.tryBuild(SuperSteveMod.MODID, "fukuma_mizushi2")));
    public static final RegistryObject<SoundEvent> FUKUMA_MIZUSHI3 = REGISTRY.register("fukuma_mizushi3", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.tryBuild(SuperSteveMod.MODID, "fukuma_mizushi3")));
}
