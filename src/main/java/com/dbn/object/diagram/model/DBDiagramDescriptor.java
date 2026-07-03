package com.dbn.object.diagram.model;

import com.dbn.object.common.DBObject;
import com.intellij.diagram.DiagramBuilder;
import com.intellij.diagram.DiagramCategory;
import com.intellij.diagram.presentation.DiagramState;

import javax.swing.Icon;
import java.util.Collection;

public interface DBDiagramDescriptor<T extends DBObject> {
    DBDiagramType getDiagramType();

    DBDiagramInput<T> createInput(T element);

    Collection<T> getRootObjects(T element);

    Collection<? extends DBObject> getChildObjects(T element);

    Collection<DBDiagramRelation<T>> getRelations(Collection<? extends T> roots);

    default Object[] getNodeItems(T root) {
        return getChildObjects(root).toArray();
    }

    default String getElementTitle(T root) {
        return root.getName();
    }

    default String getNodeTooltip(T root) {
        return root.getQualifiedNameWithType();
    }

    default String getItemName(Object item, DiagramBuilder builder) {
        return item instanceof DBObject object ? object.getName() : "";
    }

    default String getItemType(Object item) {
        return "";
    }

    default Icon getItemIcon(T root, Object item, DiagramBuilder builder) {
        return item instanceof DBObject object ? object.getIcon() : null;
    }
    default DiagramCategory[] getContentCategories() { return DiagramCategory.EMPTY_ARRAY; }
    default boolean isInCategory(Object node, Object child, DiagramCategory category, DiagramBuilder builder) { return false; }
    default boolean isInCategory(Object child, DiagramCategory category, DiagramState state) { return false; }
}
