package com.relayflow.common.util;

/**
 * 中国大陆手机号：入库与鉴权使用无空格的 11 位数字；输入端可含分段空格。
 */
public final class MobileUtils {

    private static final String MOBILE_PATTERN = "^1\\d{10}$";

    private MobileUtils() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        return trimmed.replaceAll("\\s+", "");
    }

    public static boolean isValid(String mobile) {
        return mobile != null && !mobile.isEmpty() && mobile.matches(MOBILE_PATTERN);
    }
}
