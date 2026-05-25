/*
 * Copyright 2025 Oracle and/or its affiliates
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

import lombok.SneakyThrows;

import java.util.Arrays;
import java.util.stream.Collectors;

import static com.dbn.common.util.Unsafe.cast;

class ObjectProxiesBase {
    /**
     * Copied over from java.lang.Class.methodToString()...
     */
    protected static String methodToString(String className, String name, Class<?>[] argTypes) {
        return className + '.' + name +
                ((argTypes == null || argTypes.length == 0) ?
                        "()" :
                        Arrays.stream(argTypes)
                                .map(c -> c == null ? "null" : c.getName())
                                .collect(Collectors.joining(",", "(", ")")));
    }

    protected static Class[] unwrapClasses(ClassLoader classLoader, Class[] classes) {
        if (classes == null) return null;

        Class[] copy = new Class[classes.length];
        for (int i = 0; i < classes.length; i++) {
            Class<?> clazz = classes[i];
            copy[i] = unwrapClass(classLoader, clazz);
        }
        return copy;
    }

    private static Class<?> unwrapClass(ClassLoader classLoader, Class<?> clazz) {
        if (ProxyObject.class.isAssignableFrom(clazz)) {
            Class<ProxyObject> proxyClass = cast(clazz);
            return getDelegateClass(classLoader, proxyClass);
        }
        return clazz;
    }

    protected static Class<?> getDelegateClass(ClassLoader classLoader, ProxyObject proxyObject) {
        return getDelegateClass(classLoader, proxyObject.getClass());
    }

    @SneakyThrows
    protected static Class<?> getDelegateClass(ClassLoader classLoader, Class<? extends ProxyObject> proxyObjectClass) {
        ProxyObjectInfo proxyObjectInfo = getProxyObjectInfo(proxyObjectClass);
        if (proxyObjectInfo == null) {
            throw new IllegalArgumentException("Proxy class \"" + proxyObjectClass.getName() + "\" is not annotated with @ProxyClassInfo");
        }
        String delegateClassName = proxyObjectInfo.delegateClass();
        return classLoader.loadClass(delegateClassName);
    }

    protected static ProxyObjectInfo getProxyObjectInfo(Class<?> objectClass) {
        ProxyObjectInfo proxyObjectInfo = objectClass.getAnnotation(ProxyObjectInfo.class);
        if (proxyObjectInfo != null) return proxyObjectInfo;

        Class<?>[] interfaceClasses = objectClass.getInterfaces();
        for (Class<?> interfaceClass : interfaceClasses) {
            proxyObjectInfo = interfaceClass.getAnnotation(ProxyObjectInfo.class);
            if (proxyObjectInfo != null ) return proxyObjectInfo;
        }

        Class<?> superclass = objectClass.getSuperclass();
        if (superclass.equals(Object.class)) {
            return null;
        }

        return ObjectProxiesBase.getProxyObjectInfo(superclass);
    }
}
