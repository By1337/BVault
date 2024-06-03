package org.by1337.bvault.api;

import java.util.function.Supplier;
import java.util.regex.Pattern;

public class Validate {
    private static final Pattern pattern = Pattern.compile("^[a-zA-Z0-9-_]+$");
    public static void assertPositive(double d){
        assertPositive(d, "%s must be greater than 0!");
    }
    public static void assertPositive(double d, String s){
        if (d < 0){
            throw new IllegalStateException(String.format(s, d));
        }
    }
    public static void charactersCheck(String input) {
        charactersCheck(input, () -> String.format("Invalid name. Must be [a-zA-Z0-9._-]: '%s'", input));
    }

    public static void charactersCheck(String input, Supplier<String> message) {
        if (!pattern.matcher(input).matches()) {
            throw new IllegalArgumentException(message.get());
        }
    }
}
