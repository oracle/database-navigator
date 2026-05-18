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
import com.dbn.common.thread.Synchronized;
import com.dbn.common.util.Messages;
import com.dbn.common.util.TimeUtil;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.dbn.common.approval.UserApprovalManager.COMPONENT_NAME;
import static com.dbn.common.component.Components.applicationService;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
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
    private static final String[] APPROVAL_OPTIONS = Messages.options(
            "Approve",
            "Cancel");

    private final Set<String> approvals = ConcurrentHashMap.newKeySet();
    private final Set<String> temporaryApprovals = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> rejections = new ConcurrentHashMap<>();

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
    public <T extends UserApprovable> void ensureApproved(T approvable) {
        UserApprovalAdapter<T> adapter = UserApprovalAdapters.get(approvable);
        String approvalKey = adapter.getApprovalKey(approvable);

        Synchronized.on(approvalKey, k -> {
            ensureApproved(approvable, k);
        });
    }

    private <T extends UserApprovable> void ensureApproved(T approvable, String approvalKey) {
        UserApprovalAdapter<T> adapter = UserApprovalAdapters.get(approvable);

        // check and discard temporary approval
        if (temporaryApprovals.contains(approvalKey)) {
            temporaryApprovals.remove(approvalKey);
            return;
        }

        // check persistent approval
        if (approvals.contains(approvalKey)) return;

        // check for recent rejections
        Long rejectionTimestamp = rejections.get(approvalKey);
        if (rejectionTimestamp != null && !TimeUtil.isOlderThan(rejectionTimestamp, 10, TimeUnit.SECONDS)) {
            throw new UserApprovalCancelledException();
        }

        int option = Messages.showAcknowledgementDialog(
                null,
                adapter.getApprovalTitle(approvable),
                adapter.getApprovalMessage(approvable),
                APPROVAL_OPTIONS,
                1);
        if (option != 0) {
            rejections.put(approvalKey, System.currentTimeMillis());
            throw new UserApprovalCancelledException();
        } else {
            approvals.add(approvalKey);
            rejections.remove(approvalKey);
        }
    }

    /**
     * Persistently approves the supplied object.
     */
    public void approve(UserApprovable approvable) {
        String approvalKey = getApprovalKey(approvable);
        approvals.add(approvalKey);
    }

    public <T extends UserApprovable> void revoke(T approvable) {
        String approvalKey = getApprovalKey(approvable);
        approvals.remove(approvalKey);
    }

    /**
     * Approves the supplied object for the next approval check only.
     */
    public <T extends UserApprovable> void approveTemporarily(T approvable) {
        String approvalKey = getApprovalKey(approvable);
        temporaryApprovals.add(approvalKey);
    }

    public <T extends UserApprovable> void updateApprovals(List<T> oldApprovables, List<T> newApprovables) {
        Set<String> oldKeys = oldApprovables.stream().filter(NOT_NULL).map(a -> getApprovalKey(a)).collect(Collectors.toSet());
        Set<String> newKeys = newApprovables.stream().filter(NOT_NULL).map(a -> getApprovalKey(a)).collect(Collectors.toSet());
        Set<String> acknowledgedKeys = newApprovables.stream().filter(NOT_NULL).filter(a -> a.isAcknowledged()).map(a -> getApprovalKey(a)).collect(Collectors.toSet());

        // identify removed approvables
        Set<String> removedKeys = new HashSet<>(oldKeys);
        removedKeys.removeAll(newKeys);

        // identify added approvables
        Set<String> addedKeys = new HashSet<>(newKeys);
        addedKeys.removeAll(oldKeys);

        // cleanup temporary keys
        temporaryApprovals.removeAll(oldKeys);
        temporaryApprovals.removeAll(newKeys);

        // cleanup and update approvals
        approvals.removeAll(removedKeys);
        approvals.addAll(addedKeys);
        approvals.addAll(acknowledgedKeys);
    }

    /**
     * Resolves the stable approval key for the supplied object.
     */
    private <T extends UserApprovable> String getApprovalKey(T approvable) {
        return UserApprovalAdapters.getApprovalKey(approvable);
    }

    @Nullable
    @Override
    public Element getComponentState() {
        Element element = newStateElement();
        if (!approvals.isEmpty()) {
            Element approvalsElement = newElement(element, "approvals");
            for (String approval : approvals) {
                Element approvalElement = newElement(approvalsElement, "approval");
                setStringAttribute(approvalElement, "key", approval);
            }
        }
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        approvals.clear();

        Element approvalsElement = element.getChild("approvals");
        for (Element approvalElement : childrenOf(approvalsElement, "approval")) {
            String key = stringAttribute(approvalElement, "key");
            if (key != null) approvals.add(key);
        }
    }
}
