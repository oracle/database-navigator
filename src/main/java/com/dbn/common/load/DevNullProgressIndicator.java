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

package com.dbn.common.load;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class DevNullProgressIndicator implements ProgressIndicator {
    public static final DevNullProgressIndicator INSTANCE = new DevNullProgressIndicator();

    private DevNullProgressIndicator() {}

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void cancel() {

    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public void setText(String text) {

    }

    @Override
    public String getText() {
        return null;
    }

    @Override
    public void setText2(String text) {

    }

    @Override
    public String getText2() {
        return null;
    }

    @Override
    public double getFraction() {
        return 0;
    }

    @Override
    public void setFraction(double fraction) {

    }

    @Override
    public void pushState() {

    }

    @Override
    public void popState() {

    }

    @Override
    public boolean isModal() {
        return false;
    }

    @Override
    public @NotNull ModalityState getModalityState() {
        return ModalityState.NON_MODAL;
    }

    @Override
    public void setModalityProgress(@Nullable ProgressIndicator modalityProgress) {

    }

    @Override
    public boolean isIndeterminate() {
        return false;
    }

    @Override
    public void setIndeterminate(boolean indeterminate) {

    }

    @Override
    public void checkCanceled() {

    }

    @Override
    public boolean isPopupWasShown() {
        return false;
    }

    @Override
    public boolean isShowing() {
        return false;
    }

}
