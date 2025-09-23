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

package com.dbn.execution.java.result.ui;

import com.dbn.common.data.Data;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.list.EditableStringList;
import com.dbn.common.ui.list.ListProperty;
import com.dbn.execution.common.input.ExecutionValue;
import com.intellij.ui.ToolbarDecorator;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.List;

import static com.dbn.common.ui.util.Decorators.createToolbarDecorator;
import static com.dbn.common.ui.util.Decorators.createToolbarDecoratorComponent;

public class JavaExecutionArrayResultForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel resultPanel;

    private EditableStringList stringList;

    JavaExecutionArrayResultForm(JavaExecutionResultForm parent, ExecutionValue fieldValue) {
        super(parent);
        if (fieldValue.isArrayObject()) {
            Object value = fieldValue.getValue();
            List<String> valuesStringList = Data.asStringList(value);
            stringList = new EditableStringList(this, valuesStringList, ListProperty.INDEXED);
            resultPanel.add(initListComponent());
        }
    }

    private JPanel initListComponent() {
        ToolbarDecorator decorator = createToolbarDecorator(stringList);
        return createToolbarDecoratorComponent(decorator, stringList);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
