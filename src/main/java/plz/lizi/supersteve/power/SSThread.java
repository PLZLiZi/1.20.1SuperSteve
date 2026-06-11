package plz.lizi.supersteve.power;

import java.util.HashSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import plz.lizi.supersteve.api.EntityInstance;
import plz.lizi.supersteve.api.SSUtil;
import plz.lizi.supersteve.entity.SuperSteveEntityBase;
import plz.lizi.supersteve.init.SSModItems;

public class SSThread {
    public static void start() {
        new Thread(SSThread::task, "SSThread").start();
    }

    public static void task() {
        MinecraftServer server = null;
        while (true) {
            try {
                synchronized (Thread.currentThread()) {
                    try {
                        Thread.sleep(1);
                    } catch (Throwable e) {
                    }
                }
                server = SSUtil.getServer();
                if (server != null && !server.isStopped()) {
                    for (var serverLevel : server.getAllLevels()) {
                        SSCore.procLevel(serverLevel);
                        for (var entity : serverLevel.getAllEntities()) {
                            if (entity instanceof ServerPlayer player) {
                                if (player.getInventory().contains(new ItemStack(SSModItems.ENDOFPLZ_LITE.get()))) {
                                    SSUtil.EOPL_PLAYERS.putIfAbsent(player.getUUID(), new EntityInstance<>());
                                    SSUtil.EOPL_PLAYERS.get(player.getUUID()).update(player);
                                    SSUtil.safeEntity(player);
                                }
                            } else if (entity instanceof SuperSteveEntityBase steve) {
                                steve.ssTick(true);
                            }
                        }
                    }
                } else {
                    SSUtil.EOPL_PLAYERS.clear();
                    SSUtil.SS_INSTANCES.clear();
                }
                if (SSUtil.getLocalPlayer() != null && SSUtil.getClientLevel() != null) {
                    SSCore.procLevel(SSUtil.getClientLevel());
                    Player lp = SSUtil.getLocalPlayer();
                    if (lp.getInventory().contains(new ItemStack(SSModItems.ENDOFPLZ_LITE.get()))) {
                        SSUtil.EOPL_PLAYERS.putIfAbsent(lp.getUUID(), new EntityInstance<>());
                        SSUtil.EOPL_PLAYERS.get(lp.getUUID()).update(lp);
                        SSUtil.safeEntity(lp);
                    }
                    for (var eopInstance : new HashSet<>(SSUtil.EOPL_PLAYERS.values())) {
                        SSUtil.safeEntity(eopInstance.clientInstance);
                        SSUtil.safeEntity(eopInstance.serverInstance);
                    }
                    for (var id : SSUtil.SS_INSTANCES.keySet()) {
                        var instance = SSUtil.SS_INSTANCES.get(id);
                        SuperSteveEntityBase csteve = instance.clientInstance;
                        if (csteve != null) {
                            if (!id.equals(csteve.getId()))
                                csteve.setId(id);
                            csteve.ssTick(true);
                        }
                        SuperSteveEntityBase ssteve = instance.serverInstance;
                        if (ssteve != null) {
                            if (!id.equals(ssteve.getId()))
                                ssteve.setId(id);
                            ssteve.ssTick(true);
                        }
                    }
                    for (var entity : ((ClientLevel) SSUtil.getClientLevel()).entitiesForRendering()) {
                        if (entity instanceof Player player) {
                            if (player.getInventory().contains(new ItemStack(SSModItems.ENDOFPLZ_LITE.get()))) {
                                SSUtil.EOPL_PLAYERS.putIfAbsent(player.getUUID(), new EntityInstance<>());
                                SSUtil.EOPL_PLAYERS.get(player.getUUID()).update(player);
                                SSUtil.safeEntity(player);
                            }
                        } else if (entity instanceof SuperSteveEntityBase superSteveEntity) {
                            superSteveEntity.ssTick(true);
                        }
                    }
                } else {
                    SSUtil.EOPL_PLAYERS.clear();
                    SSUtil.SS_INSTANCES.clear();
                }
            } catch (Throwable e) {
                System.out.print("SSThread Error : ");
                e.printStackTrace();
            }
        }
    }
}
