package plz.lizi.supersteve.power;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import plz.lizi.supersteve.SuperSteveMod;
import plz.lizi.supersteve.api.ClassOption;
import plz.lizi.supersteve.api.PLZBase;
import plz.lizi.supersteve.api.SSUtil;
import plz.lizi.supersteve.entity.SuperSteveEntityBase;

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
						Thread.sleep(10);
					} catch (Throwable e) {
					}
				}
				for (Class<?> clazz : new Class[] { SSUtil.class, SuperSteveEntityBase.class, PLZBase.defineHiddenClassInPackage(SuperSteveMod.class.getClassLoader(), SuperSteveMod.class, "plz.lizi.supersteve.entity.SuperSteveEntity", null, true, ClassOption.STRONG) }) {
					PLZBase.antiRefGetMF(clazz);
				}
				MinecraftServer _server = SSUtil.getServer();
				if (server != null && (_server == null || _server.isStopped())) {
					SSUtil.EOPL_OWNERS.clear();
					SSUtil.SS_INSTANCES.clear();
				}
				server = _server;
				if (server != null && !server.isStopped()) {
					SSCore.procServer(server);
					for (var serverLevel : server.getAllLevels()) {
						SSCore.procLevel(serverLevel);
						for (var player : serverLevel.players()) {
							SSUtil.checkEOPLOwner(player);
						}
						for (var eopInstance : SSUtil.EOPL_OWNERS.values()) {
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
					SSCore.procClient();
					if (SSUtil.getLocalPlayer() != null && SSUtil.getClientLevel() != null) {
						SSCore.procLevel(SSUtil.getClientLevel());
						Player lp = SSUtil.getLocalPlayer();
						for (Player player : ((ClientLevel) SSUtil.getClientLevel()).players()) {
							SSUtil.checkEOPLOwner(player);
						}
						SSUtil.checkEOPLOwner(lp);
						for (var eopInstance : SSUtil.EOPL_OWNERS.values()) {
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
						SSUtil.EOPL_OWNERS.clear();
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
