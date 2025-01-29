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

import com.dbn.common.list.FilteredList;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Classes;
import com.dbn.vfs.DBVirtualFile;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.ui.tabs.JBTabs;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.dispose.Failsafe.guarded;
import static com.dbn.common.thread.ThreadMonitor.isDispatchThread;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
@UtilityClass
public final class Disposer {
    private static final List<Class<?>> DISPATCH_CANDIDATES = Arrays.asList(
            JBPopup.class,
            JBTabs.class
            /*, ...*/);

    public static void register(@Nullable Disposable parent, @Nullable Object object) {
        if (object instanceof Disposable) {
            Disposable disposable = (Disposable) object;
            register(parent, disposable);
        }
    }

    public static void register(@Nullable Disposable parent, @Nullable Disposable disposable) {
        if (parent == null) return;
        if (disposable == null) return;

        if (disposable instanceof UnlistedDisposable) {
            log.error("Unlisted disposable {} should not be registered",
                    Classes.className(disposable),
                    new IllegalArgumentException("Unlisted disposable"));
        }

        if (Checks.isValid(parent)) {
            try {
                com.intellij.openapi.util.Disposer.register(parent, disposable);
            } catch (Throwable e) {
                conditionallyLog(e);
            }
        } else {
            // dispose if parent already disposed
            dispose(disposable);
        }
    }

    public static void dispose(@Nullable Disposable disposable) {
        if (disposable == null) return;
        try {
            guarded(disposable, d -> {
                if (isNotValid(d)) return;

                if (isDispatchCandidate(d) && !isDispatchThread()) {
                    Dispatch.run(() -> dispose(d));
                    return;
                }

                if (d instanceof UnlistedDisposable) {
                    d.dispose();
                } else {
                    com.intellij.openapi.util.Disposer.dispose(d);
                }

            });
        } catch (Throwable e) {
            log.warn("Failed to dispose entity {}", Classes.className(disposable), e);
        }
    }

    public static <T> T replace(T oldElement, T newElement) {
        dispose(oldElement);
        return newElement;
    }

    public static void dispose(@Nullable Object object) {
        if (object == null) return;

        if (object instanceof Disposable) {
            Disposable disposable = (Disposable) object;
            BackgroundDisposer.queue(() -> dispose(disposable));

        } else if (object instanceof Collection) {
            BackgroundDisposer.queue(() -> disposeCollection((Collection<?>) object));

        } else if (object instanceof Map) {
            BackgroundDisposer.queue(() -> disposeMap((Map) object));

        } else if (object.getClass().isArray()) {
            BackgroundDisposer.queue(() -> disposeArray((Object[]) object));

        } else if (object instanceof Reference) {
            Reference reference = (Reference) object;
            dispose(reference.get());
        }
    }

    public static void disposeCollection(@Nullable Collection<?> collection) {
        if (collection == null) return;

        if (collection instanceof FilteredList) {
            FilteredList<?> filteredList = (FilteredList<?>) collection;
            collection = filteredList.getBase();
        }

        if (collection.isEmpty()) return;

        for (Object object : collection) {
            if (object instanceof Disposable) {
                Disposable disposable = (Disposable) object;
                dispose(disposable);
            }
        }
        Nullifier.clearCollection(collection);
    }

    public static void disposeArray(@Nullable Object[] array) {
        if (array == null || array.length == 0) return;

        for (int i = 0; i < array.length; i++) {
            Object object = array[i];
            if (object instanceof Disposable) {
                Disposable disposable = (Disposable) object;
                dispose(disposable);
            }
            array[i] = null;
        }
    }

    public static void disposeMap(@Nullable Map<?, ?> map) {
        if (map == null || map.isEmpty()) return;

        for (Map.Entry entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (key instanceof Disposable) {
                Disposable disposable = (Disposable) key;
                dispose(disposable);
            }
            if (value instanceof Disposable) {
                Disposable disposable = (Disposable) value;
                dispose(disposable);
            }

        }
        Nullifier.clearMap(map);
    }

    public static void dispose(@Nullable Timer timer) {
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
    }

    public static void dispose(@Nullable DBVirtualFile virtualFile) {
        if (virtualFile == null) return;

        virtualFile.invalidate();
    }

    private static boolean isDispatchCandidate(Object object) {
        for (Class<?> candidate : DISPATCH_CANDIDATES) {
            if (candidate.isAssignableFrom(object.getClass())) {
                return true;
            }
        }
        return false;
    }
}
