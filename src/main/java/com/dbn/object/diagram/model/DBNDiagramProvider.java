/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.object.diagram.model;

import com.dbn.object.DBTable;
import com.dbn.object.diagram.DatabaseDiagramManager;
import com.intellij.diagram.BaseDiagramProvider;
import com.intellij.diagram.DiagramCategory;
import com.intellij.diagram.DiagramDataModel;
import com.intellij.diagram.DiagramElementManager;
import com.intellij.diagram.DiagramNodeContentManager;
import com.intellij.diagram.DiagramPresentationModel;
import com.intellij.diagram.DiagramVfsResolver;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.Collections;

public final class DBNDiagramProvider extends BaseDiagramProvider<DBTable> {
    // DiagramProvider.findByID accepts only [a-zA-Z0-9_-]* identifiers.
    public static final String ID = "dbn_database_diagram";

    private final DiagramElementManager<DBTable> elementManager = new DBNDiagramElementManager(this);
    private final DiagramVfsResolver<DBTable> vfsResolver = new DBNDiagramVfsResolver();
    private static final DiagramCategory COLUMNS = new DiagramCategory(() -> "Columns", null);

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public String getPresentableName() {
        return "Database Diagram";
    }

    @Override
    public DiagramDataModel<DBTable> createDataModel(
            Project project,
            DBTable element,
            VirtualFile file,
            DiagramPresentationModel presentationModel) {

        // The UML editor provider can recreate a persisted diagram before its
        // original element has been resolved.  In that case IntelliJ passes a
        // null element.  Do not let that become a call to the DBN model
        // builder, which requires a real DBTable and may access database state.
        if (element == null) {
            return new DBNDiagramDataModel(project, this,
                    DatabaseDiagramManager.getInstance(project).createModel(Collections.emptyList()));
        }
        return new DBNDiagramDataModel(project, this,
                DatabaseDiagramManager.getInstance(project).createModel(element));
    }

    @Override
    public DiagramElementManager<DBTable> getElementManager() {
        return elementManager;
    }

    @Override
    public DiagramVfsResolver<DBTable> getVfsResolver() {
        return vfsResolver;
    }

    @Override
    public DiagramNodeContentManager createNodeContentManager() {
        return new DBNDiagramNodeContentManager();
    }

    @Override
    public DiagramCategory[] getAllContentCategories() {
        return new DiagramCategory[]{COLUMNS};
    }
}
