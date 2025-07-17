/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.prerequisite.model;

import com.dbn.common.icon.Icons;
import com.dbn.common.operation.DatabaseOperation;
import com.dbn.common.option.InteractiveOptionBroker;
import com.dbn.common.option.OptionBroker;
import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Lists;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.prerequisite.definition.PrerequisiteDefinition;
import com.dbn.prerequisite.definition.PrerequisiteDefinitionProvider;
import com.dbn.prerequisite.evaluation.PrerequisiteRequirementEvaluator;
import com.dbn.prerequisite.resolution.PrerequisiteOption;
import lombok.Getter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;

@Getter
public class PrerequisiteData implements PersistentStateElement {
    private final ConnectionRef connection;
    private final Map<PrerequisiteType, Prerequisite> prerequisites = new ConcurrentHashMap<>();
    private final Map<DatabaseOperation, PrerequisiteGroup> groups = new ConcurrentHashMap<>();
    private final Map<DatabaseOperation, OptionBroker<PrerequisiteOption>> optionBrokers = new ConcurrentHashMap<>();

    public PrerequisiteData(ConnectionHandler connection) {
        this.connection = connection.ref();
    }

    @NotNull
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @NotNull
    public OptionBroker<PrerequisiteOption> getOptionBroker(DatabaseOperation operation) {
        return optionBrokers.computeIfAbsent(operation, k -> createOptionBroker(operation));
    }

    @NotNull
    public PrerequisiteGroup getPrerequisiteGroup(DatabaseOperation operation) {
        return groups.computeIfAbsent(operation, k -> createPrerequisiteGroup(getConnection(), operation));
    }

    @Nullable
    public Prerequisite getPrerequisite(PrerequisiteType prerequisiteType) {
        return prerequisites.get(prerequisiteType);
    }

    private PrerequisiteGroup createPrerequisiteGroup(ConnectionHandler connection, DatabaseOperation operation) {
        List<PrerequisiteMandate> mandates = resolvePrerequisiteMandates(connection, operation);
        List<PrerequisiteType> types = PrerequisiteMandate.asPrerequisiteTypes(mandates);

        List<PrerequisiteDefinition> definitions = loadPrerequisiteDefinitions(types);
        List<Prerequisite> prerequisites = ensurePrerequisites(definitions);

        // some prerequisites may miss definitions
        // TODO warn
        Set<PrerequisiteType> definedTypes = prerequisites.stream().map(p -> p.getType()).collect(Collectors.toSet());
        mandates = Lists.filter(mandates, m-> definedTypes.contains(m.getType()));

        return new PrerequisiteGroup(this, operation, mandates);
    }

    private List<Prerequisite> ensurePrerequisites(List<PrerequisiteDefinition> definitions) {
        List<Prerequisite> prerequisites = new ArrayList<>();
        for (PrerequisiteDefinition definition : definitions) {
            Prerequisite prerequisite = ensurePrerequisite(definition);
            prerequisites.add(prerequisite);
        }
        return prerequisites;
    }

    private Prerequisite ensurePrerequisite(PrerequisiteDefinition definition) {
        return prerequisites.computeIfAbsent(definition.getType(), t -> definition.createPrerequisite());
    }

    private static List<PrerequisiteMandate> resolvePrerequisiteMandates(ConnectionHandler connection, DatabaseOperation operation) {
        // use set to prevent duplicates from multiple requirement evaluators
        Set<PrerequisiteMandate> mandates = new LinkedHashSet<>();

        List<PrerequisiteRequirementEvaluator> evaluators = PrerequisiteRequirementEvaluator.EP.getExtensionList();
        for (PrerequisiteRequirementEvaluator evaluator : evaluators) {
            List<PrerequisiteMandate> applicableTypes = evaluator.resolvePrerequisites(connection, operation);
            mandates.addAll(applicableTypes);
        }
        return Collections.unmodifiableList(new ArrayList<>(mandates));
    }

    private static List<PrerequisiteDefinition> loadPrerequisiteDefinitions(List<PrerequisiteType> types) {
        Set<PrerequisiteType> prerequisiteTypes = new HashSet<>(types);
        List<PrerequisiteDefinition> definitions = new ArrayList<>();
        List<PrerequisiteDefinitionProvider> providers = PrerequisiteDefinitionProvider.EP.getExtensionList();
        for (PrerequisiteDefinitionProvider provider : providers) {
            PrerequisiteDefinition definition = provider.getDefinition();
            if (prerequisiteTypes.contains(definition.getType())) {
                definitions.add(definition);
            }
        }
        return definitions;
    }

    private static OptionBroker<PrerequisiteOption> createOptionBroker(DatabaseOperation operation) {
        return new InteractiveOptionBroker<>(
                "missing-prerequisites",
                "Missing Prerequisites",
                operation.getMissingPrerequisiteMessage() +
                        "\n\nDo you want to continue?",
                PrerequisiteOption.CONTINUE,
                PrerequisiteOption.CONTINUE,
                PrerequisiteOption.RESOLVE,
                PrerequisiteOption.CANCEL).
                withIcon(Icons.DIALOG_WARNING).
                withDoNotShowMessage("Ignore for this connection");

    }

    @Override
    public void readState(Element element) {
        List<Element> operationElements = childrenOf(element, "operation");
        for (Element operationElement : operationElements) {
            DatabaseOperation operation = enumAttribute(operationElement, "type", DatabaseOperation.class);
            PrerequisiteOption option = enumAttribute(operationElement, "option", PrerequisiteOption.class);

            OptionBroker<PrerequisiteOption> optionBroker = getOptionBroker(operation);
            optionBroker.selectOption(option);
        }

    }

    @Override
    public void writeState(Element element) {
        Set<DatabaseOperation> operations = optionBrokers.keySet();
        for (DatabaseOperation operation : operations) {
            OptionBroker<PrerequisiteOption> optionBroker = optionBrokers.get(operation);
            PrerequisiteOption option = optionBroker.getSelectedOption();

            Element operationElement = newElement(element, "operation");
            setEnumAttribute(operationElement, "type", operation);
            setEnumAttribute(operationElement, "option", option);
        }
    }
}
