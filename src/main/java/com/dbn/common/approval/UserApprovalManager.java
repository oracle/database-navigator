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
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.approval.UserApprovalManager.COMPONENT_NAME;
import static com.dbn.common.component.Components.applicationService;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

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
    public void ensureApproved(UserApprovable approval) {
        UserApprovalAdapter<UserApprovable> adapter = UserApprovalAdapters.get(approval);
        String approvalKey = adapter.getApprovalKey(approval);
        if (approvals.contains(approvalKey)) return;
        if (temporaryApprovals.contains(approvalKey)) {
            temporaryApprovals.remove(approvalKey);
            return;
        }

        int option = Messages.showConfirmationDialog(
                null,
                adapter.getApprovalTitle(approval),
                adapter.getApprovalMessage(approval),
                APPROVAL_OPTIONS,
                1);
        if (option != 0) throw new UserApprovalCancelledException();

        approve(approval);
    }

    /**
     * Persistently approves the supplied object.
     */
    public void approve(UserApprovable approval) {
        String approvalKey = getApprovalKey(approval);
        approvals.add(approvalKey);
    }

    /**
     * Approves the supplied object for the next approval check only.
     */
    public void approveTemporarily(UserApprovable approval) {
        String approvalKey = getApprovalKey(approval);
        temporaryApprovals.add(approvalKey);
    }

    /**
     * Resolves the stable approval key for the supplied object.
     */
    public String getApprovalKey(UserApprovable approval) {
        return UserApprovalAdapters.getApprovalKey(approval);
    }

    /**
     * Updates the approval store after a settings form is applied.
     * Keys in initialKeys but missing from current are revoked.
     * Keys in approvedKeys that are still in current are approved.
     * Untouched entries are left as they were.
     */
    public void updateApprovals(
            Set<String> initialKeys,
            Set<String> approvedKeys,
            Collection<? extends UserApprovable> current) {
        Set<String> currentKeys = new HashSet<>();
        for (UserApprovable approval : current) {
            currentKeys.add(getApprovalKey(approval));
        }
        for (String stale : initialKeys) {
            if (!currentKeys.contains(stale)) {
                approvals.remove(stale);
                temporaryApprovals.remove(stale);
            }
        }
        for (String approved : approvedKeys) {
            if (currentKeys.contains(approved)) {
                approvals.add(approved);
                temporaryApprovals.remove(approved);
            }
        }
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
