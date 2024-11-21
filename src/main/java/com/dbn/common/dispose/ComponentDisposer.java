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

package com.dbn.common.dispose;

import com.dbn.common.Pair;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Unsafe;
import com.intellij.util.ui.UIUtil;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public final class ComponentDisposer {
    private static final Map<Class, List<Pair<Method, Class[]>>> LISTENER_REMOVE_METHODS = new ConcurrentHashMap<>();

    public static void removeListeners(Component comp) {
        List<Pair<Method, Class[]>> methodPairs = getListenerRemovalMethods(comp);
        for (Pair<Method, Class[]> methodPair : methodPairs) {
            Method method = methodPair.first();
            Class[] params = methodPair.second();
            EventListener[] listeners = Unsafe.silent(new EventListener[0], () -> comp.getListeners(params[0]));
            for (EventListener listener : listeners) {
                Unsafe.silent(() -> method.invoke(comp, listener));
            }
        }

    }

    @NotNull
    private static List<Pair<Method, Class[]>> getListenerRemovalMethods(Component comp) {
        Class<? extends Component> clazz = comp.getClass();
        return LISTENER_REMOVE_METHODS.computeIfAbsent(clazz, c -> {
            List<Pair<Method, Class[]>> listenerMethods = new ArrayList<>();
            Method[] methods = c.getMethods();
            for (Method method : methods) {
                String name = method.getName();
                if (name.startsWith("remove") && name.endsWith("Listener")) {
                    Class[] params = method.getParameterTypes();
                    if (params.length == 1) {
                        listenerMethods.add(Pair.of(method, params));
                    }
                }
            }
            return listenerMethods;
        });
    }

    public static void dispose(@Nullable Component component) {
        if (component == null) return;

        Dispatch.run(true, () -> {
            UIUtil.dispose(component);
            removeListeners(component);
            if (component instanceof Container) {
                Container container = (Container) component;
                Component[] components = container.getComponents();
                for (Component child : components) {
                    dispose(child);
                    //Unsafe.silent(() -> container.remove(child));
                }
            }
        });
    }
}
