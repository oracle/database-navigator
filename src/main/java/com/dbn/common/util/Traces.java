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

import com.dbn.common.dispose.Failsafe;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Progress;
import com.dbn.common.thread.Synchronized;
import com.dbn.common.thread.ThreadInfo;
import com.dbn.common.thread.ThreadMonitor;
import com.dbn.diagnostics.Diagnostics;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
@UtilityClass
public final class Traces {

    public static final Set<String> SKIPPED_CALL_STACK_CLASSES = new HashSet<>(Arrays.asList(
            Traces.class.getName(),
            ThreadInfo.class.getName(),
            ThreadMonitor.class.getName(),
            Synchronized.class.getName(),
            Background.class.getName(),
            Progress.class.getName(),
            Failsafe.class.getName()));

    public static boolean isCalledThrough(String ... oneOfClassesNames) {
        StackTraceElement[] callStack = Thread.currentThread().getStackTrace();
        try {
            for (int i = 3; i < callStack.length; i++) {
                StackTraceElement stackTraceElement = callStack[i];
                String className = stackTraceElement.getClassName();
                for (String name : oneOfClassesNames) {
                    if (Objects.equals(name, className)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            conditionallyLog(e);
            return false;
        }
        return false;
    }
    public static boolean isCalledThrough(Class ... oneOfClasses) {
        StackTraceElement[] callStack = Thread.currentThread().getStackTrace();
        try {
            for (int i = 3; i < callStack.length; i++) {
                StackTraceElement stackTraceElement = callStack[i];
                String className = stackTraceElement.getClassName();
                for (Class clazz : oneOfClasses) {
                    if (Objects.equals(clazz.getName(), className) /*|| clazz.isAssignableFrom(Class.forName(className))*/) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            conditionallyLog(e);
            return false;
        }
        return false;
    }

    public static boolean isCalledThrough(Class clazz, String methodName) {
        StackTraceElement[] callStack = Thread.currentThread().getStackTrace();
        try {
            for (int i = 3; i < callStack.length; i++) {
                StackTraceElement stackTraceElement = callStack[i];
                String className = stackTraceElement.getClassName();
                if (Objects.equals(clazz.getName(), className) /*|| clazz.isAssignableFrom(Class.forName(className))*/) {
                    String methName = stackTraceElement.getMethodName();
                    if (Objects.equals(methodName, methName)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            conditionallyLog(e);
            return false;
        }
        return false;
    }

    public static StackTraceElement[] diagnosticsCallStack() {
        if (!Diagnostics.isDeveloperMode()) return null;

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        return Arrays
                .stream(stackTrace)
                .filter(st -> !SKIPPED_CALL_STACK_CLASSES.contains(st.getClassName()))
                .toArray(StackTraceElement[]::new);
    }
}
