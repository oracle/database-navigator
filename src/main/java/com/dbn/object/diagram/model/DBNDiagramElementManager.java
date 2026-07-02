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
import com.dbn.object.DBColumn;
import com.dbn.object.DBTable;
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
import java.util.Locale;

final class DBNDiagramElementManager implements DiagramElementManager<DBTable> {
    private final DiagramProvider<DBTable> provider;

    DBNDiagramElementManager(@NotNull DiagramProvider<DBTable> provider) {
        this.provider = provider;
    }

    @Override
    public void setUmlProvider(DiagramProvider<DBTable> provider) {
    }

    @Override
    public DBTable findInDataContext(DataContext dataContext) {
        Object selectedItem = dataContext.getData(PlatformCoreDataKeys.SELECTED_ITEM);
        return selectedItem instanceof DBTable table ? table : null;
    }

    @Override
    public java.util.Collection<DBTable> findElementsInDataContext(DataContext dataContext) {
        DBTable table = findInDataContext(dataContext);
        return table == null ? java.util.Collections.emptyList() : java.util.Collections.singletonList(table);
    }

    @Override
    public boolean isAcceptableAsNode(Object element) {
        return element instanceof DBTable;
    }

    @Override
    public Object[] getNodeItems(DBTable element) {
        return element.getColumns().toArray();
    }

    @Override
    public boolean canCollapse(DBTable element) {
        return false;
    }

    @Override
    public boolean isContainerFor(DBTable container, DBTable element) {
        return false;
    }

    @Override
    public String getElementTitle(DBTable element) {
        return element.getName();
    }

    @Override
    public String getNodeTooltip(DBTable element) {
        return element.getQualifiedName();
    }

    private static String getItemName(Object item) {
        return Presentation.presentableName(item);
    }

    @Override
    public SimpleColoredText getItemName(Object item, DiagramBuilder builder) {
        return getItemText(item);
    }

    @Override
    public SimpleColoredText getItemName(Object item, DiagramState state) {
        return getItemText(item);
    }

    @Override
    public SimpleColoredText getItemName(DBTable element, Object item, DiagramBuilder builder) {
        return getItemText(item);
    }

    @Override
    @SuppressWarnings("deprecation")
    public SimpleColoredText getItemType(Object item) {
        return getItemDetail(item);
    }

    @Override
    public SimpleColoredText getItemType(DBTable element, Object item, DiagramBuilder builder) {
        return getItemDetail(item);
    }

    @Override
    public Icon getItemIcon(DBTable element, Object item, DiagramBuilder builder) {
        return ((DBColumn) item).getIcon();
    }

    private static SimpleColoredText getItemDetail(Object item) {
        if (item instanceof DBColumn column) {
            String dataType = column.getDataType().getName().toLowerCase(Locale.ROOT);
            return grayedText(dataType);

        }
        return grayedText("");
    }


    private static SimpleColoredText getItemText(Object item) {
        return regularText(getItemName(item));
    }

    private static @NotNull SimpleColoredText regularText(String name) {
        return new SimpleColoredText(name, SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }

    private static @NotNull SimpleColoredText grayedText(String type) {
        return new SimpleColoredText(type, SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }


}
