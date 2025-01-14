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

package com.dbn.common;

import com.dbn.common.util.Primitives;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Unsafe.cast;

/**
 * Utility class providing reflection-based utilities for interacting with classes, methods, and annotations.
 * Offers methods for invoking methods dynamically, checking annotations, retrieving class metadata, and
 * creating objects.
 *
 * @author Dan Cioca (Oracle)
 */
@UtilityClass
public class Reflection {
    private static final Map<Class, Class> enclosingClasses = new ConcurrentHashMap<>();
    private static final Map<Class, String> classNames = new ConcurrentHashMap<>();
    private static final Map<Class, String> simpleClassNames = new ConcurrentHashMap<>();
    private static final Map<Class, Map<Class<? extends Annotation>, Boolean>> classAnnotations = new ConcurrentHashMap<>();

    public static Class<?> getEnclosingClass(Class clazz) {
        return enclosingClasses.computeIfAbsent(clazz, c -> nvl(c.getEnclosingClass(), c));
    }

    public static String getClassName(Class clazz) {
        return classNames.computeIfAbsent(clazz, c -> c.getName());
    }

    public static String getSimpleClassName(Class clazz) {
        return simpleClassNames.computeIfAbsent(clazz, c -> c.getSimpleName());
    }

    @SneakyThrows
    public static <T> T invokeMethod(Object object, Method method, Object... args) {
        return cast(method.invoke(object, args));
    }

    @SneakyThrows
    public static <T> T invokeMethod(Object object, String methodName, Object... args) {
        Class[] parameterTypes = Arrays.stream(args).map(Object::getClass).toArray(Class[]::new);
        Class<?> objectClass = object instanceof Class ? (Class) object : object.getClass();
        Method method = findMethod(objectClass, methodName, parameterTypes);
        if (method == null) return null;

        return invokeMethod(object, method, args);
    }

    @SneakyThrows
    public static <T> T invokeMethod(String className, String methodName, Object... args) {
        Class[] parameterTypes = Arrays.stream(args).map(Object::getClass).toArray(Class[]::new);
        Class<?> objectClass = findClass(className);
        if (objectClass == null) return null;

        Method method = findMethod(objectClass, methodName, parameterTypes);
        if (method == null) return null;

        return invokeMethod(null, method, args); // can only be static invocation
    }

    @Nullable
    public static Method findMethod(Class<?> objectClass, String methodName, Class... parameterTypes) {
        for (Method method : objectClass.getMethods()) {
            if (!method.getName().equals(methodName)) continue;
            if (matchesParameterTypes(method, parameterTypes)) return method;
        }

        return null;
    }

    private static boolean matchesParameterTypes(Method method, Class[] parameterTypes) {
        Class<?>[] expectedTypes = method.getParameterTypes();
        if (expectedTypes.length != parameterTypes.length) return false;

        for (int i = 0; i < expectedTypes.length; i++) {
            Class<?> expectedType = expectedTypes[i];
            Class<?> parameterType = parameterTypes[i];

            if (!expectedType.isAssignableFrom(parameterType) &&
                !Primitives.areEquivalent(expectedType, parameterType)) return false;
        }

        return true;
    }

    @Nullable
    public static Class findClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Verifies if the class is annotated with the given annotation
     * @param clazz the class to be verified
     * @param annotation the annotation to be checked for presence
     * @return true if the class is annotated with the give annotation, false otherwise
     */
    public static boolean hasAnnotation(Class<?> clazz, Class<? extends Annotation> annotation) {
        // avoid boxing / unboxing by using Boolean objects instead of primitives
        Map<Class<? extends Annotation>, Boolean> annotationMap = classAnnotations.computeIfAbsent(clazz, c -> new ConcurrentHashMap<>());
        Boolean result = annotationMap.computeIfAbsent(annotation, a -> checkHasAnnotation(clazz, a) ? Boolean.TRUE : Boolean.FALSE);
        return result == Boolean.TRUE;
    }

    /**
     * Recursive verification of presence of an annotation on a given class or its super classes
     */
    private static boolean checkHasAnnotation(Class<?> clazz, Class<? extends Annotation> annotation) {
        if (clazz == null) return false;
        if (Objects.equals(clazz, Object.class)) return false;
        if (clazz.getAnnotation(annotation) != null) return true;

        return checkHasAnnotation(clazz.getSuperclass(), annotation);
    }

    /**
     * Creates a new instance of the given class under the assumption it has an accessible no-args constructor
     */
    @SneakyThrows
    public static <T> T newInstance(Class<T> clazz) {
        return clazz.getConstructor().newInstance();
    }

}
