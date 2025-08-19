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

import com.dbn.common.text.TextContent;
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

import static com.dbn.common.ui.Layouts.verticalBoxLayout;

public class WrapperNamesEditorForm extends DBNFormBase {
    private final WrapperModel model;
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel objectsPanel;

    public WrapperNamesEditorForm(WrapperNamesEditorDialog dialog, WrapperModel model) {
        super(dialog);
        this.model = model;
        initHeaderPanel();
        initHintPanel();
        initObjectList();
    }

    private void initHeaderPanel() {
        DBObject sourceObject = model.getSourceObject();
        DBNHeaderForm headerForm = new DBNHeaderForm(this, sourceObject);
        this.headerPanel.add(headerForm.getMainComponent());
    }

    private void initHintPanel() {
        int maxIdentifierLength = model.getMaxIdentifierLength();
        TextContent hintText = TextContent.plain(
                "Some of the automatically generated wrapper names exceed the maximum of " + maxIdentifierLength + " characters allowed by your database. \n" +
                "Please adjust the names to accommodate the maximum identifier length.");
        DBNHintForm hintForm = new DBNHintForm(this, hintText, null, true);
        hintPanel.add(hintForm.getComponent());
    }


    private void initObjectList() {
        verticalBoxLayout(objectsPanel);

        List<DBObjectRef> objects = model.getWrapperObjects();
        for (DBObjectRef object : objects) {
            WrapperNameEditorForm nameEditorForm = new WrapperNameEditorForm(this, object);
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
}
