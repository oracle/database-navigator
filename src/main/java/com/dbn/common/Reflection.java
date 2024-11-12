package com.dbn.common;

import com.dbn.common.util.Unsafe;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Unsafe.cast;

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

    @Nullable
    public static Method findMethod(Class<?> objectClass, String methodName, Class... parameterTypes) {
        try {
            return objectClass.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            // WARNING: baldly assuming all parameters are primitives
            boolean adjusted = replaceWithPrimitives(parameterTypes);
            if (adjusted) return findMethod(objectClass, methodName, parameterTypes);

            return null;
        }
    }

    private static boolean replaceWithPrimitives(Class[] types) {
        if (types == null) return false;
        if (types.length == 0) return false;

        boolean adjusted = false;
        for (int i = 0; i < types.length; i++) {
            Class parameterType = types[i];
            Field type = Unsafe.silent(null, () -> parameterType.getField("TYPE"));
            if (type == null) continue;

            Class fieldValue = (Class) Unsafe.silent(types[i], () -> type.get(parameterType));
            adjusted = !Objects.equals(types[i], fieldValue);
            types[i] = fieldValue;
        }
        return adjusted;
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
