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

package com.dbn.common.acknowledgement;

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

import static com.dbn.common.acknowledgement.UserAcknowledgementManager.COMPONENT_NAME;
import static com.dbn.common.component.Components.applicationService;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@State(
        name = COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class UserAcknowledgementManager extends ApplicationComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Application.UserAcknowledgementManager";
    private static final String[] ACKNOWLEDGEMENT_OPTIONS = Messages.options(
            "Acknowledge",
            "Cancel");

    private final Set<String> acknowledgements = ConcurrentHashMap.newKeySet();
    private final Set<String> temporaryAcknowledgements = ConcurrentHashMap.newKeySet();

    public UserAcknowledgementManager() {
        super(COMPONENT_NAME);
    }

    public static UserAcknowledgementManager getInstance() {
        return applicationService(UserAcknowledgementManager.class);
    }


    public void ensureAcknowledged(UserAcknowledgeable acknowledgeable) {
        String acknowledgementKey = acknowledgeable.getAcknowledgementKey();
        if (acknowledgements.contains(acknowledgementKey)) return;
        if (temporaryAcknowledgements.contains(acknowledgementKey)) {
            temporaryAcknowledgements.remove(acknowledgementKey);
            return;
        }

        int option = Messages.showConfirmationDialog(
                null,
                acknowledgeable.getAcknowledgementTitle(),
                acknowledgeable.getAcknowledgementMessage(),
                ACKNOWLEDGEMENT_OPTIONS,
                1);
        if (option != 0) throw new UserAcknowledgementCancelledException();

        acknowledge(acknowledgeable);
    }

    public void acknowledge(UserAcknowledgeable acknowledgeable) {
        String acknowledgementKey = acknowledgeable.getAcknowledgementKey();
        acknowledgements.add(acknowledgementKey);
    }

    public void acknowledgeTemporarily(UserAcknowledgeable acknowledgeable) {
        String acknowledgementKey = acknowledgeable.getAcknowledgementKey();
        temporaryAcknowledgements.add(acknowledgementKey);
    }

    /**
     * Updates the acknowledgement store after a settings form is applied.
     * Keys in initialKeys but missing from current are revoked.
     * Keys in trustedKeys that are still in current are acknowledged.
     * Untouched entries are left as they were.
     */
    public void updateAcknowledgements(
            Set<String> initialKeys,
            Set<String> trustedKeys,
            Collection<? extends UserAcknowledgeable> current) {
        Set<String> currentKeys = new HashSet<>();
        for (UserAcknowledgeable acknowledgeable : current) {
            currentKeys.add(acknowledgeable.getAcknowledgementKey());
        }
        for (String stale : initialKeys) {
            if (!currentKeys.contains(stale)) {
                acknowledgements.remove(stale);
                temporaryAcknowledgements.remove(stale);
            }
        }
        for (String trusted : trustedKeys) {
            if (currentKeys.contains(trusted)) {
                acknowledgements.add(trusted);
                temporaryAcknowledgements.remove(trusted);
            }
        }
    }

    @Nullable
    @Override
    public Element getComponentState() {
        Element element = newStateElement();
        if (!acknowledgements.isEmpty()) {
            Element acknowledgementsElement = newElement(element, "acknowledgements");
            for (String acknowledgement : acknowledgements) {
                Element acknowledgementElement = newElement(acknowledgementsElement, "acknowledgement");
                setStringAttribute(acknowledgementElement, "key", acknowledgement);
            }
        }
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        acknowledgements.clear();

        Element acknowledgementsElement = element.getChild("acknowledgements");
        for (Element acknowledgementElement : childrenOf(acknowledgementsElement, "acknowledgement")) {
            String key = stringAttribute(acknowledgementElement, "key");
            if (key != null) acknowledgements.add(key);
        }
    }
}
