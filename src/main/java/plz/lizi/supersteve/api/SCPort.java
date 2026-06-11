package plz.lizi.supersteve.api;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public enum SCPort {
    SERVER, CLIENT, UNKNOW;

    public static SCPort of(Level level) {
        if (level instanceof ServerLevel)
            return SERVER;
        if (level instanceof ClientLevel)
            return CLIENT;
        return UNKNOW;
    }
}
