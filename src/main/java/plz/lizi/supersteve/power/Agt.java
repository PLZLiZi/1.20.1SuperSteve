package plz.lizi.supersteve.power;

import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import com.sun.jna.Function;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import plz.lizi.supersteve.api.PLZBase;

public class Agt {
    private static final AtomicReference<Instrumentation> INST = new AtomicReference<>();

    public static void start() {
        // if (!System.getProperty("os.name").toLowerCase().contains("win"))
        try {

            Class<?> agentClass = AgtCallback.class;
            Manifest manifest = new Manifest();
            Attributes mainAttributes = manifest.getMainAttributes();
            mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
            mainAttributes.put(new Attributes.Name("Agent-Class"), agentClass.getName());
            mainAttributes.put(new Attributes.Name("Can-Redefine-Classes"), "true");
            mainAttributes.put(new Attributes.Name("Can-Retransform-Classes"), "true");
            mainAttributes.put(new Attributes.Name("Can-Set-Native-Method-Prefix"), "true");
            Path jar = Files.createTempFile("ssagt", ".jar");
            jar.toFile().deleteOnExit();
            try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar.toAbsolutePath().toString()), manifest)) {
                jos.flush();
            }
            PLZBase.defineClassInPackage(null, Agt.class, agentClass.getName());
            Function JNI_GetCreatedJavaVMs = Function.getFunction("jvm", "JNI_GetCreatedJavaVMs");
            Pointer[] pJavaVMs = new Pointer[1];
            JNI_GetCreatedJavaVMs.invokeInt(new Object[] { pJavaVMs, 1, new IntByReference(1).getPointer() });
            Function.getFunction("instrument", "Agent_OnAttach").invokeInt(new Object[] { pJavaVMs[0], jar.toAbsolutePath().toString(), null });
            long time = System.currentTimeMillis();
            while (INST.get() == null) {
                if (System.currentTimeMillis() - time > 1000)
                    throw new TimeoutException("SSAgt time out");
            }
            INST.get().addTransformer(new Tsf(), true);
        } catch (Throwable e) {
            System.err.print("SSAgt load failed: ");
            e.printStackTrace();
            System.err.println("SuperSteve will not load with full mode");
        }
    }

    public static Instrumentation inst() {
        return INST.get();
    }

    public static synchronized boolean retransform(Object obj, EZTsf tsf, boolean once) {
        if (INST.get() == null)
            return false;
        Class<?> clazz = null;
        if (obj instanceof Class oc)
            clazz = oc;
        else if (obj != null)
            clazz = obj.getClass();
        if (clazz == null || clazz.isPrimitive() || clazz.isArray() || clazz.getName().contains("$Lambda$$"))
            return false;
        if (once && Tsf.TSFD_CLASSES.contains(clazz))
            return true;
        inst();
        String name = clazz.getName().replace("/", "+").replace(".", "/");
        if (tsf != null)
            Tsf.SUPPLIER.put(name, tsf);
        long klass = 0;
        int accessflags = 0;
        if (clazz.isHidden()) {
            klass = PLZBase.UNSAFE.getLong(clazz, 16L);
            accessflags = PLZBase.UNSAFE.getInt(klass + 164L);
            PLZBase.UNSAFE.putInt(klass + 164L, accessflags & 0xFBFFFFFF);
        }
        try {
            INST.get().retransformClasses(clazz);
            if (once)
                Tsf.TSFD_CLASSES.add(clazz);
        } catch (Throwable e) {
            System.err.print("SSAgt retransform " + clazz.getName() + " error: " + e.getMessage());
        }
        if (klass != 0)
            PLZBase.UNSAFE.putInt(klass + 164, accessflags);
        Tsf.SUPPLIER.remove(name);
        return true;
    }

    public static int watch(ClassFileTransformer tsf) {
        Tsf.TRANSFORMERS.add(tsf);
        return Tsf.TRANSFORMERS.indexOf(tsf);
    }

    public static ClassFileTransformer unwatch(int id) {
        return Tsf.TRANSFORMERS.remove(id);
    }

    public static interface EZTsf {
        byte[] transform(ClassLoader loader, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer);
    }
    private static class Tsf implements ClassFileTransformer {
        private static final Set<Class<?>> TSFD_CLASSES = new HashSet<>();
        private static final Map<String, EZTsf> SUPPLIER = new ConcurrentHashMap<>();
        private static final List<ClassFileTransformer> TRANSFORMERS = new CopyOnWriteArrayList<>();

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            EZTsf tsf = SUPPLIER.get(className);
            if (tsf != null) {
                return tsf.transform(loader, classBeingRedefined, protectionDomain, classfileBuffer);
            }
            byte[] crt = null;
            for (var er : TRANSFORMERS) {
                byte[] tsfd = er.transform(loader, className, classBeingRedefined, protectionDomain, crt == null ? classfileBuffer : crt);
                if (tsfd != null)
                    crt = tsfd;
            }
            return crt;
        }
    }
}
