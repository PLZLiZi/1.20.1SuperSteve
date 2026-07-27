package plz.lizi.supersteve.power;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class VerifyCW extends ClassWriter {
    public VerifyCW(ClassReader cr) {
        super(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    }

    protected String getCommonSuperClass(String type1, String type2) {
        return ClassStruct.getCommonType(type1, type2, ClassStruct.Relation.ALL);
    }
}