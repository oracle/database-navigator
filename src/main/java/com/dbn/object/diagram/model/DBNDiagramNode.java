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
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.diagram.DiagramNodeBase;
import com.intellij.diagram.DiagramProvider;
import com.intellij.ui.SimpleColoredText;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

final class DBNDiagramNode extends DiagramNodeBase<DBTable> {
    private final DBObjectRef<DBTable> table;

    DBNDiagramNode(@NotNull DBTable table, @NotNull DiagramProvider<DBTable> provider) {
        super(provider);
        this.table = DBObjectRef.of(table);
    }

    @NotNull
    @Override
    public DBTable getIdentifyingElement() {
        return table.ensure();
    }

    @Override
    public String getTooltip() {
        DBTable table = getIdentifyingElement();
        return table.getQualifiedNameWithType();
    }

    @Override
    public Icon getIcon() {
        DBTable table = getIdentifyingElement();
        return table.getIcon();
    }

    @Override
    protected SimpleColoredText computePresentableTitle() {
        return new SimpleColoredText(getObjectName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }

    public String getObjectName() {
        return table.getObjectName();
    }
}
