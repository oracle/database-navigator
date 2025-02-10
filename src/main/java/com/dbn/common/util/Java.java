/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.common.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.SourceVersion;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import static com.dbn.common.util.Strings.isEmpty;

/**
 * Utility class providing methods related to Java package and class names validations.
 * This class includes pattern definitions for package and class names, validation functionality,
 * and utility methods for creating qualified class names.
 * It is intended for use in Java language processing or validation contexts.
 *
 * @author Dan Cioca (Oracle)
 */
@NonNls
@UtilityClass
public class Java {

    private static final Pattern PACKAGE_NAME_PATTERN = Pattern.compile("^[a-zA-Z_]([a-zA-Z0-9_]*)(\\.[a-zA-Z_]([a-zA-Z0-9_]*))*$");
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    private static final Set<String> PRIMITIVES = Set.of("boolean", "byte", "char", "double", "float", "int", "long" , "short", "void");

    /**
     * Validates whether the given text is a valid Java package name.
     * An empty package name is considered valid.
     * The method checks the text against a pattern for valid package names
     * and ensures that each segment of the name is a valid Java identifier.
     *
     * @param text the text to validate as a package name; can be null or empty
     * @return true if the text is a valid package name or is empty, false otherwise
     */
    public static boolean isValidPackageName(String text) {
        if (isEmpty(text)) return true; // allow empty package names
        if (!isMatchingPattern(text, PACKAGE_NAME_PATTERN)) return false;
        if (!isIdentifierChain(text)) return false;

        return true;
    }

    /**
     * Validates whether the given text is a valid Java class name.
     * The method checks whether the provided string is non-empty, matches
     * the defined pattern for valid class names, and is a valid Java identifier.
     *
     * @param text the text to validate as a Java class name; must not be null
     * @return true if the text is a valid Java class name, false otherwise
     */
    public static boolean isValidClassName(String text) {
        if (isEmpty(text)) return false;
        if (!isMatchingPattern(text, CLASS_NAME_PATTERN)) return false;
        if (!isIdentifier(text)) return false;

        return true;
    }

    private static boolean isMatchingPattern(String text, Pattern pattern) {
        return pattern.matcher(text).matches();
    }

    private static boolean isIdentifierChain(String name) {
        String[] tokens = name.split("\\.");
        return Arrays.stream(tokens).allMatch(t -> isIdentifier(t));
    }

    private static boolean isIdentifier(String t) {
        // TODO make this logic java version aware? or leave future-proof "latest" version?
        return SourceVersion.isIdentifier(t) && !SourceVersion.isKeyword(t);
    }

    /**
     * Constructs a fully qualified class name by combining a package name and a class name.
     * If the package name is null or empty, returns the class name as is.
     *
     * @param packageName the package name, which can be null or empty
     * @param className the class name, which must not be null
     * @return the fully qualified class name in the format "packageName.className",
     *         or just "className" if the package name is null or empty
     */
    public static String getQualifiedClassName(@Nullable String packageName, String className) {
        return isEmpty(packageName) ? className : packageName + "." + className;
    }

    @NonNls
    public static boolean isPrimitive(String className) {
        return PRIMITIVES.contains(className);
    }

    @NonNls
    public static boolean isVoid(String className) {
        return Objects.equals(className, "void");
    }

}
