package plz.lizi.supersteve.power;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import plz.lizi.supersteve.api.SSUtil;

/**
 * 100% 由 ZCode Weekend Build (5E token) 计划提供的 GLM-5.3Flash 制作，开最高思考才用40%，真的太牛逼了
 * 
 * 方法级低风险热补丁（子类覆盖方案）： 对比 反编译原文 与 编辑后源码 找出被修改的方法，把它们放入一个 "public abstract class 目标$$SSPatch extends 目标类" 的子类骨架中热编译 （编译器视野中的类成员权限已被 HotCplr 用 ASM 放宽为 public）， 再将子类方法的方法体移植回目标类的混淆字节码： 移植时把所有指向 $$SSPatch 的 owner/desc 引用改写回目标类， 并把指向本类私有方法的 invokevirtual 修正为 invokespecial， 把指向目标类的 invokespecial（super 调用）改写到真实父类， 最后 Agt.retransform 一次性应用。字节码非法时 JVM 拒绝并保留原类。
 */
public class MethodPatcher {
    public static final String DEBUG_DIR = System.getProperty("sspatch.debug.dir", "sspatch_debug");

    private static void dumpFile(String name, byte[] data) {
        try {
            java.io.File dir = new java.io.File(DEBUG_DIR);
            dir.mkdirs();
            java.nio.file.Files.write(new java.io.File(dir, name).toPath(), data);
            System.out.println("[SSPatch] dumped " + name + " (" + data.length + " bytes)");
        } catch (Throwable e) {
            System.err.println("[SSPatch] dump " + name + " failed: " + e);
        }
    }

    public static void dumpFile(String name, String data) {
        dumpFile(name, data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static void log(String msg) {
        System.out.println("[SSPatch] " + msg);
    }

    public static class Member {
        public String kind; // METHOD / FIELD / TYPE
        public String name;
        public String params;
        public String paramsRaw;
        public int paramCount;
        public String text;
    }

    public static String patchAndApply(String originalSource, String editedSource, Class<?> target) throws Throwable {
        StringBuilder report = new StringBuilder();
        report.append("target: ").append(target.getName()).append("\n");
        List<Member> origMembers = scanMembers(originalSource);
        List<Member> editMembers = scanMembers(editedSource);
        Set<String> consumed = new HashSet<>();
        List<Member> modified = new ArrayList<>();
        List<String> unsupported = new ArrayList<>();
        for (Member em : editMembers) {
            if ("TYPE".equals(em.kind))
                continue;
            Member om = matchOriginal(origMembers, consumed, em);
            if (om == null) {
                unsupported.add("  [skip] add member (retransform cannot add): " + em.kind + " " + em.name + "(" + em.params + ")");
                continue;
            }
            if (!normalize(om.text).equals(normalize(em.text))) {
                if ("METHOD".equals(em.kind))
                    modified.add(em);
                else
                    unsupported.add("  [skip] field modification (retransform cannot change fields): " + em.name);
            }
        }
        for (Member om : origMembers) {
            if ("TYPE".equals(om.kind))
                continue;
            if (matchEdited(editMembers, om) == null)
                unsupported.add("  [skip] remove member (retransform cannot remove): " + om.kind + " " + om.name);
        }
        if (modified.isEmpty()) {
            report.append("no applicable modified methods detected.\n");
            appendUnsupported(report, unsupported);
            return report.toString();
        }
        report.append("diff: ").append(modified.size()).append(" modified method(s)\n");
        for (Member m : modified) {
            String dn = m.name.equals(target.getSimpleName()) ? "<init>" : m.name;
            report.append("  - ").append(dn).append("(").append(m.params).append(")\n");
        }
        String targetFQ = target.getName();
        int lastDot = targetFQ.lastIndexOf('.');
        String pkg = lastDot < 0 ? "" : targetFQ.substring(0, lastDot);
        String simple = targetFQ.substring(lastDot + 1);
        String patchSimple = simple + "$$SSPatch";
        String patchFQ = targetFQ + "$$SSPatch";
        String typeParams = typeParamsOf(originalSource, simple);
        String typeArgs = typeArgsOf(typeParams);
        StringBuilder body = new StringBuilder();
        if (!target.isInterface()) {
            Member ctor = null;
            for (Member m : origMembers)
                if ("METHOD".equals(m.kind) && m.name.equals(simple) && !m.text.trim().endsWith(";")) {
                    ctor = m;
                    break;
                }
            if (ctor != null)
                body.append("    public ").append(patchSimple).append("(").append(ctor.paramsRaw).append(") {\n        super(").append(argNames(ctor.paramsRaw)).append(");\n    }\n");
        }
        for (Member m : modified)
            body.append("\n    ").append(subclassMethodText(m.text)).append("\n");
        StringBuilder skeleton = new StringBuilder();
        if (!pkg.isEmpty())
            skeleton.append("package ").append(pkg).append(";\n\n");
        for (String imp : collectImports(editedSource, body.toString(), pkg))
            skeleton.append(imp).append("\n");
        skeleton.append("\npublic abstract class ").append(patchSimple).append(typeParams == null ? "" : typeParams);
        if (target.isInterface()) {
            skeleton.append(" implements ").append(simple).append(typeArgs == null ? "" : typeArgs);
        } else {
            skeleton.append(" extends ").append(simple).append(typeArgs == null ? "" : typeArgs);
        }
        skeleton.append(" {\n");
        skeleton.append(body);
        skeleton.append("}\n");
        // dumpFile("01_skeleton.java", skeleton.toString());
        // log("skeleton generated: " + skeleton.length() + " chars");
        ClassLoader cl = target.getClassLoader();
        byte[] skelDeobf = null;
        try {
            skelDeobf = HotCplr.compileToClassfile(skeleton.toString(), patchFQ, cl);
        } catch (Throwable e) {
            // dumpFile("00_compile_error.txt", e.getMessage() != null ? e.getMessage() : e.toString());
            throw e;
        }
        report.append("skeleton ").append(patchFQ).append(" compiled OK (").append(skelDeobf.length).append(" bytes)\n");
        // dumpFile("02_skeleton_deobf.class", skelDeobf);
        // log("skeleton compiled: " + skelDeobf.length + " bytes");
        byte[] runtimeBytes = Agt.getClassBytes(target);
        if (runtimeBytes == null)
            throw new IllegalStateException("cannot read runtime class bytes: " + target.getName());
        ClassNode obfCN = node(runtimeBytes);
        ClassNode deobfCN = node(SSUtil.MC_OBF_UTIL.deobfB(runtimeBytes));
        byte[] skelObfBytes = SSUtil.MC_OBF_UTIL.obfB(skelDeobf);
        ClassNode skelObfCN = node(skelObfBytes);
        ClassNode skelDeobfCN = node(skelDeobf);
        // dumpFile("03_target_runtime.class", runtimeBytes);
        // dumpFile("04_target_deobf.class", SSUtil.MC_OBF_UTIL.deobfB(runtimeBytes));
        // dumpFile("05_skeleton_obf.class", skelObfBytes);
        // log("target runtime: " + runtimeBytes.length + " bytes, skeleton obf: " + skelObfBytes.length + " bytes");
        // log("target superName: " + obfCN.superName + ", interfaces: " + obfCN.interfaces);
        String skelInternal = skelObfCN.name;
        String targetInternal = obfCN.name;
        Set<String> privateMethods = new HashSet<>();
        for (MethodNode mn : obfCN.methods)
            if ((mn.access & Opcodes.ACC_PRIVATE) != 0)
                privateMethods.add(mn.name + mn.desc);
        List<ClassNode[]> chain = new ArrayList<>();
        Set<String> chainVisited = new HashSet<>();
        ClassNode curObf = obfCN;
        ClassNode curDeobf = deobfCN;
        while (true) {
            chain.add(new ClassNode[] { curObf, curDeobf });
            String sup = curObf.superName;
            if (sup == null || sup.equals("java/lang/Object") || !chainVisited.add(sup))
                break;
            byte[] superBytes = null;
            try (java.io.InputStream sis = cl.getResourceAsStream(sup + ".class")) {
                if (sis != null)
                    superBytes = sis.readAllBytes();
            } catch (Throwable t) {
            }
            if (superBytes == null)
                break;
            curObf = node(superBytes);
            curDeobf = node(SSUtil.MC_OBF_UTIL.deobfB(superBytes));
        }
        Map<String, String> fieldRemap = new HashMap<>();
        Map<String, String> methodRemap = new HashMap<>();
        // StringBuilder remapDump = new StringBuilder();
        // remapDump.append("// field remap (deobf_name+obf_desc -> runtime_name), " + fieldRemap.size() + " entries" + NL);
        // for (var e : fieldRemap.entrySet())
        // remapDump.append(e.getKey()).append(" -> ").append(e.getValue()).append(NL);
        // remapDump.append(NL + "// method remap (deobf_name+obf_desc -> runtime_name), " + methodRemap.size() + " entries" + NL);
        // for (var e : methodRemap.entrySet())
        // remapDump.append(e.getKey()).append(" -> ").append(e.getValue()).append(NL);
        // dumpFile("06_remap_tables.txt", remapDump.toString());
        // log("remap tables: " + fieldRemap.size() + " fields, " + methodRemap.size() + " methods, chain depth: " + chain.size());
        for (int ci = chain.size() - 1; ci >= 0; ci--) {
            ClassNode o = chain.get(ci)[0];
            ClassNode d = chain.get(ci)[1];
            for (int i = 0; i < o.fields.size() && i < d.fields.size(); i++)
                fieldRemap.put(d.fields.get(i).name + o.fields.get(i).desc, o.fields.get(i).name);
            for (int i = 0; i < o.methods.size() && i < d.methods.size(); i++)
                methodRemap.put(d.methods.get(i).name + o.methods.get(i).desc, o.methods.get(i).name);
        }
        int patched = 0;
        for (Member m : modified) {
            String dn = m.name.equals(simple) ? "<init>" : m.name;
            report.append("splice ").append(dn).append("(").append(m.params).append("): ");
            try {
                MethodNode origDeobfM = findOriginal(deobfCN, skelDeobfCN, m);
                if (origDeobfM == null)
                    throw new IllegalStateException("method not found in target class: " + m.name);
                if ((origDeobfM.access & Opcodes.ACC_ABSTRACT) != 0)
                    throw new IllegalStateException("abstract method cannot be implemented by method patch (would change class structure)");
                MethodNode skelDeobfM = findMethod(skelDeobfCN, origDeobfM.name, origDeobfM.desc);
                if (skelDeobfM == null)
                    throw new IllegalStateException("method not found in skeleton: " + origDeobfM.name + origDeobfM.desc);
                if (!skelDeobfM.desc.equals(origDeobfM.desc))
                    throw new IllegalStateException("signature was modified - method patch requires unchanged signature");
                int idx = deobfCN.methods.indexOf(origDeobfM);
                if (idx < 0 || idx >= obfCN.methods.size())
                    throw new IllegalStateException("obfuscated member index mismatch");
                MethodNode skelObfM = skelObfCN.methods.get(skelDeobfCN.methods.indexOf(skelDeobfM));
                MethodNode obfTarget = obfCN.methods.get(idx);
                if (!obfTarget.desc.equals(skelObfM.desc))
                    throw new IllegalStateException("obfuscated descriptor mismatch: " + skelObfM.name);
                MNUtil.cloneMethodNodeInsn(skelObfM, obfTarget);
                boolean hasIndy = false;
                for (AbstractInsnNode insn : obfTarget.instructions.toArray()) {
                    if (insn instanceof FieldInsnNode f) {
                        if (skelInternal.equals(f.owner))
                            f.owner = targetInternal;
                        if (targetInternal.equals(f.owner)) {
                            String mapped = fieldRemap.get(f.name + f.desc);
                            if (mapped != null)
                                f.name = mapped;
                        }
                    } else if (insn instanceof MethodInsnNode mi) {
                        if (skelInternal.equals(mi.owner))
                            mi.owner = targetInternal;
                        if (mi.name.startsWith("$$patchSuper$")) {
                            mi.name = "<init>";
                            mi.setOpcode(Opcodes.INVOKESPECIAL);
                        }
                        if (targetInternal.equals(mi.owner)) {
                            String mapped = methodRemap.get(mi.name + mi.desc);
                            if (mapped != null)
                                mi.name = mapped;
                            if (mi.getOpcode() == Opcodes.INVOKESPECIAL && !mi.name.equals("<init>"))
                                mi.owner = obfCN.superName;
                        }
                        if (mi.getOpcode() == Opcodes.INVOKEVIRTUAL && privateMethods.contains(mi.owner + mi.name + mi.desc))
                            mi.setOpcode(Opcodes.INVOKESPECIAL);
                    } else if (insn instanceof TypeInsnNode ti) {
                        if (skelInternal.equals(ti.desc))
                            ti.desc = targetInternal;
                    } else if (insn.getOpcode() == Opcodes.INVOKEDYNAMIC) {
                        hasIndy = true;
                    }
                }
                patched++;
                report.append("OK -> ").append(obfTarget.name).append(obfTarget.desc).append(hasIndy ? " (warning: contains invokedynamic)" : "").append("\n");
            } catch (Throwable e) {
                report.append("FAILED (").append(e.getMessage()).append(")\n");
            }
        }
        appendUnsupported(report, unsupported);
        if (patched == 0) {
            report.append("no method was successfully spliced, retransform skipped.\n");
            return report.toString();
        }
        ObjCW cw = new ObjCW(new ClassReader(runtimeBytes));
        obfCN.accept(cw);
        byte[] patchedBytes = cw.toByteArray();
        // dumpFile("07_patched.class", patchedBytes);
        // dumpFile("08_report.txt", report.toString());
        // log("patched class: " + patchedBytes.length + " bytes, submitting retransform...");
        boolean submitted = Agt.retransform(target, (loader, cls, pd, buf) -> patchedBytes, false, true);
        // log("retransform submitted=" + submitted);
        report.append("retransform ").append(submitted ? "OK" : "FAILED").append(" (").append(patchedBytes.length).append(" bytes, ").append(patched).append(" method(s))\n");
        report.append("note: retransform is atomic - invalid bytecode keeps the original class\n");
        return report.toString();
    }

    private static MethodNode findOriginal(ClassNode deobfCN, ClassNode skelDeobfCN, Member m) {
        List<MethodNode> cands = byName(deobfCN, m.name);
        if (cands.isEmpty())
            return null;
        if (cands.size() == 1)
            return cands.get(0);
        List<MethodNode> skels = byName(skelDeobfCN, m.name);
        for (MethodNode sm : skels)
            for (MethodNode cm : cands)
                if (sm.desc.equals(cm.desc))
                    return cm;
        return null;
    }

    private static List<MethodNode> byName(ClassNode cn, String name) {
        List<MethodNode> out = new ArrayList<>();
        for (MethodNode mn : cn.methods)
            if (mn.name.equals(name))
                out.add(mn);
        return out;
    }

    private static MethodNode findMethod(ClassNode cn, String name, String desc) {
        for (MethodNode mn : cn.methods)
            if (mn.name.equals(name) && mn.desc.equals(desc))
                return mn;
        return null;
    }

    private static String subclassMethodText(String text) {
        String t = text.trim();
        int brace = t.indexOf('{');
        String header = brace < 0 ? t : t.substring(0, brace);
        String body = brace < 0 ? "" : t.substring(brace);
        header = header.replaceAll("(?m)^\\s*@[A-Za-z_$][\\w$.]*(\\([^)]*\\))?\\s*$\\n?", "");
        header = header.replaceAll("@[A-Za-z_$][\\w$.]*(\\([^)]*\\))?", "");
        header = header.replaceAll("(?m)^\\s*\\n", "");
        header = header.replaceAll("\\bprivate\\s+", "public ");
        header = header.replaceAll("\\bprotected\\s+", "public ");
        header = header.replaceAll("\\bfinal\\s+", "");
        header = header.replaceAll("\\bdefault\\s+", "");
        header = header.replaceAll("\\babstract\\s+", "");
        body = body.replaceAll("(?m)^\\s*void\\s+[A-Za-z_$][\\w$]*\\s*;\\s*\\n", "");
        body = body.replaceAll("(?m)^\\s*/\\* WARNING - void declaration \\*/\\s*\\n", "");
        return header + body;
    }

    private static List<String> collectImports(String src, String usageCorpus, String targetPkg) {
        List<String> out = new ArrayList<>();
        for (String line : src.split("\r?\n")) {
            String t = line.trim();
            if (!t.startsWith("import "))
                continue;
            if (!t.endsWith(";"))
                t = t + ";";
            String ref = t.substring(7, t.length() - 1).trim();
            if (ref.startsWith("static "))
                ref = ref.substring(7).trim();
            int dot = ref.lastIndexOf('.');
            String simple = dot < 0 ? ref : ref.substring(dot + 1);
            String impPkg = dot < 0 ? "" : ref.substring(0, dot);
            if (!targetPkg.isEmpty() && impPkg.equals(targetPkg))
                continue;
            if (simple.isEmpty() || !java.util.regex.Pattern.compile("\\b" + java.util.regex.Pattern.quote(simple) + "\\b").matcher(usageCorpus).find())
                continue;
            out.add(t);
        }
        return out;
    }

    private static String typeParamsOf(String src, String simple) {
        java.util.regex.Matcher mt = java.util.regex.Pattern.compile(
                "\\b(?:class|interface|enum|record)\\s+" + java.util.regex.Pattern.quote(simple) + "\\s*(<)").matcher(src);
        if (!mt.find())
            return null;
        int start = mt.start(1);
        int depth = 0;
        for (int i = start; i < src.length(); i++) {
            char ch = src.charAt(i);
            if (ch == '<')
                depth++;
            else if (ch == '>') {
                depth--;
                if (depth == 0)
                    return src.substring(start, i + 1);
            } else if (ch == '{') {
                return null;
            }
        }
        return null;
    }

    private static String typeArgsOf(String typeParams) {
        if (typeParams == null || typeParams.length() < 2)
            return null;
        String inner = typeParams.substring(1, typeParams.length() - 1);
        StringBuilder sb = new StringBuilder("<");
        boolean first = true;
        for (String seg : splitTopLevel(inner, ',')) {
            String v = firstIdentifier(seg.trim());
            if (v.isEmpty())
                continue;
            if (!first)
                sb.append(", ");
            sb.append(v);
            first = false;
        }
        return sb.append(">").toString();
    }

    private static String argNames(String paramsRaw) {
        if (paramsRaw == null || paramsRaw.trim().isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        for (String seg : splitTopLevel(paramsRaw, ',')) {
            String nm = lastIdentifier(seg.trim());
            if (nm.isEmpty())
                continue;
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(nm);
        }
        return sb.toString();
    }

    private static List<String> splitTopLevel(String text, char sep) {
        List<String> out = new ArrayList<>();
        int depth = 0;
        boolean inStr = false, inChr = false;
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (inStr) {
                if (ch == '\\')
                    i++;
                else if (ch == '"')
                    inStr = false;
                continue;
            }
            if (inChr) {
                if (ch == '\\')
                    i++;
                else if (ch == '\'')
                    inChr = false;
                continue;
            }
            if (ch == '"')
                inStr = true;
            else if (ch == '\'')
                inChr = true;
            else if (ch == '(' || ch == '<' || ch == '[')
                depth++;
            else if (ch == ')' || ch == '>' || ch == ']')
                depth--;
            else if (ch == sep && depth == 0) {
                out.add(text.substring(start, i));
                start = i + 1;
            }
        }
        out.add(text.substring(start));
        return out;
    }

    private static String firstIdentifier(String text) {
        int i = 0;
        while (i < text.length() && !Character.isJavaIdentifierStart(text.charAt(i)))
            i++;
        int j = i;
        while (j < text.length() && Character.isJavaIdentifierPart(text.charAt(j)))
            j++;
        return text.substring(i, j);
    }

    private static Member matchOriginal(List<Member> origMembers, Set<String> consumed, Member em) {
        for (Member om : origMembers) {
            if (!om.kind.equals(em.kind) || !om.name.equals(em.name) || om.paramCount != em.paramCount)
                continue;
            String key = System.identityHashCode(om) + "/" + om.name;
            if (consumed.add(key))
                return om;
        }
        return null;
    }

    private static Member matchEdited(List<Member> editMembers, Member om) {
        for (Member em : editMembers)
            if (em.kind.equals(om.kind) && em.name.equals(om.name) && em.paramCount == om.paramCount)
                return em;
        return null;
    }

    private static String normalize(String text) {
        return text.replaceAll("\\s+", "");
    }

    private static void appendUnsupported(StringBuilder report, List<String> unsupported) {
        if (!unsupported.isEmpty()) {
            report.append("skipped ").append(unsupported.size()).append(" unsupported change(s):\n");
            for (String s : unsupported)
                report.append(s).append("\n");
        }
    }
    // ==================== source member scanning ====================

    private static int classBodyBraceIndex(String src) {
        int n = src.length();
        boolean inStr = false, inChr = false, lineC = false, blockC = false;
        for (int i = 0; i < n; i++) {
            char ch = src.charAt(i);
            char next = i + 1 < n ? src.charAt(i + 1) : '\0';
            if (lineC) {
                if (ch == '\n')
                    lineC = false;
                continue;
            }
            if (blockC) {
                if (ch == '*' && next == '/') {
                    blockC = false;
                    i++;
                }
                continue;
            }
            if (inStr) {
                if (ch == '\\')
                    i++;
                else if (ch == '"')
                    inStr = false;
                continue;
            }
            if (inChr) {
                if (ch == '\\')
                    i++;
                else if (ch == '\'')
                    inChr = false;
                continue;
            }
            if (ch == '/' && next == '/') {
                lineC = true;
                i++;
                continue;
            }
            if (ch == '/' && next == '*') {
                blockC = true;
                i++;
                continue;
            }
            if (ch == '"') {
                inStr = true;
                continue;
            }
            if (ch == '\'') {
                inChr = true;
                continue;
            }
            if (Character.isJavaIdentifierStart(ch) && (i == 0 || !Character.isJavaIdentifierPart(src.charAt(i - 1)) || src.charAt(i - 1) == '.')) {
                int j = i;
                while (j < n && Character.isJavaIdentifierPart(src.charAt(j)))
                    j++;
                String w = src.substring(i, j);
                if (w.equals("class") || w.equals("interface") || w.equals("enum") || w.equals("record")) {
                    for (int k = j; k < n; k++) {
                        char c = src.charAt(k);
                        if (c == '{')
                            return k;
                        if (c == ';' || c == '=')
                            break;
                    }
                }
                i = j - 1;
            }
        }
        return -1;
    }

    private static List<Member> scanMembers(String source) {
        List<Member> list = new ArrayList<>();
        int classBrace = classBodyBraceIndex(source);
        if (classBrace < 0)
            return list;
        int depth = 0;
        int start = -1;
        boolean inStr = false, inChr = false, lineC = false, blockC = false;
        int n = source.length();
        for (int i = classBrace + 1; i < n; i++) {
            char ch = source.charAt(i);
            char next = i + 1 < n ? source.charAt(i + 1) : '\0';
            if (lineC) {
                if (ch == '\n')
                    lineC = false;
                continue;
            }
            if (blockC) {
                if (ch == '*' && next == '/') {
                    blockC = false;
                    i++;
                }
                continue;
            }
            if (inStr) {
                if (ch == '\\')
                    i++;
                else if (ch == '"')
                    inStr = false;
                continue;
            }
            if (inChr) {
                if (ch == '\\')
                    i++;
                else if (ch == '\'')
                    inChr = false;
                continue;
            }
            if (ch == '/' && next == '/') {
                lineC = true;
                i++;
                continue;
            }
            if (ch == '/' && next == '*') {
                blockC = true;
                i++;
                continue;
            }
            if (ch == '"') {
                inStr = true;
                continue;
            }
            if (ch == '\'') {
                inChr = true;
                continue;
            }
            if (ch == '{') {
                if (depth == 0 && start < 0)
                    start = i;
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth < 0)
                    break;
                if (depth == 0 && start >= 0) {
                    addMember(list, source.substring(start, i + 1));
                    start = -1;
                }
            } else if (ch == ';' && depth == 0 && start >= 0) {
                addMember(list, source.substring(start, i + 1));
                start = -1;
            } else if (depth == 0 && start < 0 && !Character.isWhitespace(ch)) {
                start = i;
            }
        }
        return list;
    }

    private static void addMember(List<Member> list, String text) {
        Member m = new Member();
        String trimmed = text.trim();
        m.text = trimmed;
        String cls = trimmed.replaceAll("(?m)^\\s*@[A-Za-z_$][\\w$.]*(\\([^)]*\\))?\\s*$\\n?", "");
        if (cls.matches("(?s)^(?:(?:public|private|protected|static|final|abstract|sealed|non-sealed|strictfp)\\s+)*(?:class|interface|enum|record)\\b.*")) {
            m.kind = "TYPE";
            m.name = typeName(cls);
            list.add(m);
            return;
        }
        if (cls.endsWith(";")) {
            m.kind = "FIELD";
            m.name = lastIdentifier(cls);
            m.paramCount = 0;
            return;
        }
        int p = topParenOpen(cls);
        if (p < 0) {
            m.kind = "FIELD";
            m.name = lastIdentifier(cls);
            m.paramCount = 0;
            if (!cls.endsWith(";") && !cls.endsWith("}"))
                m.text = cls + ";";
        } else {
            m.kind = "METHOD";
            m.name = methodNameBefore(cls, p);
            m.paramsRaw = paramsRaw(cls, p);
            m.params = m.paramsRaw.replaceAll("\\s+", "");
            m.paramCount = m.params.isEmpty() ? 0 : m.params.split(",").length;
        }
        list.add(m);
    }

    private static int topParenOpen(String text) {
        boolean inStr = false, inChr = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (inStr) {
                if (ch == '\\')
                    i++;
                else if (ch == '"')
                    inStr = false;
                continue;
            }
            if (inChr) {
                if (ch == '\\')
                    i++;
                else if (ch == '\'')
                    inChr = false;
                continue;
            }
            if (ch == '"')
                inStr = true;
            else if (ch == '\'')
                inChr = true;
            else if (ch == '(')
                return i;
        }
        return -1;
    }

    private static String paramsRaw(String text, int openIdx) {
        int depth = 0;
        boolean inStr = false, inChr = false;
        for (int i = openIdx; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (inStr) {
                if (ch == '\\')
                    i++;
                else if (ch == '"')
                    inStr = false;
                continue;
            }
            if (inChr) {
                if (ch == '\\')
                    i++;
                else if (ch == '\'')
                    inChr = false;
                continue;
            }
            if (ch == '"')
                inStr = true;
            else if (ch == '\'')
                inChr = true;
            else if (ch == '(')
                depth++;
            else if (ch == ')') {
                depth--;
                if (depth == 0)
                    return text.substring(openIdx + 1, i);
            }
        }
        return "";
    }

    private static String methodNameBefore(String text, int parenIdx) {
        int i = parenIdx - 1;
        while (i >= 0 && Character.isWhitespace(text.charAt(i)))
            i--;
        int end = i + 1;
        while (i >= 0 && (Character.isJavaIdentifierPart(text.charAt(i)) || text.charAt(i) == '.'))
            i--;
        return text.substring(i + 1, end);
    }

    private static String lastIdentifier(String text) {
        int end = text.length();
        while (end > 0 && (text.charAt(end - 1) == ';' || Character.isWhitespace(text.charAt(end - 1))))
            end--;
        int i = end;
        while (i > 0 && Character.isJavaIdentifierPart(text.charAt(i - 1)))
            i--;
        return text.substring(i, end);
    }

    private static String typeName(String text) {
        java.util.regex.Matcher mt = java.util.regex.Pattern.compile(
                "\\b(?:class|interface|enum|record)\\s+([A-Za-z_$][\\w$]*)").matcher(text);
        return mt.find() ? mt.group(1) : "";
    }

    private static ClassNode node(byte[] bytes) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, ClassReader.EXPAND_FRAMES);
        return cn;
    }
}
