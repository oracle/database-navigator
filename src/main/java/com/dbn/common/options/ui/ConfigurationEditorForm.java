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

package com.dbn.common.options.ui;

import com.dbn.common.dispose.Failsafe;
import com.dbn.common.options.BasicConfiguration;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.list.CheckBoxList;
import com.dbn.common.ui.util.ClientProperty;
import com.dbn.common.ui.util.TextFields;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.DocumentAdapter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelListener;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

import static com.dbn.common.util.Classes.simpleClassName;

@Slf4j
public abstract class ConfigurationEditorForm<E extends BasicConfiguration> extends DBNFormBase {
    private ItemListener itemListener;
    private ActionListener actionListener;
    private DocumentListener documentListener;
    private TableModelListener tableModelListener;
    private final E configuration;

    protected ConfigurationEditorForm(E configuration) {
        super(null, configuration.resolveProject());
        this.configuration = configuration;
    }

    public final E getConfiguration() {
        return Failsafe.nn(configuration);
    }

    public abstract void applyFormChanges() throws ConfigurationException;

    public void applyFormChanges(E configuration) throws ConfigurationException {
        log.error("Cannot apply form changes for {}",
                simpleClassName(configuration),
                new UnsupportedOperationException("Not implemented"));
    }
    public abstract void resetFormChanges();

    protected DocumentListener createDocumentListener() {
        return new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                getConfiguration().setModified(true);
            }
        };
    }

    protected ActionListener createActionListener() {
        return e -> getConfiguration().setModified(true);
    }

    protected ItemListener createItemListener() {
        return e -> getConfiguration().setModified(true);
    }

    protected TableModelListener createTableModelListener() {
        return e -> getConfiguration().setModified(true);
    }

    protected void registerComponents(JComponent ... components) {
        for (JComponent component : components){
            registerComponent(component);
        }
    }

    protected void registerComponent(JComponent component) {
        if (ClientProperty.REGISTERED.isSet(component)) return;

        ClientProperty.REGISTERED.set(component, true);
        if (component instanceof AbstractButton) {
            AbstractButton abstractButton = (AbstractButton) component;
            if (actionListener == null) actionListener = createActionListener();
            abstractButton.addActionListener(actionListener);
        }
        else if (component instanceof CheckBoxList) {
            CheckBoxList<?> checkBoxList = (CheckBoxList<?>) component;
            if (actionListener == null) actionListener = createActionListener();
            checkBoxList.addActionListener(actionListener);
        } else if (component instanceof JTextField) {
            JTextField textField = (JTextField) component;
            if (documentListener == null) documentListener = createDocumentListener();
            TextFields.addDocumentListener(textField, documentListener);
        } else if (component instanceof JComboBox) {
            JComboBox<?> comboBox = (JComboBox<?>) component;
            if (itemListener == null) itemListener = createItemListener();
            comboBox.addItemListener(itemListener);
        } else if (component instanceof JTable) {
            JTable table = (JTable) component;
            if (tableModelListener == null) tableModelListener = createTableModelListener();
            table.getModel().addTableModelListener(tableModelListener);
        } else {
            for (Component childComponent : component.getComponents()) {
                if (childComponent instanceof JComponent) {
                    registerComponent((JComponent) childComponent);
                }
            }
        }

    }
}
