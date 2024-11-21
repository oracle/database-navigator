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
import lombok.Setter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.dbn.common.file.util.VirtualFiles.findFileByUrl;
import static com.dbn.common.file.util.VirtualFiles.isLocalFileSystem;
import static com.dbn.common.file.util.VirtualFiles.isValidFile;
import static com.dbn.common.util.Lists.anyMatch;

@Setter
public class FileMappings<T> implements Disposable {
    private final Map<String, T> mappings = new ConcurrentHashMap<>();
    private final Set<BiPredicate<String, T>> verifiers = new HashSet<>();
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
        if (event instanceof VFileDeleteEvent) {
            VFileDeleteEvent deleteEvent = (VFileDeleteEvent) event;
            target = remove(deleteEvent.getFile().getUrl());

        } else if (event instanceof VFileMoveEvent) {
            VFileMoveEvent moveEvent = (VFileMoveEvent) event;
            target = updateMapping(file,
                    moveEvent.getOldPath(),
                    moveEvent.getNewPath());

        } else if (event instanceof VFilePropertyChangeEvent) {
            VFilePropertyChangeEvent propertyChangeEvent = (VFilePropertyChangeEvent) event;
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
        T removed = mappings.remove(fileUrl);
        if (removed == null) return null;

        clearCache();
        return removed;
    }

    public boolean contains(T value) {
        return anyMatch(mappings.values(), o -> Objects.equals(value, o));
    }

    public void put(@NotNull String url, T value) {
        mappings.put(url, value);
        clearCache();
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
        return mappings.computeIfAbsent(fileUrl, k -> {
            clearCache();
            return valueProvider.apply(k);
        });
    }
}
