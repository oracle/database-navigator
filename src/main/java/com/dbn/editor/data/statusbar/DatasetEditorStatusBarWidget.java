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

package com.dbn.editor.data.statusbar;

import com.dbn.common.compatibility.CompatibilityUtil;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.icon.Icons;
import com.dbn.common.listener.DBNFileEditorManagerListener;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.MathResult;
import com.dbn.common.util.Safe;
import com.dbn.editor.data.DatasetEditor;
import com.dbn.editor.data.ui.table.DatasetEditorTable;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.CustomStatusBarWidget;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import static com.dbn.common.component.Components.projectService;

public class DatasetEditorStatusBarWidget extends ProjectComponentBase implements CustomStatusBarWidget {
    private static final String WIDGET_ID = DatasetEditorStatusBarWidget.class.getName();
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatasetEditorStatusBarWidget";

    private final JLabel textLabel;
    private final Alarm updateAlarm = Dispatch.alarm(this);
    private final JPanel component = new JPanel(new BorderLayout());

    DatasetEditorStatusBarWidget(@NotNull Project project) {
        super(project, COMPONENT_NAME);
        textLabel = new JLabel();
        component.add(textLabel, BorderLayout.WEST);

        ProjectEvents.subscribe(project, this, FileEditorManagerListener.FILE_EDITOR_MANAGER, fileEditorManagerListener());
    }

    public static DatasetEditorStatusBarWidget getInstance(@NotNull Project project) {
        return projectService(project, DatasetEditorStatusBarWidget.class);
    }

    FileEditorManagerListener fileEditorManagerListener() {
        return new DBNFileEditorManagerListener() {
            @Override
            public void whenSelectionChanged(@NotNull FileEditorManagerEvent event) {
                update();
            }

            @Override
            public void whenFileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                update();
            }

            @Override
            public void whenFileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                update();
            }
        };
    }


    @NotNull
    @Override
    public String ID() {
        return WIDGET_ID;
    }

    @Nullable
    private DatasetEditor getSelectedEditor() {
        Project project = getProject();
        FileEditor selectedEditor = CompatibilityUtil.getSelectedEditor(project);
        if (selectedEditor instanceof DatasetEditor) {
            return (DatasetEditor) selectedEditor;
        }
        return null;
    }

    @Nullable
    private DatasetEditorTable getEditorTable() {
        DatasetEditor selectedEditor = getSelectedEditor();
        return selectedEditor == null ? null : selectedEditor.getEditorTable();
    }

    public void update() {
        Dispatch.alarmRequest(updateAlarm, 100, true, () -> {
            DatasetEditorTable editorTable = getEditorTable();
            MathResult mathResult = Safe.call(editorTable, table -> table.getSelectionMath());

            if (mathResult == null) {
                textLabel.setText("");
                textLabel.setIcon(null);
            } else {
                textLabel.setText(" " +
                        "Sum " +  mathResult.getSum() + "   " +
                        "Count " + mathResult.getCount() + "   " +
                        "Average " + mathResult.getAverage());
                textLabel.setIcon(Icons.COMMON_DATA_GRID);
            }
            UserInterface.repaint(getComponent());
        });
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {

    }

    @Override
    public JComponent getComponent() {
        return component;
    }

    @Override
    public void dispose() {
        if (isDisposed()) return;
        setDisposed(true);

        disposeInner();
    }
}
