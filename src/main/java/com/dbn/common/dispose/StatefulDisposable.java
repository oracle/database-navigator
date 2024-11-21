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

import com.dbn.common.util.Unsafe;
import com.intellij.openapi.progress.ProgressIndicator;

import javax.swing.JPanel;

public interface StatefulDisposable extends com.intellij.openapi.Disposable {
    JPanel DISPOSED_COMPONENT = new JPanel();

    boolean isDisposed();

    void setDisposed(boolean disposed);

    void disposeInner();

    default void checkDisposed() {
        if (isDisposed()) throw new AlreadyDisposedException(this);
    }

    default void checkDisposed(ProgressIndicator progress) {
        checkDisposed();
        progress.checkCanceled();
    }

    @Override
    default void dispose() {
        if (isDisposed()) return;
        setDisposed(true);

        Unsafe.warned(() -> disposeInner());
    }

    default void nullify() {
        Nullifier.nullify(this);
    }
}
