package com.dbn.object.diagram.impl;

import com.dbn.common.presentation.Presentation;
import com.dbn.object.DBColumn;
import com.dbn.object.DBTable;
import com.dbn.object.common.DBObject;
import com.dbn.object.diagram.model.DBDiagramDescriptor;
import com.dbn.object.diagram.model.DBDiagramInput;
import com.dbn.object.diagram.model.DBDiagramRelation;
import com.dbn.object.diagram.model.DBDiagramType;
import com.intellij.diagram.DiagramBuilder;
import com.intellij.diagram.DiagramCategory;
import com.intellij.diagram.presentation.DiagramState;

import javax.swing.Icon;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.dbn.common.load.ProgressMonitor.checkCancelled;
import static com.dbn.common.load.ProgressMonitor.setProgressDetail;

public final class DBDataModelDiagramDescriptor implements DBDiagramDescriptor<DBTable> {
    private static final DiagramCategory COLUMNS = new DiagramCategory(() -> "Columns", null);
    @Override
    public DBDiagramType getDiagramType() {
        return DBDiagramType.DATA_MODEL;
    }

    @Override
    public DBDiagramInput<DBTable> createInput(DBTable table) {
        return new DBDiagramInput<>(this, table);
    }

    @Override
    public Collection<DBTable> getRootObjects(DBTable element) {
        return findRelatedTables(element);
    }

    private Collection<DBTable> findRelatedTables(DBTable source) {
        List<DBTable> result = new ArrayList<>();
        Set<Object> visited = new LinkedHashSet<>();
        Deque<DBTable> pending = new ArrayDeque<>();
        pending.add(source);
        while (!pending.isEmpty()) {
            DBTable table = pending.removeFirst();
            if (!visited.add(table.ref())) continue;
            checkCancelled();
            setProgressDetail("Exploring table " + table.getQualifiedName());
            result.add(table);
            for (DBColumn column : table.getColumns()) {
                addRelated(pending, column.getForeignKeyColumn());
                if (column.isPrimaryKey()) {
                    for (DBColumn referencing : column.getReferencingColumns()) addRelated(pending, referencing);
                }
            }
        }
        return result;
    }

    private static void addRelated(Deque<DBTable> pending, DBColumn column) {
        if (column != null && column.getDataset() instanceof DBTable table) pending.addLast(table);
    }

    @Override
    public Collection<? extends DBObject> getChildObjects(DBTable element) {
        return new ArrayList<>(element.getColumns());
    }

    @Override
    public String getItemName(Object item, DiagramBuilder builder) {
        return Presentation.presentableName(item);
    }

    @Override
    public String getItemType(Object item) {
        if (item instanceof DBColumn column && column.getDataType() != null) {
            return column.getDataType().getName().toLowerCase(Locale.ROOT);
        }
        return "";
    }

    @Override public Icon getItemIcon(DBTable root, Object item, DiagramBuilder builder) {
        return item instanceof DBColumn column ? column.getIcon() : null;
    }
    @Override public DiagramCategory[] getContentCategories() { return new DiagramCategory[]{COLUMNS}; }
    @Override public boolean isInCategory(Object node, Object child, DiagramCategory category, DiagramBuilder builder) {
        return COLUMNS.equals(category) && child instanceof DBColumn;
    }
    @Override public boolean isInCategory(Object child, DiagramCategory category, DiagramState state) {
        return COLUMNS.equals(category) && child instanceof DBColumn;
    }

    @Override
    public Collection<DBDiagramRelation<DBTable>> getRelations(Collection<? extends DBTable> roots) {
        List<DBDiagramRelation<DBTable>> relations = new ArrayList<>();
        for (DBObject root : roots) {
            if (!(root instanceof DBTable source)) continue;

            for (DBColumn column : source.getColumns()) {
                DBColumn targetColumn = column.getForeignKeyColumn();
                if (targetColumn == null) continue;
                if (!(targetColumn.getDataset() instanceof DBTable target)) continue;

                String name =
                        source.getName() + "." + column.getName() + " -> " +
                        target.getName() + "." + targetColumn.getName();


                relations.add(new DBDiagramRelation<>(source, target, name));
            }
        }
        return relations;
    }
}
