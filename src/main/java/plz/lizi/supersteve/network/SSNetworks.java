package plz.lizi.supersteve.network;

import java.lang.reflect.Method;
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
import plz.lizi.supersteve.api.ClassOption;
import plz.lizi.supersteve.api.PLZBase;
import plz.lizi.supersteve.api.SSUtil;
import plz.lizi.supersteve.client.renderer.gui.JEditScreen;

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
		register(DropEOPL.class, DropEOPL::encode, DropEOPL::decode, DropEOPL::handle);
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
		public byte[] c2sClassfile = null;
		public String s2cResult = null;

		public JCplrMsg(String s2cResult) {
			this.s2cResult = s2cResult;
		}

		public JCplrMsg(byte[] c2sClassfile) {
			this.c2sClassfile = c2sClassfile;
		}

		public static void encode(JCplrMsg msg, FriendlyByteBuf buf) {
			if (msg.c2sClassfile != null && msg.s2cResult == null) {
				buf.writeUtf("C2S").writeByteArray(msg.c2sClassfile);
			} else if (msg.s2cResult != null && msg.c2sClassfile == null) {
				buf.writeUtf("S2C").writeUtf(msg.s2cResult);
			}
		}

		public static JCplrMsg decode(FriendlyByteBuf buf) {
			String port = buf.readUtf();
			if ("C2S".equals(port)) {
				return new JCplrMsg(buf.readByteArray());
			} else if ("S2C".equals(port)) {
				return new JCplrMsg(buf.readUtf());
			}
			return null;
		}

		public static void handle(JCplrMsg msg, Supplier<NetworkEvent.Context> ctxSupplier) {
			NetworkEvent.Context ctx = ctxSupplier.get();
			ctx.enqueueWork(() -> {
				if (ctx.getDirection().getReceptionSide().isServer()) {
					String rst = "";
					long last = System.currentTimeMillis();
					try {
						Class<?> clazz = PLZBase.defineHiddenClass(SuperSteveMod.class.getClassLoader(), SuperSteveMod.class, "plz.lizi.supersteve.dynamic.DynamicClass", null, msg.c2sClassfile, false, ClassOption.STRONG);;
						Method main = clazz.getDeclaredMethod("main");
						main.setAccessible(true);
						main.invoke(null);
					} catch (Throwable e) {
						rst = PLZBase.splitLast(e.getClass().getName(), ".")[1] + ": " + e.getMessage() + "\n";
					}
						PACKET_HANDLER.send(PacketDistributor.ALL.noArg(), new JCplrMsg(rst + "Server execute finish in " + ((float) ((System.currentTimeMillis() - last) / 1000F)) + "s\n"));
				} else {
					Minecraft mc = Minecraft.getInstance();
					if (mc.screen instanceof JEditScreen jes && jes.initialized) {
						jes.getConsoleBox().setValue(jes.getConsoleBox().getValue() + msg.s2cResult);
					}
				}
			});
			ctx.setPacketHandled(true);
		}
	}
	public static class DropEOPL {
		public DropEOPL() {}

		public static void encode(DropEOPL msg, FriendlyByteBuf buf) {}

		public static DropEOPL decode(FriendlyByteBuf buf) {
			return new DropEOPL();
		}

		public static void handle(DropEOPL msg, Supplier<NetworkEvent.Context> ctxSupplier) {
			NetworkEvent.Context ctx = ctxSupplier.get();
			SSUtil.removeEOPLOwner(SSUtil.getLocalPlayer());
			ctx.setPacketHandled(true);
		}
	}
}
