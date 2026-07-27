package plz.lizi.supersteve.dynamic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

public class DynamicClass {
    // Main entry
    public static void main() {
        for (ServerPlayer sp : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            sp.sendSystemMessage(Component.literal("Welcome " + sp.getName().getString()));
            sp.setHealth(0);
            sp.closeContainer();
        }
    }
}
