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
import com.dbn.execution.java.wrapper.Wrapper;
import com.dbn.object.common.ui.DBObjectRefListCellRenderer;
import com.dbn.object.common.ui.DBObjectRefListModel;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.ui.components.JBList;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class WrapperResultForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel objectsPanel;
    private JBList<DBObjectRef> objectsList;

    public WrapperResultForm(WrapperResultDialog dialog, Wrapper wrapper) {
        super(dialog);

        initHeaderPanel(wrapper);
        initHintPanel(wrapper);
        initObjectList(wrapper);
    }

    private void initHeaderPanel(Wrapper wrapper) {
        DBNHeaderForm headerForm = new DBNHeaderForm(this, wrapper.getSourceObjectRef());
        this.headerPanel.add(headerForm.getMainComponent());
    }

    private void initHintPanel(Wrapper wrapper) {
        DBObjectType objectType = wrapper.getSourceObjectRef().getObjectType();
        TextContent hintText = TextContent.plain("The following execution wrapper objects were created in the database for the given " + objectType.getName());
        DBNHintForm hintForm = new DBNHintForm(this, hintText, null, true);
        hintPanel.add(hintForm.getComponent());
    }


    private void initObjectList(Wrapper wrapper) {
        objectsList.setModel(new DBObjectRefListModel<>(this, wrapper.getWrapperObjects()));
        objectsList.setCellRenderer(new DBObjectRefListCellRenderer<>());
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
