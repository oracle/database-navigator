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

package com.dbn.common.state;

import com.dbn.common.approval.UserApprovalAction;
import com.dbn.common.approval.UserApprovalAdapter;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Messages;
import com.dbn.common.util.Modality;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

import static com.dbn.common.approval.UserApprovalAction.STATE_ENCRYPTION_CHANGE;
import static com.dbn.common.state.StateEncryption.isMemoryPasswordSafe;
import static com.dbn.nls.NlsResources.txt;

public class StateEncryptionApprovalAdapter implements UserApprovalAdapter<StateEncryptionApproval> {
    private static final @NonNls String PASSWORD_SAFE_CONFIGURABLE_ID = "application.passwordSafe";
    private static final int OPTION_DECIDE_LATER = 1;
    private static final int OPTION_OPEN_PASSWORD_SETTINGS = 2;
    private static final String[] APPROVAL_OPTIONS = Messages.options(
            txt("msg.settings.button.UseUnencryptedStorage"),
            txt("msg.settings.button.DecideLater"),
            txt("msg.settings.button.OpenPasswordSettings"));

    @Override
    public Class<StateEncryptionApproval> getApprovalClass() {
        return StateEncryptionApproval.class;
    }

    @Override
    public UserApprovalAction getApprovalAction() {
        return STATE_ENCRYPTION_CHANGE;
    }

    @Override
    public String getApprovalTitle(StateEncryptionApproval approval) {
        return txt("msg.settings.title.UnencryptedPersistentState");
    }

    @Override
    public String getApprovalMessage(StateEncryptionApproval approval) {
        return txt("msg.settings.message.UnencryptedPersistentState");
    }

    @Override
    @NonNls
    public String getApprovalKey(StateEncryptionApproval approval) {
        return "state-encryption:unencrypted-persistent-state";
    }

    @Override
    @NonNls
    public String getApprovalSignature(StateEncryptionApproval approval) {
        return "memory-only=" + isMemoryPasswordSafe();
    }

    @Override
    public String[] getApprovalOptions(StateEncryptionApproval approval) {
        return APPROVAL_OPTIONS;
    }

    @Override
    public void processApprovalOption(StateEncryptionApproval approval, int option) {
        if (option != OPTION_OPEN_PASSWORD_SETTINGS) return;

        Dispatch.run(Modality.nonModal(), () -> ShowSettingsUtil.getInstance().showSettingsDialog(
                null,
                c -> c instanceof SearchableConfigurable configurable && PASSWORD_SAFE_CONFIGURABLE_ID.equals(configurable.getId()),
                null));
    }

    @Override
    @Nullable
    public Duration getRejectionCooldown(StateEncryptionApproval approval, int option) {
        if (option == OPTION_DECIDE_LATER) return Duration.ofMinutes(10);
        return Duration.ofSeconds(10);
    }
}
