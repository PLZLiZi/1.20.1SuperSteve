package plz.lizi.supersteve.power;

import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import com.google.common.collect.Iterables;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.AbortableIterationConsumer.Continuation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.LevelEntityGetterAdapter;
import plz.lizi.supersteve.api.PLZBase;
import plz.lizi.supersteve.api.SCPort;
import plz.lizi.supersteve.api.SSUtil;
import plz.lizi.supersteve.entity.SuperSteveEntityBase;

public class SSCore {
    public static final Set<Class<?>> TSF_SERVERS = new CopyOnWriteArraySet<>();
    public static final Map<SuperSteveEntityBase, Long> SERVER_TICK_MANAGER = new WeakHashMap<>();
    public static final Map<SuperSteveEntityBase, Long> CLIENT_TICK_MANAGER = new WeakHashMap<>();
    public static final Map<SCPort, Set<LevelEntityGetter<Entity>>> GETTERS = Map.of(SCPort.SERVER, PLZBase.weakHashSet(), SCPort.CLIENT, PLZBase.weakHashSet(), SCPort.UNKNOW, PLZBase.weakHashSet());

    public static Iterable<Entity> getAllEntities(ServerLevel zhis) {
        GETTERS.get(SCPort.of(zhis)).add(zhis.getEntities());
        Set<Entity> val = StreamSupport.stream(zhis.getEntities().getAll().spliterator(), false).collect(Collectors.toSet());
        Set<Entity> sss = new HashSet<>();
        for (var istc : SSUtil.SS_INSTANCES.values()) {
            sss.add(istc.serverInstance);
        }
        sss.removeIf(Objects::isNull);
        val.addAll(sss);
        return Iterables.unmodifiableIterable(val);
    }

    public static Iterable<Entity> entitiesForRendering(ClientLevel zhis) {
        GETTERS.get(SCPort.of((Level) (Object) zhis)).add(zhis.getEntities());
        Set<Entity> val = StreamSupport.stream(zhis.getEntities().getAll().spliterator(), false).collect(Collectors.toSet());
        Set<Entity> sss = new HashSet<>();
        for (var istc : SSUtil.SS_INSTANCES.values()) {
            sss.add(istc.clientInstance);
        }
        sss.removeIf(Objects::isNull);
        val.addAll(sss);
        return Iterables.unmodifiableIterable(val);
    }

    public static Iterable<Entity> getAll(LevelEntityGetter<Entity> zhis) {
        Set<Entity> val = null;
        if (zhis instanceof LevelEntityGetterAdapter<Entity> lega) {
            val = StreamSupport.stream(lega.visibleEntities.byId.values().spliterator(), false).collect(Collectors.toSet());
        } else {
            val = new HashSet<>();
        }
        Set<Entity> sss = new HashSet<>();
        for (var istc : SSUtil.SS_INSTANCES.values()) {
            if (GETTERS.get(SCPort.SERVER).contains(zhis)) {
                sss.add(istc.serverInstance);
            } else if (GETTERS.get(SCPort.CLIENT).contains(zhis)) {
                sss.add(istc.clientInstance);
            }
        }
        sss.removeIf(Objects::isNull);
        val.addAll(sss);
        return Iterables.unmodifiableIterable(val);
    }

    public static <T extends Entity> void getEntities(ServerLevel zhis, EntityTypeTest<Entity, T> pTypeTest, Predicate<? super T> pPredicate, List<? super T> pOutput, int pMaxResults) {
        zhis.getEntities().get(pTypeTest, (p_261428_) -> {
            if (pPredicate.test(p_261428_)) {
                pOutput.add(p_261428_);
                if (pOutput.size() >= pMaxResults) {
                    return Continuation.ABORT;
                }
            }
            return Continuation.CONTINUE;
        });
        for (var instance : SSUtil.SS_INSTANCES.values()) {
            if (instance != null && instance.serverInstance != null && !pOutput.contains(instance.serverInstance)) {
                T t = pTypeTest.tryCast(instance.serverInstance);
                if (t != null && pPredicate.test(t))
                    pOutput.add(t);
            }
            if (pOutput.size() >= pMaxResults)
                break;
        }
    }

    public static void mcTickStart(Minecraft zhis, boolean pRenderLevel) {
        if (pRenderLevel) {
            long now = System.currentTimeMillis();
            for (var instance : SSUtil.SS_INSTANCES.values()) {
                if (instance.clientInstance != null && (now - CLIENT_TICK_MANAGER.getOrDefault(instance.clientInstance, now)) > 55 && instance.clientInstance.level instanceof ClientLevel cl) {
                    SSUtil.clientTickEntity(cl, instance.clientInstance);
                }
            }
        }
    }

    public static void mcTickEnd(Minecraft zhis, boolean pRenderLevel) {}

    public static void serverTickStart(MinecraftServer zhis, BooleanSupplier pHasTimeLeft) {
        if (zhis.getPlayerCount() > 0) {
            for (var instance : SSUtil.SS_INSTANCES.values()) {
                if (instance != null && instance.serverInstance != null)
                    SERVER_TICK_MANAGER.put(instance.serverInstance, 0L);
            }
        }
    }

    public static void serverTickEnd(MinecraftServer zhis, BooleanSupplier pHasTimeLeft) {
        if (SERVER_TICK_MANAGER.size() > 0) {
            for (var instance : SERVER_TICK_MANAGER.entrySet()) {
                if (instance.getValue() == 0 && instance.getKey().level instanceof ServerLevel sl)
                    SSUtil.serverTickEntity(sl, instance.getKey());
            }
            SERVER_TICK_MANAGER.clear();
        }
    }

    public static void procServer(MinecraftServer server) {
        if (TSF_SERVERS.contains(server.getClass()))
            return;
        for (var sc : SSUtil.classChain(server.getClass(), MinecraftServer.class)) {
            try {
                sc.getDeclaredMethod("m_5705_", BooleanSupplier.class);
                Agt.retransform(sc, (ClassLoader loader, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) -> {
                    var cr = new ClassReader(classfileBuffer);
                    var cn = new ClassNode();
                    cr.accept(cn, ClassReader.EXPAND_FRAMES);
                    for (var mn : cn.methods) {
                        String spcSign = mn.desc + " " + mn.name;
                        if (spcSign.equals("(Ljava/util/function/BooleanSupplier;)V m_5705_")) {
                            for (AbstractInsnNode insn : mn.instructions.toArray()) {
                                if (insn.getOpcode() == Opcodes.RETURN) {
                                    InsnList il = new InsnList();
                                    il.add(new VarInsnNode(Opcodes.ALOAD, 0));
                                    il.add(new VarInsnNode(Opcodes.ALOAD, 1));
                                    il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "plz/lizi/supersteve/power/SSCore", "serverTickEnd", "(Lnet/minecraft/server/MinecraftServer;Ljava/util/function/BooleanSupplier;)V", false));
                                    mn.instructions.insertBefore(insn, il);
                                }
                            }
                            InsnList il = new InsnList();
                            il.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            il.add(new VarInsnNode(Opcodes.ALOAD, 1));
                            il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "plz/lizi/supersteve/power/SSCore", "serverTickStart", "(Lnet/minecraft/server/MinecraftServer;Ljava/util/function/BooleanSupplier;)V", false));
                            mn.instructions.insert(il);
                        }
                    }
                    var cw = new VerifyCW(cr);
                    cn.accept(cw);
                    return cw.toByteArray();
                }, true);
                break;
            } catch (Throwable e) {
            }
        }
        TSF_SERVERS.add(server.getClass());
    }

    public static void procClient() {
        Agt.retransform(Minecraft.getInstance(), (ClassLoader loader, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) -> {
            var cr = new ClassReader(classfileBuffer);
            var cn = new ClassNode();
            cr.accept(cn, ClassReader.EXPAND_FRAMES);
            for (var mn : cn.methods) {
                String spcSign = mn.desc + " " + mn.name;
                if (spcSign.equals("(Z)V m_91383_")) {
                    for (AbstractInsnNode insn : mn.instructions.toArray()) {
                        if (insn.getOpcode() == Opcodes.RETURN) {
                            InsnList il = new InsnList();
                            il.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            il.add(new VarInsnNode(Opcodes.ILOAD, 1));
                            il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "plz/lizi/supersteve/power/SSCore", "mcTickEnd", "(Lnet/minecraft/client/Minecraft;Z)V", false));
                            mn.instructions.insertBefore(insn, il);
                        }
                    }
                    InsnList il = new InsnList();
                    il.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    il.add(new VarInsnNode(Opcodes.ILOAD, 1));
                    il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "plz/lizi/supersteve/power/SSCore", "mcTickStart", "(Lnet/minecraft/client/Minecraft;Z)V", false));
                    mn.instructions.insert(il);
                }
            }
            var cw = new VerifyCW(cr);
            cn.accept(cw);
            return cw.toByteArray();
        }, true);
    }

    public static void procLevel(Level level) {
        if (level == null)
            return;
        if (level instanceof ServerLevel serverLevel) {
            Agt.retransform(serverLevel.getClass(), (ClassLoader loader, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) -> {
                var cr = new ClassReader(classfileBuffer);
                var cn = new ClassNode();
                cr.accept(cn, ClassReader.EXPAND_FRAMES);
                for (var mn : cn.methods) {
                    String spcSign = mn.desc + " " + mn.name;
                    if (spcSign.equals("()Ljava/lang/Iterable; m_8583_")) {
                        InsnList il = new InsnList();
                        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "plz/lizi/supersteve/power/SSCore", "getAllEntities", "(Lnet/minecraft/server/level/ServerLevel;)Ljava/lang/Iterable;", false));
                        il.add(new InsnNode(Opcodes.ARETURN));
                        mn.instructions.clear();
                        mn.instructions.insert(il);
                        mn.localVariables = new ArrayList<>();
                        mn.tryCatchBlocks = new ArrayList<>();
                    } else if (spcSign.equals("(Lnet/minecraft/world/level/entity/EntityTypeTest;Ljava/util/function/Predicate;Ljava/util/List;I)V m_261178_")) {
                        InsnList il = new InsnList();
                        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        il.add(new VarInsnNode(Opcodes.ALOAD, 1));
                        il.add(new VarInsnNode(Opcodes.ALOAD, 2));
                        il.add(new VarInsnNode(Opcodes.ALOAD, 3));
                        il.add(new VarInsnNode(Opcodes.ILOAD, 4));
                        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "plz/lizi/supersteve/power/SSCore", "getEntities", "(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/entity/EntityTypeTest;Ljava/util/function/Predicate;Ljava/util/List;I)V", false));
                        il.add(new InsnNode(Opcodes.RETURN));
                        mn.instructions.clear();
                        mn.instructions.insert(il);
                        mn.localVariables = new ArrayList<>();
                        mn.tryCatchBlocks = new ArrayList<>();
                    }
                }
                var cw = new VerifyCW(cr);
                cn.accept(cw);
                return cw.toByteArray();
            }, true);
        } else if (level instanceof ClientLevel clientLevel) {
            Agt.retransform(clientLevel.getClass(), (ClassLoader loader, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) -> {
                var cr = new ClassReader(classfileBuffer);
                var cn = new ClassNode();
                cr.accept(cn, ClassReader.EXPAND_FRAMES);
                for (var mn : cn.methods) {
                    String spcSign = mn.desc + " " + mn.name;
                    if (spcSign.equals("()Ljava/lang/Iterable; m_104735_")) {
                        InsnList il = new InsnList();
                        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "plz/lizi/supersteve/power/SSCore", "entitiesForRendering", "(Lnet/minecraft/client/multiplayer/ClientLevel;)Ljava/lang/Iterable;", false));
                        il.add(new InsnNode(Opcodes.ARETURN));
                        mn.instructions.clear();
                        mn.instructions.insert(il);
                        mn.localVariables = new ArrayList<>();
                        mn.tryCatchBlocks = new ArrayList<>();
                    }
                }
                var cw = new VerifyCW(cr);
                cn.accept(cw);
                return cw.toByteArray();
            }, true);
        }
        GETTERS.get(SCPort.of(level)).add(level.getEntities());
        Agt.retransform(level.getEntities().getClass(), (ClassLoader loader, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) -> {
            var cr = new ClassReader(classfileBuffer);
            var cn = new ClassNode();
            cr.accept(cn, ClassReader.EXPAND_FRAMES);
            for (var mn : cn.methods) {
                String spcSign = mn.desc + " " + mn.name;
                if (spcSign.equals("()Ljava/lang/Iterable; m_142273_")) {
                    InsnList il = new InsnList();
                    il.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "plz/lizi/supersteve/power/SSCore", "getAll", "(Lnet/minecraft/world/level/entity/LevelEntityGetter;)Ljava/lang/Iterable;", false));
                    il.add(new InsnNode(Opcodes.ARETURN));
                    mn.instructions.clear();
                    mn.instructions.insert(il);
                    mn.localVariables = new ArrayList<>();
                    mn.tryCatchBlocks = new ArrayList<>();
                }
            }
            var cw = new VerifyCW(cr);
            cn.accept(cw);
            return cw.toByteArray();
        }, true);
    }
}
