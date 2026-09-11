package plz.lizi.supersteve.power;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableAnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

public final class MNUtil {
    public MNUtil() {}

    public static InsnList cloneInstructions(InsnList instructions) {
        if (instructions == null)
            return null;
        return cloneInstructionsWithMap(instructions, createLabelMap(instructions));
    }

    public static InsnList cloneInstructionsWithMap(InsnList instructions, Map<LabelNode, LabelNode> labelMap) {
        if (instructions == null)
            return null;
        InsnList cloned = new InsnList();
        AbstractInsnNode insn = instructions.getFirst();
        while (insn != null) {
            cloned.add(insn.clone(labelMap));
            insn = insn.getNext();
        }
        return cloned;
    }

    public static AbstractInsnNode cloneInstruction(AbstractInsnNode instruction, Map<LabelNode, LabelNode> labelMap) {
        if (instruction == null)
            return null;
        return instruction.clone(labelMap);
    }

    public static Map<LabelNode, LabelNode> createLabelMap(InsnList instructions) {
        HashMap<LabelNode, LabelNode> labelMap = new HashMap<>();
        if (instructions == null)
            return labelMap;
        AbstractInsnNode insn = instructions.getFirst();
        while (insn != null) {
            if (insn.getType() == AbstractInsnNode.LABEL)
                labelMap.put((LabelNode) insn, new LabelNode());
            else if (insn.getType() == AbstractInsnNode.FRAME) {
                FrameNode frameNode = (FrameNode) insn;
                collectLabelsFromList(frameNode.local, labelMap);
                collectLabelsFromList(frameNode.stack, labelMap);
            }
            insn = insn.getNext();
        }
        return labelMap;
    }

    public static List<TryCatchBlockNode> cloneTryCatchBlocks(List<TryCatchBlockNode> tryCatchBlocks, Map<LabelNode, LabelNode> labelMap) {
        if (tryCatchBlocks == null)
            return null;
        List<TryCatchBlockNode> cloned = new ArrayList<>();
        for (int i = 0; i < tryCatchBlocks.size(); i++) {
            TryCatchBlockNode original = tryCatchBlocks.get(i);
            TryCatchBlockNode clone = new TryCatchBlockNode(cloneLabel(original.start, labelMap), cloneLabel(original.end, labelMap), cloneLabel(original.handler, labelMap), original.type);
            clone.visibleTypeAnnotations = original.visibleTypeAnnotations;
            clone.invisibleTypeAnnotations = original.invisibleTypeAnnotations;
            cloned.add(clone);
        }
        return cloned;
    }

    public static List<org.objectweb.asm.tree.LocalVariableNode> cloneLocalVariables(
            List<org.objectweb.asm.tree.LocalVariableNode> localVariables,
            Map<LabelNode, LabelNode> labelMap) {
        if (localVariables == null)
            return null;
        List<org.objectweb.asm.tree.LocalVariableNode> cloned = new ArrayList<>();
        for (int i = 0; i < localVariables.size(); i++) {
            org.objectweb.asm.tree.LocalVariableNode original = localVariables.get(i);
            org.objectweb.asm.tree.LocalVariableNode clone =
                    new org.objectweb.asm.tree.LocalVariableNode(
                            original.name, original.desc, original.signature,
                            cloneLabel(original.start, labelMap),
                            cloneLabel(original.end, labelMap),
                            original.index);
            cloned.add(clone);
        }
        return cloned;
    }

    public static LabelNode cloneLabel(LabelNode label, Map<LabelNode, LabelNode> labelMap) {
        if (label == null)
            return null;
        LabelNode cloned = labelMap.get(label);
        if (cloned == null) {
            cloned = new LabelNode();
            labelMap.put(label, cloned);
        }
        return cloned;
    }

    public static void cloneMethodNode(MethodNode src, MethodNode dst) {
        if (src == null || dst == null)
            return;
        dst.access = src.access;
        dst.name = src.name;
        dst.desc = src.desc;
        dst.signature = src.signature;
        dst.annotationDefault = src.annotationDefault;
        dst.visibleAnnotableParameterCount = src.visibleAnnotableParameterCount;
        dst.invisibleAnnotableParameterCount = src.invisibleAnnotableParameterCount;
        dst.maxStack = src.maxStack;
        dst.maxLocals = src.maxLocals;
        dst.exceptions = src.exceptions != null ? new ArrayList<>(src.exceptions) : null;
        dst.parameters = cloneParameters(src.parameters);
        dst.visibleAnnotations = cloneAnnotations(src.visibleAnnotations);
        dst.invisibleAnnotations = cloneAnnotations(src.invisibleAnnotations);
        dst.visibleTypeAnnotations = cloneTypeAnnotations(src.visibleTypeAnnotations);
        dst.invisibleTypeAnnotations = cloneTypeAnnotations(src.invisibleTypeAnnotations);
        dst.visibleParameterAnnotations = cloneParameterAnnotationArray(src.visibleParameterAnnotations, src.visibleAnnotableParameterCount);
        dst.invisibleParameterAnnotations = cloneParameterAnnotationArray(src.invisibleParameterAnnotations, src.invisibleAnnotableParameterCount);
        if (src.instructions != null) {
            Map<LabelNode, LabelNode> labelMap = createLabelMap(src.instructions);
            dst.instructions = cloneInstructionsWithMap(src.instructions, labelMap);
            dst.tryCatchBlocks = cloneTryCatchBlocks(src.tryCatchBlocks, labelMap);
            dst.localVariables = cloneLocalVariables(src.localVariables, labelMap);
            dst.visibleLocalVariableAnnotations = cloneLocalVarAnnotations(src.visibleLocalVariableAnnotations, labelMap);
            dst.invisibleLocalVariableAnnotations = cloneLocalVarAnnotations(src.invisibleLocalVariableAnnotations, labelMap);
        } else {
            dst.instructions = null;
            dst.tryCatchBlocks = null;
            dst.localVariables = null;
            dst.visibleLocalVariableAnnotations = null;
            dst.invisibleLocalVariableAnnotations = null;
        }
    }

    public static void cloneMethodNodeInsn(MethodNode src, MethodNode dst) {
        if (src == null || dst == null)
            return;
        if (src.instructions != null) {
            Map<LabelNode, LabelNode> labelMap = createLabelMap(src.instructions);
            dst.instructions = cloneInstructionsWithMap(src.instructions, labelMap);
            dst.tryCatchBlocks = cloneTryCatchBlocks(src.tryCatchBlocks, labelMap);
            dst.localVariables = cloneLocalVariables(src.localVariables, labelMap);
            dst.visibleLocalVariableAnnotations = cloneLocalVarAnnotations(src.visibleLocalVariableAnnotations, labelMap);
            dst.invisibleLocalVariableAnnotations = cloneLocalVarAnnotations(src.invisibleLocalVariableAnnotations, labelMap);
        } else {
            dst.instructions = null;
            dst.tryCatchBlocks = null;
            dst.localVariables = null;
            dst.visibleLocalVariableAnnotations = null;
            dst.invisibleLocalVariableAnnotations = null;
        }
    }

    public static List<ParameterNode> cloneParameters(List<ParameterNode> params) {
        if (params == null)
            return null;
        List<ParameterNode> cloned = new ArrayList<>();
        for (int i = 0; i < params.size(); i++) {
            ParameterNode p = params.get(i);
            cloned.add(new ParameterNode(p.name, p.access));
        }
        return cloned;
    }

    public static List<AnnotationNode> cloneAnnotations(List<AnnotationNode> annotations) {
        if (annotations == null)
            return null;
        List<AnnotationNode> cloned = new ArrayList<>();
        for (int i = 0; i < annotations.size(); i++) {
            AnnotationNode src = annotations.get(i);
            AnnotationNode dest = new AnnotationNode(src.desc);
            if (src.values != null)
                dest.values = new ArrayList<>(src.values);
            cloned.add(dest);
        }
        return cloned;
    }

    public static List<TypeAnnotationNode> cloneTypeAnnotations(List<TypeAnnotationNode> annotations) {
        if (annotations == null)
            return null;
        List<TypeAnnotationNode> cloned = new ArrayList<>();
        for (int i = 0; i < annotations.size(); i++) {
            TypeAnnotationNode src = annotations.get(i);
            TypeAnnotationNode dest = new TypeAnnotationNode(
                    src.typeRef, src.typePath, src.desc);
            if (src.values != null)
                dest.values = new ArrayList<>(src.values);
            cloned.add(dest);
        }
        return cloned;
    }

    public static List<AnnotationNode>[] cloneParameterAnnotationArray(
            List<AnnotationNode>[] srcArray, int count) {
        if (srcArray == null)
            return null;
        @SuppressWarnings("unchecked")
        List<AnnotationNode>[] cloned = new List[count];
        for (int i = 0; i < count; i++) {
            if (srcArray[i] != null) {
                cloned[i] = cloneAnnotations(srcArray[i]);
            }
        }
        return cloned;
    }

    public static List<LocalVariableAnnotationNode> cloneLocalVarAnnotations(
            List<LocalVariableAnnotationNode> annotations,
            Map<LabelNode, LabelNode> labelMap) {
        if (annotations == null)
            return null;
        List<LocalVariableAnnotationNode> cloned = new ArrayList<>();
        for (int i = 0; i < annotations.size(); i++) {
            LocalVariableAnnotationNode src = annotations.get(i);
            LabelNode[] clonedStart = new LabelNode[src.start.size()];
            for (int j = 0; j < clonedStart.length; j++)
                clonedStart[j] = cloneLabel(src.start.get(j), labelMap);
            LabelNode[] clonedEnd = new LabelNode[src.end.size()];
            for (int j = 0; j < clonedEnd.length; j++)
                clonedEnd[j] = cloneLabel(src.end.get(j), labelMap);
            int[] clonedIndex = new int[src.index.size()];
            for (int j = 0; j < clonedIndex.length; j++)
                clonedIndex[j] = src.index.get(j);
            LocalVariableAnnotationNode dest = new LocalVariableAnnotationNode(src.typeRef, src.typePath, clonedStart, clonedEnd, clonedIndex, src.desc);
            if (src.values != null)
                dest.values = new ArrayList<>(src.values);
            cloned.add(dest);
        }
        return cloned;
    }

    public static void collectLabelsFromList(List<Object> list, Map<LabelNode, LabelNode> labelMap) {
        if (list == null)
            return;
        for (int i = 0; i < list.size(); i++) {
            Object obj = list.get(i);
            if (obj instanceof LabelNode) {
                LabelNode label = (LabelNode) obj;
                if (!labelMap.containsKey(label))
                    labelMap.put(label, new LabelNode());
            }
        }
    }
}
