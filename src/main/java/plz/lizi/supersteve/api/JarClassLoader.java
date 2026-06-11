package plz.lizi.supersteve.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class JarClassLoader extends ClassLoader {
    private final Map<String, byte[]> files = new HashMap<>();

    public JarClassLoader(byte[] jarBytes, ClassLoader parent) {
        super(parent);
        try {
            try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(jarBytes))) {
                JarEntry entry;
                while ((entry = jis.getNextJarEntry()) != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    jis.transferTo(baos);
                    files.put(entry.getName(), baos.toByteArray());
                }
            }
        } catch (Throwable e) {
            PLZBase.throwEx(e);
        }
    }

    public Class<?> addClass(String name, byte[] buf) {
        return defineClass(name, buf, 0, buf.length);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = files.get(name.replace(".", "/") + ".class");
        if (bytes == null) {
            throw new ClassNotFoundException(name);
        }
        return defineClass(name, bytes, 0, bytes.length);
    }

    
}
