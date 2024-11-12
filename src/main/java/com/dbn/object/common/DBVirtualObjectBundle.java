package com.dbn.object.common;

import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.routine.Consumer;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.connection.VirtualConnectionHandler;
import com.dbn.data.type.DBDataTypeBundle;
import com.dbn.data.type.DBNativeDataType;
import com.dbn.database.DatabaseObjectIdentifier;
import com.dbn.object.*;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.common.list.DBObjectListContainer;
import com.dbn.object.type.DBObjectType;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class DBVirtualObjectBundle extends StatefulDisposableBase implements DBObjectBundle{
    private final VirtualConnectionHandler connection;
    private final DBDataTypeBundle dataTypes;
    private boolean disposed;

    public DBVirtualObjectBundle(@NotNull VirtualConnectionHandler connection) {
        this.connection = connection;
        this.dataTypes = new DBDataTypeBundle(connection);
    }

    @Override
    public List<DBConsole> getConsoles() {
        return Collections.emptyList();
    }

    @Override
    public List<DBSchema> getSchemas() {
        return Collections.emptyList();
    }

    @Override
    public List<DBSchema> getSchemas(boolean filtered) {
        return Collections.emptyList();
    }

    @Override
    public List<DBSchema> getPublicSchemas() {
        return Collections.emptyList();
    }

    @Override
    public List<SchemaId> getSchemaIds() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public List<DBUser> getUsers() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public List<DBRole> getRoles() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public List<DBSystemPrivilege> getSystemPrivileges() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public List<DBCharset> getCharsets() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public DBNativeDataType getNativeDataType(String name) {
        return null;
    }

    @Nullable
    @Override
    public DBSchema getSchema(String name) {
        return null;
    }

    @Nullable
    @Override
    public DBSchema getPublicSchema() {
        return null;
    }

    @Nullable
    @Override
    public DBSchema getUserSchema() {
        return null;
    }

    @Nullable
    @Override
    public DBUser getUser(String name) {
        return null;
    }

    @Nullable
    @Override
    public DBRole getRole(String name) {
        return null;
    }

    @Nullable
    @Override
    public DBPrivilege getPrivilege(String name) {
        return null;
    }

    @Nullable
    @Override
    public DBSystemPrivilege getSystemPrivilege(String name) {
        return null;
    }

    @Nullable
    @Override
    public DBCharset getCharset(String name) {
        return null;
    }

    @Nullable
    @Override
    public DBObject getObject(DatabaseObjectIdentifier objectIdentifier) {
        return null;
    }

    @Nullable
    @Override
    public DBObject getObject(DBObjectType objectType, String name) {
        return null;
    }

    @Nullable
    @Override
    public DBObject getObject(DBObjectType objectType, String name, short overload) {
        return null;
    }

    @Override
    public void lookupObjectsOfType(Consumer<? super DBObject> consumer, DBObjectType objectType) {

    }

    @Override
    public void lookupChildObjectsOfType(Consumer<? super DBObject> consumer, DBObject parentObject, DBObjectType objectType, ObjectTypeFilter filter, DBSchema currentSchema) {

    }

    @Override
    public DBObjectListContainer getObjectLists() {
        return null;
    }

    @Override
    public DBObjectInitializer getObjectInitializer() {
        return null;
    }

    @Override
    public <T extends DBObject> DBObjectList<T> getObjectList(DBObjectType objectType) {
        return null;
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @NotNull
    @Override
    public ConnectionId getConnectionId() {
        return connection.getConnectionId();
    }

    @Override
    public void initTreeElement() {

    }

    @Override
    public boolean canExpand() {
        return false;
    }

    @Override
    public int getTreeDepth() {
        return 0;
    }

    @Override
    public boolean isTreeStructureLoaded() {
        return false;
    }

    @Override
    public BrowserTreeNode getChildAt(int index) {
        return null;
    }

    @Nullable
    @Override
    public BrowserTreeNode getParent() {
        return null;
    }

    @Override
    public List<? extends BrowserTreeNode> getChildren() {
        return null;
    }

    @Override
    public void refreshTreeChildren(@NotNull DBObjectType... objectTypes) {

    }

    @Override
    public void rebuildTreeChildren() {

    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public int getIndex(BrowserTreeNode child) {
        return 0;
    }

    @Override
    public Icon getIcon(int flags) {
        return null;
    }

    @Override
    public String getPresentableText() {
        return null;
    }

    @Nullable
    @Override
    public Icon getIcon(boolean unused) {
        return null;
    }

    @Override
    public String getPresentableTextDetails() {
        return null;
    }

    @Override
    public String getPresentableTextConditionalDetails() {
        return null;
    }

    @NotNull
    @Override
    public Project getProject() {
        return connection.getProject();
    }

    @NotNull
    @Override
    public String getName() {
        return "";
    }

    @Nullable
    @Override
    public ItemPresentation getPresentation() {
        return null;
    }

    @Override
    public void navigate(boolean requestFocus) {

    }

    @Override
    public boolean canNavigate() {
        return false;
    }

    @Override
    public boolean canNavigateToSource() {
        return false;
    }

    @Override
    public String getToolTip() {
        return null;
    }

    @Override
    public PsiFile getFakeObjectFile() {
        return null;
    }

    @Override
    public void disposeInner() {

    }
}
