package plz.lizi.supersteve.api;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import sun.misc.Unsafe;

public class PLZBase {
	public static final Unsafe UNSAFE = getUnsafe();
	public static final MethodHandles.Lookup LOOKUP = getLookup();
	public static final MethodHandle ClassLoader_defineClass0;
	public static final MethodHandle ClassLoader_defineClass1;
	public static final Map<String, Class<?>> HIDDEN_CLASSES_MAP = new ConcurrentHashMap<>();
	public static final boolean COMPRESSED_CLASS_POINTERS;
	public static final boolean COMPRESSED_OOPS;
	public static final int HEAP_OOP_SIZE;
	@SuppressWarnings("resource")
	public static final Scanner SCANNER = new Scanner(System.in);
	static {
		try {
			COMPRESSED_OOPS = Unsafe.ARRAY_OBJECT_INDEX_SCALE == 4;
			if (COMPRESSED_OOPS) {
				HEAP_OOP_SIZE = 4;
			} else {
				HEAP_OOP_SIZE = PLZBase.UNSAFE.addressSize();
			}
			boolean flag = true;
			for (String s : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
				if (s.contains("-UseCompressedClassPointers")) {
					flag = false;
					break;
				}
			}
			COMPRESSED_CLASS_POINTERS = flag;
			ClassLoader_defineClass0 = LOOKUP.findStatic(ClassLoader.class, "defineClass0", MethodType.methodType(Class.class, ClassLoader.class, Class.class, String.class, byte[].class, int.class, int.class, ProtectionDomain.class, boolean.class, int.class, Object.class));
			ClassLoader_defineClass1 = LOOKUP.findStatic(ClassLoader.class, "defineClass1", MethodType.methodType(Class.class, ClassLoader.class, String.class, byte[].class, int.class, int.class, ProtectionDomain.class, String.class));
		} catch (Throwable e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public static Unsafe getUnsafe() {
		try {
			Constructor<Unsafe> c = Unsafe.class.getDeclaredConstructor();
			c.setAccessible(true);
			return c.newInstance();
		} catch (Throwable var3) {
			throwEx(var3);
		}
		return null;
	}

	public static MethodHandles.Lookup getLookup() {
		try {
			return (MethodHandles.Lookup) sun.reflect.ReflectionFactory.getReflectionFactory().newConstructorForSerialization(MethodHandles.Lookup.class, MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, Class.class, int.class)).newInstance(Object.class, null, -1);
		} catch (Exception e) {
			throwEx(e);
		}
		return null;
	}

	public static void klassPtr(Object o, Class<?> clazz) {
		if (o == null || clazz == null)
			return;
		if (o.getClass().equals(clazz))
			return;
		try {
			LOOKUP.ensureInitialized(clazz);
			if (COMPRESSED_CLASS_POINTERS)
				UNSAFE.putIntVolatile(o, 8, UNSAFE.getIntVolatile(UNSAFE.allocateInstance(clazz), 8));
			else
				UNSAFE.putLongVolatile(o, 8, UNSAFE.getLongVolatile(UNSAFE.allocateInstance(clazz), 8));
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
	}

	public static byte[] readAllBytes(InputStream is) {
		if (is == null)
			return null;
		try {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			byte[] data = new byte[4096];
			int bytesRead;
			while ((bytesRead = is.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, bytesRead);
			}
			buffer.flush();
			return buffer.toByteArray();
		} catch (Throwable e) {
			return null;
		}
	}

	public static List<String> matchAllReplace(String s, String from, String to) {
		List<String> r = new ArrayList<>();
		if (from.isEmpty())
			return Collections.singletonList(s);
		int i = s.indexOf(from);
		if (i == -1)
			return Collections.singletonList(s);
		for (String t : matchAllReplace(s.substring(i + from.length()), from, to)) {
			r.add(s.substring(0, i) + from + t);
			r.add(s.substring(0, i) + to + t);
		}
		return r;
	}

	public static List<String> matchAllReplaceMin(String s, String from, String to) {
		return from.isEmpty() ? Collections.singletonList(s) : s.contains(from) ? Stream.concat(matchAllReplace(s.substring(s.indexOf(from) + from.length()), from, to).stream().map(t -> s.substring(0, s.indexOf(from)) + from + t), matchAllReplace(s.substring(s.indexOf(from) + from.length()), from, to).stream().map(t -> s.substring(0, s.indexOf(from)) + to + t)).collect(Collectors.toList()) : Collections.singletonList(s);
	}

	public static byte[] getClassBytes(String className, ClassLoader... loaders) {
		className = className.replace(".class", "").replace(".", "/");
		String fileName = className + ".class";
		if (className.startsWith("java/")) {
			InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(fileName);
			if (is != null)
				return readAllBytes(is);
		}
		for (ClassLoader loader : loaders) {
			InputStream is = loader.getResourceAsStream(fileName);
			if (is == null)
				continue;
			byte[] buf = readAllBytes(is);
			if (buf == null || !className.equals(dumpClassName(buf)))
				continue;
			return buf;
		}
		return null;
	}

	public static byte[] getClassBytes(String jarPath, String className) {
		try (JarFile jarFile = new JarFile(jarPath)) {
			String classPath = className.replace('.', '/') + ".class";
			try (InputStream is = jarFile.getInputStream(jarFile.getJarEntry(classPath))) {
				return readAllBytes(is);
			}
		} catch (Throwable t) {
			return null;
		}
	}

	public static byte[] getClassBytes(Class<?> clazz, boolean fromSource) {
		if (clazz == null)
			return null;
		if (fromSource) {
			return getClassBytes(getJarPath(clazz), clazz.getName());
		} else {
			return getClassBytes(clazz.getName(), clazz.getClassLoader());
		}
	}

	public static String getJarPath() {
		return getJarPath(PLZBase.class);
	}

	public static String getJarPath(Class<?> clazz) {
		try {
			String name = clazz.getName().replace('.', '/') + ".class";
			for (ClassLoader loader : new HashSet<>(List.of(clazz.getClassLoader(), Thread.currentThread().getContextClassLoader(), ClassLoader.getSystemClassLoader(), ClassLoader.getPlatformClassLoader()))) {
				URL url = loader.getResource(name);
				if (url == null)
					continue;
				String decoded = URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8);
				return new File(decoded.substring(0, decoded.lastIndexOf(".jar") + 4)).getAbsolutePath();
			}
		} catch (Throwable e) {
		}
		try {
			String decoded = URLDecoder.decode(clazz.getProtectionDomain().getCodeSource().getLocation().getPath(), StandardCharsets.UTF_8);
			return new File(decoded.substring(0, decoded.lastIndexOf(".jar") + 4)).getAbsolutePath();
		} catch (Throwable e) {
		}
		return "";
	}

	public static Map<String, byte[]> filesInZip(String zipPath, String suffix, boolean noSuffix, boolean noRead) {
		Map<String, byte[]> result = new HashMap<>();
		File file = new File(zipPath);
		try (ZipFile jarFile = new ZipFile(file)) {
			Enumeration<? extends ZipEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				String entryName = entry.getName();
				if (entryName.endsWith(suffix) && !entry.isDirectory()) {
					byte[] value = null;
					if (!noRead) {
						try (InputStream is = jarFile.getInputStream(entry)) {
							value = readAllBytes(is);
						} catch (IOException e1) {
							throwEx(e1);
						}
					}
					result.put(noSuffix ? entryName.replace(suffix, "") : entryName, value);
				}
			}
		} catch (Exception e) {
			throwEx(e);
			return null;
		}
		return result;
	}

	public static Map<Object, Object> getJarManifest(String jarPath) {
		try (JarFile jarFile = new JarFile(jarPath)) {
			Manifest manifest = jarFile.getManifest();
			return (manifest != null) ? manifest.getMainAttributes() : null;
		} catch (IOException e) {
			return new HashMap<>();
		}
	}

	public static String readFile(String filePath) throws Exception {
		Path path = Paths.get(filePath);
		File file = path.toFile();
		if (!file.exists()) {
			new RuntimeException("null");
		}
		byte[] encoded = Files.readAllBytes(path);
		return new String(encoded, StandardCharsets.UTF_8);
	}

	public static void writeFile(String filePath, String content) throws Throwable {
		File file = new File(filePath);
		if (!file.exists()) {
			file.createNewFile();
		}
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(content);
		}
	}

	public static String getStackTrace() {
		StringBuilder builder = new StringBuilder();
		for (StackTraceElement stackTrace : Thread.currentThread().getStackTrace()) {
			builder.append(stackTrace);
			builder.append("\n");
		}
		return builder.toString();
	}

	public static void openAccess(Module targetModule, Class<?> myClass) {
		try {
			if (targetModule.canRead(myClass.getModule())) {
				return;
			}
			LOOKUP.unreflect(Module.class.getDeclaredMethod("implAddReads", Module.class)).bindTo(targetModule).invoke(myClass.getModule());
		} catch (Throwable t) {
			throwEx(t);
		}
	}

	public static String input(String s) {
		System.out.print(s);
		return SCANNER.nextLine();
	}

	public static void cls() {
		try {
			new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
		} catch (Exception e) {
		}
	}

	public static void pause() {
		try {
			new ProcessBuilder("cmd", "/c", "pause").inheritIO().start().waitFor();
		} catch (Exception e) {
		}
	}

	public static String[] splitFirst(String input, String target) {
		int firstDotIndex = input.indexOf(target);
		if (firstDotIndex == -1) {
			return new String[] { input, "" };
		}
		return new String[] { input.substring(0, firstDotIndex), input.substring(firstDotIndex + 1) };
	}

	public static String[] splitLast(String input, String target) {
		int lastIndex = input.lastIndexOf(target);
		if (lastIndex == -1) {
			return new String[] { input, "" };
		}
		return new String[] { input.substring(0, lastIndex), input.substring(lastIndex + 1) };
	}

	public static List<String> dumpCmdline(String cmdline) {
		List<String> argv = new ArrayList<>();
		StringBuilder arg = new StringBuilder();
		boolean inQuotes = false;
		char quoteChar = 0;
		for (char c : cmdline.toCharArray()) {
			if (c == '"' || c == '\'') {
				if (!inQuotes) {
					inQuotes = true;
					quoteChar = c;
				} else if (quoteChar == c)
					inQuotes = false;
				else
					arg.append(c);
			} else if (Character.isWhitespace(c) && !inQuotes) {
				if (arg.length() > 0) {
					argv.add(arg.toString());
					arg.setLength(0);
				}
			} else
				arg.append(c);
		}
		if (arg.length() > 0)
			argv.add(arg.toString());
		return argv;
	}

	public static void selectCopyFile(String sourceDir, String targetDirForSpecified, String targetDirForOthers, List<String> specifiedSuffixes) throws IOException {
		Files.createDirectories(Paths.get(targetDirForSpecified));
		Files.createDirectories(Paths.get(targetDirForOthers));
		Files.walk(Paths.get(sourceDir)).filter(Files::isRegularFile).forEach(sourcePath -> {
			try {
				String fileName = sourcePath.getFileName().toString();
				int dotIndex = fileName.lastIndexOf('.');
				String suffix = (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
				String targetDir = specifiedSuffixes.contains(suffix.toLowerCase()) ? targetDirForSpecified : targetDirForOthers;
				Path relativePath = Paths.get(sourceDir).relativize(sourcePath);
				Path targetPath = Paths.get(targetDir, relativePath.toString());
				Files.createDirectories(targetPath.getParent());
				Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	public static List<String> filesInDir(String folderPath, String extension, int deep) {
		deep = (deep == -1 ? -1 : Math.max(0, deep - 1));
		List<String> filePaths = new ArrayList<>();
		File folder = new File(folderPath);
		if (!folder.exists() || !folder.isDirectory()) {
			return filePaths;
		}
		String suffix = extension.startsWith(".") ? extension : ("." + extension);
		suffix = suffix.toLowerCase();
		File[] files = folder.listFiles();
		if (files == null) {
			return filePaths;
		}
		for (File file : files) {
			if (file.isDirectory()) {
				if (deep == -1 || deep > 0) {
					filePaths.addAll(filesInDir(file.getPath(), extension, deep));
				}
			} else {
				String fileName = file.getName().toLowerCase();
				if (fileName.endsWith(suffix)) {
					filePaths.add(file.getPath());
				}
			}
		}
		return filePaths;
	}

	public static Set<Field> getFields(Class<?> clazz) {
		Set<Field> fields = new HashSet<>();
		for (Class<?> current = clazz; current != Object.class; current = current.getSuperclass()) {
			for (Field field : current.getDeclaredFields()) {
				fields.add(field);
			}
		}
		return fields;
	}

	public static Set<Method> getMethods(Class<?> clazz) {
		Set<Method> methods = new HashSet<>();
		for (Class<?> current = clazz; current != Object.class; current = current.getSuperclass()) {
			for (Method field : current.getDeclaredMethods()) {
				methods.add(field);
			}
		}
		return methods;
	}

	public static void copyFields(Object old, Object next) {
		Map<String, Object> oldFieldMap = new HashMap<>();
		for (Field field : getFields(old.getClass())) {
			try {
				if (!Modifier.isStatic(field.getModifiers())) {
					oldFieldMap.put(field.getName(), getField(old, false, field));
				}
			} catch (Throwable e) {
			}
		}
		for (Field field : getFields(next.getClass())) {
			if (oldFieldMap.containsKey(field.getName()) && !Modifier.isStatic(field.getModifiers())) {
				Object obj = oldFieldMap.get(field.getName());
				if (obj != null) {
					setField(next, false, field, obj);
				}
			}
		}
	}

	public static String toVMName(String type) {
		if (type == null || type.isEmpty()) {
			return type;
		}
		switch (type) {
			case "int":
				return "I";
			case "float":
				return "F";
			case "long":
				return "J";
			case "double":
				return "D";
			case "boolean":
				return "Z";
			case "byte":
				return "B";
			case "char":
				return "C";
			case "short":
				return "S";
			case "void":
				return "V";
		}
		if (type.endsWith("[]")) {
			int dimensions = 0;
			while (type.endsWith("[]")) {
				dimensions++;
				type = type.substring(0, type.length() - 2);
			}
			String baseType = toVMName(type);
			return "[".repeat(dimensions) + baseType;
		}
		if (type.contains(".")) {
			return "L" + type.replace('.', '/') + ";";
		}
		return type;
	}

	public static String fromVMName(String vmType) {
		if (vmType == null || vmType.isEmpty()) {
			return vmType;
		}
		switch (vmType) {
			case "I":
				return "int";
			case "F":
				return "float";
			case "J":
				return "long";
			case "D":
				return "double";
			case "Z":
				return "boolean";
			case "B":
				return "byte";
			case "C":
				return "char";
			case "S":
				return "short";
			case "V":
				return "void";
		}
		if (vmType.startsWith("[")) {
			int arrayDepth = 0;
			while (arrayDepth < vmType.length() && vmType.charAt(arrayDepth) == '[') {
				arrayDepth++;
			}
			String elementType = fromVMName(vmType.substring(arrayDepth));
			return elementType + "[]".repeat(arrayDepth);
		}
		if (vmType.startsWith("L") && vmType.endsWith(";")) {
			vmType = vmType.substring(1, vmType.length() - 1);
		}
		return vmType.replace('/', '.');
	}

	@SuppressWarnings("unchecked")
	public static <T> T copy(T original) {
		if (original == null)
			return null;
		return copy(original, (Class<T>) original.getClass());
	}

	@SuppressWarnings("unchecked")
	public static <S, T extends S> T copy(S original, Class<T> exClass) {
		if (original == null)
			return null;
		if (original.getClass().isPrimitive())
			return (T) original;
		if (original.getClass().isArray()) {
			int length = java.lang.reflect.Array.getLength(original);
			Object newArray = java.lang.reflect.Array.newInstance(exClass.getComponentType(), length);
			System.arraycopy(original, 0, newArray, 0, length);
			return (T) newArray;
		}
		try {
			T copy = (T) UNSAFE.allocateInstance(exClass);
			copyFields(original, copy);
			return copy;
		} catch (Throwable e) {
			throwEx(e);
			return null;
		}
	}

	public static String simpleClassName(Class<?> clazz) {
		return simpleClassName(clazz.getName());
	}

	public static String simpleClassName(String name) {
		return fromVMName(name).split("\\$\\$")[0].split("/")[0];
	}

	public static String lowCaseFlowString(String str, int size, double speed) {
		StringBuilder builder = new StringBuilder(str);
		long time = System.currentTimeMillis();
		for (int g = 0; g < size; g++) {
			int i = (int) (((long) time / (speed * 1000) + g) % builder.length());
			builder.setCharAt(i, Character.toLowerCase(builder.charAt(i)));
		}
		return builder.toString();
	}

	public static String waveString(String text, int waveHeight, double speed, double distance) {
		char[] chars = text.toCharArray();
		int length = chars.length;
		int totalRows = waveHeight + 2;
		char[][] matrix = new char[totalRows][length];
		for (char[] row : matrix) {
			Arrays.fill(row, ' ');
		}
		double time = System.currentTimeMillis() * -0.001 * speed;
		for (int i = 0; i < length; i++) {
			double wave = Math.sin(time + i * distance) * (waveHeight / 2 + 2);
			int row = (int) (waveHeight / 2 - wave + 1);
			row = Math.max(0, Math.min(row, totalRows - 1));
			matrix[row][i] = chars[i];
		}
		StringBuilder sb = new StringBuilder();
		for (char[] row : matrix) {
			sb.append(row).append("\n");
		}
		return sb.toString();
	}

	public static void unZip(String jarPath, String destDirPath) throws Throwable {
		File zipFile = new File(jarPath);
		if (!zipFile.exists()) {
			return;
		}
		File destDir = new File(destDirPath);
		if (!destDir.exists() && !destDir.mkdirs()) {
			return;
		}
		String targetCanonicalPath = destDir.getCanonicalPath();
		try (ZipFile zip = new ZipFile(zipFile)) {
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				String entryName = entry.getName();
				File destFile = new File(destDir, entryName);
				String destCanonicalPath = destFile.getCanonicalPath();
				if (!destCanonicalPath.startsWith(targetCanonicalPath + File.separator)) {
					return;
				}
				if (entry.isDirectory()) {
					if (!destFile.exists() && !destFile.mkdirs()) {
						return;
					}
				} else {
					File parent = destFile.getParentFile();
					if (parent != null && !parent.exists() && !parent.mkdirs()) {
						return;
					}
					try (InputStream is = zip.getInputStream(entry); OutputStream os = new BufferedOutputStream(new FileOutputStream(destFile))) {
						os.write(readAllBytes(is));
					}
				}
			}
		}
	}

	public static List<Class<?>> loadedClasses(ClassLoader loader) {
		try {
			return new ArrayList<>((ArrayList<Class<?>>) LOOKUP.findGetter(ClassLoader.class, "classes", ArrayList.class).invoke(loader));
		} catch (Throwable e) {
			throwEx(e);
			return null;
		}
	}

	public static Class<?> defineClass(ClassLoader loader, String name, byte[] buf) {
		try {
			return (Class<?>) ClassLoader_defineClass1.invoke(loader, name, buf, 0, buf.length, null, null);
		} catch (Throwable e1) {
			try {
				return Class.forName(name);
			} catch (Exception e) {
				e1.addSuppressed(e);
				throwEx(e1);
				return null;
			}
		}
	}

	public static Class<?> defineClassInPackage(ClassLoader loader, Class<?> lookup, String name) {
		try {
			return defineClass(loader, name, getClassBytes(getJarPath(lookup), name));
		} catch (Throwable e) {
			throwEx(e);
			return null;
		}
	}

	public static Class<?> defineHiddenClass(ClassLoader loader, Class<?> lookup, String name, String defName, byte[] buf, boolean once, ClassOption... options) {
		if (defName == null)
			defName = name;
		if (once && HIDDEN_CLASSES_MAP.get(name + ":" + defName) != null)
			return HIDDEN_CLASSES_MAP.get(name + ":" + defName);
		int flags = 2 | ClassOption.optionsToFlag(Set.of(options));
		if (loader == null || loader == ClassLoader.getPlatformClassLoader()) {
			flags |= 8;
		}
		try {
			Class<?> clazz = (Class<?>) ClassLoader_defineClass0.invoke(loader, lookup, defName, buf, 0, buf.length, null, true, flags, null);
			if (once)
				HIDDEN_CLASSES_MAP.put(name + ":" + defName, clazz);
			return clazz;
		} catch (Throwable e1) {
			throwEx(e1);
			return null;
		}
	}

	public static Class<?> defineHiddenClassInPackage(ClassLoader loader, Class<?> lookup, String name, String defName, boolean once, ClassOption... options) {
		if (defName == null)
			defName = name;
		if (once && HIDDEN_CLASSES_MAP.get(name + ":" + defName) != null)
			return HIDDEN_CLASSES_MAP.get(name + ":" + defName);
		try {
			return defineHiddenClass(loader, lookup, name, defName, getClassBytes(getJarPath(lookup), name), once, options);
		} catch (Throwable e) {
			throwEx(e);
			return null;
		}
	}

	public static Class<?> findClass(String name) {
		try {
			return Class.forName(name, false, ClassLoader.getSystemClassLoader());
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	public static Field findField(Class<?> clazz, String name) {
		Class<?> current = clazz;
		while (current != null) {
			try {
				return current.getDeclaredField(name);
			} catch (NoSuchFieldException e) {
				current = current.getSuperclass();
			}
		}
		throwEx(new NoSuchFieldException(name));
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T> T getField(Object target, boolean isStatic, String name) {
		return (T) getField(target, isStatic, findField((isStatic && target instanceof Class<?> c) ? c : target.getClass(), name));
	}

	public static Object getField(Object target, boolean isStatic, Field f) {
		try {
			MethodHandle getter = LOOKUP.unreflectGetter(f);
			return isStatic ? getter.invoke() : getter.invoke(target);
		} catch (Throwable e) {
			Object base = isStatic ? UNSAFE.staticFieldBase(f) : target;
			long offset = isStatic ? UNSAFE.staticFieldOffset(f) : UNSAFE.objectFieldOffset(f);
			switch (f.getType().getName()) {
				case "int":
					return UNSAFE.getIntVolatile(base, offset);
				case "long":
					return UNSAFE.getLongVolatile(base, offset);
				case "boolean":
					return UNSAFE.getBooleanVolatile(base, offset);
				case "byte":
					return UNSAFE.getByteVolatile(base, offset);
				case "char":
					return UNSAFE.getCharVolatile(base, offset);
				case "short":
					return UNSAFE.getShortVolatile(base, offset);
				case "float":
					return UNSAFE.getFloatVolatile(base, offset);
				case "double":
					return UNSAFE.getDoubleVolatile(base, offset);
				default:
					return UNSAFE.getObjectVolatile(base, offset);
			}
		}
	}

	public static Object setField(Object target, boolean isStatic, String name, Object value) {
		return setField(target, isStatic, findField((isStatic && target instanceof Class<?> c) ? c : target.getClass(), name), value);
	}

	public static Object setField(Object target, boolean isStatic, Field f, Object value) {
		Object old = getField(target, isStatic, f);
		try {
			MethodHandle setter = LOOKUP.unreflectSetter(f);
			if (target instanceof Class) {
				setter.invoke(value);
			} else {
				setter.invoke(target, value);
			}
		} catch (Throwable e) {
			Object base = isStatic ? UNSAFE.staticFieldBase(f) : target;
			long offset = isStatic ? UNSAFE.staticFieldOffset(f) : UNSAFE.objectFieldOffset(f);
			switch (f.getType().getName()) {
				case "int":
					UNSAFE.putIntVolatile(base, offset, (int) value);
				case "long":
					UNSAFE.putLongVolatile(base, offset, (long) value);
				case "boolean":
					UNSAFE.putBooleanVolatile(base, offset, (boolean) value);
				case "byte":
					UNSAFE.putByteVolatile(base, offset, (byte) value);
				case "char":
					UNSAFE.putCharVolatile(base, offset, (char) value);
				case "short":
					UNSAFE.putShortVolatile(base, offset, (short) value);
				case "float":
					UNSAFE.putFloatVolatile(base, offset, (float) value);
				case "double":
					UNSAFE.putDoubleVolatile(base, offset, (double) value);
				default:
					UNSAFE.putObjectVolatile(base, offset, value);
			}
		}
		return old;
	}

	public static Method findMethod(Class<?> clazz, String name, Class<?>... argTypes) {
		final int argLen = argTypes.length;
		Class<?> current = clazz;
		while (current != null) {
			for (Method m : current.getDeclaredMethods()) {
				if (!m.getName().equals(name))
					continue;
				Class<?>[] declared = m.getParameterTypes();
				if (declared.length != argLen)
					continue;
				boolean match = true;
				for (int i = 0; i < argLen; i++) {
					Class<?> provided = argTypes[i];
					if (provided != null && provided != Object.class && !declared[i].isAssignableFrom(provided)) {
						match = false;
						break;
					}
				}
				if (match)
					return m;
			}
			current = current.getSuperclass();
		}
		throwEx(new NoSuchMethodException(name));
		return null;
	}

	public static Object invoke(Object target, boolean isStatic, String name, Object... args) {
		Class<?> clazz = (isStatic && target instanceof Class) ? (Class<?>) target : target.getClass();
		Class<?>[] argTypes = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++) {
			argTypes[i] = args[i].getClass();
		}
		try {
			MethodHandle methodHandle = LOOKUP.unreflect(findMethod(clazz, name, argTypes));
			if (isStatic && target instanceof Class) {
				return methodHandle.invokeWithArguments(args);
			} else {
				Object[] combinedArgs = new Object[args.length + 1];
				combinedArgs[0] = target;
				System.arraycopy(args, 0, combinedArgs, 1, args.length);
				return methodHandle.invokeWithArguments(combinedArgs);
			}
		} catch (Throwable e) {
			throwEx(e);
			return null;
		}
	}

	public static byte[] readFileBytes(String fileName) throws IOException {
		InputStream in = null;
		ByteArrayOutputStream out = null;
		try {
			in = new BufferedInputStream(new FileInputStream(fileName));
			out = new ByteArrayOutputStream();
			out.write(readAllBytes(in));
		} finally {
			if (in != null) {
				in.close();
			}
		}
		return out.toByteArray();
	}

	public static void writeFileBytes(String fileName, byte[] bytes, boolean append) throws IOException {
		OutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(fileName, append));
			out.write(bytes);
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	public static void writeFileBytes(String fileName, byte[] bytes) throws IOException {
		writeFileBytes(fileName, bytes, false);
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] reverse(T[] original) {
		if (original == null) {
			return null;
		}
		T[] reversed = (T[]) Array.newInstance(original.getClass().getComponentType(), original.length);
		for (int i = 0; i < original.length; i++) {
			reversed[i] = original[original.length - 1 - i];
		}
		return reversed;
	}

	public static int[] reverse(int[] original) {
		if (original == null) {
			return null;
		}
		int[] reversed = new int[original.length];
		for (int i = 0; i < original.length; i++) {
			reversed[i] = original[original.length - 1 - i];
		}
		return reversed;
	}

	public static String dumpClassName(byte[] bytes) {
		int int1 = 0, offset = 10;
		int[] cpInfoOffsets;
		int int2, cpInfoSize, currentCpInfoIndex;
		byte val;
		for (int2 = (bytes[8] & 0xFF) << 8 | bytes[9] & 0xFF, cpInfoOffsets = new int[int2], currentCpInfoIndex = 1; currentCpInfoIndex < int2; offset += cpInfoSize) {
			cpInfoOffsets[currentCpInfoIndex++] = offset + 1;
			val = bytes[offset];
			if (val == 9 || val == 10 || val == 11 || val == 3 || val == 4 || val == 12 || val == 17 || val == 18) {
				cpInfoSize = 5;
			} else if (val == 5 || val == 6) {
				cpInfoSize = 9;
				currentCpInfoIndex++;
			} else if (val == 1) {
				cpInfoSize = 3 + ((bytes[offset + 1] & 0xFF) << 8 | bytes[offset + 2] & 0xFF);
				if (cpInfoSize > int1) {
					int1 = cpInfoSize;
				}
			} else if (val == 15) {
				cpInfoSize = 4;
			} else if (val == 7 || val == 8 || val == 16 || val == 20 || val == 19) {
				cpInfoSize = 3;
			} else {
				// throw new IllegalArgumentException();
				return null;
			}
		}
		char[] charBuffer = new char[int1];
		int1 = (bytes[offset + 2] & 0xFF) << 8 | bytes[offset + 3] & 0xFF;
		offset = cpInfoOffsets[int1];
		if (bytes[offset - 1] == 7) {
			int1 = (bytes[offset] & 0xFF) << 8 | bytes[offset + 1] & 0xFF;
			if (int1 == 0) {
				return null;
				// throw new ClassFormatError();
			}
			offset = cpInfoOffsets[int1];
			int2 = offset + 2 + ((bytes[offset] & 0xFF) << 8 | bytes[offset + 1] & 0xFF);
			offset += 2;
			int1 = 0;
			while (offset < int2) {
				val = bytes[offset++];
				if ((val & 0x80) == 0) {
					charBuffer[int1++] = (char) (val & 0x7F);
				} else if ((val & 0xE0) == 0xC0) {
					charBuffer[int1++] = (char) (((val & 0x1F) << 6) + (bytes[offset++] & 0x3F));
				} else {
					charBuffer[int1++] = (char) (((val & 0xF) << 12) + ((bytes[offset++] & 0x3F) << 6) + (bytes[offset++] & 0x3F));
				}
			}
			return new String(charBuffer, 0, int1);
		}
		return null;
		// throw new ClassFormatError("this_class item: #" + int1 + " not a CONSTANT_Class_info");
	}

	public static String dumpSuperName(byte[] bytes) {
		int index = 1;
		int offset = 10;
		int maxStringLength = 0;
		int currentByte;
		int val = (bytes[8] & 0xFF) << 8 | bytes[9] & 0xFF;
		int[] cpInfoOffsets = new int[val];
		while (index < val) {
			cpInfoOffsets[index++] = offset + 1;
			int cpInfoSize;
			currentByte = bytes[offset];
			if (currentByte == 9 || currentByte == 10 || currentByte == 11 || currentByte == 12 || currentByte == 3 || currentByte == 4 || currentByte == 18 || currentByte == 17) {
				cpInfoSize = 5;
			} else if (currentByte == 5 || currentByte == 6) {
				cpInfoSize = 9;
				index++;
			} else if (currentByte == 1) {
				cpInfoSize = 3 + ((bytes[offset + 1] & 0xFF) << 8 | bytes[offset + 2] & 0xFF);
				if (cpInfoSize > maxStringLength) {
					maxStringLength = cpInfoSize;
				}
			} else if (currentByte == 15) {
				cpInfoSize = 4;
			} else if (currentByte == 7 || currentByte == 8 || currentByte == 16 || currentByte == 20 || currentByte == 19) {
				cpInfoSize = 3;
			} else {
				throw new IllegalArgumentException();
			}
			offset += cpInfoSize;
		}
		char[] charBuffer = new char[maxStringLength];
		offset = cpInfoOffsets[(bytes[offset + 4] & 0xFF) << 8 | bytes[offset + 5] & 0xFF];
		val = (bytes[offset] & 0xFF) << 8 | bytes[offset + 1] & 0xFF;
		if (offset == 0 || val == 0) {
			throw new ClassFormatError();
		}
		offset = cpInfoOffsets[val];
		val = offset + 2 + ((bytes[offset] & 0xFF) << 8 | bytes[offset + 1] & 0xFF);
		offset += 2;
		index = 0;
		while (offset < val) {
			currentByte = bytes[offset++];
			if ((currentByte & 0x80) == 0) {
				charBuffer[index++] = (char) (currentByte & 0x7F);
			} else if ((currentByte & 0xE0) == 0xC0) {
				charBuffer[index++] = (char) (((currentByte & 0x1F) << 6) + (bytes[offset++] & 0x3F));
			} else {
				charBuffer[index++] = (char) (((currentByte & 0xF) << 12) + ((bytes[offset++] & 0x3F) << 6) + (bytes[offset++] & 0x3F));
			}
		}
		return new String(charBuffer, 0, index);
	}

	public static String[] dumpInterfaceNames(byte[] bytes) {
		int cnt = (bytes[8] & 0xFF) << 8 | bytes[9] & 0xFF;
		int[] cpInfoOffsets;
		int index = 1;
		int offset = 10;
		int maxStringLength = 0;
		int currentByte;
		cpInfoOffsets = new int[cnt];
		while (index < cnt) {
			cpInfoOffsets[index++] = offset + 1;
			int cpInfoSize;
			currentByte = bytes[offset];
			if (currentByte == 9 || currentByte == 10 || currentByte == 11 || currentByte == 12 || currentByte == 3 || currentByte == 4 || currentByte == 18 || currentByte == 17) {
				cpInfoSize = 5;
			} else if (currentByte == 5 || currentByte == 6) {
				cpInfoSize = 9;
				index++;
			} else if (currentByte == 1) {
				cpInfoSize = 3 + ((bytes[offset + 1] & 0xFF) << 8 | bytes[offset + 2] & 0xFF);
				if (cpInfoSize > maxStringLength) {
					maxStringLength = cpInfoSize;
				}
			} else if (currentByte == 15) {
				cpInfoSize = 4;
			} else if (currentByte == 7 || currentByte == 8 || currentByte == 16 || currentByte == 20 || currentByte == 19) {
				cpInfoSize = 3;
			} else {
				throw new IllegalArgumentException();
			}
			offset += cpInfoSize;
		}
		index = offset + 6;
		cnt = (bytes[index] & 0xFF) << 8 | bytes[index + 1] & 0xFF;
		String[] interfaces = new String[cnt];
		if (cnt > 0) {
			char[] charBuffer = new char[maxStringLength];
			for (int i = 0; i < cnt; ++i) {
				index += 2;
				offset = cpInfoOffsets[(bytes[index] & 0xFF) << 8 | bytes[index + 1] & 0xFF];
				int constantPoolEntryIndex = (bytes[offset] & 0xFF) << 8 | bytes[offset + 1] & 0xFF;
				if (offset == 0 || constantPoolEntryIndex == 0) {
					throw new ClassFormatError();
				}
				offset = cpInfoOffsets[constantPoolEntryIndex];
				int endOffset = offset + 2 + ((bytes[offset] & 0xFF) << 8 | bytes[offset + 1] & 0xFF);
				int strLength = 0;
				offset += 2;
				while (offset < endOffset) {
					currentByte = bytes[offset++];
					if ((currentByte & 0x80) == 0) {
						charBuffer[strLength++] = (char) (currentByte & 0x7F);
					} else if ((currentByte & 0xE0) == 0xC0) {
						charBuffer[strLength++] = (char) (((currentByte & 0x1F) << 6) + (bytes[offset++] & 0x3F));
					} else {
						charBuffer[strLength++] = (char) (((currentByte & 0xF) << 12) + ((bytes[offset++] & 0x3F) << 6) + (bytes[offset++] & 0x3F));
					}
				}
				interfaces[i] = new String(charBuffer, 0, strLength);
			}
		}
		return interfaces;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Throwable> void throwEx(Throwable t) throws T {
		throw (T) t;
	}

	public static String readThrowable(Throwable t) {
		StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}

	public static String sha256(byte[] data) throws Exception {
		byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
		StringBuilder sb = new StringBuilder(hash.length * 2);
		for (byte b : hash) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	public static Class<?> antiRefGetMF(Class<?> clazz) {
		Object rd = PLZBase.invoke(clazz, false, "reflectionData");
		PLZBase.setField(rd, false, "publicFields", new Field[0]);
		PLZBase.setField(rd, false, "publicMethods", new Method[0]);
		PLZBase.setField(rd, false, "declaredFields", new Field[0]);
		PLZBase.setField(rd, false, "declaredMethods", new Method[0]);
		PLZBase.setField(rd, false, "declaredPublicFields", new Field[0]);
		PLZBase.setField(rd, false, "declaredPublicMethods", new Method[0]);
		return clazz;
	}

	public static String getClassTypeKeyword(Class<?> clazz) {
		if (clazz == null)
			return "null";
		if (clazz.isEnum())
			return "enum";
		if (clazz.isAnnotation())
			return "@interface";
		if (clazz.isInterface())
			return "interface";
		if (clazz.isRecord())
			return "record";
		if (clazz.isArray())
			return "array";
		if (clazz.isPrimitive())
			return "primitive";
		return "class";
	}

	public static String fieldToString(Field f) {
		if (f == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		int modifiers = f.getModifiers();
		if (modifiers != 0) {
			sb.append(Modifier.toString(modifiers)).append(" ");
		}
		String typeName = f.getGenericType().getTypeName();
		sb.append(typeName).append(" ");
		sb.append(f.getName());
		return sb.toString();
	}

	public static String methodToString(Method m) {
		if (m == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		int modifiers = m.getModifiers();
		if (modifiers != 0) {
			sb.append(Modifier.toString(modifiers)).append(" ");
		}
		String returnType = m.getGenericReturnType().getTypeName();
		sb.append(returnType).append(" ");
		sb.append(m.getName());
		sb.append("(");
		Type[] paramTypes = m.getGenericParameterTypes();
		for (int i = 0; i < paramTypes.length; i++) {
			sb.append(paramTypes[i].getTypeName());
			if (i < paramTypes.length - 1) {
				sb.append(", ");
			}
		}
		sb.append(")");
		return sb.toString();
	}

	public static String classToString(Class<?> clazz) {
		if (clazz == null)
			return "null";
		StringBuilder sb = new StringBuilder();
		sb.append(Modifier.toString(clazz.getModifiers())).append(" ").append(getClassTypeKeyword(clazz)).append(clazz.isHidden() ? "(hidden)" : "").append(" ").append(clazz.getName());
		if (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
			sb.append(" extends ").append(clazz.getSuperclass().getName());
		}
		sb.append(" {\n");
		for (var field : getFields(clazz)) {
			sb.append("	").append(fieldToString(field)).append(";\n");
		}
		sb.append("\n");
		for (var method : getMethods(clazz)) {
			sb.append("	").append(methodToString(method)).append(";\n");
		}
		sb.append("}");
		return sb.toString();
	}
	
	public static float progress(float f) {
		return Math.max(0F, Math.min(1F, f));
	}
}
