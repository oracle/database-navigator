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

package com.dbn.data.editor.ui;

import com.dbn.common.action.BasicAction;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.icon.Icons;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.util.Keyboard;
import com.dbn.common.ui.util.Popups;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Strings;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.dispose.Disposer.replace;
import static com.dbn.common.ui.util.Popups.popupBuilder;
import static com.dbn.common.util.Strings.nonEmptyStrings;

@Getter
@Setter
public class ValueListPopupProvider implements TextFieldPopupProvider{
    private final WeakRef<TextFieldWithPopup> editorComponent;
    private final ListPopupValuesProvider valuesProvider;
    private DBObjectRef<?> contextObject;

    private final boolean autoPopup;
    private final boolean buttonVisible;
    private boolean enabled = true;
    private boolean preparing = false;

    private JComponent button;
    private transient ListPopup popup;

    ValueListPopupProvider(TextFieldWithPopup editorComponent, ListPopupValuesProvider valuesProvider, @Nullable DBObject contextObject, boolean autoPopup, boolean buttonVisible) {
        this.editorComponent = WeakRef.of(editorComponent);
        this.valuesProvider = valuesProvider;
        this.contextObject = DBObjectRef.of(contextObject);
        this.autoPopup = autoPopup;
        this.buttonVisible = buttonVisible;
    }

    public TextFieldWithPopup getEditorComponent() {
        return editorComponent.ensure();
    }

    @Nullable
    public DBObject getContextObject() {
        return DBObjectRef.get(contextObject);
    }

    @Override
    public TextFieldPopupType getPopupType() {
        return null;
    }

    @Override
    public boolean isShowingPopup() {
        return popup != null && popup.isVisible();
    }

    @Override
    public void showPopup() {
        if (valuesProvider.isLoaded()) {
            doShowPopup();
            return;
        }

        if (preparing) return;
        preparing = true;
        Project project = getEditorComponent().getProject();
        DBObject contextObject = getContextObject();

        Progress.prompt(project, null, true,
                "Loading Values",
                contextObject == null ?
                        "Loading possible values" :
                        "Loading possible values for " + contextObject.getQualifiedNameWithType(),
                p -> loadAndDisplayValues(p));
    }

    private void loadAndDisplayValues(ProgressIndicator p) {
        ensureValuesLoaded();
        if (p.isCanceled()) {
            preparing = false;
            return;
        }
        Dispatch.run(getEditorComponent(), () -> invokeShowPopup());
    }

    private void invokeShowPopup() {
        try {
            if (!isShowingPopup()) doShowPopup();
        } finally {
            preparing = false;
        }
    }

    private void ensureValuesLoaded() {
        getValues();
        getSecondaryValues();
        valuesProvider.setLoaded(true);
    }

    private void doShowPopup() {
        TextFieldWithPopup editorComponent = getEditorComponent();
        List<String> values = getValues();
        List<String> secondaryValues = getSecondaryValues();
        if (false && values.size() < 20)  {
            String[] valuesArray = values.toArray(new String[0]);
            BaseListPopupStep<String> listPopupStep = new BaseListPopupStep<>(null, valuesArray) {
                @Override
                public PopupStep onChosen(String selectedValue, boolean finalChoice) {
                    editorComponent.setText(selectedValue);
                    return FINAL_CHOICE;
                }
            };
            popup = JBPopupFactory.getInstance().createListPopup(listPopupStep);
        } else {
            List<AnAction> actions = new ArrayList<>();

            for (String value : values) {
                if (Strings.isNotEmpty(value)) {
                    actions.add(new ValueSelectAction(value));
                }
            }
            if (!secondaryValues.isEmpty()) {
                if (!values.isEmpty()) {
                    actions.add(Actions.SEPARATOR);
                }
                for (String secondaryValue : secondaryValues) {
                    if (Strings.isNotEmpty(secondaryValue)) {
                        actions.add(new ValueSelectAction(secondaryValue));
                    }
                }
            }

            popup = popupBuilder(actions, editorComponent).
                    withTitle(getName()).
                    withTitleVisible(false).
                    withSpeedSearch().
                    withMaxRowCount(10).
                    build();
        }


        Popups.showUnderneathOf(popup, editorComponent, 4, 200);
    }

    private List<String> getValues() {
        return nonEmptyStrings(valuesProvider.getValues());
    }

    private List<String> getSecondaryValues() {
        return nonEmptyStrings(valuesProvider.getSecondaryValues());
    }

    @Override
    public void hidePopup() {
        if (popup != null) {
            if (popup.isVisible()) popup.cancel();
            Disposer.dispose(popup);
        }
    }

    @Override
    public String getName() {
        return valuesProvider.getName();
    }

    @Override
    public String getKeyShortcutDescription() {
        return KeymapUtil.getShortcutsText(getShortcuts());
    }

    @Override
    public Shortcut[] getShortcuts() {
        return Keyboard.getShortcuts(IdeActions.ACTION_CODE_COMPLETION);
    }

    @Nullable
    @Override
    public Icon getButtonIcon() {
        return Icons.DATA_EDITOR_LIST;
    }

    @Override
    public void dispose() {
        popup = replace(popup, null);
    }

    private class ValueSelectAction extends BasicAction {
        private final String value;

        ValueSelectAction(String value) {
            this.value = value;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            TextFieldWithPopup editorComponent = getEditorComponent();
            editorComponent.setText(value);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            presentation.setText(value, false);
        }
    }
}
