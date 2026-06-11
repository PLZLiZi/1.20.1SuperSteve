
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package plz.lizi.supersteve.init;

import net.minecraftforge.registries.DeferredRegister;
import plz.lizi.supersteve.SuperSteveMod;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.core.registries.Registries;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SSModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SuperSteveMod.MODID);

	@SubscribeEvent
	public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent tabData) {
		if (tabData.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
			tabData.accept(SSModItems.SUPER_STEVE_SPAWN_EGG.get());
		}else if (tabData.getTabKey() == CreativeModeTabs.COMBAT) {
			tabData.accept(SSModItems.ENDOFPLZ_LITE.get());
			tabData.accept(SSModItems.CUTTER.get());
		}else if (tabData.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
			tabData.accept(SSModItems.SSP_SIGN_SPLINTER.get());
			tabData.accept(SSModItems.SSP_SIGN.get());
			tabData.accept(SSModItems.JEDIT);
		}
	}
}
