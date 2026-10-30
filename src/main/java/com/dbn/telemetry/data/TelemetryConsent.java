/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.telemetry.data;

import com.dbn.common.state.PersistentStateElement;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;

/** Persistent sharing choice with a transient guard against duplicate prompts. */
public final class TelemetryConsent implements PersistentStateElement {
    private Choice choice = Choice.UNKNOWN;
    private boolean promptPending;

    private enum Choice { UNKNOWN, ACCEPTED, DECLINED }

    public synchronized boolean needsDecision() {
        return choice == Choice.UNKNOWN;
    }

    public synchronized boolean isEnabled() {
        return choice == Choice.ACCEPTED;
    }

    /** Returns whether the choice changed and previously collected data must be discarded. */
    public synchronized boolean setEnabled(boolean enabled) {
        Choice updated = enabled ? Choice.ACCEPTED : Choice.DECLINED;
        if (choice == updated) return false;
        choice = updated;
        return true;
    }

    public synchronized boolean beginPrompt() {
        if (promptPending || !needsDecision()) return false;
        promptPending = true;
        return true;
    }

    public synchronized void endPrompt() {
        promptPending = false;
    }

    @Override
    public synchronized void readState(Element element) {
        choice = enumAttribute(element, "consent", Choice.UNKNOWN);
    }

    @Override
    public synchronized void writeState(Element element) {
        setEnumAttribute(element, "consent", choice);
    }
}
