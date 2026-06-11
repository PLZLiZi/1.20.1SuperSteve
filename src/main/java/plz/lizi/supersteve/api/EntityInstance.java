package plz.lizi.supersteve.api;

import java.util.List;
import net.minecraft.world.entity.Entity;

public class EntityInstance<E extends Entity> {
    public E serverInstance;
    public E clientInstance;

    public EntityInstance() {}

    public void put(E entity) {
        if (!entity.level().isClientSide()) {
            serverInstance = serverInstance != null ? serverInstance : entity;
        } else if (entity.level().isClientSide()) {
            clientInstance = clientInstance != null ? clientInstance : entity;
        }
    }

    public void update(E entity) {
        if (!entity.level().isClientSide()) {
            serverInstance = entity;
        } else if (entity.level().isClientSide()) {
            clientInstance = entity;
        }
    }

    public List<E> getEntities() {
        return List.of(serverInstance, clientInstance);
    }

    @Override
    public String toString() {
        return "Instance[client:" + clientInstance + "&server:" + serverInstance + "]";
    }
}
