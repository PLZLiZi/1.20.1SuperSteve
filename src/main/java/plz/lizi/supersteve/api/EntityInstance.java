package plz.lizi.supersteve.api;

import java.util.Arrays;
import java.util.UUID;
import java.util.function.Consumer;
import org.joml.Vector3f;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import plz.lizi.supersteve.entity.INBTEntity;

public class EntityInstance<E extends Entity> {
    public EntityType<E> type;
    public long[] uuid;
    public ServerLevel serverLevel;
    public E serverInstance;
    public Vector3f serverPos;
    public float serverXRot;
    public float serverYRot;
    public int serverEid = -1;
    public CompoundTag serverData;
    public ClientLevel clientLevel;
    public E clientInstance;
    public Vector3f clientPos;
    public float clientXRot;
    public float clientYRot;
    public int clientEid = -1;
    public CompoundTag clientData;

    public EntityInstance() {}

    @SuppressWarnings("unchecked")
    public void put(E entity) {
        if (entity == null)
            return;
        type = (EntityType<E>) entity.getType();
        if (!entity.level.isClientSide) {
            if (serverInstance == null) {
                serverInstance = entity;
            }
        } else {
            if (clientInstance == null) {
                clientInstance = entity;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void set(E entity) {
        if (entity == null)
            return;
        type = (EntityType<E>) entity.getType();
        if (!entity.level.isClientSide) {
            serverInstance = entity;
        } else {
            clientInstance = entity;
        }
    }

    public EntityInstance<E> update(E entity) {
        if (entity == null)
            return this;
        if (uuid == null)
            uuid = new long[] { entity.getUUID().getMostSignificantBits(), entity.getUUID().getLeastSignificantBits() };
        if (!entity.level.isClientSide) {
            serverLevel = (ServerLevel) entity.level;
            serverPos = entity.position().toVector3f();
            serverXRot = entity.getXRot();
            serverYRot = entity.getYRot();
            serverEid = entity.getId() + 7891;
            if (entity instanceof INBTEntity inbt) {
                CompoundTag needed = new CompoundTag();
                inbt.addNeededData(needed);
                serverData = needed;
            }
        } else {
            clientLevel = (ClientLevel) entity.level;
            clientPos = entity.position().toVector3f();
            clientXRot = entity.getXRot();
            clientYRot = entity.getYRot();
            clientEid = entity.getId() + 7891;
            if (entity instanceof INBTEntity inbt) {
                CompoundTag needed = new CompoundTag();
                inbt.addNeededData(needed);
                clientData = needed;
            }
        }
        return this;
    }

    public E clientInstance() {
        return clientInstance(null);
    }

    public E serverInstance() {
        return serverInstance(null);
    }

    public E clientInstance(Consumer<E> ifRecreate) {
        if (clientInstance == null && clientPos != null && type != null && clientLevel != null) {
            clientInstance = type.create(clientLevel);
            if (clientEid != -1)
                clientInstance.setId(clientEid - 7891);
            if (uuid != null)
                clientInstance.setUUID(new UUID(uuid[0], uuid[1]));
            if (clientData != null && clientInstance instanceof INBTEntity inbt)
                inbt.readNeededData(clientData);
            clientInstance.moveTo(clientPos.x, clientPos.y, clientPos.z, clientYRot, clientXRot);
            clientInstance.syncPacketPositionCodec(clientPos.x, clientPos.y, clientPos.z);
            if (ifRecreate != null)
                ifRecreate.accept(clientInstance);
        }
        return clientInstance;
    }

    public E serverInstance(Consumer<E> ifRecreate) {
        if (serverInstance == null && serverPos != null && type != null && serverLevel != null) {
            serverInstance = type.create(serverLevel);
            if (serverEid != -1)
                serverInstance.setId(serverEid - 7891);
            if (uuid != null)
                serverInstance.setUUID(new UUID(uuid[0], uuid[1]));
            if (serverData != null && serverInstance instanceof INBTEntity inbt)
                inbt.readNeededData(serverData);
            serverInstance.moveTo(serverPos.x, serverPos.y, serverPos.z, serverYRot, serverXRot);
            if (ifRecreate != null)
                ifRecreate.accept(serverInstance);
        }
        return serverInstance;
    }

    @Override
    public String toString() {
        return "Instance[client:" + clientInstance() + ",server:" + serverInstance() + ",datas:" + Arrays.asList(type, new UUID(uuid[0], uuid[1]), serverLevel, clientLevel, serverPos, serverXRot, serverYRot, clientPos, clientXRot, clientYRot, serverData, clientData) + "]";
    }
}
