package plz.lizi.supersteve.api;

public class FloatLock {
    // 10个不连续且乱序的字母作为映射表，打破 a-j 的连续性特征
    private static final char[] ENCODE_MAP = { 'd', 'j', 'a', 'h', 'c', 'i', 'b', 'g', 'e', 'f' };
    // 反向查找表：利用输入字母直接定位数字，免去循环查找，保持 O(1) 极高性能
    private static final int[] DECODE_MAP = new int[256];
    static {
        // 初始化反向表
        for (int i = 0; i < ENCODE_MAP.length; i++) {
            DECODE_MAP[ENCODE_MAP[i]] = i;
        }
    }

    public static String enc(float f) {
        char[] chars = String.valueOf(f).toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c >= '0' && c <= '9') {
                int digit = c - '0';
                // 核心对抗：将数字映射后，再与当前位置的索引进行异或混淆 (XOR)
                // 这样即使两个相同的数字，在不同位置生成的字母也可能不同！
                char encodedChar = ENCODE_MAP[digit];
                chars[i] = (char) (encodedChar ^ (i & 0x07));
            }
        }
        return new String(chars);
    }

    public static void dec(String s, float[] res) {
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            // 逆向解密：先还原异或，再通过动态数组直接查出原始数字
            char originalEncoded = (char) (c ^ (i & 0x07));
            if (originalEncoded >= 'a' && originalEncoded <= 'j') {
                chars[i] = (char) (DECODE_MAP[originalEncoded] + '0');
            }
        }
        res[0] = Float.parseFloat(new String(chars));
    }
}
