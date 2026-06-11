package plz.lizi.supersteve.api;

import java.lang.reflect.Method;

public class CfrBridge {
    private static final JarClassLoader CFR_LOADER;
    private static Method DECOMPILE;
    static {
        CFR_LOADER = new JarClassLoader(PLZBase.readAllBytes(CfrBridge.class.getClassLoader().getResourceAsStream("plz/lizi/supersteve/api/cfr.jar")), ClassLoader.getSystemClassLoader());
        CFR_LOADER.addClass(PLZBase.class.getName(), PLZBase.getClassBytes(PLZBase.class.getName(), PLZBase.class.getClassLoader()));
    }

    public static String decompile(byte[] classBytes) {
        try {
            if (DECOMPILE == null)
                DECOMPILE = CFR_LOADER.loadClass("org.benf.cfr.reader.CfrUtil").getMethod("decompile", byte[].class);
            return (String) DECOMPILE.invoke(null, classBytes);
        } catch (Throwable e) {
            PLZBase.throwEx(e);
            return null;
        }
    }
}
