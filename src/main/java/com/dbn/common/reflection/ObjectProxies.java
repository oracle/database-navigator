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

package com.dbn.common.reflection;

import com.dbn.common.exception.Exceptions;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Map;

import static com.dbn.common.util.Unsafe.cast;
import static com.intellij.util.containers.CollectionFactory.createConcurrentWeakKeyWeakValueMap;
import static com.intellij.util.containers.CollectionFactory.createConcurrentWeakMap;

@UtilityClass
public class ObjectProxies extends ObjectProxiesBase{
	private static final Map<Class, Map<Method, Method>> methodCache = createConcurrentWeakMap();

    @SneakyThrows
    public static <T extends ProxyObject> T create(ClassLoader classLoader, Class<T> proxyClass) {
        Class<?> delegateClass = getDelegateClass(classLoader, proxyClass);
        Object delegate = delegateClass.getConstructor().newInstance();
        return create(delegate, proxyClass);
    }

    /**
     * Creates a wrapper proxy for the given delegate, matching the given type which routes all method calls to the delegate
     * @param delegate the object to be wrapped
     * @param proxyClass the proxy class to be created (see {@link ProxyObject})
     * @return returns a wrapper implementation of the given proxyClass
     * @param <T> the type of the wrapper proxy object
     */
    public static <T extends ProxyObject> T create(Object delegate, Class<T> proxyClass) {
        InvocationHandler invocationHandler = (proxy, proxyMethod, proxyArgs) -> {
            try {
                if (ProxyObject.isDelegateMethod(proxyMethod)) {
                    return delegate;
                }

                Class<?> delegateClass = delegate.getClass();
                Method delegateMethod = getTargetMethod(proxyMethod, delegateClass);

                ClassLoader delegateClassLoader = delegateClass.getClassLoader();
                Object[] delegateArgs = unwrapObjects(delegateClassLoader, proxyArgs);
                Object delegateResult = delegateMethod.invoke(delegate, delegateArgs);

                Class<?> proxyReturnType = proxyMethod.getReturnType();

                return wrap(delegateResult, proxyReturnType);
            } catch (Exception e) {
                throw Exceptions.unwrap(e);
            }
        };

        return createProxyInstance(proxyClass, invocationHandler);
    }

    private static Object wrap(Object delegate, Class<?> targetClass) {
        if (delegate == null) return null;

        if (delegate.getClass().isArray()) {
            Object[] delegateResultArray = (Object[]) delegate;
            Class<?> proxyResultArrayType = targetClass.getComponentType();
            if (ProxyObject.class.isAssignableFrom(proxyResultArrayType)) {
                Class<ProxyObject> proxyReturnClass = cast(proxyResultArrayType);

                Object[] proxyResultArray = cast(Array.newInstance(proxyResultArrayType, delegateResultArray.length));
                for (int i = 0; i < delegateResultArray.length; i++) {
                    proxyResultArray[i] = create(delegateResultArray[i], proxyReturnClass);
                }

                return proxyResultArray;
            } else {
                return delegateResultArray;
            }
        }

        if (delegate instanceof Collection) {
            //throw new UnsupportedOperationException("Collections are not supported");
            // TODO use Method.getGenericReturnType() to introspect the collection
        }

        if (ProxyObject.class.isAssignableFrom(targetClass)) {
            Class<ProxyObject> proxyReturnClass = cast(targetClass);
            return create(delegate, proxyReturnClass);
        }

        return delegate;
    }


    private static Object[] wrap(Object[] sourceObjects, Class<?>[] targetClasses) {
        if (sourceObjects == null) return null;

        int length = sourceObjects.length;
        if (length == 0) return sourceObjects;
        if (length != targetClasses.length) throw new IllegalArgumentException("Source-object and target-class arrays must have the same length");

        Object[] targetObjects = new Object[length];
        for (int i = 0; i < length; i++) {
            Object sourceObject = sourceObjects[i];
            Class<?> targetClass = targetClasses[i];
            if (ProxyObject.class.isAssignableFrom(targetClass)) {
                Class<? extends ProxyObject> proxyClass = cast(targetClass);
                targetObjects[i] = create(sourceObject, proxyClass);
            } else {
                targetObjects[i] = sourceObject;
            }
        }
        return targetObjects;
    }

    private static Object unwrap(ClassLoader delegateClassLoader, ProxyObject proxyObject) {
        Object delegateObject = proxyObject.getDelegate();
        if (delegateObject != null) return delegateObject;

        Class<?> delegateClass = getDelegateClass(delegateClassLoader, proxyObject);
        if (delegateClass.isInterface()) {
            // create interface proxy
            InvocationHandler invocationHandler = (delegate, delegateMethod, delegateArgs) -> {
                try {
                    Class<? extends ProxyObject> proxyClass = proxyObject.getClass();
                    Method proxyMethod = getTargetMethod(delegateMethod, proxyClass);

                    Class<?>[] proxyParameterTypes = proxyMethod.getParameterTypes();
                    Object[] proxyArgs = wrap(delegateArgs, proxyParameterTypes);

                    Object proxyResult = proxyMethod.invoke(proxyObject, proxyArgs);
                    if (proxyResult == null) return null;

                    if (proxyResult instanceof ProxyObject proxyResultObject) {
                        return proxyResultObject.getDelegate();
                    }
                    return proxyResult;
                } catch (Exception e) {
                    throw Exceptions.unwrap(e);
                }
            };

            return createProxyInstance(delegateClass, invocationHandler);
        }

        return null;
    }

    private static Method getTargetMethod(Method sourceMethod, Class targetClass) {
        Class<?> sourceClass = sourceMethod.getDeclaringClass();
        Map<Method, Method> methods = methodCache.computeIfAbsent(sourceClass, c -> createConcurrentWeakKeyWeakValueMap());
        return methods.computeIfAbsent(sourceMethod, m -> findTargetMethod(m, targetClass));
    }

    @SneakyThrows
    private static Method findTargetMethod(Method sourceMethod, Class<?> targetClass) {
        Class<?>[] sourceParameterTypes = sourceMethod.getParameterTypes();

        String methodName = sourceMethod.getName();
        try {
            Method targetMethod = targetClass.getMethod(methodName, sourceParameterTypes);
            targetMethod.setAccessible(true);
            return targetMethod;
        } catch (Exception e) {
            Method[] targetMethods = targetClass.getMethods();
            for (Method targetMethod : targetMethods) {
                if (matchMethods(sourceMethod, targetMethod)) {
                    targetMethod.setAccessible(true);
                    return targetMethod;
                }
            }
        }
        String methodSignature = methodToString(targetClass.getName(), methodName, sourceParameterTypes);
        throw new NoSuchMethodException(methodSignature);
    }

    private static boolean matchMethods(Method sourceMethod, Method targetMethod) {
        if (!sourceMethod.getName().equals(targetMethod.getName())) return false;

        ClassLoader sourceClassLoader = sourceMethod.getDeclaringClass().getClassLoader();
        ClassLoader targetClassLoader = targetMethod.getDeclaringClass().getClassLoader();

        Class<?>[] targetParameterTypes = targetMethod.getParameterTypes();
        Class<?>[] sourceParameterTypes = sourceMethod.getParameterTypes();
        if (targetParameterTypes.length != sourceParameterTypes.length) return false;

        sourceParameterTypes = unwrapClasses(targetClassLoader, sourceParameterTypes);
        targetParameterTypes = unwrapClasses(sourceClassLoader, targetParameterTypes);
        for (int i = 0; i < targetParameterTypes.length; i++) {
            Class<?> targetParameterType = targetParameterTypes[i];
            Class<?> sourceParameterType = sourceParameterTypes[i];
            if (!targetParameterType.isAssignableFrom(sourceParameterType)) return false;
        }
        return true;
    }

    private static Object[] unwrapObjects(ClassLoader classLoader, Object[] objects) {
        if (objects == null) return null;

        Object[] copy = new Object[objects.length];
        for (int i = 0; i < objects.length; i++) {
            Object object = objects[i];
            copy[i] = unwrapObject(classLoader, object);
        }
        return copy;
    }


    private static Object unwrapObject(ClassLoader classLoader, Object object) {
        if (object instanceof ProxyObject proxyObject) {
            return unwrap(classLoader, proxyObject);
        }
        return object;
    }


    private static <T> T createProxyInstance(Class<T> proxyClass, InvocationHandler invocationHandler) {
        ClassLoader classLoader = proxyClass.getClassLoader();
        Class<?>[] proxyInterface = {proxyClass};
        Object proxyObject = Proxy.newProxyInstance(classLoader, proxyInterface, invocationHandler);
        return cast(proxyObject);
    }

}
