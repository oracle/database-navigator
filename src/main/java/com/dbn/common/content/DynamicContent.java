package com.dbn.common.content;

import com.dbn.common.content.dependency.ContentDependencyAdapter;
import com.dbn.common.content.loader.DynamicContentLoader;
import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.dispose.UnlistedDisposable;
import com.dbn.common.filter.Filter;
import com.dbn.common.property.PropertyHolder;
import com.dbn.connection.DatabaseEntity;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface DynamicContent<T extends DynamicContentElement> extends
        DatabaseEntity,
        StatefulDisposable,
        UnlistedDisposable,
        PropertyHolder<DynamicContentProperty> {

    /**
     * Triggering the actual load of the content
     */
    void load();

    /**
     * Rebuilds the content. This method is called when reloading the content
     * is triggered deliberately by the user directly or by a ddl change.
     */
    void reload();

    /**
     * Soft reload. Mark sources dirty
     */
    void refresh();

    void reloadInBackground();

    void loadInBackground();

    /**
     * The signature of the last change on the content (incrementing byte).
     */
    byte getSignature();

    boolean isReady();

    /**
     * A load attempt has been made already
     */
    boolean isLoaded();

    boolean isLoading();

    boolean isLoadingInBackground();

    /**
     * The content has been loaded but with errors (e.g. because of database connectivity problems)
     */
    boolean isDirty();

    boolean isMaster();

    default boolean canLoad() {
        return getDependencyAdapter().canLoad();
    }

    default boolean canLoadFast() {
        return getDependencyAdapter().canLoadFast();
    }

    default boolean canLoadInBackground() {
        return getDependencyAdapter().canLoadInBackground();
    }

    default boolean isDependencyDirty() {
        return getDependencyAdapter().isDependencyDirty();
    }

    boolean isEmpty();

    void markDirty();

    DynamicContentType getContentType();

    String getContentDescription();

    List<T> getElements();

    List<T> getElements(String name);

    List<T> getAllElements();

    @Nullable
    default Filter<T> getFilter() {
        return null;
    }


    T getElement(String name, short overload);

    void setElements(@Nullable List<T> elements);

    int size();

    DynamicContentLoader getLoader();

    ContentDependencyAdapter getDependencyAdapter();

    default String getParentSchemaName() {
        DatabaseEntity entity = getParentEntity();
        if (entity instanceof DBObject) {
            DBObject object = (DBObject) entity;
            DBSchema schema = object.getSchema();
            if (schema != null) return schema.getName();
        }
        return null;
    }
    default String getParentObjectName() {
        DatabaseEntity entity = getParentEntity();
        if (entity instanceof DBObject) {
            DBObject object = (DBObject) entity;
            return object.getName();
        }
        return null;
    }
}
