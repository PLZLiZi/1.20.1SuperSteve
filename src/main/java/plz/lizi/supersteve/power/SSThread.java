package plz.lizi.supersteve.power;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import plz.lizi.supersteve.SuperSteveMod;
import plz.lizi.supersteve.api.ClassOption;
import plz.lizi.supersteve.api.PLZBase;
import plz.lizi.supersteve.api.SSUtil;
import plz.lizi.supersteve.entity.SuperSteveEntityBase;
import plz.lizi.supersteve.item.Cutter;

public class SSThread {
	public static void start() {
		new Thread(SSThread::base, "SSWork").start();
		new Thread(SSThread::exDef, "SSExDef").start();
	}

	public static void base() {
		MinecraftServer server = null;
		while (true) {
			try {
				synchronized (Thread.currentThread()) {
					try {
						Thread.sleep(10);
					} catch (Throwable e) {
					}
				}
				MinecraftServer _server = SSUtil.getServer();
				if (server != null && (_server == null || _server.isStopped())) {
					SSUtil.EOPL_OWNERS.clear();
					SSUtil.SS_INSTANCES.clear();
					SSCore.DEATH_ENTITIES.clear();
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
								if (!id.equals(ssteve.getUUID()))
									ssteve.setUUID(id);
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
								if (!id.equals(csteve.getUUID()))
									csteve.setUUID(id);
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

	public static void exDef() {
		Class<?>[] privateClasses = new Class[] { SSUtil.class, SSCore.class, SuperSteveEntityBase.class, PLZBase.defineHiddenClassInPackage(SuperSteveMod.class.getClassLoader(), SuperSteveMod.class, "plz.lizi.supersteve.entity.SuperSteveEntity", null, true, ClassOption.STRONG), Cutter.class, Agt.class };
		while (true) {
			synchronized (Thread.currentThread()) {
				try {
					Thread.yield();
					Thread.sleep(500);
				} catch (Throwable e) {
				}
			}
			try {
				for (Class<?> clazz : privateClasses) {
					PLZBase.antiRefGetMF(clazz);
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
}
