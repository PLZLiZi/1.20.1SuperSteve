package plz.lizi.supersteve.power;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
import plz.lizi.supersteve.api.PLZBase;

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

	public static Class<?> compileToClass(String javaCode, ClassLoader cl) throws Exception {
		String fullClassName = resolveFullClassName(javaCode);
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			throw new IllegalStateException("ToolProvider.getSystemJavaCompiler() is null");
		}
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		MyFileManager fileManager = new MyFileManager(compiler.getStandardFileManager(diagnostics, null, null), cl);
		if (compiler.getTask(null, fileManager, diagnostics, List.of("-encoding", "UTF-8"), null, List.of(new MySimpleJavaFileObject(fullClassName, javaCode))).call()) {
			MyJavaClassFileObject javaClassObject = fileManager.getJavaClassObject();
			return PLZBase.defineHiddenClass(cl, HotCplr.class, fullClassName, null, javaClassObject.getBytes(), false, ClassOption.STRONG);
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
				throw new RuntimeException("compile error: " + diagnostics.getDiagnostics());
			}
		}
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

	protected static class MySimpleJavaFileObject extends SimpleJavaFileObject {
		private String contents = null;
		private final String className;

		public MySimpleJavaFileObject(String className, String contents) {
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
	protected static class MyJavaClassFileObject extends SimpleJavaFileObject {
		private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		public MyJavaClassFileObject(String name, Kind kind) {
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
	protected static class MemoryByteCodeFileObject extends SimpleJavaFileObject {
		private final String binaryName;
		private final byte[] byteCode;

		public MemoryByteCodeFileObject(String binaryName, byte[] byteCode) {
			super(URI.create("mem:///" + binaryName.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
			this.binaryName = binaryName;
			this.byteCode = byteCode;
		}

		public String getBinaryName() {
			return binaryName;
		}

		@Override
		public InputStream openInputStream() {
			return new ByteArrayInputStream(byteCode);
		}
	}
	protected static class MyFileManager extends ForwardingJavaFileManager<JavaFileManager> {
		private final ClassLoader targetClassLoader;
		private MyJavaClassFileObject javaClassObject;
		private static final Map<String, List<MemoryByteCodeFileObject>> PACKAGE_OBJS = new ConcurrentHashMap<>();

		protected MyFileManager(StandardJavaFileManager standardManager, ClassLoader targetClassLoader) {
			super(standardManager);
			this.targetClassLoader = targetClassLoader;
		}

		@Override
		public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) {
			this.javaClassObject = new MyJavaClassFileObject(className, kind);
			return javaClassObject;
		}

		@Override
		public Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException {
			Iterable<JavaFileObject> standard = super.list(location, packageName, kinds, recurse);
			if (location == StandardLocation.CLASS_PATH && kinds.contains(Kind.CLASS)) {
				List<JavaFileObject> result = new ArrayList<>();
				standard.forEach(result::add);
				List<MemoryByteCodeFileObject> cached = PACKAGE_OBJS.computeIfAbsent(packageName, pn -> buildPackageClasses(pn));
				result.addAll(cached);
				return result;
			}
			return standard;
		}

		private List<MemoryByteCodeFileObject> buildPackageClasses(String packageName) {
			List<MemoryByteCodeFileObject> list = new ArrayList<>();
			for (Class<?> clazz : PLZBase.loadedClasses(targetClassLoader)) {
				if (packageName.equals(clazz.getPackageName())) {
					byte[] bytes = PLZBase.getClassBytes(clazz.getName(), targetClassLoader);
					if (bytes != null) {
						list.add(new MemoryByteCodeFileObject(clazz.getName(), bytes));
					}
				}
			}
			return list;
		}

		@Override
		public String inferBinaryName(Location location, JavaFileObject file) {
			if (file instanceof MemoryByteCodeFileObject m) {
				return m.getBinaryName();
			}
			return super.inferBinaryName(location, file);
		}

		public MyJavaClassFileObject getJavaClassObject() {
			return javaClassObject;
		}
	}
	protected static class MyClassLoader extends ClassLoader {
		public Class<?> loadClass(String fullName, MyJavaClassFileObject javaClassObject) {
			byte[] classData = javaClassObject.getBytes();
			return this.defineClass(fullName, classData, 0, classData.length);
		}
	}
}
