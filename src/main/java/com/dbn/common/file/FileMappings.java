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

package com.dbn.common.file;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.ref.WeakRefCache;
import com.dbn.common.routine.LazyInitialized;
import com.dbn.common.routine.ParametricRunnable;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.dbn.common.file.util.VirtualFiles.findFileByUrl;
import static com.dbn.common.file.util.VirtualFiles.isLocalFileSystem;
import static com.dbn.common.file.util.VirtualFiles.isValidFile;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.common.util.Lists.anyMatch;

public class FileMappings<T> extends LazyInitialized implements Disposable {
    private final Map<String, T> mappings = new ConcurrentHashMap<>();
    private final Map<String, T> unverifiedMappings = new ConcurrentHashMap<>();
    private final Set<BiPredicate<String, T>> verifiers = new CopyOnWriteArraySet<>();
    private final WeakRefCache<T, List<String>> urlCache = WeakRefCache.weakKey();
    private final WeakRefCache<T, List<VirtualFile>> fileCache = WeakRefCache.weakKey();
    private final List<ParametricRunnable<FileMappingEvent<T>, Throwable>> eventHandlers = new ArrayList<>();

    public FileMappings(@NotNull Project project, @Nullable Disposable parentDisposable) {
        addVerifier((f, v) -> isValidFile(f));
        Disposer.register(parentDisposable, this);
        ProjectEvents.subscribe(project, this, VirtualFileManager.VFS_CHANGES, createFileListener());
    }

    @NotNull
    private BulkFileListener createFileListener() {
        return new BulkFileListener() {
            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {
                events.forEach(e -> handleFileEvent(e));
            }
        };
    }

    private void handleFileEvent(VFileEvent event) {
        VirtualFile file = event.getFile();
        if (file == null) return;
        if (!isLocalFileSystem(file)) return;

        T target = null;
        if (event instanceof VFileDeleteEvent deleteEvent) {
            target = remove(deleteEvent.getFile().getUrl());

        } else if (event instanceof VFileMoveEvent moveEvent) {
            target = updateMapping(file,
                    moveEvent.getOldPath(),
                    moveEvent.getNewPath());

        } else if (event instanceof VFilePropertyChangeEvent propertyChangeEvent) {
            target = updateMapping(file,
                    propertyChangeEvent.getOldPath(),
                    propertyChangeEvent.getNewPath());
        }

        handleEvent(target, event);
    }

    @SneakyThrows
    private void handleEvent(T target, VFileEvent event) {
        if (eventHandlers.isEmpty()) return;

        FileMappingEvent<T> mappingEvent = new FileMappingEvent<>(target, event);
        for (ParametricRunnable<FileMappingEvent<T>, Throwable> handler : eventHandlers) {
            handler.run(mappingEvent);
        }
    }

    @SneakyThrows
    private T updateMapping(VirtualFile file, String oldPath, String newPath) {
        if (Objects.equals(oldPath, newPath)) return null;

        String protocol = file.getFileSystem().getProtocol();
        String oldUrl = VirtualFileManager.constructUrl(protocol, oldPath);
        String newUrl = VirtualFileManager.constructUrl(protocol, newPath);

        T value = remove(oldUrl);
        if (value == null) return null;

        put(newUrl, value);
        return value;
    }

    private void clearCache() {
        urlCache.clear();
        fileCache.clear();
    }

    public void addEventHandler(ParametricRunnable<FileMappingEvent<T>, Throwable> handler) {
        eventHandlers.add(handler);
    }

    public void addVerifier(BiPredicate<String, T> verifier) {
        verifiers.add(verifier);
    }

    public void removeIf(Predicate<T> condition) {
        unverifiedMappings.forEach(
                (u, v) -> unverifiedMappings.computeIfPresent(u, (key, pending) ->
                condition.test(pending) ? null : pending));
        mappings
                .entrySet()
                .stream()
                .filter(e -> condition.test(e.getValue()))
                .map(m -> m.getKey())
                .forEach(k -> remove(k));
    }

    public T get(@NotNull String fileUrl) {
        return mappings.get(fileUrl);
    }

    public T remove(String fileUrl) {
        AtomicReference<T> removed = new AtomicReference<>();
        unverifiedMappings.compute(fileUrl, (url, pending) -> {
            removed.set(mappings.remove(url));
            return null;
        });
        T value = removed.get();
        if (value == null) return null;

        clearCache();
        return value;
    }

    public boolean contains(T value) {
        return anyMatch(mappings.values(), o -> Objects.equals(value, o));
    }

    public void put(@NotNull String url, T value) {
        unverifiedMappings.compute(url, (key, pending) -> mappings.put(key, value));
        clearCache();
    }

    /** Keeps persisted entries unavailable until initialization verifies them. */
    public void addMappings(@NotNull Map<String, T> entries) {
        entries.forEach((url, value) ->
                when(!mappings.containsKey(url),
                        () -> unverifiedMappings.putIfAbsent(url, value)));
    }

    @Override
    protected void initialize() {
        unverifiedMappings.forEach((url, value) -> {
            boolean trusted = verifiers.stream().allMatch(verifier -> verifier.test(url, value));
            unverifiedMappings.computeIfPresent(url, (key, pending) -> {
                if (pending != value) return pending;
                if (trusted) {
                    mappings.putIfAbsent(key, pending);
                    clearCache();
                }
                return null;
            });
        });
    }

    /** Includes pending entries so an early settings save does not discard them. */
    public Map<String, T> mappings() {
        Map<String, T> snapshot = new ConcurrentHashMap<>(unverifiedMappings);
        snapshot.putAll(mappings);
        return snapshot;
    }

    public Set<String> fileUrls() {
        return mappings.keySet();
    }

    public List<String> fileUrls(T value) {
        return urlCache.computeIfAbsent(value, v ->
                mappings.entrySet()
                        .stream()
                        .filter(e -> Objects.equals(e.getValue(), v))
                        .map(e -> e.getKey())
                        .collect(Collectors.toList()));
    }

    public List<VirtualFile> files(T value) {
        return fileCache.computeIfAbsent(value, v ->
                fileUrls(value)
                        .stream()
                        .map(u -> findFileByUrl(u))
                        .filter(f -> f != null)
                        .collect(Collectors.toList()));
    }

    public void cleanup() {
        for (BiPredicate<String, T> verifier : verifiers) {
            mappings.keySet().removeIf(url -> {
                T value = mappings.get(url);
                return !verifier.test(url, value);
            });
        }
        clearCache();
    }

    public void clear() {
        unverifiedMappings.clear();
        mappings.clear();
        clearCache();
    }

    public Collection<T> values() {
        return mappings.values();
    }

    @Override
    public void dispose() {
        clear();
        verifiers.clear();
        eventHandlers.clear();
    }

    public T computeIfAbsent(String fileUrl, Function<String, T> valueProvider) {
        AtomicReference<T> result = new AtomicReference<>();
        unverifiedMappings.compute(fileUrl, (url, pending) -> {
            result.set(mappings.computeIfAbsent(url, key -> {
                clearCache();
                return valueProvider.apply(key);
            }));
            return null;
        });
        return result.get();
    }
}
