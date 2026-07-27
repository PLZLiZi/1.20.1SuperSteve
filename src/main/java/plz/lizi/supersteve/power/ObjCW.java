package plz.lizi.supersteve.power;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class ObjCW extends ClassWriter {

    public ObjCW(ClassReader classReader) {
        super(classReader, COMPUTE_FRAMES | COMPUTE_MAXS);
    }
    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        if (type1.equals(type2))
            return type1;
        if ("java/lang/Object".equals(type1) || "java/lang/Object".equals(type2))
            return "java/lang/Object";
        try {
            return super.getCommonSuperClass(type1, type2);
        } catch (Throwable e) {
            return "java/lang/Object";
        }
    }
}
