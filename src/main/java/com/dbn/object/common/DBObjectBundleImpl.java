package com.dbn.object.common;

import com.dbn.browser.DatabaseBrowserManager;
import com.dbn.browser.DatabaseBrowserUtils;
import com.dbn.browser.model.BrowserTreeEventListener;
import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.browser.model.LoadInProgressTreeNode;
import com.dbn.browser.ui.HtmlToolTipBuilder;
import com.dbn.common.content.DynamicContent;
import com.dbn.common.content.DynamicContentType;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.filter.Filter;
import com.dbn.common.latent.Latent;
import com.dbn.common.notification.NotificationSupport;
import com.dbn.common.routine.Consumer;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Read;
import com.dbn.common.ui.tree.TreeEventType;
import com.dbn.common.util.Lists;
import com.dbn.connection.*;
import com.dbn.data.type.DBDataTypeBundle;
import com.dbn.data.type.DBNativeDataType;
import com.dbn.database.DatabaseObjectIdentifier;
import com.dbn.editor.code.SourceCodeEditor;
import com.dbn.editor.code.SourceCodeManagerListener;
import com.dbn.execution.compiler.CompileManagerListener;
import com.dbn.execution.statement.DataDefinitionChangeListener;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.object.*;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.common.list.DBObjectListContainer;
import com.dbn.object.common.list.DBObjectListImpl;
import com.dbn.object.event.ObjectChangeListener;
import com.dbn.object.impl.DBObjectLoaders;
import com.dbn.object.status.ObjectStatusManager;
import com.dbn.object.type.DBObjectRelationType;
import com.dbn.object.type.DBObjectType;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

import static com.dbn.browser.DatabaseBrowserUtils.treeVisibilityChanged;
import static com.dbn.common.content.DynamicContentProperty.GROUPED;
import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.dispose.Failsafe.nn;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.object.type.DBObjectRelationType.*;
import static com.dbn.object.type.DBObjectType.*;

public class DBObjectBundleImpl extends StatefulDisposableBase implements DBObjectBundle, NotificationSupport {
    static { DBObjectLoaders.initLoaders();}

    private final ConnectionRef connection;
    private final List<BrowserTreeNode> allPossibleTreeChildren;
    private volatile List<BrowserTreeNode> visibleTreeChildren;
    private boolean treeChildrenLoaded;

    private final DBObjectList<DBConsole> consoles;
    private final DBObjectList<DBSchema> schemas;
    private final DBObjectList<DBUser> users;
    private final DBObjectList<DBRole> roles;
    private final DBObjectList<DBSystemPrivilege> systemPrivileges;
    private final DBObjectList<DBObjectPrivilege> objectPrivileges = null; // TODO
    private final DBObjectList<DBCharset> charsets;

    private final DBDataTypeBundle dataTypes;

    private final DBObjectListContainer objectLists;
    private final DBObjectInitializer objectInitializer;
    private final long configSignature;
    private final Latent<PsiFile> fakeObjectFile = Latent.basic(() -> createFakePsiFile());

    private final Latent<List<DBSchema>> publicSchemas;

    public DBObjectBundleImpl(ConnectionHandler connection) {
        this.connection = ConnectionRef.of(connection);
        this.dataTypes = new DBDataTypeBundle(connection);
        this.configSignature = connection.getSettings().getDatabaseSettings().getSignature();

        this.objectLists = new DBObjectListContainer(this);
        this.objectInitializer = new DBObjectInitializer(connection);
        this.consoles = objectLists.createObjectList(CONSOLE, this);
        this.users = objectLists.createObjectList(USER, this);
        this.schemas = objectLists.createObjectList(SCHEMA, this);
        this.roles = objectLists.createObjectList(ROLE, this);
        this.systemPrivileges = objectLists.createObjectList(SYSTEM_PRIVILEGE, this);
        this.charsets = objectLists.createObjectList(CHARSET, this);
        this.allPossibleTreeChildren = DatabaseBrowserUtils.createList(consoles, schemas, users, roles, systemPrivileges, charsets);

        this.objectLists.createObjectRelationList(USER_ROLE, this, users, roles, GROUPED);
        this.objectLists.createObjectRelationList(USER_PRIVILEGE, this, users, systemPrivileges, GROUPED);
        this.objectLists.createObjectRelationList(ROLE_ROLE, this, roles, roles, GROUPED);
        this.objectLists.createObjectRelationList(ROLE_PRIVILEGE, this, roles, systemPrivileges, GROUPED);

        this.publicSchemas = Latent.mutable(
                () -> nd(schemas).getSignature(),
                () -> nvl(Lists.filter(getSchemas(), s -> s.isPublicSchema()), Collections.emptyList()));

        Project project = connection.getProject();
        ProjectEvents.subscribe(project, this, DataDefinitionChangeListener.TOPIC, dataDefinitionChangeListener());
        ProjectEvents.subscribe(project, this, SourceCodeManagerListener.TOPIC, sourceCodeManagerListener());
        ProjectEvents.subscribe(project, this, CompileManagerListener.TOPIC, compileManagerListener());
        ProjectEvents.subscribe(project, this, ObjectChangeListener.TOPIC, objectChangeListener());

        Disposer.register(connection, this);
    }

    private PsiFile createFakePsiFile() {
        PsiFileFactory psiFileFactory = PsiFileFactory.getInstance(getProject());
        return Read.call(psiFileFactory, f -> f.createFileFromText("object", SQLLanguage.INSTANCE, ""));
    }

    @NotNull
    private DataDefinitionChangeListener dataDefinitionChangeListener() {
        return new DataDefinitionChangeListener() {
            @Override
            public void dataDefinitionChanged(DBSchema schema, DBObjectType objectType) {
                if (schema.getConnection() == DBObjectBundleImpl.this.getConnection()) {
                    schema.refresh(objectType);
                    for (DBObjectType childObjectType : objectType.getChildren()) {
                        schema.refresh(childObjectType);
                    }
                }
            }

            @Override
            public void dataDefinitionChanged(@NotNull DBSchemaObject schemaObject) {
                if (schemaObject.getConnection() == DBObjectBundleImpl.this.getConnection()) {
                    schemaObject.refresh();
                }
            }
        };
    }

    @NotNull
    private SourceCodeManagerListener sourceCodeManagerListener() {
        return new SourceCodeManagerListener() {
            @Override
            public void sourceCodeSaved(@NotNull DBSourceCodeVirtualFile sourceCodeFile, @Nullable SourceCodeEditor fileEditor) {
                if (sourceCodeFile.getConnectionId() == getConnectionId()) {
                    Background.run(getProject(), () -> sourceCodeFile.getObject().refresh());
                }
            }
        };
    }

    @NotNull
    private CompileManagerListener compileManagerListener() {
        return (connection, object) -> {
            if (!Objects.equals(getConnection(), connection)) return;

            ObjectStatusManager statusManager = ObjectStatusManager.getInstance(getProject());
            statusManager.refreshObjectsStatus(getConnection(), object);
        };
    }

    private ObjectChangeListener objectChangeListener() {
        return (connectionId, ownerId, objectType) -> {
            if (ownerId == null) {
                DBObjectList<DBObject> objectList = getObjectLists().getObjectList(objectType);
                if (objectList != null) objectList.markDirty();
            } else {
                DBSchema schema = getSchema(ownerId.id());
                if (schema != null) {
                    DBObjectList<DBObject> objectList = schema.getChildObjectList(objectType);
                    if (objectList != null) objectList.markDirty();
                }
            }
        };
    }

    @Override
    public DynamicContentType<?> getDynamicContentType() {
        return CONNECTION;
    }

    @Override
    public PsiFile getFakeObjectFile() {
        return fakeObjectFile.get();
    }

    @Override
    public boolean isValid() {
        return configSignature == this.getConnection().getSettings().getDatabaseSettings().getSignature();
    }

    @NotNull
    @Override
    public ConnectionId getConnectionId() {
        return connection.getConnectionId();
    }

    @Override
    @NotNull
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @Override
    public List<DBConsole> getConsoles() {
        return this.getConnection().getConsoleBundle().getConsoles();
    }

    @Override
    public List<DBSchema> getSchemas() {
        return getSchemas(false);
    }

    @Override
    public List<DBSchema> getSchemas(boolean filtered) {
        return filtered ? nn(schemas).getElements() : nn(schemas).getAllElements();
    }

    @Override
    public List<DBSchema> getPublicSchemas() {
        return publicSchemas.get();
    }

    @Override
    public List<SchemaId> getSchemaIds() {
        return Lists.convert(getSchemas(), schema -> SchemaId.get(schema.getName()));
    }

    @Override
    @Nullable
    public List<DBUser> getUsers() {
        return DBObjectListImpl.getObjects(users);
    }

    @Override
    @Nullable
    public List<DBRole> getRoles() {
        return DBObjectListImpl.getObjects(roles);
    }

    @Override
    @Nullable
    public List<DBSystemPrivilege> getSystemPrivileges() {
        return DBObjectListImpl.getObjects(systemPrivileges);
    }

    @Override
    @Nullable
    public List<DBCharset> getCharsets() {
        return DBObjectListImpl.getObjects(charsets);
    }

    @Override
    @Nullable
    public DBNativeDataType getNativeDataType(String name) {
        return dataTypes.getNativeDataType(name);
    }

    @NotNull
    @Override
    public DBDataTypeBundle getDataTypes() {
        return dataTypes;
    }

    @Override
    @Nullable
    public DBSchema getSchema(String name) {
        return nn(schemas).getObject(name);
    }

    @Override
    @Nullable
    public DBSchema getPublicSchema() {
        return getSchema("PUBLIC");
    }

    @Override
    @Nullable
    public DBSchema getUserSchema() {
        for (DBSchema schema : getSchemas()) {
            if (schema.isUserSchema()) return schema;
        }
        return null;
    }

    @Override
    @Nullable
    public DBUser getUser(String name) {
        return DBObjectListImpl.getObject(users, name);
    }

    @Override
    @Nullable
    public DBRole getRole(String name) {
        return DBObjectListImpl.getObject(roles, name);
    }

    @Nullable
    @Override
    public DBPrivilege getPrivilege(String name) {
        return DBObjectListImpl.getObject(systemPrivileges, name);
    }

    @Override
    @Nullable
    public DBSystemPrivilege getSystemPrivilege(String name) {
        return DBObjectListImpl.getObject(systemPrivileges, name);
    }

    @Override
    @Nullable
    public DBCharset getCharset(String name) {
        return DBObjectListImpl.getObject(charsets, name);
    }


    /*********************************************************
     *                     TreeElement                       *
     *********************************************************/
    @Override
    public boolean isTreeStructureLoaded() {
        return treeChildrenLoaded;
    }

    @Override
    public boolean canExpand() {
        return treeChildrenLoaded && getChildAt(0).isTreeStructureLoaded();
    }

    @Override
    public int getTreeDepth() {
        return 2;
    }

    @Override
    public BrowserTreeNode getChildAt(int index) {
        return getChildren().get(index);
    }

    @Override
    @Nullable
    public BrowserTreeNode getParent() {
        return getConnection().getConnectionBundle();
    }

    @Override
    public List<? extends BrowserTreeNode> getChildren() {
        if (visibleTreeChildren == null) {
            synchronized (this) {
                if (visibleTreeChildren == null) {
                    visibleTreeChildren = new ArrayList<>();
                    visibleTreeChildren.add(new LoadInProgressTreeNode(this));

                    Background.run(getProject(), () -> buildTreeChildren());
                }
            }
        }
        return visibleTreeChildren;
    }

    private void buildTreeChildren() {
        checkDisposed();
        ConnectionHandler connection = this.getConnection();
        Filter<BrowserTreeNode> objectTypeFilter = connection.getObjectTypeFilter();

        List<BrowserTreeNode> treeChildren = Lists.filter(allPossibleTreeChildren, objectTypeFilter);
        treeChildren = nvl(treeChildren, Collections.emptyList());

        Project project = getProject();
        for (BrowserTreeNode objectList : treeChildren) {
            Background.run(project, () -> objectList.initTreeElement());
            checkDisposed();
        }

        if (visibleTreeChildren.size() == 1 && visibleTreeChildren.get(0) instanceof LoadInProgressTreeNode) {
            visibleTreeChildren.get(0).dispose();
        }

        visibleTreeChildren = treeChildren;
        treeChildrenLoaded = true;

        ProjectEvents.notify(project,
                BrowserTreeEventListener.TOPIC,
                (listener) -> listener.nodeChanged(this, TreeEventType.STRUCTURE_CHANGED));

        DatabaseBrowserManager.scrollToSelectedElement(connection);
    }

    @Override
    public void refreshTreeChildren(@NotNull DBObjectType... objectTypes) {
        if (visibleTreeChildren != null) {
            for (BrowserTreeNode treeNode : visibleTreeChildren) {
                treeNode.refreshTreeChildren(objectTypes);
            }
        }
    }

    @Override
    public void rebuildTreeChildren() {
        if (visibleTreeChildren != null) {
            Filter<BrowserTreeNode> filter = this.getConnection().getObjectTypeFilter();
            if (treeVisibilityChanged(allPossibleTreeChildren, visibleTreeChildren, filter)) {
                buildTreeChildren();
            }
            for (BrowserTreeNode treeNode : visibleTreeChildren) {
                treeNode.rebuildTreeChildren();
            }
        }
    }

    @Override
    public int getChildCount() {
        return getChildren().size();
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public int getIndex(BrowserTreeNode child) {
        return getChildren().indexOf(child);
    }

    @Override
    public Icon getIcon(int flags) {
        return this.getConnection().getIcon();
    }

    @Override
    public String getPresentableText() {
        return this.getConnection().getName();
    }

    @Override
    public String getPresentableTextDetails() {
        //return getCache().isAutoCommit() ? "[Auto Commit]" : null;
        return null;
    }

    @Override
    public String getPresentableTextConditionalDetails() {
        return null;
    }

    /*********************************************************
     *                  HtmlToolTipBuilder                   *
     *********************************************************/
    @Override
    public String getToolTip() {
        return new HtmlToolTipBuilder() {
            @Override
            public void buildToolTip() {
                append(true, "connection", true);
                ConnectionHandler connection = DBObjectBundleImpl.this.getConnection();
                if (connection.getConnectionStatus().isConnected()) {
                    append(false, " - active", true);
                } else if (connection.canConnect() && !connection.isValid()) {
                    append(false, " - invalid", true);
                    append(true, connection.getConnectionStatus().getStatusMessage(), null, "red", false);
                }
                createEmptyRow();

                append(true, connection.getProject().getName(), false);
                append(false, "/", false);
                append(false, connection.getName(), false);

                ConnectionPool connectionPool = connection.getConnectionPool();
                append(true, "Pool size: ", null, null, false);
                append(false, String.valueOf(connectionPool.getSize()), false);
                append(false, " (", false);
                append(false, "peak&nbsp;" + connectionPool.getPeakPoolSize(), false);
                append(false, ")", false);
            }
        }.getToolTip();
    }



    /*********************************************************
     *                   NavigationItem                      *
     *********************************************************/
    @Override
    public void navigate(boolean requestFocus) {
        DatabaseBrowserManager browserManager = DatabaseBrowserManager.getInstance(getProject());
        browserManager.navigateToElement(this, requestFocus, true);
    }
    @Override
    public boolean canNavigate() {return true;}
    @Override
    public boolean canNavigateToSource() {return false;}

    @NotNull
    @Override
    public String getName() {
        return nvl(getPresentableText(), "Object Bundle");
    }

    @Override
    public ItemPresentation getPresentation() {
        return this;
    }

    /*********************************************************
     *                   NavigationItem                      *
     *********************************************************/

    @Override
    public Icon getIcon(boolean open) {
        return getIcon(0);
    }

    /*********************************************************
     *                 Lookup utilities                      *
     *********************************************************/


    @Override
    @Nullable
    public DBObject getObject(DatabaseObjectIdentifier objectIdentifier) {
        DBObject object = null;
        for (int i=0; i<objectIdentifier.getObjectTypes().length; i++){
            DBObjectType objectType = objectIdentifier.getObjectTypes()[i];
            String objectName = objectIdentifier.getObjectNames()[i];
            if (object == null) {
                object = getObject(objectType, objectName);
            } else {
                object = object.getChildObject(objectType, objectName, true);
            }
            if (object == null) break;
        }
        return object;
    }

    @Override
    @Nullable
    public DBObject getObject(DBObjectType objectType, String name) {
        return getObject(objectType, name, (short) 0);
    }

    @Override
    @Nullable
    public DBObject getObject(DBObjectType objectType, String name, short overload) {
        if (objectType == CONSOLE) return this.getConnection().getConsoleBundle().getConsole(name);
        if (objectType == SCHEMA) return getSchema(name);
        if (objectType == USER) return getUser(name);
        if (objectType == ROLE) return getRole(name);
        if (objectType == CHARSET) return getCharset(name);
        if (objectType == SYSTEM_PRIVILEGE) return getSystemPrivilege(name);

        if (objectType.isSchemaObject()) {
            for (DBSchema schema : getPublicSchemas()) {
                DBObject childObject = schema.getChildObject(objectType, name, overload, true);
                if (childObject != null) {
                    return childObject;
                }
            }
        }
        return null;
    }

    private Filter<DBObjectType> getConnectionObjectTypeFilter() {
        return this.getConnection().getSettings().getFilterSettings().getObjectTypeFilterSettings().getTypeFilter();
    }

    @Override
    public void lookupObjectsOfType(Consumer<? super DBObject> consumer, DBObjectType objectType) {
        if (!getConnectionObjectTypeFilter().accepts(objectType)) return;

        if (objectType == SCHEMA) consumer.acceptAll(getSchemas()); else
        if (objectType == USER) consumer.acceptAll(getUsers()); else
        if (objectType == ROLE) consumer.acceptAll(getRoles()); else
        if (objectType == CHARSET) consumer.acceptAll(getCharsets());
        if (objectType == SYSTEM_PRIVILEGE) consumer.acceptAll(getSystemPrivileges());
    }

    @Override
    public void lookupChildObjectsOfType(Consumer<? super DBObject> consumer, DBObject parentObject, DBObjectType objectType, ObjectTypeFilter filter, DBSchema currentSchema) {
        if (currentSchema == null) return;
        if (parentObject == null) return;
        if (!getConnectionObjectTypeFilter().accepts(objectType)) return;

        if (parentObject instanceof DBSchema) {
            DBSchema schema = (DBSchema) parentObject;
            if (objectType.isGeneric()) {
                Set<DBObjectType> concreteTypes = objectType.getInheritingTypes();
                for (DBObjectType concreteType : concreteTypes) {
                    if (filter.acceptsObject(schema, currentSchema, concreteType)) {
                        schema.collectChildObjects(concreteType, consumer);
                    }
                }
            } else {
                if (filter.acceptsObject(schema, currentSchema, objectType)) {
                    schema.collectChildObjects(objectType, consumer);
                }
            }

            boolean synonymsSupported = SYNONYM.isSupported(parentObject);
            if (synonymsSupported && filter.acceptsObject(schema, currentSchema, SYNONYM)) {
                for (DBSynonym synonym : schema.getSynonyms()) {
                    DBObjectType underlyingObjectType = synonym.getUnderlyingObjectType();
                    if (underlyingObjectType != null && underlyingObjectType.matches(objectType)) {
                        consumer.accept(synonym);
                    }
                }
            }
        } else {
            if (objectType.isGeneric()) {
                Set<DBObjectType> concreteTypes = objectType.getInheritingTypes();
                for (DBObjectType concreteType : concreteTypes) {
                    if (filter.acceptsRootObject(concreteType)) {
                        parentObject.collectChildObjects(concreteType, consumer);
                    }
                }
            } else {
                if (filter.acceptsRootObject(objectType)) {
                    parentObject.collectChildObjects(objectType, consumer);
                }
            }
        }
    }

    @Override
    public DBObjectListContainer getObjectLists() {
        return nn(objectLists);
    }

    @Override
    public DBObjectInitializer getObjectInitializer() {
        return nn(objectInitializer);
    }

    @Override
    public <T extends DBObject> DBObjectList<T> getObjectList(DBObjectType objectType) {
        return getObjectLists().getObjectList(objectType);
    }

    @Override
    @NotNull
    public Project getProject() {
        return getConnection().getProject();
    }

    @Override
    @Nullable
    public DynamicContent<?> getDynamicContent(DynamicContentType<?> dynamicContentType) {
        if(dynamicContentType instanceof DBObjectType) {
            DBObjectType objectType = (DBObjectType) dynamicContentType;
            return objectLists.getObjectList(objectType);
        }

        if (dynamicContentType instanceof DBObjectRelationType) {
            DBObjectRelationType relationType = (DBObjectRelationType) dynamicContentType;
            return objectLists.getRelations(relationType);
        }

        return null;
    }

    @Override
    public void initTreeElement() {}

    @Override
    public String toString() {
        return this.getConnection().getName();
    }

    @Override
    public void disposeInner() {
        Disposer.dispose(objectLists);
        Disposer.dispose(dataTypes);
        nullify();
    }
}
