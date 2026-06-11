package plz.lizi.supersteve.power;

import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import com.google.common.collect.Iterables;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.LevelEntityGetterAdapter;
import plz.lizi.supersteve.api.SCPort;
import plz.lizi.supersteve.api.SSUtil;

public class SSCore {
    public static final Map<SCPort, Set<LevelEntityGetter<Entity>>> GETTERS = Map.of(SCPort.SERVER, new HashSet<>(), SCPort.CLIENT, new HashSet<>(), SCPort.UNKNOW, new HashSet<>());

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
        GETTERS.get(SCPort.of(zhis)).add(zhis.getEntities());
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
                    }
                }
                var cw = new MyClassWriter(cr);
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
                var cw = new MyClassWriter(cr);
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
            var cw = new MyClassWriter(cr);
            cn.accept(cw);
            return cw.toByteArray();
        }, true);
    }
}
