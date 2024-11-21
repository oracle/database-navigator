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

import com.dbn.common.Pair;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public abstract class FileContentCache<T> {
    private final Map<File, Pair<T, Long>> cache = new ConcurrentHashMap<>();

    public final T get(File file) {
        return cache.compute(file, (f, v) -> {
            long timestamp = lastModified(f);
            if (v != null && v.second() == timestamp) return v;
            return Pair.of(load(f), timestamp);
        }).first();
    }

    protected abstract T load(File f);

    private static long lastModified(File file) {
        try {
            Path filePath = file.toPath();
            BasicFileAttributes fileAttributes = Files.readAttributes(filePath, BasicFileAttributes.class);
            return fileAttributes.lastModifiedTime().toMillis();
        } catch (Exception e) {
            conditionallyLog(e);
            return 0;
        }
    }
}
