package plz.lizi.supersteve.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MCDeobfUtil {
    public static String REFMAP = "/";
    public static boolean firstInit = true;
    public static Map<String, String> MCP2SRG = new HashMap<>();
    public static Map<String, String> SRG2MCP = new HashMap<>();
    public static String FIELD_SPLITER = "", METHOD_SPLITER = "";

    public static Map<String, String> makeReflect(String input) {
        Map<String, String> nameMap = new HashMap<>();
        String[] lines = input.split("\\r?\\n");
        for (String line : lines) {
            if (line.startsWith("f=")) {
                FIELD_SPLITER = line.replace("f=", "");
            } else if (line.startsWith("m=")) {
                METHOD_SPLITER = line.replace("m=", "");
            }
        }
        for (String line : lines) {
            Matcher fieldMacher = Pattern.compile("^(.*?)\\s+" + FIELD_SPLITER + "$").matcher(line);
            if (fieldMacher.find()) {
                nameMap.put(fieldMacher.group(0).split(" ")[1].trim(), fieldMacher.group(1).trim());
                continue;
            }
            Matcher methodMatcher = Pattern.compile("^(.*?)\\s+[^\\s]+\\s+" + METHOD_SPLITER + "$").matcher(line);
            if (methodMatcher.find()) {
                nameMap.put(methodMatcher.group(0).split(" ")[2].trim(), methodMatcher.group(1).trim());
            }
        }
        return nameMap;
    }

    public static void init(String tsrgPath) throws Exception {
        if (REFMAP != tsrgPath || firstInit) {
            firstInit = false;
            SRG2MCP = makeReflect(tsrgPath.startsWith("/") ? new String(PLZBase.readAllBytes(MCDeobfUtil.class.getResourceAsStream((tsrgPath.endsWith(".tsrg") ? tsrgPath : tsrgPath + ".tsrg")))) : PLZBase.readFile(tsrgPath));
            REFMAP = tsrgPath;
            SRG2MCP.forEach((srg, mcp) -> {
                MCP2SRG.put(mcp, srg);
            });
        }
    }

    public static List<String> splitCodeWithSRG(String input) {
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile(FIELD_SPLITER + "|" + METHOD_SPLITER);
        Matcher matcher = pattern.matcher(input);
        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                result.add(input.substring(lastEnd, matcher.start()));
            }
            result.add(matcher.group());
            lastEnd = matcher.end();
        }
        if (lastEnd < input.length()) {
            result.add(input.substring(lastEnd));
        }
        return result;
    }

    public static String deobfCode(String obf) {
        String deobf = "";
        List<String> obfs = splitCodeWithSRG(obf);
        for (String obfPart : obfs) {
            String tryDeobf = SRG2MCP.get(obfPart);
            if (tryDeobf == null) {
                deobf += obfPart;
            } else {
                deobf += tryDeobf;
            }
        }
        return deobf;
    }

    public static String deobfVar(String srg) {
        String deobfName = SRG2MCP.get(srg);
        return deobfName == null ? (SRG2MCP.isEmpty() ? "empty_refmap" + REFMAP : srg) : deobfName;
    }

    public static void extractJarWithDeobf(String jarPath, String destDir) throws IOException {
        File destDirectory = new File(destDir);
        if (!destDirectory.exists()) {
            destDirectory.mkdirs();
        }
        try (JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarPath))) {
            JarEntry entry;
            while ((entry = jarInputStream.getNextJarEntry()) != null) {
                String entryName = entry.getName();
                File entryFile = new File(destDir, entryName);
                if (entry.isDirectory()) {
                    entryFile.mkdirs();
                    continue;
                }
                File parent = entryFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                try (OutputStream outputStream = new FileOutputStream(entryFile)) {
                    byte[] buffer = PLZBase.readAllBytes(jarInputStream);
                    if (entry.getName().endsWith(".java")) {
                        String code = new String(buffer);
                        outputStream.write(deobfCode(code).getBytes());
                    } else {
                        outputStream.write(buffer);
                    }
                }
            }
        }
    }
}
