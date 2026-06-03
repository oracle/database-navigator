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

package com.dbn.execution.java.wrapper.ui;

import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.execution.java.wrapper.WrapperModel;
import com.dbn.object.DBMethod;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dbn.common.ui.Layouts.verticalBoxLayout;

public class WrapperNamesEditorForm extends DBNFormBase {
    private final WrapperModel model;
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel objectsPanel;

    private final List<WrapperNameEditorForm> nameEditorForms = DisposableContainers.list(this);

    public WrapperNamesEditorForm(WrapperNamesEditorDialog dialog, WrapperModel model) {
        super(dialog);
        this.model = model;
        initHeaderPanel();
        initHintPanel();
        initObjectList();
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerForms(() -> nameEditorForms);
    }

    private void initHeaderPanel() {
        DBObject sourceObject = model.getSourceObject();
        DBNHeaderForm headerForm = new DBNHeaderForm(this, sourceObject);
        this.headerPanel.add(headerForm.getComponent());
    }

    private void initHintPanel() {
        int maxIdentifierLength = model.getMaxIdentifierLength();
        TextContent hintText = TextContent.plain(
                txt("msg.java.hint.WrapperNamesEditor", maxIdentifierLength));
        DBNHintForm hintForm = new DBNHintForm(this, hintText, null, true);
        hintPanel.add(hintForm.getComponent());
    }


    private void initObjectList() {
        verticalBoxLayout(objectsPanel);

        List<DBObjectRef> objects = model.getWrapperObjects();
        for (DBObjectRef object : objects) {
            WrapperNameEditorForm nameEditorForm = new WrapperNameEditorForm(this, object);
            nameEditorForms.add(nameEditorForm);
            objectsPanel.add(nameEditorForm.getComponent());

            if (object.getObjectType() == DBObjectType.PACKAGE) {
                List<DBObjectRef<DBMethod>> methods = model.getWrapperMethods();
                for (DBObjectRef<DBMethod> method : methods) {
                    WrapperNameEditorForm methodNameEditorForm = new WrapperNameEditorForm(this, method);
                    objectsPanel.add(methodNameEditorForm.getComponent());
                }
            }
        }
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public int getMaxIdentifierLength() {
        return model.getMaxIdentifierLength();
    }

    public Set<String> getIdentifierNames(WrapperNameEditorForm exceptFor) {
        return nameEditorForms
                .stream()
                .filter(f -> f != exceptFor)
                .map(f -> f.getIdentifierName())
                .collect(Collectors.toSet());
    }
}
