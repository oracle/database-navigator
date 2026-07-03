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

import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.diagram.DiagramNodeBase;
import com.intellij.diagram.DiagramProvider;
import com.intellij.ui.SimpleColoredText;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

final class DBDiagramNode<T extends DBObject> extends DiagramNodeBase<T> {
    private final DBObjectRef<T> object;

    DBDiagramNode(@NotNull T object, @NotNull DiagramProvider<T> provider) {
        super(provider);
        this.object = DBObjectRef.of(object);
    }

    @NotNull
    @Override
    public T getIdentifyingElement() {
        return object.ensure();
    }

    @Override
    public String getTooltip() {
        DBObject object = getIdentifyingElement();
        return object.getQualifiedNameWithType();
    }

    @Override
    public Icon getIcon() {
        DBObject object = getIdentifyingElement();
        return object.getIcon();
    }

    @Override
    protected SimpleColoredText computePresentableTitle() {
        return new SimpleColoredText(getObjectName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }

    public String getObjectName() {
        return object.getObjectName();
    }
}
