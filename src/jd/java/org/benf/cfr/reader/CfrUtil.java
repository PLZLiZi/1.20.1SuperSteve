package org.benf.cfr.reader;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import plz.lizi.supersteve.api.PLZBase;
import java.io.InputStream;
import java.util.*;

public class CfrUtil {
    /**
     * 将 byte[] 字节码反编译为 Java 源码
     *
     * @param classBytes 类的字节码数组
     * @return 反编译后的源码字符串
     */
    public static String decompile(byte[] classBytes) {
        final String virtualPath = "VIRTUAL_TARGET.class";
        final StringBuilder resultBuilder = new StringBuilder();
        // 1. 实现 ClassFileSource 接口
        ClassFileSource classFileSource = new ClassFileSource() {
            @Override
            public void informAnalysisRelativePathDetail(String usePath, String classFilePath) {}

            @Override
            public Collection<String> addJar(String jarPath) {
                return Collections.emptyList();
            }

            @Override
            public Pair<byte[], String> getClassFileContent(String path) {
                if (path.equals(virtualPath)) {
                    return Pair.make(classBytes, virtualPath);
                }
                try {
                    return Pair.make(PLZBase.getClassBytes(path, CfrUtil.class.getClassLoader()), path);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public String getPossiblyRenamedPath(String path) {
                return path;
            }
        };
        // 2. 实现 OutputSinkFactory 接口
        OutputSinkFactory sinkFactory = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> available) {
                if (sinkType == SinkType.JAVA) {
                    return Arrays.asList(SinkClass.STRING);
                }
                return Collections.emptyList();
            }

            @Override
            public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
                if (sinkType == SinkType.JAVA && sinkClass == SinkClass.STRING) {
                    return new Sink<T>() {
                        @Override
                        public void write(T sinkable) {
                            resultBuilder.append(sinkable.toString());
                        }
                    };
                }
                return null;
            }
        };
        // 3. 配置与启动 CFR
        CfrDriver driver = new CfrDriver.Builder()
                .withClassFileSource(classFileSource)
                .withOutputSink(sinkFactory)
                .withOptions(Map.of("hideutf",
                        "false", "comments",
                        "false", "innerclasses",
                        "true")) // 将配置注入驱动
                .build();
        driver.analyse(Collections.singletonList(virtualPath));
        return resultBuilder.toString();
    }

    public static void main(String[] args) throws Exception {
        // 测试示例：获取当前类的字节码并尝试用 CFR 反编译自身
        String targetClass = CfrUtil.class.getName();
        InputStream is = CfrUtil.class.getResourceAsStream("/" + targetClass.replace('.', '/') + ".class");
        if (is != null) {
            byte[] bytes = is.readAllBytes();
            String sourceCode = decompile(bytes);
            System.out.println("--- CFR 反编译结果 ---");
            System.out.println(sourceCode);
        }
    }
}
