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

import com.intellij.openapi.progress.ProcessCanceledException;
import org.jetbrains.annotations.Nullable;

public class AlreadyDisposedException extends ProcessCanceledException {
    @Deprecated // TODO only use for disposed object checks / use constructor on runtime
    public static final AlreadyDisposedException INSTANCE = new AlreadyDisposedException();
    private AlreadyDisposedException() {};

    public AlreadyDisposedException(@Nullable Object o) {
        super(o == null ?
                new IllegalArgumentException("Object is null") :
                new IllegalStateException(o.getClass().getSimpleName() + " is invalid or disposed"));
    }
}
