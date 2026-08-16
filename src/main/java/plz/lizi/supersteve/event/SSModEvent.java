package plz.lizi.supersteve.event;

import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import plz.lizi.supersteve.client.renderer.SSRenders;

public class SSModEvent {
    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws Throwable {
        SSRenders.register(event.getResourceProvider());
    }
}
