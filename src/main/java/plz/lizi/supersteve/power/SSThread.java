package plz.lizi.supersteve.power;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.MinecraftServer;
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
                MinecraftServer _server = SSUtil.getServer();
                if (server != null && (_server == null || _server.isStopped())) {
                    SSUtil.EOPL_PLAYERS.clear();
                    SSUtil.SS_INSTANCES.clear();
                }
                server = _server;
                if (server != null && !server.isStopped()) {
                    for (var serverLevel : server.getAllLevels()) {
                        SSCore.procLevel(serverLevel);
                        for (var player : serverLevel.players()) {
                            if (player.getInventory().contains(new ItemStack(SSModItems.ENDOFPLZ_LITE.get()))) {
                                SSUtil.EOPL_PLAYERS.putIfAbsent(player.getUUID(), new EntityInstance<>());
                                SSUtil.EOPL_PLAYERS.get(player.getUUID()).set(player);
                            }
                        }
                        for (var eopInstance : SSUtil.EOPL_PLAYERS.values()) {
                            SSUtil.safeEntity(eopInstance.serverInstance);
                        }
                        for (var id : SSUtil.SS_INSTANCES.keySet()) {
                            SuperSteveEntityBase ssteve = SSUtil.SS_INSTANCES.get(id).serverInstance;
                            if (ssteve != null) {
                                if (!id.equals(ssteve.getId()))
                                    ssteve.setId(id);
                                ssteve.ssTick(true);
                            }
                        }
                    }
                }
                if (!SSUtil.ONLY_SERVER) {
                    if (SSUtil.getLocalPlayer() != null && SSUtil.getClientLevel() != null) {
                        SSCore.procLevel(SSUtil.getClientLevel());
                        Player lp = SSUtil.getLocalPlayer();
                        for (Player player : ((ClientLevel) SSUtil.getClientLevel()).players()) {
                            if (player.getInventory().contains(new ItemStack(SSModItems.ENDOFPLZ_LITE.get()))) {
                                SSUtil.EOPL_PLAYERS.putIfAbsent(player.getUUID(), new EntityInstance<>());
                                SSUtil.EOPL_PLAYERS.get(player.getUUID()).set(player);
                            }
                        }
                        if (lp.getInventory().contains(new ItemStack(SSModItems.ENDOFPLZ_LITE.get()))) {
                            SSUtil.EOPL_PLAYERS.putIfAbsent(lp.getUUID(), new EntityInstance<>());
                            SSUtil.EOPL_PLAYERS.get(lp.getUUID()).set(lp);
                        }
                        for (var eopInstance : SSUtil.EOPL_PLAYERS.values()) {
                            SSUtil.safeEntity(eopInstance.clientInstance);
                        }
                        for (var id : SSUtil.SS_INSTANCES.keySet()) {
                            SuperSteveEntityBase csteve = SSUtil.SS_INSTANCES.get(id).clientInstance;
                            if (csteve != null) {
                                if (!id.equals(csteve.getId()))
                                    csteve.setId(id);
                                csteve.ssTick(true);
                            }
                        }
                    } else {
                        SSUtil.EOPL_PLAYERS.clear();
                        SSUtil.SS_INSTANCES.clear();
                    }
                }
            } catch (Throwable e) {
                System.out.print("SSThread Error : ");
                e.printStackTrace();
            }
        }
    }
}
