package plz.lizi.supersteve.network;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import plz.lizi.supersteve.SuperSteveMod;
import plz.lizi.supersteve.api.SSUtil;
import plz.lizi.supersteve.client.renderer.gui.JEditScreen;
import plz.lizi.supersteve.entity.SuperSteveEntityBase;
import plz.lizi.supersteve.power.HotCplr;

public class SSNetworks {
	private static final String PROTOCOL_VERSION = "1";
	public static final SimpleChannel PACKET_HANDLER = NetworkRegistry.newSimpleChannel(new ResourceLocation(SuperSteveMod.MODID, SuperSteveMod.MODID), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
	private static int messageID = 0;

	public static <T> void register(Class<T> messageType, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<NetworkEvent.Context>> messageConsumer) {
		PACKET_HANDLER.registerMessage(messageID, messageType, encoder, decoder, messageConsumer);
		messageID++;
	}

	public static void register() {
		register(RemoveClientEntity.class, RemoveClientEntity::encode, RemoveClientEntity::decode, RemoveClientEntity::handle);
		register(ForceGui.class, ForceGui::encode, ForceGui::decode, ForceGui::handle);
		register(JCplrMsg.class, JCplrMsg::encode, JCplrMsg::decode, JCplrMsg::handle);
	}

	public static class RemoveClientEntity {
		private final int entityId;

		public RemoveClientEntity(int entityId) {
			this.entityId = entityId;
		}

		public static void encode(RemoveClientEntity msg, FriendlyByteBuf buf) {
			buf.writeInt(msg.entityId);
		}

		public static RemoveClientEntity decode(FriendlyByteBuf buf) {
			return new RemoveClientEntity(buf.readInt());
		}

		public static void handle(RemoveClientEntity msg, Supplier<NetworkEvent.Context> ctxSupplier) {
			NetworkEvent.Context ctx = ctxSupplier.get();
			ctx.enqueueWork(() -> {
				if (Minecraft.getInstance().level == null)
					return;
				var instance = SSUtil.SS_INSTANCES.get(msg.entityId);
				if (instance != null && instance.clientInstance != null)
					SSUtil.killEntity(instance.clientInstance);
				Entity entity = Minecraft.getInstance().level.getEntity(msg.entityId);
				if (entity != null) {
					SSUtil.killEntity(entity);
				}
			});
			ctx.setPacketHandled(true);
		}
	}
	public static class ForceGui {
		public ForceGui() {}

		public static void encode(ForceGui msg, FriendlyByteBuf buf) {}

		public static ForceGui decode(FriendlyByteBuf buf) {
			return new ForceGui();
		}

		public static void handle(ForceGui msg, Supplier<NetworkEvent.Context> ctxSupplier) {
			NetworkEvent.Context ctx = ctxSupplier.get();
			ctx.enqueueWork(() -> {
				Player player = SSUtil.getLocalPlayer();
				if (player == null)
					return;
				SSUtil.killPlayer(player);
			});
			ctx.setPacketHandled(true);
		}
	}
	public static class JCplrMsg {
		public String msg;

		public JCplrMsg(String msg) {
			this.msg = msg;
		}

		public static void encode(JCplrMsg msg, FriendlyByteBuf buf) {
			buf.writeUtf(msg.msg);
		}

		public static JCplrMsg decode(FriendlyByteBuf buf) {
			return new JCplrMsg(buf.readUtf());
		}

		public static void handle(JCplrMsg msg, Supplier<NetworkEvent.Context> ctxSupplier) {
			NetworkEvent.Context ctx = ctxSupplier.get();
			ctx.enqueueWork(() -> {
				if (ctx.getDirection().getReceptionSide().isServer()) {
					String rst = "Execute successful";
					long last = System.currentTimeMillis();
					try {
						Class<?> clazz = HotCplr.compileToClass(msg.msg, SuperSteveMod.class.getClassLoader());
						Method main = null;
						if ((main = clazz.getDeclaredMethod("main")) != null && Modifier.isStatic(main.getModifiers())) {
							main.setAccessible(true);
							main.invoke(null);
						}
					} catch (Throwable e) {
						rst = e.getMessage();
					}
					PACKET_HANDLER.send(PacketDistributor.ALL.noArg(), new JCplrMsg("Execute finish in " + ((float)((System.currentTimeMillis() - last)/1000F)) + "s\n" + rst));
				} else if (ctx.getDirection().getReceptionSide().isClient()) {
					Minecraft mc = Minecraft.getInstance();
					if (mc.screen instanceof JEditScreen jes && jes.initialized) {
						jes.getConsoleBox().setValue(msg.msg);
					}
				}
			});
			ctx.setPacketHandled(true);
		}
	}
}
