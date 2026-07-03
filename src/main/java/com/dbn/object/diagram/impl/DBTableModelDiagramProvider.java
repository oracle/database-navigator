package com.dbn.object.diagram.impl;

import com.dbn.object.DBColumn;
import com.dbn.object.DBTable;
import com.dbn.object.common.DBObject;
import com.dbn.object.diagram.model.DBDiagramInput;
import com.dbn.object.diagram.model.DBDiagramProvider;
import com.dbn.object.diagram.model.DBDiagramRelation;
import com.dbn.object.diagram.model.DBDiagramType;
import com.intellij.diagram.DiagramBuilder;
import com.intellij.diagram.DiagramCategory;
import com.intellij.diagram.presentation.DiagramState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.dbn.common.load.ProgressMonitor.checkCancelled;
import static com.dbn.object.type.DBObjectType.COLUMN;

public final class DBTableModelDiagramProvider extends DBDiagramProvider<DBTable> {
    private static final DiagramCategory COLUMNS = createCategory(COLUMN);

    DBTableModelDiagramProvider() {
        super(DBDiagramType.TABLE_MODEL);
    }

    @Override
    public DBDiagramInput<DBTable> createInput(DBTable source) {
        return new DBDiagramInput<>(this, source);
    }

    @Override
    public Collection<DBTable> getRootObjects(DBTable source) {
        List<DBTable> result = new ArrayList<>();
        Set<Object> visited = new HashSet<>();
        Deque<DBTable> pending = new ArrayDeque<>();
        pending.add(source);
        while (!pending.isEmpty()) {
            DBTable table = pending.removeFirst();
            if (!visited.add(table.ref())) continue;
            checkCancelled();
            result.add(table);
            for (DBColumn column : table.getColumns()) {
                addRelated(pending, column.getForeignKeyColumn());
                if (column.isPrimaryKey()) for (DBColumn ref : column.getReferencingColumns()) addRelated(pending, ref);
            }
        }
        return result;
    }

    private static void addRelated(Deque<DBTable> pending, DBColumn column) {
        if (column != null && column.getDataset() instanceof DBTable table) pending.addLast(table);
    }

    @Override
    public Collection<? extends DBObject> getChildObjects(DBTable root) {
        return root.getColumns();
    }

    @Override
    public Collection<DBDiagramRelation<DBTable>> getRelations(Collection<? extends DBTable> roots) {
        List<DBDiagramRelation<DBTable>> result = new ArrayList<>();
        for (DBTable source : roots)
            for (DBColumn column : source.getColumns()) {
                DBColumn targetColumn = column.getForeignKeyColumn();
                if (targetColumn != null && targetColumn.getDataset() instanceof DBTable target)
                    result.add(new DBDiagramRelation<>(source, target, source.getName() + "." + column.getName() + " -> " + target.getName() + "." + targetColumn.getName()));
            }
        return result;
    }

    @Override
    public String getItemName(Object item, DiagramBuilder builder) {
        return item instanceof DBObject object ? object.getName() : "";
    }

    @Override
    public String getItemType(Object item) {
        return item instanceof DBColumn column && column.getDataType() != null ? column.getDataType().getName().toLowerCase(Locale.ROOT) : "";
    }

    @Override
    public DiagramCategory[] getContentCategories() {
        return new DiagramCategory[]{COLUMNS};
    }

    @Override
    public boolean isInCategory(Object node, Object child, DiagramCategory category, DiagramBuilder builder) {
        if (COLUMNS.equals(category)) return child instanceof DBColumn;
        return false;
    }

    @Override
    public boolean isInCategory(Object child, DiagramCategory category, DiagramState state) {
        if (COLUMNS.equals(category)) return child instanceof DBColumn;
        return false;
    }
}
