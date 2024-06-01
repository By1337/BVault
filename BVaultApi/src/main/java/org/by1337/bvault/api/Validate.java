package org.by1337.bvault.api;

public class Validate {
    public static void assertPositive(double d){
        assertPositive(d, "%s must be greater than 0!");
    }
    public static void assertPositive(double d, String s){
        if (d < 0){
            throw new IllegalStateException(String.format(s, d));
        }
    }
}
