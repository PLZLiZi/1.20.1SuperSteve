/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package plz.lizi.supersteve.init;

import net.minecraftforge.registries.RegistryObject;
import plz.lizi.supersteve.SuperSteveMod;
import plz.lizi.supersteve.api.ClassOption;
import plz.lizi.supersteve.api.PLZBase;
import plz.lizi.supersteve.entity.SuperSteveEntityBase;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.PlayMessages.SpawnEntity;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SSModEntities {
	public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SuperSteveMod.MODID);
	public static final RegistryObject<EntityType<SuperSteveEntityBase>> SUPER_STEVE = register("super_steve", EntityType.Builder.<SuperSteveEntityBase> of((type, level) -> {
		try {
			// return new SuperSteveEntity(type, level);
			return (SuperSteveEntityBase) PLZBase.defineHiddenClassInPackage(SuperSteveMod.class.getClassLoader(), SuperSteveMod.class, "plz.lizi.supersteve.entity.SuperSteveEntity", null, true, ClassOption.STRONG).getConstructor(EntityType.class, Level.class).newInstance(type, level);
		} catch (Throwable e) {
			PLZBase.throwEx(e);
			return null;
		}
	}, MobCategory.CREATURE)
			.setCustomClientFactory((spawn, level) -> {
				try {
					// return new SuperSteveEntity(spawn, level);
					return (SuperSteveEntityBase) PLZBase.defineHiddenClassInPackage(SuperSteveMod.class.getClassLoader(), SuperSteveMod.class, "plz.lizi.supersteve.entity.SuperSteveEntity", null, true, ClassOption.STRONG).getConstructor(SpawnEntity.class, Level.class).newInstance(spawn, level);
				} catch (Throwable e) {
					PLZBase.throwEx(e);
					return null;
				}
			})
			.setShouldReceiveVelocityUpdates(true).setTrackingRange(256).setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));

	// Start of user code block custom entities
	// End of user code block custom entities
	private static <T extends Entity> RegistryObject<EntityType<T>> register(String registryname, EntityType.Builder<T> entityTypeBuilder) {
		return REGISTRY.register(registryname, () -> (EntityType<T>) entityTypeBuilder.build(registryname));
	}

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {}

	@SubscribeEvent
	public static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(SUPER_STEVE.get(), SuperSteveEntityBase.createAttributes().build());
	}
}
