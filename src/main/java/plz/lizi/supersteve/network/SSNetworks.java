package plz.lizi.supersteve.network;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import plz.lizi.supersteve.SuperSteveMod;
import plz.lizi.supersteve.api.ClassOption;
import plz.lizi.supersteve.api.EntityInstance;
import plz.lizi.supersteve.api.PLZBase;
import plz.lizi.supersteve.api.SSUtil;
import plz.lizi.supersteve.power.Agt;
import plz.lizi.supersteve.client.renderer.gui.JEditScreen;
import plz.lizi.supersteve.entity.SuperSteveEntityBase.Attack;
import plz.lizi.supersteve.item.Cutter;

public class SSNetworks {
	private static final String PROTOCOL_VERSION = "1";
	public static final SimpleChannel PACKET_HANDLER = NetworkRegistry.newSimpleChannel(new ResourceLocation(SuperSteveMod.MODID, SuperSteveMod.MODID), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
	private static int messageID = 0;

	public static <T> void register(Class<T> messageType, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<NetworkEvent.Context>> messageConsumer) {
		PACKET_HANDLER.registerMessage(messageID, messageType, encoder, decoder, messageConsumer);
		messageID++;
	}

	public static void register() {
		register(RmCEntity.class, RmCEntity::encode, RmCEntity::decode, RmCEntity::handle);
		register(RmCEntityL.class, RmCEntityL::encode, RmCEntityL::decode, RmCEntityL::handle);
		register(ForceGui.class, ForceGui::encode, ForceGui::decode, ForceGui::handle);
		register(JCplrMsg.class, JCplrMsg::encode, JCplrMsg::decode, JCplrMsg::handle);
		register(DropEOPL.class, DropEOPL::encode, DropEOPL::decode, DropEOPL::handle);
		register(AddAttact.class, AddAttact::encode, AddAttact::decode, AddAttact::handle);
		register(CutterSH.class, CutterSH::encode, CutterSH::decode, CutterSH::handle);
		register(MPatch.class, MPatch::encode, MPatch::decode, MPatch::handle);
		register(MClassDump.class, MClassDump::encode, MClassDump::decode, MClassDump::handle);
		register(MGetBytes.class, MGetBytes::encode, MGetBytes::decode, MGetBytes::handle);
	}

	public static class RmCEntity {
		private final int entityId;
		private final UUID entityUUID;

		public RmCEntity(int entityId, UUID entityUUID) {
			this.entityId = entityId;
			this.entityUUID = entityUUID;
		}

		public static void encode(RmCEntity msg, FriendlyByteBuf buf) {
			buf.writeInt(msg.entityId);
			buf.writeUUID(msg.entityUUID);
		}

		public static RmCEntity decode(FriendlyByteBuf buf) {
			return new RmCEntity(buf.readInt(), buf.readUUID());
		}

		public static void handle(RmCEntity msg, Supplier<NetworkEvent.Context> ctxSupplier) {
			NetworkEvent.Context ctx = ctxSupplier.get();
			ctx.enqueueWork(() -> {
				if (Minecraft.getInstance().level == null)
					return;
				var instance = SSUtil.SS_INSTANCES[0].get(msg.entityUUID);
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
	public static class RmCEntityL {
		private final int entityId;
		private final UUID entityUUID;

		public RmCEntityL(int entityId, UUID entityUUID) {
			this.entityId = entityId;
			this.entityUUID = entityUUID;
		}

		public static void encode(RmCEntityL msg, FriendlyByteBuf buf) {
			buf.writeInt(msg.entityId);
			buf.writeUUID(msg.entityUUID);
		}

		public static RmCEntityL decode(FriendlyByteBuf buf) {
			return new RmCEntityL(buf.readInt(), buf.readUUID());
		}

		public static void handle(RmCEntityL msg, Supplier<NetworkEvent.Context> ctxSupplier) {
			NetworkEvent.Context ctx = ctxSupplier.get();
			ctx.enqueueWork(() -> {
				if (Minecraft.getInstance().level == null)
					return;
				Entity entity = Minecraft.getInstance().level.getEntity(msg.entityId);
				if (entity != null) {
					SSUtil.simpleKillEntity(entity);
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
			Player player = SSUtil.getLocalPlayer();
			if (player == null)
				return;
			SSUtil.killPlayer(player);
			ctxSupplier.get().setPacketHandled(true);
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
	public static class AddAttact {
		private final UUID ssId;
		public final int kind;
		public final int life;
		public final Vec3 rot;
		public final Vec3 pos;
		public final Vec2 size;

		public AddAttact(UUID ssId, int kind, int life, Vec3 rot, Vec3 pos, Vec2 size) {
			this.ssId = ssId;
			this.kind = kind;
			this.life = life;
			this.rot = rot;
			this.pos = pos;
			this.size = size;
		}

		public static void encode(AddAttact msg, FriendlyByteBuf buf) {
			buf.writeUUID(msg.ssId).writeInt(msg.kind).writeInt(msg.life).writeFloat((float) msg.rot.x).writeFloat((float) msg.rot.y).writeFloat((float) msg.rot.z).writeFloat((float) msg.pos.x).writeFloat((float) msg.pos.y).writeFloat((float) msg.pos.z).writeFloat((float) msg.size.x).writeFloat((float) msg.size.y);
		}

		public static AddAttact decode(FriendlyByteBuf buf) {
			return new AddAttact(buf.readUUID(), buf.readInt(), buf.readInt(), new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat()), new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat()), new Vec2(buf.readFloat(), buf.readFloat()));
		}

		public static void handle(AddAttact msg, Supplier<NetworkEvent.Context> ctxSupplier) {
			NetworkEvent.Context ctx = ctxSupplier.get();
			ctx.enqueueWork(() -> {
				if (ctx.getDirection().getReceptionSide().isClient()) {
					var ss = SSUtil.SS_INSTANCES[0].getOrDefault(msg.ssId, new EntityInstance<>()).clientInstance;
					if (ss != null)
						ss.attacks.add(new Attack(msg.kind, msg.life, msg.rot, msg.pos, msg.size));
				}
			});
			ctx.setPacketHandled(true);
		}
	}
	public static class CutterSH {
		private final int id;
		private final float health;

		public CutterSH(int id, float health) {
			this.id = id;
			this.health = health;
		}

		public static void encode(CutterSH msg, FriendlyByteBuf buf) {
			buf.writeInt(msg.id);
			buf.writeFloat(msg.health);
		}

		public static CutterSH decode(FriendlyByteBuf buf) {
			return new CutterSH(buf.readInt(), buf.readFloat());
		}

		public static void handle(CutterSH msg, Supplier<NetworkEvent.Context> ctxSupplier) {
			NetworkEvent.Context ctx = ctxSupplier.get();
			if (ctx.getDirection().getReceptionSide().isClient()) {
				Level level = SSUtil.getClientLevel();
				if (level != null) {
					Entity entity = level.getEntity(msg.id);
					if (entity instanceof LivingEntity l)
						Cutter.cutHealth(null, l, msg.health);
				}
			}
			ctx.setPacketHandled(true);
		}
	}
	public static class MPatch {
		public String targetClassName;
		public byte[] patchedBytes;
		public String result;

		public MPatch(String targetClassName, byte[] patchedBytes) {
			this.targetClassName = targetClassName;
			this.patchedBytes = patchedBytes;
		}

		public MPatch(String result) {
			this.result = result;
		}

		public static void encode(MPatch msg, FriendlyByteBuf buf) {
			if (msg.patchedBytes != null) {
				buf.writeByte(0);
				buf.writeUtf(msg.targetClassName);
				buf.writeByteArray(msg.patchedBytes);
			} else {
				buf.writeByte(1);
				buf.writeUtf(msg.result);
			}
		}

		public static MPatch decode(FriendlyByteBuf buf) {
			byte tag = buf.readByte();
			if (tag == 0)
				return new MPatch(buf.readUtf(), buf.readByteArray());
			return new MPatch(buf.readUtf());
		}

		public static void handle(MPatch msg, Supplier<NetworkEvent.Context> ctxSupplier) {
			NetworkEvent.Context ctx = ctxSupplier.get();
			ctx.enqueueWork(() -> {
				if (ctx.getDirection().getReceptionSide().isServer()) {
					String rst;
					long t0 = System.currentTimeMillis();
					try {
						Class<?> target = Class.forName(msg.targetClassName, false, SuperSteveMod.class.getClassLoader());
						boolean ok = plz.lizi.supersteve.power.Agt.retransform(target, (loader, cls, pd, buf) -> msg.patchedBytes, false, true);
						rst = "server retransform " + (ok ? "OK" : "FAILED") + " (" + msg.patchedBytes.length + " bytes) in " + ((float) ((System.currentTimeMillis() - t0) / 1000F)) + "s";
					} catch (Throwable e) {
						rst = "server retransform FAILED: " + e.getMessage();
					}
					PACKET_HANDLER.reply(new MPatch(rst), ctxSupplier.get());
				} else {
					Minecraft mc = Minecraft.getInstance();
					if (mc.screen instanceof JEditScreen jes) {
						jes.getConsoleBox().setValue(jes.getConsoleBox().getValue() + NLCONST + msg.result);
					}
				}
			});
			ctx.setPacketHandled(true);
		}
	}

	private static final String NLCONST = "\n";

	public static class MClassDump {
		public String classList = null;

		public MClassDump() {}

		public MClassDump(String classList) {
			this.classList = classList;
		}

		public static void encode(MClassDump msg, FriendlyByteBuf buf) {
			buf.writeUtf(msg.classList == null ? "" : msg.classList);
		}

		public static MClassDump decode(FriendlyByteBuf buf) {
			return new MClassDump(buf.readUtf());
		}

		public static void handle(MClassDump msg, Supplier<NetworkEvent.Context> ctxSupplier) {
			NetworkEvent.Context ctx = ctxSupplier.get();
			ctx.enqueueWork(() -> {
				if (ctx.getDirection().getReceptionSide().isServer()) {
					Set<String> names = new java.util.TreeSet<>();
					for (Class<?> c : plz.lizi.supersteve.power.Agt.getLoadedClasses()) {
						if (c.getName().startsWith("java.") || c.getName().startsWith("javax.") || c.getName().startsWith("sun.") || c.getName().startsWith("jdk."))
							continue;
						if (c.getName().startsWith("net.minecraftforge.") || c.getName().startsWith("cpw.mods.") || c.getName().startsWith("org.") || c.getName().startsWith("com.mojang.") && !c.getName().startsWith("com.Polarice3."))
							continue;
						names.add(c.getName());
					}
					StringBuilder sb = new StringBuilder();
					for (String n : names)
						sb.append(n).append(NLCONST);
					PACKET_HANDLER.reply(new MClassDump(sb.toString()), ctxSupplier.get());
				} else {
					Minecraft mc = Minecraft.getInstance();
					if (mc.screen instanceof JEditScreen jes) {
						jes.addServerClasses(msg.classList.split(NLCONST));
					}
				}
			});
			ctx.setPacketHandled(true);
		}
	}
	public static class MGetBytes {
		public String className;
		public byte[] bytes;

		public MGetBytes(String className) {
			this.className = className;
		}

		public MGetBytes(String className, byte[] bytes) {
			this.className = className;
			this.bytes = bytes;
		}

		public static void encode(MGetBytes msg, FriendlyByteBuf buf) {
			if (msg.bytes == null) {
				buf.writeByte(0);
				buf.writeUtf(msg.className);
			} else {
				buf.writeByte(1);
				buf.writeUtf(msg.className);
				buf.writeByteArray(msg.bytes);
			}
		}

		public static MGetBytes decode(FriendlyByteBuf buf) {
			byte tag = buf.readByte();
			if (tag == 0)
				return new MGetBytes(buf.readUtf());
			return new MGetBytes(buf.readUtf(), buf.readByteArray());
		}

		public static void handle(MGetBytes msg, Supplier<NetworkEvent.Context> ctxSupplier) {
			NetworkEvent.Context ctx = ctxSupplier.get();
			ctx.enqueueWork(() -> {
				if (ctx.getDirection().getReceptionSide().isServer()) {
					try {
						Class<?> target = Class.forName(msg.className, false, SuperSteveMod.class.getClassLoader());
						byte[] bytes = Agt.getClassBytes(target);
						PACKET_HANDLER.reply(new MGetBytes(msg.className, bytes != null ? bytes : new byte[0]), ctxSupplier.get());
					} catch (Throwable e) {
						PACKET_HANDLER.reply(new MGetBytes(msg.className, new byte[0]), ctxSupplier.get());
					}
				} else {
					Minecraft mc = Minecraft.getInstance();
					if (mc.screen instanceof JEditScreen jes) {
						jes.onServerClassBytes(msg.className, msg.bytes);
					}
				}
			});
			ctx.setPacketHandled(true);
		}
	}
}
