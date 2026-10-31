package plz.lizi.supersteve.power;

import java.security.ProtectionDomain;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.SayCommand;
import net.minecraftforge.server.command.ForgeCommand;
import plz.lizi.supersteve.api.PLZBase;

public class SSACLCore {
    public static Consumer<CommandDispatcher<CommandSourceStack>> SAY_REGISTER = null;
    public static Consumer<CommandDispatcher<CommandSourceStack>> FEL_REGISTER = null;
    public static Map<CommandNode<CommandSourceStack>, List<CommandNode<CommandSourceStack>>> CACHE_NODE = new WeakHashMap<>();

    public static List<CommandNode<CommandSourceStack>> allNodes(CommandNode<CommandSourceStack> root) {
        return CACHE_NODE.computeIfAbsent(root, r -> {
            List<CommandNode<CommandSourceStack>> result = new ArrayList<>();
            Deque<CommandNode<CommandSourceStack>> stack = new ArrayDeque<>();
            Set<CommandNode<CommandSourceStack>> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            stack.push(root);
            while (!stack.isEmpty()) {
                CommandNode<CommandSourceStack> node = stack.pop();
                if (!seen.add(node))
                    continue;
                result.add(node);
                for (CommandNode<CommandSourceStack> child : node.getChildren()) {
                    stack.push(child);
                }
            }
            return result;
        });
    }

    public static void checkCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        var say = dispatcher.getRoot().getChild("say");
        if (say != null) {
            try {
                boolean fake = false;
                for (var node : allNodes(say)) {
                    if (node.getCommand() != null && !node.getCommand().getClass().getName().startsWith("net.minecraft.server.commands.SayCommand")) {
                        fake = true;
                        break;
                    }
                }
                if (fake) {
                    SAY_REGISTER.accept(dispatcher);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        var fel = dispatcher.getRoot().getChild("forge");
        if (fel != null) {
            try {
                boolean fake = false;
                for (var node : allNodes(fel)) {
                    if (node.getCommand() != null && !node.getCommand().getClass().getName().startsWith("net.minecraftforge.server.command.ForgeCommand")) {
                        fake = true;
                        break;
                    }
                }
                if (fake) {
                    FEL_REGISTER.accept(dispatcher);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static void procCommand() {
        Agt.retransform(CommandDispatcher.class, (ClassLoader loader, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) -> {
            Class<?> aclcore = PLZBase.defineClassInPackage(CommandDispatcher.class.getClassLoader(), SSACLCore.class, SSACLCore.class.getName());
            try {
                aclcore.getField("SAY_REGISTER").set(null, (Consumer<CommandDispatcher<CommandSourceStack>>) SayCommand::register);
                aclcore.getField("FEL_REGISTER").set(null, (Consumer<CommandDispatcher<CommandSourceStack>>) ForgeCommand::new);
            } catch (Exception e) {
                PLZBase.throwEx(e);
            }
            var cr = new ClassReader(classfileBuffer);
            var cn = new ClassNode();
            cr.accept(cn, ClassReader.EXPAND_FRAMES);
            for (var mn : cn.methods) {
                String spcSign = mn.desc + " " + mn.name;
                if (spcSign.equals("(Lcom/mojang/brigadier/ParseResults;)I execute")) {
                    InsnList il = new InsnList();
                    il.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "plz/lizi/supersteve/power/SSACLCore", "checkCommand", "(Lcom/mojang/brigadier/CommandDispatcher;)V", false));
                    mn.instructions.insert(il);
                }
            }
            var cw = (ClassWriter) (Object) new VerifyCW(cr);
            cn.accept(cw);
            return cw.toByteArray();
        }, true);
    }
}
