package plz.lizi.supersteve.power;

import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
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
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import com.sun.jna.Function;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import plz.lizi.supersteve.api.ClassOption;
import plz.lizi.supersteve.api.PLZBase;

public class Agt {
    private static final Class<?> AGT;
    private static final MethodHandle MH_START;
    private static final MethodHandle MH_RETRANSFORM;
    private static final Object[] INST = new Object[2];
    static {
        AGT = PLZBase.defineHiddenClassInPackage(Agt.class.getClassLoader(), Agt.class, "plz.lizi.supersteve.power.Agt$AgtLoader", null, true, ClassOption.STRONG, ClassOption.NESTMATE);
        try {
            MH_START = PLZBase.LOOKUP.findStatic(AGT, "start", MethodType.methodType(void.class));
            MH_RETRANSFORM = PLZBase.LOOKUP.findStatic(AGT, "retransform", MethodType.methodType(boolean.class, Object.class, EZTsf.class, boolean.class));
        } catch (Exception e) {
            PLZBase.throwEx(e);
            throw null;
        }
    }

    public static Instrumentation inst() {
        return (Instrumentation) Agt.INST[0];
    }

    public static void start() {
        try {
            MH_START.invoke();
        } catch (Throwable e) {
            PLZBase.throwEx(e);
        }
    }

    public static synchronized boolean retransform(Object obj, EZTsf tsf, boolean once) {
        try {
            return (boolean) MH_RETRANSFORM.invoke(obj, tsf, once);
        } catch (Throwable e) {
            PLZBase.throwEx(e);
            return false;
        }
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
    @SuppressWarnings("all")
    private static class AgtLoader {
        private static final Class<?> IMPL_CLASS;
        private static final Class<?> TM_CLASS;
        private static final VarHandle VH_TRANSFORMER_MANAGER;
        private static final VarHandle VH_RETRANSFORMABLE_MANAGER;
        private static final VarHandle VH_NATIVE_AGENT;
        private static final MethodHandle MH_RETRNASFORM_CLASSES_0;
        private static final MethodHandle MH_TM_ADD_TRANSFORMER;
        private static final MethodHandle MH_TM_GET_TRANSFORMER_COUNT;
        private static final MethodHandle MH_SET_HAS_TRANSFORMERS;
        private static final MethodHandle MH_SET_HAS_RETRANSFORMABLE_TRANSFORMERS;
        private static final MethodHandle MH_TM_CONSTRUCTOR;
        static {
            try {
                IMPL_CLASS = Class.forName("sun.instrument.InstrumentationImpl");
                TM_CLASS = Class.forName("sun.instrument.TransformerManager");
                MH_RETRNASFORM_CLASSES_0 = PLZBase.LOOKUP.findVirtual(IMPL_CLASS, "retransformClasses0", MethodType.methodType(void.class, long.class, Class[].class));
                VH_TRANSFORMER_MANAGER = PLZBase.LOOKUP.findVarHandle(IMPL_CLASS, "mTransformerManager", TM_CLASS);
                VH_RETRANSFORMABLE_MANAGER = PLZBase.LOOKUP.findVarHandle(IMPL_CLASS, "mRetransfomableTransformerManager", TM_CLASS);
                VH_NATIVE_AGENT = PLZBase.LOOKUP.findVarHandle(IMPL_CLASS, "mNativeAgent", long.class);
                MH_TM_ADD_TRANSFORMER = PLZBase.LOOKUP.findVirtual(TM_CLASS, "addTransformer", MethodType.methodType(void.class, ClassFileTransformer.class));
                MH_TM_GET_TRANSFORMER_COUNT = PLZBase.LOOKUP.findVirtual(TM_CLASS, "getTransformerCount", MethodType.methodType(int.class));
                MH_SET_HAS_TRANSFORMERS = PLZBase.LOOKUP.findVirtual(IMPL_CLASS, "setHasTransformers", MethodType.methodType(void.class, long.class, boolean.class));
                MH_SET_HAS_RETRANSFORMABLE_TRANSFORMERS = PLZBase.LOOKUP.findVirtual(IMPL_CLASS, "setHasRetransformableTransformers", MethodType.methodType(void.class, long.class, boolean.class));
                MH_TM_CONSTRUCTOR = PLZBase.LOOKUP.findConstructor(TM_CLASS, MethodType.methodType(void.class, boolean.class));
            } catch (Throwable e) {
                PLZBase.throwEx(e);
                throw null;
            }
        }
        private static long mNativeAgent = 0;

        private static void addTransformer(Instrumentation zhis, ClassFileTransformer transformer, boolean canRetransform) {
            if (zhis == null) {
                throw new NullPointerException("null passed as 'zhis' (Instrumentation)");
            }
            if (transformer == null) {
                throw new NullPointerException("null passed as 'transformer' in addTransformer");
            }
            synchronized (zhis) {
                try {
                    if (canRetransform) {
                        if (!zhis.isRetransformClassesSupported())
                            throw new UnsupportedOperationException("adding retransformable transformers is not supported in this environment");
                        Object retransformMgr = VH_RETRANSFORMABLE_MANAGER.get(zhis);
                        if (retransformMgr == null) {
                            retransformMgr = MH_TM_CONSTRUCTOR.invoke(true);
                            VH_RETRANSFORMABLE_MANAGER.set(zhis, retransformMgr);
                        }
                        MH_TM_ADD_TRANSFORMER.invoke(retransformMgr, transformer);
                        int count = (int) MH_TM_GET_TRANSFORMER_COUNT.invoke(retransformMgr);
                        if (count == 1) {
                            long nativeAgent = (long) VH_NATIVE_AGENT.get(zhis);
                            MH_SET_HAS_RETRANSFORMABLE_TRANSFORMERS.invoke(zhis, nativeAgent, true);
                        }
                    } else {
                        Object mgr = VH_TRANSFORMER_MANAGER.get(zhis);
                        MH_TM_ADD_TRANSFORMER.invoke(mgr, transformer);
                        int count = (int) MH_TM_GET_TRANSFORMER_COUNT.invoke(mgr);
                        if (count == 1) {
                            long nativeAgent = (long) VH_NATIVE_AGENT.get(zhis);
                            MH_SET_HAS_TRANSFORMERS.invoke(zhis, nativeAgent, true);
                        }
                    }
                } catch (Throwable t) {
                    PLZBase.throwEx(t);
                }
            }
        }

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
                while (Agt.INST[0] == null) {
                    if (System.currentTimeMillis() - time > 1000)
                        throw new TimeoutException("SSAgt time out");
                }
                Agt.INST[1] = PLZBase.UNSAFE.allocateInstance(IMPL_CLASS);
                mNativeAgent = (long) VH_NATIVE_AGENT.get(Agt.INST[0]);
                addTransformer((Instrumentation) Agt.INST[0], new Tsf(), true);
            } catch (Throwable e) {
                System.err.print("SSAgt load failed: ");
                e.printStackTrace();
                System.err.println("SuperSteve will not load with full mode");
            }
        }

        public static synchronized boolean retransform(Object obj, EZTsf tsf, boolean once) {
            if (Agt.INST[0] == null || Agt.INST[1] == null)
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
                // retransformClasses0(long nativeAgent, Class<?>[] classes);
                // PLZBase.klassPtr(INST[0], IMPL_CLASS);
                MH_RETRNASFORM_CLASSES_0.invoke(Agt.INST[1], mNativeAgent, new Class<?>[] { clazz });
                // INST.get().retransformClasses(clazz);
                if (once)
                    Tsf.TSFD_CLASSES.add(clazz);
            } catch (Throwable e) {
                System.err.println("SSAgt retransform " + clazz.getName() + " error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            if (klass != 0)
                PLZBase.UNSAFE.putInt(klass + 164, accessflags);
            Tsf.SUPPLIER.remove(name);
            return true;
        }
    }
}
