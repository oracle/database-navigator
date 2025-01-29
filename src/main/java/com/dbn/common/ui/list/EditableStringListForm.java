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

package com.dbn.common.ui.list;

import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNFormBase;
import com.intellij.ui.ToolbarDecorator;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.ui.util.Decorators.createToolbarDecorator;
import static com.dbn.common.ui.util.Decorators.createToolbarDecoratorComponent;

public class EditableStringListForm extends DBNFormBase {
    private JPanel component;
    private JLabel titleLabel;
    private JPanel listPanel;

    private final EditableStringList stringList;

    public EditableStringListForm(DBNComponent parent, String title, boolean sorted) {
        this(parent, title, new ArrayList<>(), sorted);
    }

    public EditableStringListForm(DBNComponent parent, String title, List<String> elements, boolean sorted) {
        super(parent);
        titleLabel.setText(title);

        stringList = new EditableStringList(this, elements, sorted, false);
        listPanel.add(initListComponent());
    }

    private JPanel initListComponent() {
        ToolbarDecorator decorator = createToolbarDecorator(stringList);
        decorator.setAddAction(b -> stringList.insertRow());
        decorator.setRemoveAction(b -> stringList.removeRow());
        decorator.setMoveUpAction(b -> stringList.moveRowUp());
        decorator.setMoveDownAction(b -> stringList.moveRowDown());

        return createToolbarDecoratorComponent(decorator, stringList);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return component;
    }

    public List<String> getStringValues() {
        return stringList.getStringValues();
    }

    public void setStringValues(List<String> stringValues) {
        stringList.setStringValues(stringValues);
    }
}
