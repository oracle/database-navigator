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

package com.dbn.common.proxy;

import com.dbn.common.compatibility.Experimental;
import com.dbn.common.dispose.Failsafe;
import lombok.SneakyThrows;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.util.Unsafe.cast;

@Experimental
public class ServiceEnhancer<T> implements InvocationHandler {
    private final T innerService;
    private static final Map<Method, Method> methodCache = new ConcurrentHashMap<>();

    public ServiceEnhancer(T innerService) {
        this.innerService = innerService;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Method serviceMethod = methodCache.computeIfAbsent(method, m -> getServiceMethod(m));
        if (serviceMethod.getAnnotation(Guarded.class) != null) {
            Failsafe.guarded(() -> serviceMethod.invoke(args));
        } else {
            serviceMethod.invoke(args);
        }

        return null;
    }

    @SneakyThrows
    private Method getServiceMethod(Method proxyMethod) {
        return innerService.getClass().getMethod(proxyMethod.getName(), proxyMethod.getParameterTypes());
    }


    public static <T> T wrap(T service) {
        Class<?> serviceClass = service.getClass();
        ServiceEnhancer<T> serviceEnhancer = new ServiceEnhancer<>(service);
        return cast(Proxy.newProxyInstance(serviceClass.getClassLoader(), new Class<?>[]{serviceClass}, serviceEnhancer));
    }
}
