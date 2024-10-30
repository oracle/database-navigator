package com.dbn.common.content;

import com.dbn.common.collections.CompactArrayList;
import com.dbn.common.content.dependency.ContentDependencyAdapter;
import com.dbn.common.content.dependency.VoidContentDependencyAdapter;
import com.dbn.common.content.loader.DynamicContentLoader;
import com.dbn.common.dispose.BackgroundDisposer;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.filter.FilterDelegate;
import com.dbn.common.list.FilteredList;
import com.dbn.common.notification.NotificationSupport;
import com.dbn.common.property.DisposablePropertyHolder;
import com.dbn.common.property.PropertyHolderBase;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Synchronized;
import com.dbn.common.thread.ThreadMonitor;
import com.dbn.common.util.Lists;
import com.dbn.common.util.Strings;
import com.dbn.common.util.Unsafe;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.DatabaseEntity;
import com.dbn.nls.NlsSupport;
import com.intellij.openapi.progress.ProcessCanceledException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.dbn.common.notification.NotificationGroup.METADATA;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
public abstract class DynamicContentBase<T extends DynamicContentElement>
        extends PropertyHolderBase.IntStore<DynamicContentProperty>
        implements DisposablePropertyHolder<DynamicContentProperty>,
                   DynamicContent<T>, NotificationSupport, NlsSupport {

    protected static final List<?> EMPTY_CONTENT = Collections.unmodifiableList(new ArrayList<>(0));
    protected static final List<?> EMPTY_DISPOSED_CONTENT = Collections.unmodifiableList(new ArrayList<>(0));
    protected static final List<?> EMPTY_UNTOUCHED_CONTENT = Collections.unmodifiableList(new ArrayList<>(0));

    private ContentDependencyAdapter dependencyAdapter;
    private DatabaseEntity parent;
    private volatile byte signature = 0;

    protected List<T> elements = Unsafe.cast(EMPTY_UNTOUCHED_CONTENT);

    protected DynamicContentBase(
            @NotNull DatabaseEntity parent,
            ContentDependencyAdapter dependencyAdapter,
            DynamicContentProperty... properties) {

        this.parent = parent;
        this.dependencyAdapter = dependencyAdapter;
        if (properties != null) {
            for (DynamicContentProperty status : properties) {
                set(status, true);
            }
        }
    }

    @Override
    protected DynamicContentProperty[] properties() {
        return DynamicContentProperty.VALUES;
    }

    @Override
    @NotNull
    public <E extends DatabaseEntity> E getParentEntity() {
        return Unsafe.cast(Failsafe.nn(parent));
    }

    @NotNull
    public ConnectionId getConnectionId() {
        return getParentEntity().getConnectionId();
    }

    @Override
    @NotNull
    public ConnectionHandler getConnection() {
        return getParentEntity().getConnection();
    }

    private boolean canConnect() {
        ConnectionHandler connection = getConnection();
        return ConnectionHandler.canConnect(connection);
    }

    @Override
    public abstract DynamicContentLoader<T, ?> getLoader();

    @Override
    public ContentDependencyAdapter getDependencyAdapter() {
        return dependencyAdapter;
    }

    @Override
    public byte getSignature() {
        return signature;
    }

    @Override
    public boolean isMaster() {
        return is(DynamicContentProperty.MASTER);
    }

    @Override
    public boolean isLoaded() {
        return is(DynamicContentProperty.LOADED);
    }

    @Override
    public boolean isReady() {
        return isLoaded() && !isLoading() && !isDirty();
    }

    @Override
    public boolean isLoading() {
        return is(DynamicContentProperty.LOADING);
    }

    public boolean isLoadingInBackground() {
        return is(DynamicContentProperty.LOADING_IN_BACKGROUND);
    }

    @Override
    public boolean isDirty() {
        return is(DynamicContentProperty.DIRTY) || isDependencyDirty();
    }



    @Override
    public void markDirty() {
        set(DynamicContentProperty.DIRTY, true);
    }

    private boolean shouldLoad() {
        if (!isLoaded()) {
            if (isDisposed()) return false;
            if (isLoading()) return false;
            if (!canConnect()) return false;
            return true;
        }

        if (isDirty()) {
            if (isDisposed()) return false;
            if (isLoading()) return false;
            if (!canLoad()) return false;
            if (!canConnect()) return false;
            return true;
        }

        return false;
    }

    private boolean shouldLoadInBackground() {
        if (shouldLoad()) {
            if (isLoadingInBackground()) return false;
            if (!canLoadInBackground()) return false;
            return true;
        }

        return false;
    }

    private boolean shouldReload() {
        // only allow refresh / reload if already loaded
        if (isLoaded()) {
            if (isDisposed()) return false;
            if (isLoading()) return false;
            if (isLoadingInBackground()) return false;

            return true;
        }

        return false;
    }

    private boolean shouldRefresh() {
        if (shouldReload()) {
            if (isDirty()) return false;

            return true;
        }

        return false;
    }

    @Override
    public void load() {
        ensureLoaded(false);
    }

    @Override
    public final void loadInBackground() {
        if (shouldLoadInBackground()) {
            set(DynamicContentProperty.LOADING_IN_BACKGROUND, true);
            Background.run(getProject(), () -> {
                try {
                    ensureLoaded(false);
                } finally {
                    set(DynamicContentProperty.LOADING_IN_BACKGROUND, false);
                }
            });
        }
    }

    @Override
    public final void reload() {
        if (shouldReload()) {
            markDirty();
            ensureLoaded(true);
            refreshElements();
        }
    }

    @Override
    public void refresh() {
        if (shouldRefresh()) {
            markDirty();
            refreshSources();
            if (!is(DynamicContentProperty.INTERNAL)){
                refreshElements();
            }
        }
    }

    private void refreshSources() {
        dependencyAdapter.refreshSources();
    }

    private void refreshElements() {
        elements.forEach(e -> e.refresh());
    }

    /**
     * Synchronised block making sure the content is loaded before the thread is released
     */
    private void ensureLoaded(boolean force) {
        if (isReady()) return;

        Synchronized.on(this, o -> {
            if (o.isReady()) return; // content meanwhile loaded by another thread

            o.set(DynamicContentProperty.LOADING, true);
            try {
                o.performLoad(force);
            } finally {
                o.set(DynamicContentProperty.LOADING, false);
                o.changeSignature();
            }
        });

    }

    private void performLoad(boolean force) {
        checkDisposed();
        dependencyAdapter.beforeLoad(force);
        checkDisposed();

        try {
            DynamicContentLoader<T, ?> loader = getLoader();
            loader.loadContent(this);
            set(DynamicContentProperty.DIRTY, false);
            set(DynamicContentProperty.LOADED, true);

            // refresh inner elements
            if (force) {
                for (T element : elements) {
                    element.refresh();
                }
            }

        } catch (ProcessCanceledException e) {
            conditionallyLog(e);
            throw e;

        } catch (SQLFeatureNotSupportedException e) {
            conditionallyLog(e);
            // unsupported feature: log in notification area
            elements = Unsafe.cast(EMPTY_CONTENT);
            set(DynamicContentProperty.DIRTY, false);
            set(DynamicContentProperty.LOADED, true);
            set(DynamicContentProperty.ERROR, true);
            sendWarningNotification(METADATA,
                    txt("ntf.metadata.error.FailedToLoadContent", getContentDescription(), e));

        } catch (SQLException e) {
            conditionallyLog(e);
            // connectivity / timeout exceptions: mark content dirty (no logging)
            elements = Unsafe.cast(EMPTY_CONTENT);
            set(DynamicContentProperty.DIRTY, true);

        } catch (Throwable e) {
            conditionallyLog(e);
            // any other exception: log error
            log.error("Failed to load content", e);
            elements = Unsafe.cast(EMPTY_CONTENT);
            set(DynamicContentProperty.DIRTY, true);
        }

        checkDisposed();
        dependencyAdapter.afterLoad();
    }

    protected void changeSignature() {
        signature++;
    }


    /**
     * do whatever is needed after the content is loaded (e.g. refresh browser tree..)
     */
    public abstract void notifyChangeListeners();

    @Override
    public void setElements(List<T> elements) {
        conditional(DynamicContentProperty.CHANGING, () -> replaceElements(elements));
    }

    private void replaceElements(List<T> elements) {
        beforeUpdate();
        if (isDisposed() || elements == null || elements.isEmpty()) {
            elements = Unsafe.cast(EMPTY_CONTENT);
        } else {
            sortElements(elements);
            elements = CompactArrayList.from(elements);
        }
        List<T> oldElements = this.elements;
        if (elements != EMPTY_CONTENT && isNot(DynamicContentProperty.INTERNAL) && isNot(DynamicContentProperty.VIRTUAL)) {
            elements = FilteredList.stateful((FilterDelegate<T>) () -> getFilter(), elements);
        }

        this.elements = elements;

        afterUpdate();
        if (!oldElements.isEmpty() || !elements.isEmpty()){
            notifyChangeListeners();
        }
        if (isMaster()) {
            BackgroundDisposer.queue(() -> Disposer.disposeCollection(oldElements));
        }
    }

    protected void beforeUpdate() {}
    protected void afterUpdate() {}

    protected abstract void sortElements(List<T> elements);

    @Override
    public List<T> getAllElements() {
        return FilteredList.unwrap(getElements());
    }

    @Override
    public List<T> getElements() {
        if (isLoaded() && !isDirty()) return elements;
        if (isDisposed()) return elements;

        if (allowSyncLoad()) {
            load();
        } else {
            loadInBackground();
        }

        return elements;
    }

    private boolean allowSyncLoad() {
        if (ThreadMonitor.isDispatchThread()) return false;
        if (ThreadMonitor.isWriteActionThread()) return false;

        if (canLoadFast()) return true;
        if (ThreadMonitor.isReadActionThread()) return false;
        if (ThreadMonitor.isBackgroundProcess()) return true;
        if (ThreadMonitor.isProgressProcess()) return true;
        if (ThreadMonitor.isModalProcess()) return true;
        return false;
    }

    @Override
    public T getElement(String name, short overload) {
        if (name != null) {
            return Lists.first(elements, element -> matchElement(element, name, overload));
        }
        return null;
    }

    private boolean matchElement(T element, String name, short overload) {
        return (overload == 0 || overload == element.getOverload()) &&
                Strings.equalsIgnoreCase(element.getName(), name);
    }

    @Override
    public List<T> getElements(String name) {
        return Lists.filter(getAllElements(), element -> Strings.equalsIgnoreCase(element.getName(), name));
    }

    @Override
    public int size() {
        return getElements().size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public void disposeInner() {
        if (elements != EMPTY_CONTENT && elements != EMPTY_UNTOUCHED_CONTENT) {
            if (isMaster()) {
                BackgroundDisposer.queue(() -> Disposer.disposeCollection(elements));
            }
            elements = Unsafe.cast(EMPTY_DISPOSED_CONTENT);
        }
        dependencyAdapter.dispose();
        dependencyAdapter = VoidContentDependencyAdapter.INSTANCE;
        parent = null;
        nullify();
    }

    @Override
    public DynamicContentProperty getDisposedProperty() {
        return DynamicContentProperty.DISPOSED;
    }
}
