/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.common.load;

import com.dbn.common.ref.WeakRef;
import com.intellij.openapi.progress.ProgressIndicator;
import lombok.experimental.Delegate;

import static com.dbn.common.util.Commons.nvl;

public class ProgressIndicatorDelegate implements ProgressIndicator{
    private final WeakRef<ProgressIndicator> delegate;

    public ProgressIndicatorDelegate(ProgressIndicator delegate) {
        this.delegate = WeakRef.of(delegate);
    }

    @Delegate
    public ProgressIndicator getDelegate() {
        ProgressIndicator progressIndicator = delegate.get();
        return nvl(progressIndicator, CancelledProgressIndicator.INSTANCE);
    }
}
