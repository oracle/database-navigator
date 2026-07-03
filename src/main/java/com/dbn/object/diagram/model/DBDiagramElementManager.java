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

import com.dbn.common.presentation.Presentation;
import com.dbn.object.common.DBObject;
import com.intellij.diagram.DiagramBuilder;
import com.intellij.diagram.DiagramElementManager;
import com.intellij.diagram.DiagramProvider;
import com.intellij.diagram.presentation.DiagramState;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.ui.SimpleColoredText;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.Collection;

import static com.dbn.common.util.Unsafe.cast;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

final class DBDiagramElementManager<T extends DBObject> implements DiagramElementManager<T> {
    private final DBDiagramProvider<T> provider;

    DBDiagramElementManager(@NotNull DBDiagramProvider<T> provider) {
        this.provider = provider;
    }

    @Override
    public void setUmlProvider(DiagramProvider<T> provider) {
    }

    @Override
    public T findInDataContext(DataContext dataContext) {
        Object selectedItem = dataContext.getData(PlatformCoreDataKeys.SELECTED_ITEM);
        return selectedItem instanceof DBObject object ? cast(object) : null;
    }

    @Override
    public Collection<T> findElementsInDataContext(DataContext dataContext) {
        T object = findInDataContext(dataContext);
        return object == null ? emptyList() : singletonList(object);
    }

    @Override
    public boolean isAcceptableAsNode(Object element) {
        return element instanceof DBObject;
    }

    @Override
    public Object[] getNodeItems(T element) {
        return descriptor().getNodeItems(element);
    }

    @Override
    public boolean canCollapse(T element) {
        return false;
    }

    @Override
    public boolean isContainerFor(DBObject container, DBObject element) {
        return false;
    }

    @Override
    public String getElementTitle(T element) {
        return descriptor().getElementTitle(element);
    }

    @Override
    public String getNodeTooltip(T element) {
        return descriptor().getNodeTooltip(element);
    }

    @Override
    public SimpleColoredText getItemName(Object item, DiagramBuilder builder) {
        return regularText(descriptor().getItemName(item, builder));
    }

    @Override
    public SimpleColoredText getItemName(Object item, DiagramState state) {
        return regularText(descriptor().getItemName(item, null));
    }

    @Override
    public SimpleColoredText getItemName(DBObject element, Object item, DiagramBuilder builder) {
        return regularText(descriptor().getItemName(item, builder));
    }

    @Override
    @SuppressWarnings("deprecation")
    public SimpleColoredText getItemType(Object item) {
        return grayedText(descriptor().getItemType(item));
    }

    @Override
    public SimpleColoredText getItemType(DBObject element, Object item, DiagramBuilder builder) {
        return grayedText(descriptor().getItemType(item));
    }

    @Override
    public Icon getItemIcon(DBObject element, Object item, DiagramBuilder builder) {
        return Presentation.presentableIcon(item);
    }

    private static SimpleColoredText regularText(String text) {
        return new SimpleColoredText(text, SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }

    private static SimpleColoredText grayedText(String text) {
        return new SimpleColoredText(text, SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }

    private DBDiagramDescriptor<T> descriptor() {
        return provider.getDescriptor();
    }

}
