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
import com.dbn.common.approval.UserApprovalOption;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Modality;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import org.jetbrains.annotations.NonNls;

import java.time.Duration;

import static com.dbn.common.approval.UserApprovalAction.STATE_ENCRYPTION_CHANGE;
import static com.dbn.common.approval.UserApprovalOption.none;
import static com.dbn.common.approval.UserApprovalOption.one;
import static com.dbn.common.state.StateEncryption.isMemoryPasswordSafe;
import static com.dbn.nls.NlsResources.txt;

/**
 * Prepares user approval information for falling back to unencrypted persistent state
 * when password-safe encryption is not available.
 */
public class StateEncryptionApprovalAdapter implements UserApprovalAdapter<StateEncryptionApproval> {
    private static final @NonNls String PASSWORD_SAFE_CONFIGURABLE_ID = "application.passwordSafe";
    private static final UserApprovalOption OPTION_DECIDE_LATER = none(txt("msg.settings.button.DecideLater"), Duration.ofMinutes(10));
    private static final UserApprovalOption OPTION_OPEN_PASSWORD_SETTINGS = none(txt("msg.settings.button.OpenPasswordSettings"));
    private static final UserApprovalOption[] APPROVAL_OPTIONS = {
            one(txt("msg.settings.button.UseUnencryptedStorage")),
            OPTION_DECIDE_LATER,
            OPTION_OPEN_PASSWORD_SETTINGS};

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
    public UserApprovalOption[] getApprovalOptions(StateEncryptionApproval approval) {
        return APPROVAL_OPTIONS;
    }

    @Override
    public void processApprovalOption(StateEncryptionApproval approval, UserApprovalOption option) {
        if (option != OPTION_OPEN_PASSWORD_SETTINGS) return;

        Dispatch.run(Modality.nonModal(),
                () -> ShowSettingsUtil.getInstance().showSettingsDialog(null,
                        c -> isPasswordSafeConfig(c), null));
    }

    private static boolean isPasswordSafeConfig(Configurable configurable) {
        return configurable instanceof SearchableConfigurable config && PASSWORD_SAFE_CONFIGURABLE_ID.equals(config.getId());
    }
}
