/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package plz.lizi.supersteve.init;

import net.minecraftforge.fml.common.Mod;
import plz.lizi.supersteve.client.renderer.SuperSteveRenderer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.api.distmarker.Dist;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SSModEntityRenderers {

	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(SSModEntities.SUPER_STEVE.get(), SuperSteveRenderer::new);
	}
}
