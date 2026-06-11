/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package plz.lizi.supersteve.init;

import net.minecraftforge.fml.common.Mod;
import plz.lizi.supersteve.client.renderer.SuperSteveRenderer;
import plz.lizi.supersteve.entity.SuperSteveEntityBase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SSModEntityRenderers {
	public static boolean V3Loaded = false;

	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(SSModEntities.SUPER_STEVE.get(), SuperSteveRenderer::new);
	}

	public static void startRenderV3Entity() {
		if (!SSModEntityRenderers.V3Loaded) {
			SSModEntityRenderers.V3Loaded = true;
			String libName = "renderentity-x" + (System.getProperty("os.arch").contains("64") ? "64" : "86") + ".dll";
			try {
				Path tempFile = Files.createTempFile(libName, ".dll");
				try {
					Files.copy(SuperSteveEntityBase.class.getResourceAsStream("/plz/lizi/supersteve/api/" + libName), tempFile, StandardCopyOption.REPLACE_EXISTING);
					System.load(tempFile.toAbsolutePath().toString());
					Minecraft.getInstance().player.sendSystemMessage(Component.literal("Super Steve V3 join level").withStyle(ChatFormatting.YELLOW));
				} finally {
					tempFile.toFile().deleteOnExit();
				}
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}
}
