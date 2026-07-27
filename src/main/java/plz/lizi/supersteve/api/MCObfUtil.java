package plz.lizi.supersteve.api;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import plz.lizi.supersteve.power.ClassStruct;
import plz.lizi.supersteve.power.ObjCW;

public class MCObfUtil {
    private final Map<String, String> deobfToObfMap = new HashMap<>();
    private final Map<String, String> obfToDeobfMap = new HashMap<>();

    public MCObfUtil(String tsrgContent) {
        parseTsrg(tsrgContent);
    }

    private void parseTsrg(String content) {
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String line;
            String currentDeobfClass = null;
            String currentObfClass = null;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty())
                    continue;
                if (line.startsWith("\t\t"))
                    continue;
                if (line.startsWith("\t")) {
                    String sub = line.substring(1);
                    String[] parts = sub.split("\\s+");
                    if (currentDeobfClass == null)
                        continue;
                    if (parts.length == 2) {
                        String deobfName = parts[0];
                        String obfName = parts[1];
                        String deobfKey = currentDeobfClass + "." + deobfName;
                        String obfKey = currentObfClass + "." + obfName;
                        deobfToObfMap.put(deobfKey, obfName);
                        obfToDeobfMap.put(obfKey, deobfName);
                    } else if (parts.length == 3) {
                        String deobfName = parts[0];
                        String desc = parts[1];
                        String obfName = parts[2];
                        String deobfKey = currentDeobfClass + "." + deobfName + desc;
                        deobfToObfMap.put(deobfKey, obfName);
                        String obfDesc = remapDescriptor(desc, deobfToObfMap);
                        String obfKey = currentObfClass + "." + obfName + obfDesc;
                        obfToDeobfMap.put(obfKey, deobfName);
                    }
                } else {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 2) {
                        currentDeobfClass = parts[0];
                        currentObfClass = parts[1];
                        deobfToObfMap.put(currentDeobfClass, currentObfClass);
                        obfToDeobfMap.put(currentObfClass, currentDeobfClass);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing TSRG mapping", e);
        }
    }

    private String remapDescriptor(String desc, Map<String, String> classMap) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < desc.length()) {
            char c = desc.charAt(i);
            if (c == 'L') {
                int end = desc.indexOf(';', i);
                String className = desc.substring(i + 1, end);
                String mappedClass = classMap.getOrDefault(className, className);
                sb.append('L').append(mappedClass).append(';');
                i = end + 1;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static class HierarchyRemapper extends Remapper {
        private final Map<String, String> mapping;

        HierarchyRemapper(Map<String, String> mapping) {
            this.mapping = mapping;
        }

        @Override
        public String mapMethodName(String owner, String name, String descriptor) {
            if ("<init>".equals(name) || "<clinit>".equals(name))
                return name;
            String mapped = mapping.get(owner + '.' + name + descriptor);
            if (mapped != null)
                return mapped;
            String resolved = resolveMember(owner, name, descriptor, true, new HashSet<>());
            return resolved != null ? resolved : name;
        }

        @Override
        public String mapFieldName(String owner, String name, String descriptor) {
            String mapped = mapping.get(owner + '.' + name);
            if (mapped != null)
                return mapped;
            String resolved = resolveMember(owner, name, descriptor, false, new HashSet<>());
            return resolved != null ? resolved : name;
        }

        @Override
        public String map(String internalName) {
            return mapping.getOrDefault(internalName, internalName);
        }

        private String resolveMember(String owner, String name, String descriptor, boolean isMethod, Set<String> visited) {
            if (owner == null || !visited.add(owner))
                return null;
            String key = isMethod ? owner + '.' + name + descriptor : owner + '.' + name;
            String direct = mapping.get(key);
            if (direct != null)
                return direct;
            ClassStruct cs = ClassStruct.as(owner);
            if (cs == null)
                return null;
            if (cs.superName() != null) {
                String r = resolveMember(cs.superName(), name, descriptor, isMethod, visited);
                if (r != null)
                    return r;
            }
            if (cs.impls() != null) {
                for (String iface : cs.impls()) {
                    String r = resolveMember(iface, name, descriptor, isMethod, visited);
                    if (r != null)
                        return r;
                }
            }
            return null;
        }
    }

    public ClassNode obfCN(ClassNode deobfClassNode) {
        ClassNode obfClassNode = new ClassNode();
        ClassRemapper remapper = new ClassRemapper(obfClassNode, new HierarchyRemapper(deobfToObfMap));
        deobfClassNode.accept(remapper);
        return obfClassNode;
    }

    public ClassNode deobfCN(ClassNode obfClassNode) {
        ClassNode deobfClassNode = new ClassNode();
        ClassRemapper remapper = new ClassRemapper(deobfClassNode, new HierarchyRemapper(obfToDeobfMap));
        obfClassNode.accept(remapper);
        return deobfClassNode;
    }

    public byte[] obfB(byte[] classFileBuffer) {
        if (classFileBuffer == null)
            return null;
        ClassReader cr = new ClassReader(classFileBuffer);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.EXPAND_FRAMES);
        ClassWriter cw = new ObjCW(cr);
        obfCN(cn).accept(cw);
        return cw.toByteArray();
    }

    public byte[] deobfB(byte[] classFileBuffer) {
        if (classFileBuffer == null)
            return null;
        ClassReader cr = new ClassReader(classFileBuffer);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.EXPAND_FRAMES);
        ClassWriter cw = new ObjCW(cr);
        deobfCN(cn).accept(cw);
        return cw.toByteArray();
    }

    public String obfV(String className, String symbolName) {
        return lookupSymbol(className, symbolName, deobfToObfMap);
    }

    public String deobfV(String className, String symbolName) {
        return lookupSymbol(className, symbolName, obfToDeobfMap);
    }

    private String lookupSymbol(String className, String symbolName, Map<String, String> map) {
        if (symbolName == null)
            return map.getOrDefault(className, className);
        String directKey = className + "." + symbolName;
        if (map.containsKey(directKey))
            return map.get(directKey);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(className + "." + symbolName + "(")) {
                return entry.getValue();
            }
        }
        return symbolName;
    }
}
