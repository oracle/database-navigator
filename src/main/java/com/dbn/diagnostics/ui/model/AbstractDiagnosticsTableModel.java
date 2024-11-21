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

package com.dbn.diagnostics.ui.model;

import com.dbn.common.latent.Latent;
import com.dbn.common.project.ProjectRef;
import com.dbn.common.ui.table.DBNMutableTableModel;
import com.dbn.diagnostics.data.DiagnosticBundle;
import com.dbn.diagnostics.data.DiagnosticEntry;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractDiagnosticsTableModel<T extends Comparable<T>> extends DBNMutableTableModel<DiagnosticEntry<T>> {
    private final ProjectRef project;
    private final Latent<DiagnosticBundle<T>> diagnostics = Latent.weak(() -> resolveDiagnostics());
    private transient int signature = -1;

    public AbstractDiagnosticsTableModel(Project project) {
        this.project = ProjectRef.of(project);
    }

    @NotNull
    protected abstract String[] getColumnNames();

    @NotNull
    protected abstract DiagnosticBundle<T> resolveDiagnostics();

    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    public DiagnosticBundle<T> getDiagnostics() {
        DiagnosticBundle<T> diagnostics = this.diagnostics.get();
        int newSignature = diagnostics.getSignature();
        if (this.signature != newSignature) {
            this.signature = newSignature;
            notifyRowChanges();
        }
        return diagnostics;
    }

    @Override
    public final int getRowCount() {
        return getDiagnostics().getKeys().size();
    }

    @Override
    public final int getColumnCount() {
        return getColumnNames().length;
    }

    @Override
    public final String getColumnName(int columnIndex) {
        return getColumnNames()[columnIndex];
    }

    @Override
    public final Class<?> getColumnClass(int columnIndex) {
        return DiagnosticEntry.class;
    }

    @Override
    public final Object getValueAt(int rowIndex, int columnIndex) {
        DiagnosticBundle<T> bundle = getDiagnostics();
        T key = bundle.getKeys().get(rowIndex);
        return bundle.get(key);
    }
}
