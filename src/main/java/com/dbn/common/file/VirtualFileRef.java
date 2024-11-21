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

import com.dbn.common.ref.WeakRef;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.dispose.Failsafe.nn;

@EqualsAndHashCode
public class VirtualFileRef{
    private final WeakRef<VirtualFile> file;

    private VirtualFileRef(VirtualFile file) {
        this.file = WeakRef.of(file);
    }

    @Nullable
    public VirtualFile get() {
        VirtualFile file = this.file.get();
        return isValid(file) ? file : null;
    }

    public static VirtualFileRef of(@NotNull VirtualFile file) {
        return new VirtualFileRef(file);
    }

    @Nullable
    public static VirtualFile get(@Nullable VirtualFileRef ref) {
        return ref == null ? null : ref.get();
    }

    @NotNull
    public static VirtualFile ensure(@Nullable VirtualFileRef ref) {
        return nn(get(ref));
    }

    @NotNull
    public VirtualFile ensure() {
        return nn(get());
    }
}
