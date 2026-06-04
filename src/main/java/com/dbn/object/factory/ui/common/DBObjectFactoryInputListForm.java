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
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.factory.model.DBObjectSpecList;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dbn.common.ui.Layouts.verticalBoxLayout;
import static com.dbn.nls.NlsResources.txt;

public abstract class DBObjectFactoryInputListForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel listPanel;
    private JPanel actionPanel;

    private final List<DBObjectFactoryInputForm> inputForms = DisposableContainers.list(this);

    public DBObjectFactoryInputListForm(DBNComponent parent) {
        super(parent);
        verticalBoxLayout(listPanel);

        if (!isReadonlyList()) {
            actionPanel.add(new DetailSelector());
        }
    }

    public boolean isReadonlyList() {
        return getChildInputs().isReadonly();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    protected abstract DBObjectSpecList getChildInputs();

    public ConnectionHandler getConnection() {
        return getChildInputs().getConnection();
    }

    protected abstract DBObjectFactoryInputForm createChildInputForm(DBObjectSpec input);

    public abstract DBObjectType getObjectType();

    public abstract List<Presentable> getObjectDetailOptions();

    @Override
    public void applyFormChanges() throws ConfigurationException {
        for (DBObjectFactoryInputForm inputForm : inputForms) {
            inputForm.applyFormChanges();
        }
    }

    @Override
    public void resetFormChanges() {
        List<DBObjectFactoryInputForm> oldInputForms = new ArrayList<>(inputForms);
        inputForms.clear();

        for (DBObjectSpec input : getChildInputs()) {
            createChildInputPanel(input);
        }

        Disposer.dispose(oldInputForms);
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerForms(() -> inputForms);
    }

    private class DetailSelector extends ValueSelector<Presentable> {
        DetailSelector() {
            super(PlatformIcons.ADD_ICON, txt("app.objects.action.AddObject", getObjectType().getName()), null, ValueSelectorOption.HIDE_DESCRIPTION);
            addListener((oldValue, newValue) -> createChildInputPanel(newValue));

            setEmptyValueFactory(new ValueFactory<>(txt("app.objects.action.CustomType")) {
                @Override
                public void createValue(Consumer<Presentable> consumer) {
                    createChildInputPanel((Presentable) null);
                }
            });
        }

        @Override
        public List<Presentable> loadValues() {
            return getObjectDetailOptions();
        }
    }

    protected abstract DBObjectSpec createChildInput(Presentable detail);

    private void createChildInputPanel(Presentable detail) {
        DBObjectSpec input = createChildInput(detail);
        DBObjectSpecList inputs = getChildInputs();
        inputs.add(input);
        createChildInputPanel(input);
    }

    public void createChildInputPanel(DBObjectSpec input) {
        DBObjectFactoryInputForm inputForm = createChildInputForm(input);
        DBObjectFactoryInputListItemForm listItemForm = new DBObjectFactoryInputListItemForm(this, inputForm);

        inputForms.add(inputForm);
        listPanel.add(listItemForm.getComponent());

        updateFieldAlignment();
        if (isInitialized()) {
            UserInterface.repaint(mainPanel);
            inputForm.focus();
        }
    }

    public void removeObjectPanel(int index) {
        DBObjectFactoryInputForm inputForm = inputForms.remove(index);
        Disposer.dispose(inputForm);
        listPanel.remove(index);
    }

    public void removeObjectPanel(DBObjectFactoryInputListItemForm child) {
        inputForms.remove(child.getObjectDetailsPanel());
        listPanel.remove(child.getComponent());
        Disposer.dispose(child);
        DBObjectSpec input = child.getObjectDetailsPanel().getInput();
        DBObjectSpecList childInputs = getChildInputs();
        childInputs.remove(input);

        UserInterface.repaint(mainPanel);
        validateInput();  // clear validation errors produced by this form
    }

    public Set<String> getObjectNames(DBObjectFactoryInputForm excludedForm) {
        return inputForms.
                stream().
                filter(f -> f != excludedForm).
                map(f -> f.getObjectName()).
                collect(Collectors.toSet());
    }
}
