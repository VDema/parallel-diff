package com.vd.diff.utils;

public class KeyUtils {
    public static String higherKey(String v1, String v2) {
        return v1.compareTo(v2) > 0 ? v1 : v2;
    }

    public static String lowerKey(String v1, String v2) {
        return v1.compareTo(v2) < 0 ? v1 : v2;
    }
}
