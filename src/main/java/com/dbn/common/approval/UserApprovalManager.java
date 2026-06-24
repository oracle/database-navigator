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

package com.dbn.common.approval;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.ApplicationComponentBase;
import com.dbn.common.component.PersistentState;
import com.dbn.common.util.Messages;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.progress.ProcessCanceledException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.dbn.common.approval.UserApprovalManager.COMPONENT_NAME;
import static com.dbn.common.component.Components.applicationService;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.common.util.Commons.NOT_NULL;

/**
 * Application-level approval store and prompt coordinator.
 * <p>
 * Persistent approvals are remembered across IDE restarts. Temporary approvals
 * allow a single operation, such as verifying an endpoint, to proceed without
 * persisting trust in that endpoint.
 */
@State(
        name = COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class UserApprovalManager extends ApplicationComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Application.UserApprovalManager";

    private final Map<String, UserApprovalData> approvalData = new ConcurrentHashMap<>();

    public UserApprovalManager() {
        super(COMPONENT_NAME);
    }

    public static UserApprovalManager getInstance() {
        return applicationService(UserApprovalManager.class);
    }


    /**
     * Ensures the supplied object has been approved by the user.
     *
     * @throws UserApprovalCancelledException if the user cancels the approval prompt
     */
    public <T extends UserApprovable> void ensureApproved(UserApprovalAction action, T approvable) {
        UserApprovalAdapter<T> adapter = UserApprovalAdapters.get(action, approvable);

        String approvalKey = getApprovalKey(action, approvable);
        String approvalSignature = adapter.getApprovalSignature(approvable);
        updateApprovalSignature(approvalKey, approvalSignature);

        UserApprovalData data = getApprovalData(approvalKey);
        // check and discard temporary approval
        if (data.consumeTemporary()) return;

        // check persistent approval
        if (data.isApproved()) return;

        // check for recent rejections
        if (data.isRejected()) {
            throw new UserApprovalCancelledException();
        }

        if (data.isPending()) {
            throw new ProcessCanceledException();
        }

        obtainUserApproval(action, approvable, data);
    }

    private <T extends UserApprovable> void obtainUserApproval(UserApprovalAction action, T approvable, UserApprovalData data) {
        UserApprovalAdapter<T> adapter = UserApprovalAdapters.get(action, approvable);
        data.setPending(true);
        int option = Messages.showAcknowledgementDialog(
                null,
                adapter.getApprovalTitle(approvable),
                adapter.getApprovalMessage(approvable),
                adapter.getApprovalOptions(approvable),
                1, o -> data.setPending(false));

        if (option != 0) {
            adapter.processApprovalOption(approvable, option);
            data.reject(adapter.getRejectionCooldown(approvable, option));
            throw new UserApprovalCancelledException();
        }

        data.setApproved(true);
        data.clearRejection();
    }

    public <T extends UserApprovable> void updateApprovalSignature(UserApprovalAction action, T approvable) {
        UserApprovalAdapter<T> adapter = UserApprovalAdapters.get(action, approvable);
        updateApprovalSignature(getApprovalKey(action, approvable), adapter.getApprovalSignature(approvable));
    }

    private void updateApprovalSignature(String approvalKey, @Nullable String approvalSignature) {
        if (approvalSignature == null) return;

        UserApprovalData data = getApprovalData(approvalKey);
        if (data.updateSignatureRequiresApprovalClear(approvalSignature)) {
            clearApproval(approvalKey, data);
        }
    }

    private void clearApproval(String approvalKey) {
        UserApprovalData data = approvalData.get(approvalKey);
        if (data == null) return;

        clearApproval(approvalKey, data);
    }

    private void clearApproval(String approvalKey, UserApprovalData data) {
        data.clearApproval();
        removeApprovalDataIfEmpty(approvalKey, data);
    }

    /**
     * Persistently approves the supplied object.
     */
    public void approve(UserApprovalAction action, UserApprovable approvable) {
        String approvalKey = getApprovalKey(action, approvable);
        String approvalSignature = getApprovalSignature(action, approvable);
        updateApprovalSignature(approvalKey, approvalSignature);
        getApprovalData(approvalKey).setApproved(true);
    }

    public <T extends UserApprovable> void revoke(UserApprovalAction action, T approvable) {
        String approvalKey = getApprovalKey(action, approvable);
        UserApprovalData data = approvalData.get(approvalKey);
        if (data == null) return;

        data.clearApproval();
        data.setSignature(null);
        removeApprovalDataIfEmpty(approvalKey, data);
    }

    /**
     * Approves the supplied object for the next approval check only.
     */
    public <T extends UserApprovable> void approveTemporarily(UserApprovalAction action, T approvable) {
        String approvalKey = getApprovalKey(action, approvable);
        String approvalSignature = getApprovalSignature(action, approvable);
        updateApprovalSignature(approvalKey, approvalSignature);
        getApprovalData(approvalKey).setTemporary(true);
    }

    public <T extends UserApprovable> void updateApprovals(UserApprovalAction action, List<T> oldApprovables, List<T> newApprovables) {
        updateApprovals(action, oldApprovables, newApprovables, List.of());
    }

    public <T extends UserApprovable> void updateApprovals(UserApprovalAction action, List<T> oldApprovables, List<T> newApprovables, List<T> acknowledgedApprovables) {
        Set<String> oldKeys = oldApprovables.stream().filter(NOT_NULL).map(a -> getApprovalKey(action, a)).collect(Collectors.toSet());
        Set<String> newKeys = newApprovables.stream().filter(NOT_NULL).map(a -> getApprovalKey(action, a)).collect(Collectors.toSet());
        Set<String> acknowledgedKeys = acknowledgedApprovables.stream().filter(NOT_NULL).map(a -> getApprovalKey(action, a)).collect(Collectors.toSet());
        newApprovables.stream().filter(NOT_NULL).forEach(a -> updateApprovalSignature(action, a));

        // identify removed approvables
        Set<String> removedKeys = new HashSet<>(oldKeys);
        removedKeys.removeAll(newKeys);

        // identify added approvables
        Set<String> addedKeys = new HashSet<>(newKeys);
        addedKeys.removeAll(oldKeys);

        oldKeys.forEach(this::clearTemporaryApproval);
        newKeys.forEach(this::clearTemporaryApproval);
        removedKeys.forEach(this::removeApproval);

        addedKeys.forEach(k -> getApprovalData(k).setApproved(true));
        acknowledgedKeys.forEach(k -> getApprovalData(k).setApproved(true));
    }

    /**
     * Resolves the stable approval key for the supplied object.
     */
    private <T extends UserApprovable> String getApprovalKey(UserApprovalAction action, T approvable) {
        return UserApprovalAdapters.getApprovalKey(action, approvable);
    }

    private static <T extends UserApprovable> @Nullable String getApprovalSignature(UserApprovalAction action, T approvable) {
        return UserApprovalAdapters.getApprovalSignature(action, approvable);
    }

    private UserApprovalData getApprovalData(String approvalKey) {
        return approvalData.computeIfAbsent(approvalKey, UserApprovalData::new);
    }

    private void clearTemporaryApproval(String approvalKey) {
        UserApprovalData data = approvalData.get(approvalKey);
        if (data == null) return;

        data.setTemporary(false);
        removeApprovalDataIfEmpty(approvalKey, data);
    }

    private void removeApproval(String approvalKey) {
        UserApprovalData data = approvalData.get(approvalKey);
        if (data == null) return;

        data.setApproved(false);
        data.setSignature(null);
        removeApprovalDataIfEmpty(approvalKey, data);
    }

    private void removeApprovalDataIfEmpty(String approvalKey, UserApprovalData data) {
        synchronized (data) {
            if (data.isEmpty()) {
                approvalData.remove(approvalKey, data);
            }
        }
    }

    @Nullable
    @Override
    public Element getComponentState() {
        Element element = newStateElement();
        Element approvalsElement = newElement(element, "approvals");
        for (Map.Entry<String, UserApprovalData> entry : approvalData.entrySet()) {
            UserApprovalData data = entry.getValue();
            if (!data.isPersistent()) continue;

            Element approvalElement = newElement(approvalsElement, "approval");
            data.writeState(approvalElement);
        }
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        approvalData.clear();

        Element approvalsElement = element.getChild("approvals");
        for (Element approvalElement : childrenOf(approvalsElement, "approval")) {
            UserApprovalData data = new UserApprovalData(approvalElement);
            approvalData.put(data.getKey(), data);
        }
    }
}
