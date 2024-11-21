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

package com.dbn.common.ui.misc;

import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Timers;

import javax.swing.JLabel;
import java.util.concurrent.TimeUnit;

public class TemporaryLabel extends JLabel {

    public void show(int timeout, TimeUnit timeoutUnit) {
        changeVisibility(true);
        Timers.executeLater("TemporaryLabelTimeout", timeout, timeoutUnit, () -> changeVisibility(false));
    }

    private void changeVisibility(boolean aFlag) {
        Dispatch.run(() -> setVisible(aFlag));
    }
}
