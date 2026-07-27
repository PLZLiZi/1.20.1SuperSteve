package plz.lizi.supersteve.power;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import javax.tools.JavaFileObject.Kind;
import plz.lizi.supersteve.api.ClassOption;
import plz.lizi.supersteve.api.MCObfUtil;
import plz.lizi.supersteve.api.PLZBase;
import plz.lizi.supersteve.api.SSUtil;

public class HotCplr {
	public static final Map<String, byte[]> MEMORY_CLASS_CACHE = new ConcurrentHashMap<>();
	private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)*);");
	private static final Pattern CLASS_PATTERN = Pattern.compile("(class|interface|enum|record)\\s+([a-zA-Z_][a-zA-Z0-9_]*)");
	private static final Thread CPLR_THREAD = new Thread(HotCplr::cplrHandle, "CplrPoolTask");
	private static final List<Runnable> CPLR_TASKS = new CopyOnWriteArrayList<>();
	static {
		CPLR_THREAD.start();
	}

	public static void cplrHandle() {
		for (var task : CPLR_TASKS) {
			task.run();
		}
	}

	public static byte[] compileToClassfile(String javaCode, ClassLoader cl) throws Exception {
		String fullClassName = resolveFullClassName(javaCode);
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			throw new IllegalStateException("ToolProvider.getSystemJavaCompiler() is null (env isn't a JDK)");
		}
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		FileManager fileManager = new FileManager(compiler.getStandardFileManager(diagnostics, null, null), cl);
		if (compiler.getTask(null, fileManager, diagnostics, List.of("-encoding", "UTF-8"), null, List.of(new DirectJavaFile(fullClassName, javaCode))).call()) {
			return fileManager.getJavaClassObject().getBytes();
		} else {
			StringBuilder allErrors = new StringBuilder();
			boolean hasRealError = false;
			for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
				if (diagnostic.getLineNumber() != -1 && diagnostic.getKind() == Diagnostic.Kind.ERROR) {
					hasRealError = true;
					String error = compileError(diagnostic, javaCode);
					allErrors.append(error).append("\n");
				}
			}
			if (hasRealError) {
				throw new RuntimeException(allErrors.toString());
			} else {
				throw new RuntimeException("Compile error: " + diagnostics.getDiagnostics() + "\n");
			}
		}
	}

	public static Class<?> compileToClass(String javaCode, ClassLoader cl) throws Exception {
		return PLZBase.defineHiddenClass(cl, HotCplr.class, resolveFullClassName(javaCode), null, compileToClassfile(javaCode, cl), false, ClassOption.STRONG);
	}

	protected static String resolveFullClassName(String javaCode) {
		String packageName = "";
		String className = "";
		Matcher packMatcher = PACKAGE_PATTERN.matcher(javaCode);
		if (packMatcher.find()) {
			packageName = packMatcher.group(1);
		}
		Matcher classMatcher = CLASS_PATTERN.matcher(javaCode);
		if (classMatcher.find()) {
			className = classMatcher.group(2);
		} else {
			throw new IllegalArgumentException("Class name not found");
		}
		return packageName.isEmpty() ? className : packageName + "." + className;
	}

	protected static String compileError(Diagnostic<? extends JavaFileObject> diagnostic, String javaCode) {
		StringBuilder res = new StringBuilder();
		res.append("§cCompile error\n");
		res.append("§c==============================================§r\n");
		long lineNum = diagnostic.getLineNumber();
		long colNum = diagnostic.getColumnNumber();
		res.append("Position  : Line ").append(lineNum).append(", Column ").append(colNum).append("\n");
		res.append("Cause     : ").append(diagnostic.getMessage(null)).append("\n\n");
		if (javaCode != null && !javaCode.isEmpty() && lineNum > 0) {
			String[] lines = javaCode.split("\\r?\\n");
			int targetIndex = (int) lineNum - 1;
			if (targetIndex < lines.length) {
				if (targetIndex - 1 >= 0) {
					res.append(String.format("§8%4d | %s\n", lineNum - 1, lines[targetIndex - 1]));
				}
				res.append(String.format("§c%4d | %s\n", lineNum, lines[targetIndex]));
				res.append("§8     | ");
				for (int i = 1; i < colNum; i++) {
					if (i <= lines[targetIndex].length() && lines[targetIndex].charAt(i - 1) == '\t') {
						res.append("\t");
					} else {
						res.append(" ");
					}
				}
				res.append("§c^\n");
				if (targetIndex + 1 < lines.length) {
					res.append(String.format("§8%4d | %s\n", lineNum + 1, lines[targetIndex + 1]));
				}
			}
		}
		res.append("§c==============================================§r");
		return res.toString();
	}

	protected static class DirectJavaFile extends SimpleJavaFileObject {
		private String contents = null;
		private final String className;

		public DirectJavaFile(String className, String contents) {
			super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
			this.className = className;
			this.contents = contents;
		}

		public CharSequence getCharContent(boolean ignoredEncodingErrors) throws IOException {
			return contents;
		}

		public String getClassName() {
			return className;
		}
	}
	protected static class Result extends SimpleJavaFileObject {
		private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		public Result(String name, Kind kind) {
			super(URI.create("string:///" + name.replace('.', '/') + kind.extension), kind);
		}

		public byte[] getBytes() {
			return outputStream.toByteArray();
		}

		@Override
		public OutputStream openOutputStream() throws IOException {
			return outputStream;
		}
	}
	protected static class DirectClassFile extends SimpleJavaFileObject {
		private final String binaryName;
		private final byte[] rawBytes;
		private final MCObfUtil deobfuscator;
		private volatile byte[] deobfBytes;

		public DirectClassFile(String binaryName, byte[] rawBytes, MCObfUtil deobfuscator) {
			super(URI.create("mem:///" + binaryName.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
			this.binaryName = binaryName;
			this.rawBytes = rawBytes;
			this.deobfuscator = deobfuscator;
		}

		public String getBinaryName() {
			return binaryName;
		}

		@Override
		public InputStream openInputStream() {
			byte[] bytes = deobfBytes;
			if (bytes == null) {
				bytes = deobfuscator.deobfB(rawBytes);
				deobfBytes = bytes;
			}
			return new ByteArrayInputStream(bytes);
		}
	}
	protected static class FileManager extends ForwardingJavaFileManager<JavaFileManager> {
		private static final Map<String, List<DirectClassFile>> PACKAGE_CACHE = new ConcurrentHashMap<>();
		private static final Map<String, Map<String, byte[]>> ZIP_CACHE = new ConcurrentHashMap<>();
		private final ClassLoader targetClassLoader;
		private Result javaClassObject;

		protected FileManager(StandardJavaFileManager standardManager, ClassLoader targetClassLoader) {
			super(standardManager);
			this.targetClassLoader = targetClassLoader;
		}

		@Override
		public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) {
			return this.javaClassObject = new Result(className, kind);
		}

		@Override
		public Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException {
			Iterable<JavaFileObject> standard = super.list(location, packageName, kinds, recurse);
			if (location == StandardLocation.CLASS_PATH && kinds.contains(Kind.CLASS)) {
				Map<String, JavaFileObject> result = new HashMap<>();
				standard.forEach(s -> result.put(s.getName(), s));
				for (var o : PACKAGE_CACHE.computeIfAbsent(packageName, pn -> buildPackageClasses(pn))) {
					result.put(o.getName(), o);
				}
				return result.values();
			}
			return standard;
		}

		private List<DirectClassFile> buildPackageClasses(String packageName) {
			List<DirectClassFile> list = new ArrayList<>();
			if (packageName == null || targetClassLoader == null)
				return list;
			String packagePath = packageName.replace('.', '/');
			try {
				Enumeration<URL> resources = targetClassLoader.getResources(packagePath);
				while (resources.hasMoreElements()) {
					URL url = resources.nextElement();
					String protocol = url.getProtocol();
					if ("file".equals(protocol)) {
					} else if ("jar".equals(protocol) || "union".equals(protocol)) {
						try {
							URLConnection connection = url.openConnection();
							if (connection instanceof JarURLConnection jarConnection) {
								try (JarFile jarFile = jarConnection.getJarFile()) {
									for (var file : cachedFilesInZip(jarFile).entrySet()) {
										if (!file.getKey().startsWith(packagePath))
											continue;
										list.add(new DirectClassFile(file.getKey().replace('/', '.'), file.getValue(), SSUtil.MC_OBF_UTIL));
									}
								}
							} else if (connection != null) {
								String urlStr = url.toString();
								int exclIdx = urlStr.indexOf("!/");
								if (exclIdx >= 0) {
									int colonIdx = urlStr.indexOf(':');
									if (colonIdx >= 0 && colonIdx < exclIdx) {
										String pathPart = urlStr.substring(colonIdx + 1, exclIdx);
										if (pathPart.startsWith("file://")) {
											pathPart = pathPart.substring(7);
										} else if (pathPart.startsWith("file:")) {
											pathPart = pathPart.substring(5);
										}
										pathPart = URLDecoder.decode(pathPart, "UTF-8");
										int hashIdx = pathPart.indexOf('#');
										if (hashIdx >= 0) {
											pathPart = pathPart.substring(0, hashIdx);
										}
										for (var file : cachedFilesInZip(pathPart).entrySet()) {
											if (file.getKey().contains("com/mojang/brigadier")) {
												System.out.println(file.getKey());
											}
											if (!file.getKey().startsWith(packagePath))
												continue;
											list.add(new DirectClassFile(file.getKey().replace('/', '.'), file.getValue(), SSUtil.MC_OBF_UTIL));
										}
									}
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return list;
		}

		private Map<String, byte[]> cachedFilesInZip(Object jarOrPath) {
			if (jarOrPath instanceof JarFile jar) {
				return ZIP_CACHE.computeIfAbsent(jar.getName(), p -> PLZBase.filesInZip(p, ".class", true, false));
			}else if (jarOrPath instanceof String jarPath)
				return ZIP_CACHE.computeIfAbsent(jarPath, p -> PLZBase.filesInZip(p, ".class", true, false));
			return null;
		}

		@Override
		public String inferBinaryName(Location location, JavaFileObject file) {
			if (file instanceof DirectClassFile m) {
				return m.getBinaryName();
			}
			return super.inferBinaryName(location, file);
		}

		public Result getJavaClassObject() {
			return javaClassObject;
		}
	}
}
