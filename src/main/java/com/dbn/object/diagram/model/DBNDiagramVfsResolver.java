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
import com.intellij.diagram.DiagramVfsResolver;
import com.intellij.openapi.project.Project;

final class DBNDiagramVfsResolver implements DiagramVfsResolver<DBTable> {
    @Override
    public String getQualifiedName(DBTable element) {
        return element == null ? "" : element.getQualifiedName();
    }

    @Override
    public DBTable resolveElementByFQN(String fqn, Project project) {
        return null;
    }
}
