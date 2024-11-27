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

package com.dbn.common.thread;

import com.dbn.common.property.PropertyHolder;
import com.dbn.common.property.PropertyHolderBase;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.lang.StackWalker.StackFrame;
import java.lang.reflect.Method;
import java.util.function.Consumer;

@Getter
@Setter
public class ThreadInfo extends PropertyHolderBase.IntStore<ThreadProperty> implements Consumer<ThreadProperty> {
    private static final ThreadLocal<ThreadInfo> THREAD_INFO = new ThreadLocal<>();

    public static ThreadInfo copy() {
        ThreadInfo current = current();
        ThreadInfo copy = new ThreadInfo();
        copy.inherit(current);
        collectThreadProperties(copy);
        return copy;
    }

    public static ThreadInfo current() {
        ThreadInfo threadInfo = THREAD_INFO.get();
        if (threadInfo == null) {
            threadInfo = new ThreadInfo();
            THREAD_INFO.set(threadInfo);
        }
        return threadInfo;
    }

    @Override
    protected ThreadProperty[] properties() {
        return ThreadProperty.VALUES;
    }

    @Override
    public void merge(@Nullable PropertyHolder<ThreadProperty> source) {
        if (source == null) return;

        for (ThreadProperty property : properties()) {
            if (property.propagatable() && source.is(property)) {
                set(property, true);
            }
        }
    }

    @Override
    public void unmerge(@Nullable PropertyHolder<ThreadProperty> source) {
        if (source == null) return;

        for (ThreadProperty property : properties()) {
            if (property.propagatable() && source.is(property)) {
                set(property, false);
            }
        }
    }

    /**
     * Walk the call stack and collect all {@link ThreadProperty} from methods annotated with {@link ThreadPropertyGate}
     * @param consumer the consumer for the collected thread properties
     */
    private static void collectThreadProperties(Consumer<ThreadProperty> consumer) {
        StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        stackWalker.walk(frames -> {
            frames.takeWhile(frame -> !frame.getClassName().startsWith("com.dci"))
                    .map(f -> collectThreadProperty(f))
                    .filter(p -> p != null)
                    .forEach(consumer);
            return null;
        });
    }

    private static ThreadProperty collectThreadProperty(StackFrame frame) {
        Class<?> declaringClass = frame.getDeclaringClass();
        String methodName = frame.getMethodName();

        for (Method method : declaringClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                ThreadPropertyGate propertyGate = method.getAnnotation(ThreadPropertyGate.class);
                return propertyGate == null ? null : propertyGate.value();
            }
        }
        return null;
    }

    @Override
    public void accept(ThreadProperty property) {
        set(property, true);
    }
}
