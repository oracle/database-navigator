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

package com.dbn.database.common.security;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.security.DatabaseIdentifierCache;
import lombok.SneakyThrows;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static com.dbn.common.dispose.Failsafe.nd;

/**
 * The {@code ObjectIdentifierMonitor} class acts as an {@link InvocationHandler} for dynamic proxy objects
 * to monitor and manage method calls that return specific object identifiers, particularly
 * those decorated with the {@code @ObjectIdentifier} annotation. It integrates with database security
 * mechanisms to handle and register quoted identifiers for secure usage.
 *
 * @param <T> The type of the proxied target object.
 * @author Dan Cioca (Oracle)
 */
public class ObjectIdentifierMonitor<T> implements InvocationHandler {
    private final T target;
    private final DBNConnection connection;
    private final DatabaseIdentifierCache identifierCache;

    private ObjectIdentifierMonitor(T target, DBNConnection connection) {
        this.target = target;
        this.connection = connection;

        ConnectionHandler handler = nd(connection.getConnectionHandler());
        this.identifierCache = handler.getIdentifierCache();
    }

    /**
     * Installs a dynamic proxy on the provided target object, using the given DBNResource
     * to monitor and manage method invocations. The resulting proxy wraps the target object,
     * intercepting calls and applying custom behaviors defined by the associated {@code ObjectIdentifierMonitor}.
     *
     * @param <S> The type of the target object.
     * @param target The original object to be wrapped by a dynamic proxy.
     * @param resource The {@code DBNResource} instance used to manage and monitor the proxy behavior.
     * @return The proxy instance, which is a dynamic proxy wrapping the provided target object.
     */
    public static <S> S install(S target, DBNConnection resource) {
        Class<?> targetClass = target.getClass();
        Object proxy = Proxy.newProxyInstance(
                targetClass.getClassLoader(),
                targetClass.getInterfaces(),
                new ObjectIdentifierMonitor<>(target, resource));
        return  (S) proxy;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            Object result = method.invoke(target, args);
            return registerIdentifier(method, result);
        } catch (InvocationTargetException e) {
            // unwrap InvocationTargetException produced by the proxy
            throw e.getTargetException();
        }
    }

    private Object registerIdentifier(Method method, Object result) {
        if (!isIdentifier(result)) return result;

        ObjectIdentifier annotation = method.getAnnotation(ObjectIdentifier.class);
        if (annotation == null) return result;

        String identifier = (String) result;
        identifierCache.registerIdentifier(identifier, i -> enquoteIdentifier(identifier));

        return result;
    }

    @SneakyThrows
    private String enquoteIdentifier(String identifier) {
        return connection.enquoteIdentifier(identifier);
    }

    private boolean isIdentifier(Object result) {
        return result instanceof String;
    }
}
