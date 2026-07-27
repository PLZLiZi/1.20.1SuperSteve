package plz.lizi.supersteve.power;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import plz.lizi.supersteve.api.PLZBase;

public class ClassStruct {
	@SuppressWarnings("unused")
	private static final String INFO = "POWER BY ENDOFPLZ";
	private static final Map<String, ClassStruct> CACHED_STRUCTS = new ConcurrentHashMap<>();
	private static final Map<String, Set<SimpleEntry<String, Relation>>> RELS_CHAINS = new HashMap<>();
	public static Function<String, byte[]> CODE_GETTER = null;
	private final ClassNode node;
	private final String zhis;
	private final String supr;
	private final int access;
	private final List<String> impls;
	private final Map<String, FieldNode> fields;
	private final Map<String, MethodNode> methods;

	private static byte[] getCode(String name) {
		if (CODE_GETTER != null)
			return CODE_GETTER.apply(name);
		return PLZBase.getClassBytes(name, ClassStruct.class.getClassLoader(), ClassLoader.getSystemClassLoader());
	}

	public static ClassStruct as(ClassNode cn) {
		if (cn == null)
			return null;
		return new ClassStruct(cn);
	}

	public static ClassStruct as(byte[] buf) {
		if (buf == null)
			return null;
		ClassReader cr = null;
		try {
			cr = new ClassReader(buf);
		} catch (Throwable e) {
			PLZBase.throwEx(new Throwable("ClassReader error " + PLZBase.dumpClassName(buf) + ": " + e.getMessage()));
		}
		ClassNode cn = new ClassNode();
		cr.accept(cn, ClassReader.EXPAND_FRAMES);
		return as(cn);
	}

	public static ClassStruct as(Class<?> clazz) {
		if (clazz == null)
			return null;
		String vmname = clazz.getName().replace(".", "/");
		if (CACHED_STRUCTS.containsKey(vmname))
			return CACHED_STRUCTS.get(vmname);
		ClassStruct cs = as(PLZBase.getClassBytes(clazz, false));
		if (cs != null)
			CACHED_STRUCTS.putIfAbsent(cs.zhis, cs);
		return cs;
	}

	public static ClassStruct as(String name) {
		if (name == null)
			return null;
		name = name.replace(".", "/");
		if (CACHED_STRUCTS.containsKey(name))
			return CACHED_STRUCTS.get(name);
		ClassStruct cs = as(getCode(name));
		if (cs != null)
			CACHED_STRUCTS.putIfAbsent(cs.zhis, cs);
		return cs;
	}

	private ClassStruct(ClassNode cn) {
		this.zhis = cn.name;
		this.supr = cn.superName;
		this.impls = cn.interfaces;
		this.access = cn.access;
		this.node = cn;
		Map<String, FieldNode> fields = new HashMap<>();
		for (FieldNode fn : cn.fields) {
			fields.put(fn.name, fn);
		}
		this.fields = Collections.unmodifiableMap(fields);
		Map<String, MethodNode> methods = new HashMap<>();
		for (MethodNode mn : cn.methods) {
			methods.put(mn.name + mn.desc, mn);
		}
		this.methods = Collections.unmodifiableMap(methods);
	}

	public String name() {
		return zhis;
	}

	public String superName() {
		return supr;
	}

	public List<String> impls() {
		return impls;
	}

	public int access() {
		return access;
	}

	public ClassStruct down() {
		return as(superName());
	}

	public ClassNode node() {
		return node;
	}

	public MethodNode getMethod(String sign) {
		return methods.get(sign);
	}

	public FieldNode getField(String name) {
		return fields.get(name);
	}

	public Map<String, MethodNode> getMethods() {
		return methods;
	}

	public Map<String, FieldNode> getFields() {
		return fields;
	}

	/**
	 * get realtion chain
	 * 
	 * @param name
	 * @param relation if EXTENDS return value will include self
	 * @return relation chain
	 */
	public static synchronized Set<String> getRelsChain(String name, Relation relation) {
		if (RELS_CHAINS.containsKey(name)) {
			Set<String> chain = new LinkedHashSet<>();
			for (SimpleEntry<String, Relation> entry : RELS_CHAINS.get(name)) {
				if (relation.is(entry.getValue())) {
					chain.add(entry.getKey());
				}
			}
			return chain;
		}
		Set<SimpleEntry<String, Relation>> chain = new LinkedHashSet<>();
		ClassStruct cs = as(name);
		while (cs != null) {
			// TODO: include self
			chain.add(new SimpleEntry<>(cs.zhis, Relation.EXTENDS));
			for (String pl : cs.impls) {
				chain.add(new SimpleEntry<>(pl, Relation.IMPLEMENTS));
			}
			cs = cs.down();
		}
		RELS_CHAINS.put(name, chain);
		return getRelsChain(name, relation);
	}

	public Set<String> getRelsChain(Relation relation) {
		return getRelsChain(zhis, relation);
	}

	public static boolean assignableOf(String zhis, String other) {
		return getRelsChain(other.replace(".", "/"), Relation.ALL).contains(zhis.replace(".", "/"));
	}

	public boolean assignableOf(String other) {
		return assignableOf(this.zhis, other);
	}

	public boolean assignableOf(ClassStruct other) {
		if (assignableOf(this.zhis, other.supr))
			return true;
		return assignableOf(this.zhis, other.zhis);
	}

	public static boolean extendsTo(String zhis, String other) {
		return getRelsChain(zhis.replace(".", "/"), Relation.EXTENDS).contains(other.replace(".", "/"));
	}

	public boolean extendsTo(String other) {
		if (extendsTo(this.supr, other))
			return true;
		return extendsTo(this.zhis, other);
	}

	public boolean extendsTo(ClassStruct other) {
		if (extendsTo(this.supr, other.zhis))
			return true;
		return extendsTo(this.zhis, other.zhis);
	}

	public static String getCommonType(String type1, String type2, Relation relation) {
		if (type1 == null || type2 == null || type1.equals("java/lang/Object") || type2.equals("java/lang/Object")) {
			return "java/lang/Object";
		}
		if (type1.equals(type2)) {
			return type1;
		}
		if (extendsTo(type1, type2))
			return type2;
		if (assignableOf(type1, type2))
			return type1;
		Set<String> supers1 = getRelsChain(type1, relation);
		Set<String> supers2 = getRelsChain(type2, relation);
		supers1.retainAll(supers2);
		return supers1.isEmpty() ? "java/lang/Object" : supers1.toArray(new String[0])[0];
	}

	public static Set<String> getCommonTypes(String type1, String type2, Relation relation) {
		if (type1 == null || type2 == null) {
			return Set.of("java/lang/Object");
		}
		Set<String> supers1 = getRelsChain(type1, relation);
		Set<String> supers2 = getRelsChain(type2, relation);
		if (type1.equals(type2)) {
			return supers1;
		}
		if (extendsTo(type1, type2))
			return supers2;
		if (assignableOf(type1, type2))
			return supers1;
		supers1.retainAll(supers2);
		return supers1;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ClassStruct cs) {
			return extendsTo(cs) || assignableOf(cs);
		}
		return false;
	}

	public static enum Relation {
		EXTENDS, IMPLEMENTS, ALL;

		boolean is(Relation relation) {
			return this == ALL || relation == this;
		}
	}
}
