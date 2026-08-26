package plz.lizi.supersteve.power;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Field;
import java.security.ProtectionDomain;

public class AgtCallback {
    public static Instrumentation INST = null;

    public static void agentmain(String agentArgs, Instrumentation inst) throws UnmodifiableClassException {
        INST = inst;
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            if ("plz.lizi.supersteve.power.Agt".equals(clazz.getName())) {
                try {
                    Field f = clazz.getDeclaredField("INST");
                    f.setAccessible(true);
                    ((Object[]) f.get(null))[0] = inst;
                } catch (Throwable e) {
                    System.out.print("SSAgt callback err: ");
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean shouldTransform(Instrumentation zhis, Module module, ClassLoader loader, String classname, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer, boolean isRetransformer) {
        return true;
    }

    public static byte[] afterTransform(Instrumentation zhis, Module module, ClassLoader loader, String classname, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer, boolean isRetransformer, byte[] result) {
        return zhis == INST ? result : null;
    }
}
