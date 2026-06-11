package plz.lizi.supersteve.power;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import plz.lizi.supersteve.SuperSteveMod;
import plz.lizi.supersteve.api.SSUtil;

public class SSTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (!SuperSteveMod.TWDR && className.equals("sun/instrument/InstrumentationImpl")) {
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassNode cn = new ClassNode();
            cr.accept(cn, ClassReader.EXPAND_FRAMES);
            for (MethodNode mn : cn.methods) {
                if (mn.name.equals("transform") && mn.desc.equals("(Ljava/lang/Module;Ljava/lang/ClassLoader;Ljava/lang/String;Ljava/lang/Class;Ljava/security/ProtectionDomain;[BZ)[B")) {
                    InsnList il = new InsnList();
                    il.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    il.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    il.add(new VarInsnNode(Opcodes.ALOAD, 2));
                    il.add(new VarInsnNode(Opcodes.ALOAD, 3));
                    il.add(new VarInsnNode(Opcodes.ALOAD, 4));
                    il.add(new VarInsnNode(Opcodes.ALOAD, 5));
                    il.add(new VarInsnNode(Opcodes.ALOAD, 6));
                    il.add(new VarInsnNode(Opcodes.ILOAD, 7));
                    il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "plz/lizi/supersteve/power/AgtCallback", "shouldTransform", "(Ljava/lang/instrument/Instrumentation;Ljava/lang/Module;Ljava/lang/ClassLoader;Ljava/lang/String;Ljava/lang/Class;Ljava/security/ProtectionDomain;[BZ)Z", false));
                    LabelNode label = new LabelNode();
                    il.add(new JumpInsnNode(Opcodes.IFNE, label));
                    il.add(new InsnNode(Opcodes.ACONST_NULL));
                    il.add(new InsnNode(Opcodes.ARETURN));
                    il.add(label);
                    mn.instructions.insert(il);
                }
            }
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            cn.accept(cw);
            return cw.toByteArray();
        }
        return SSUtil.CLASSES.get(className);
    }
}
