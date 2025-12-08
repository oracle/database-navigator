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

package com.dbn.object.factory.ui.common;

import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.routine.Consumer;
import com.dbn.common.ui.Presentable;
import com.dbn.common.ui.ValueFactory;
import com.dbn.common.ui.ValueSelector;
import com.dbn.common.ui.ValueSelectorOption;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.object.factory.ObjectFactoryInput;
import com.dbn.object.type.DBObjectType;
import com.intellij.util.PlatformIcons;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class ObjectListForm<T extends ObjectFactoryInput> extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel listPanel;
    private JPanel actionPanel;
    private final ConnectionRef connection;

    private final List<ObjectFactoryInputForm<T>> inputForms = DisposableContainers.list(this);

    public ObjectListForm(DBNComponent parent, @NotNull ConnectionHandler connection) {
        super(parent);
        this.connection = connection.ref();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        actionPanel.add(new DetailSelector());
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    protected abstract ObjectFactoryInputForm<T> createObjectDetailsPanel(int index, @Nullable ObjectDetail detail);

    public abstract DBObjectType getObjectType();

    public abstract List<ObjectDetail> getObjectDetailOptions();

    private class DetailSelector extends ValueSelector<ObjectDetail> {
        DetailSelector() {
            super(PlatformIcons.ADD_ICON, "Add " + getObjectType().getName(), null, ValueSelectorOption.HIDE_DESCRIPTION);
            addListener((oldValue, newValue) -> createObjectPanel(newValue));

            setEmptyValueFactory(new ValueFactory<>("(custom type)") {
                @Override
                public void create(Consumer<ObjectDetail> consumer) {
                    createObjectPanel(null);
                }
            });
        }

        @Override
        public List<ObjectDetail> loadValues() {
            return getObjectDetailOptions();
        }
    }

    public ObjectFactoryInputForm createObjectPanel(ObjectDetail detail) {
        ObjectFactoryInputForm<T> inputForm = createObjectDetailsPanel(inputForms.size(), detail);
        ObjectListItemForm listItemForm = new ObjectListItemForm(this, inputForm);

        inputForms.add(inputForm);
        listPanel.add(listItemForm.getComponent());

        if (isInitialized()) {
            UserInterface.repaint(mainPanel);
            inputForm.focus();
        }
        return inputForm;
    }

    public void removeObjectPanel(int index) {
        ObjectFactoryInputForm<T> inputForm = inputForms.remove(index);
        Disposer.dispose(inputForm);
        listPanel.remove(index);
    }

    public void removeObjectPanel(ObjectListItemForm child) {
        inputForms.remove(child.getObjectDetailsPanel());
        listPanel.remove(child.getComponent());

        // rebuild indexes
        for (int i=0; i< inputForms.size(); i++) {
            inputForms.get(i).setIndex(i);
        }

        UserInterface.repaint(mainPanel);
        validateInput();  // clear validation errors produced by this form
    }

    public List<T> createFactoryInputs(ObjectFactoryInput parent) {
        List<T> objectFactoryInputs = new ArrayList<>();
        for (ObjectFactoryInputForm<T> inputForm : this.inputForms) {
            T objectFactoryInput = inputForm.createFactoryInput(parent);
            objectFactoryInputs.add(objectFactoryInput);
        }
        return objectFactoryInputs;
    }

    @Getter
    public static class ObjectDetail implements Presentable {
        private final String name;

        public ObjectDetail(String name) {
            this.name = name;
        }
    }

    public Set<String> getObjectNames(ObjectFactoryInputForm excludedForm) {
        return inputForms.
                stream().
                filter(f -> f != excludedForm).
                map(f -> f.getObjectName()).
                collect(Collectors.toSet());
    }
}
