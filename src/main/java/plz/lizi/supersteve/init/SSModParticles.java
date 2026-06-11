package plz.lizi.supersteve.init;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import plz.lizi.supersteve.SuperSteveMod;
import plz.lizi.supersteve.client.particle.AttackParticle;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SSModParticles {
	public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, SuperSteveMod.MODID);
	public static final RegistryObject<SimpleParticleType> ATTACK_PARTICLE = REGISTRY.register("attack", () -> new SimpleParticleType(true));

	@SubscribeEvent
	public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(ATTACK_PARTICLE.get(), AttackParticle.Provider::new);
		//event.registerSpecial(ATTACK_PARTICLE.get(), new AttackParticle.Provider());
	}
}
