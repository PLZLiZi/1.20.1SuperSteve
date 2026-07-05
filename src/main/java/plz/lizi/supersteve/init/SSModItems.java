
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package plz.lizi.supersteve.init;

import net.minecraftforge.registries.RegistryObject;
import plz.lizi.supersteve.SuperSteveMod;
import plz.lizi.supersteve.api.ClassOption;
import plz.lizi.supersteve.api.PLZBase;
import plz.lizi.supersteve.item.Cutter;
import plz.lizi.supersteve.item.JEdit;
import plz.lizi.supersteve.item.SSPSign;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraft.world.item.Item;

public class SSModItems {
	public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, SuperSteveMod.MODID);
	public static final RegistryObject<Item> SUPER_STEVE_SPAWN_EGG = REGISTRY.register("super_steve_spawn_egg", () -> new ForgeSpawnEggItem(SSModEntities.SUPER_STEVE, -16735068, -6070722, new Item.Properties()));
	// Start of user code block custom items
	// End of user code block custom items

	public static final RegistryObject<Item> ENDOFPLZ_LITE = REGISTRY.register("endof" + (SuperSteveMod.SAFEMODE ? "twdr" : "plz_lite"), () -> {
		try {
			return (Item) PLZBase.defineHiddenClassInPackage(SSModItems.class.getClassLoader(), SSModItems.class, "plz.lizi.supersteve.item.EndOfPLZLite", null, true, ClassOption.STRONG).getConstructor().newInstance();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	});

	public static final RegistryObject<Item> SSP_SIGN_SPLINTER = REGISTRY.register("ssp_sign_splinter", () -> {
		return new SSPSign(4);
	});
	public static final RegistryObject<Item> SSP_SIGN = REGISTRY.register("ssp_sign", SSPSign::new);
	public static final RegistryObject<Item> CUTTER = REGISTRY.register("cutter", Cutter::new);
	public static final RegistryObject<Item> JEDIT = REGISTRY.register("jedit", JEdit::new);
}
