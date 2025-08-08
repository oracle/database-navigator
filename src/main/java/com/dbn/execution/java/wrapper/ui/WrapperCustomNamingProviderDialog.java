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

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.execution.java.wrapper.WrapperModel;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.util.HashMap;
import java.util.Map;

@Getter
public class WrapperCustomNamingProviderDialog extends DBNDialog<WrapperCustomNamingProviderForm> {
    private final WrapperModel model;

    private String javaWrapperName;
    private String sqlWrapperName;
    private Map<String, String> sqlTypeNames;
    private Map<String, String> packageMethodNames;

    private final boolean classLevel;
    private final int maxIdentifierLength;
    public WrapperCustomNamingProviderDialog(Project project, WrapperModel model, boolean classLevel, int maxIdentifierLength) {
        super(project, "Wrapper Custom Name", false);
        this.setModal(true);
        this.setAutoSize(true);
        this.model = model;
        this.classLevel = classLevel;
        this.maxIdentifierLength = maxIdentifierLength;
        init();
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[]{getOKAction()};
    }

    @Override
    protected @NotNull WrapperCustomNamingProviderForm createForm() {
        return new WrapperCustomNamingProviderForm(this, model, classLevel, maxIdentifierLength);
    }

    @Override
    protected void doOKAction() {
        javaWrapperName = getForm().getJavaWrapperName();
        sqlWrapperName = getForm().getSqlWrapperName();
        sqlTypeNames = new HashMap<>(getForm().getSqlTypeNames());
        packageMethodNames = new HashMap<>(getForm().getPackageMethodNames());
        super.doOKAction();
    }
}
