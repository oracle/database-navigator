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

package com.dbn.common.ui.listener;

import com.dbn.common.dispose.Disposer;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import org.jetbrains.annotations.NotNull;

public class PopupCloseListener implements JBPopupListener {
    private final Disposable content;

    private PopupCloseListener(@NotNull Disposable content) {
        this.content = content;
    }

    public static PopupCloseListener create(@NotNull Disposable disposable) {
        return new PopupCloseListener(disposable);
    }

    @Override
    public void onClosed(@NotNull LightweightWindowEvent event) {
        Disposer.dispose(content);
    }
}
