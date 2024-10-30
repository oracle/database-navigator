package com.dbn.object.common;

import com.dbn.code.common.lookup.LookupItemBuilder;
import com.dbn.code.common.lookup.ObjectLookupItemBuilder;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.ref.WeakRefCache;
import com.dbn.common.routine.Consumer;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Lists;
import com.dbn.common.util.Strings;
import com.dbn.common.util.TimeUtil;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionManager;
import com.dbn.database.common.metadata.DBObjectMetadata;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.element.util.IdentifierCategory;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.IdentifierPsiElement;
import com.dbn.language.common.psi.LeafPsiElement;
import com.dbn.language.common.psi.QualifiedIdentifierPsiElement;
import com.dbn.language.common.psi.lookup.LookupAdapters;
import com.dbn.language.common.psi.lookup.ObjectLookupAdapter;
import com.dbn.language.common.psi.lookup.ObjectReferenceLookupAdapter;
import com.dbn.language.common.psi.lookup.PsiLookupAdapter;
import com.dbn.language.common.psi.lookup.TokenTypeLookupAdapter;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.common.list.DBObjectListContainer;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.dbn.vfs.file.DBContentVirtualFile;
import com.intellij.ide.util.EditSourceUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiReference;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.content.DynamicContentProperty.LOADED;
import static com.dbn.common.content.DynamicContentProperty.MASTER;
import static com.dbn.common.content.DynamicContentProperty.MUTABLE;
import static com.dbn.common.content.DynamicContentProperty.VIRTUAL;
import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.dispose.Failsafe.nn;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Documents.getDocument;
import static com.dbn.common.util.Documents.getEditors;
import static com.dbn.common.util.Lists.convert;
import static com.dbn.common.util.Strings.toUpperCase;
import static com.dbn.language.common.psi.lookup.LookupAdapters.aliasDefinition;
import static com.dbn.language.common.psi.lookup.LookupAdapters.aliasReference;
import static com.dbn.language.common.psi.lookup.LookupAdapters.identifierReference;
import static com.dbn.object.common.sorting.DBObjectComparator.compareName;
import static com.dbn.object.common.sorting.DBObjectComparator.compareType;
import static com.dbn.object.type.DBObjectType.COLUMN;
import static com.dbn.object.type.DBObjectType.DATASET;
import static java.util.concurrent.TimeUnit.SECONDS;

public class DBVirtualObject extends DBRootObjectImpl implements PsiReference {
    private static final PsiLookupAdapter CHR_STAR_LOOKUP_ADAPTER = new TokenTypeLookupAdapter(element -> element.getLanguage().getSharedTokenTypes().getChrStar());
    private static final PsiLookupAdapter COL_INDEX_LOOKUP_ADAPTER = new TokenTypeLookupAdapter(element -> element.getLanguage().getSharedTokenTypes().getInteger());
    private static final ObjectReferenceLookupAdapter DATASET_LOOKUP_ADAPTER = new ObjectReferenceLookupAdapter(null, DATASET, null);

    private static final WeakRefCache<DBVirtualObject, BasePsiElement> underlyingPsiElements = WeakRefCache.weakKey();
    private static final WeakRefCache<DBVirtualObject, BasePsiElement> relevantPsiElements = WeakRefCache.weakKey();

    private volatile boolean loadingChildren;
    private final Map<String, ObjectLookupItemBuilder> lookupItemBuilder = new ConcurrentHashMap<>();
    private boolean valid = true;
    private long validCheckTimestamap = 0;

    public DBVirtualObject(@NotNull BasePsiElement psiElement) {
        super(
            psiElement.getConnection(),
            psiElement.elementType.virtualObjectType,
            psiElement.getText());

        underlyingPsiElements.set(this, psiElement);
        String name = resolveName();
        ref = new DBObjectRef<>(this, name);
    }

    private String resolveName() {
        BasePsiElement psiElement = getRelevantPsiElement();
        DBObjectType objectType = getObjectType();
        switch (objectType) {
            case DATASET: return resolveDatasetName(psiElement);
            case COLUMN: return resolveColumnName(psiElement);
            case CURSOR:
            case TYPE:
            case TYPE_ATTRIBUTE: return resolveObjectName(psiElement);
        }
        return "";
    }

    private String resolveObjectName(@NotNull BasePsiElement psiElement) {
        BasePsiElement relevantPsiElement = psiElement.findFirstPsiElement(ElementTypeAttribute.SUBJECT);
        if (relevantPsiElement != null) {
            relevantPsiElements.set(this, relevantPsiElement);
            return relevantPsiElement.getText();
        }
        return "";
    }

    private String resolveDatasetName(@NotNull BasePsiElement psiElement) {
        if (psiElement instanceof LeafPsiElement) {
            LeafPsiElement leafPsiElement = (LeafPsiElement) psiElement;
            ObjectLookupAdapter lookupAdapter = new ObjectLookupAdapter(leafPsiElement, IdentifierCategory.REFERENCE, DATASET);
            BasePsiElement dataset = lookupAdapter.findInParentScopeOf(psiElement);
            if (dataset != null) {
                relevantPsiElements.set(this, dataset);
                return dataset.getText();
            } else {
                return  "UNKNOWN";
            }
        }

        List<String> tableNames = new ArrayList<>();
        ObjectLookupAdapter lookupAdapter = new ObjectLookupAdapter(null, IdentifierCategory.REFERENCE, DATASET);
        lookupAdapter.collectInElement(psiElement, basePsiElement -> {
            if (basePsiElement instanceof IdentifierPsiElement) {
                IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) basePsiElement;
                String tableName = toUpperCase(identifierPsiElement.getText());
                if (!tableNames.contains(tableName)) {
                    tableNames.add(tableName);
                }
            }
        });

        Collections.sort(tableNames);

        StringBuilder nameBuilder = new StringBuilder();
        for (CharSequence tableName : tableNames) {
            if (nameBuilder.length() > 0) nameBuilder.append(", ");
            nameBuilder.append(tableName);
        }

        return "subquery " + nameBuilder;
    }

    private String resolveColumnName(BasePsiElement psiElement) {
        BasePsiElement specificPsiElement = Commons.coalesce(psiElement,
                e -> aliasDefinition(COLUMN).findInElement(e),
                e -> aliasReference(COLUMN).findInElement(e),
                e -> identifierReference(COLUMN).findInElement(e));

        if (specificPsiElement != null) {
            relevantPsiElements.set(this, specificPsiElement);
            return specificPsiElement.getText();
        }

        specificPsiElement = relevantPsiElements.get(this);
        if (specificPsiElement != null) {
            String text = specificPsiElement.getText();
            if (!text.contains("\\s*")) {
                return text.trim();
            }
        }
        return "";
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBObjectMetadata metadata) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @NotNull
    public LookupItemBuilder getLookupItemBuilder(DBLanguage language) {
        return lookupItemBuilder.computeIfAbsent(language.getID(), id -> new ObjectLookupItemBuilder(ref(), language));
    }

    @Override
    public boolean isValid() {
        if (isDisposed()) return false;
        if (!valid) return false;

        if (TimeUtil.isOlderThan(validCheckTimestamap, 10, SECONDS)) {
            valid = checkValid();
            if (!valid) Disposer.dispose(this);
        }

        return valid;
    }

    private boolean checkValid() {
        validCheckTimestamap = System.currentTimeMillis();
        if (isDisposed()) return false;

        BasePsiElement underlyingPsiElement = getUnderlyingPsiElement();
        if (underlyingPsiElement == null) return false;

        boolean psiElementValid = underlyingPsiElement.isValid();
        if (!psiElementValid) return false;

        DBObjectType objectType = getObjectType();
        if (objectType.matches(DATASET) || objectType.matches(DBObjectType.TYPE)) return true; // no special checks

        BasePsiElement relevantPsiElement = getRelevantPsiElement();
        if (!Strings.equalsIgnoreCase(getName(), relevantPsiElement.getText())) return false;

        if (relevantPsiElement instanceof IdentifierPsiElement) {
            IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) relevantPsiElement;
            return identifierPsiElement.getObjectType() == objectType;
        }
        return true;
    }

    @Override
    public boolean isVirtual() {
        return true;
    }

    @Override
    @NotNull
    public List<DBObject> collectChildObjects(DBObjectType objectType) {
        return getChildObjects(objectType);
    }

    @Override
    public DBObject getChildObject(DBObjectType type, String name, short overload, boolean lookupHidden) {
        DBObjectList<DBObject> childObjectList = getChildObjectList(type);
        if (childObjectList != null) {
            DBObject object = childObjectList.getObject(name, overload);
            if (object != null) return object;
        }

        BasePsiElement underlyingPsiElement = getUnderlyingPsiElement();
        DBObject underlyingObject = underlyingPsiElement.getUnderlyingObject();
        if (underlyingObject != null && underlyingObject != this) {
            DBObject object = underlyingObject.getChildObject(type, name, overload, lookupHidden);
            if (object != null) return object;
        }

        return getChildObject(name, overload);
    }

    @Override
    public void collectChildObjects(DBObjectType objectType, Consumer consumer) {
        super.collectChildObjects(objectType, consumer);
        BasePsiElement underlyingPsiElement = getUnderlyingPsiElement();
        DBObject underlyingObject = underlyingPsiElement.getUnderlyingObject();
        if (underlyingObject != null && underlyingObject != this) {
            underlyingObject.collectChildObjects(objectType, consumer);
        }

    }

    @Override
    @Nullable
    public DBObjectList<DBObject> getChildObjectList(DBObjectType objectType) {
        if (loadingChildren) return null;

        synchronized (this) {
            if (loadingChildren) return null;

            try {
                loadingChildren = true;
                return loadChildObjectList(objectType);
            } finally {
                loadingChildren = false;
            }
        }
    }

    private DBObjectList<DBObject> loadChildObjectList(DBObjectType objectType) {
        DBObjectListContainer childObjects = ensureChildObjects();
        DBObjectList<DBObject> objectList = childObjects.getObjectList(objectType);

        if (objectList == null) {
            if (!objectType.isChildOf(getObjectType())) return null;

            objectList = childObjects.createObjectList(objectType, this, MUTABLE, VIRTUAL, MASTER);
            if (objectList == null) return null; // not supported (?)

            loadChildObjects(objectType, objectList);
            objectList.set(LOADED, true);
        } else {
            // unloaded lists may be the result of ProcessCancelledExceptions during annotation processing (force reload)
            boolean invalid = !objectList.isLoaded() || Lists.anyMatch(objectList.getObjects(), o -> !o.isValid());
            if (!invalid) return objectList;

            objectList.setElements(Collections.emptyList()); // reset
            loadChildObjects(objectType, objectList);
            objectList.set(LOADED, true);
        }
        return objectList;
    }

    private void loadChildObjects(DBObjectType childObjectType, DBObjectList<DBObject> objectList) {
        BasePsiElement<?> underlyingPsiElement = getUnderlyingPsiElement();
        if (underlyingPsiElement == null) return;

        DBObjectType objectType = getObjectType();
        Set<DBObject> objects = new LinkedHashSet<>();
        PsiLookupAdapter lookupAdapter = LookupAdapters.virtualObject(objectType, childObjectType);
        underlyingPsiElement.collectPsiElements(lookupAdapter, 100, element -> {
            // handle STAR column
            if (childObjectType == COLUMN) {
                LeafPsiElement starPsiElement = (LeafPsiElement) CHR_STAR_LOOKUP_ADAPTER.findInElement(element);
                if (starPsiElement != null) loadAllColumns(starPsiElement, objects);

                LeafPsiElement indexPsiElement = (LeafPsiElement) COL_INDEX_LOOKUP_ADAPTER.findInElement(element);
                if (indexPsiElement != null) loadColumns(indexPsiElement, objects);
            }

            DBObject object = element.getUnderlyingObject();
            if (object != null && object != this && Strings.isNotEmpty(object.getName()) && object.getObjectType().isChildOf(objectType) && !objectList.contains(object)) {
                if (object instanceof DBVirtualObject) {
                    DBVirtualObject virtualObject = (DBVirtualObject) object;
                    virtualObject.setParentObject(this);
                }
                objects.add(object);
            }

        });

        objectList.setElements(convert(objects, o -> delegate(o)));
        objectList.set(MASTER, false);
    }

    private void loadAllColumns(LeafPsiElement starPsiElement, Collection<DBObject> objects) {
        PsiElement parent = starPsiElement.getParent();
        if (parent instanceof QualifiedIdentifierPsiElement) {
            QualifiedIdentifierPsiElement qualifiedIdentifierPsiElement = (QualifiedIdentifierPsiElement) parent;
            int index = qualifiedIdentifierPsiElement.getIndexOf(starPsiElement);
            if (index <= 0) return;

            IdentifierPsiElement parentPsiElement = qualifiedIdentifierPsiElement.getLeafAtIndex(index - 1);
            DBObject object = parentPsiElement.getUnderlyingObject();
            if (object == null) return;
            if (!object.getObjectType().matches(DATASET)) return;

            objects.addAll(object.collectChildObjects(COLUMN));
        } else {
            BasePsiElement underlyingPsiElement = nn(getUnderlyingPsiElement());
            DATASET_LOOKUP_ADAPTER.collectInElement(underlyingPsiElement, basePsiElement -> {
                DBObject object = basePsiElement.getUnderlyingObject();
                if (object == null || object == this) return;
                if (!object.getObjectType().matches(DATASET)) return;

                objects.addAll(object.collectChildObjects(COLUMN));
            });
        }
    }

    private static DBObject delegate(DBObject object) {
        return object instanceof DBVirtualObject || object instanceof DBObjectDelegate ? object : new DBObjectDelegate(object);
    }


    private void loadColumns(LeafPsiElement indexPsiElement, Collection<DBObject> objects) {
        BasePsiElement<?> columnPsiElement = indexPsiElement.findEnclosingVirtualObjectElement(COLUMN);
        if (columnPsiElement == null) return;

        String text = columnPsiElement.getText();
        if (!Strings.isIndex(text)) return;

        int columnIndex = Integer.parseInt(text) -1 ; // switch from DB indexing to 0 based
        if (columnIndex < 0) return;

        if (indexPsiElement.getParent() instanceof QualifiedIdentifierPsiElement) {
            QualifiedIdentifierPsiElement qualifiedIdentifierPsiElement = (QualifiedIdentifierPsiElement) indexPsiElement.getParent();
            int index = qualifiedIdentifierPsiElement.getIndexOf(indexPsiElement);
            if (index <= 0) return;

            IdentifierPsiElement parentPsiElement = qualifiedIdentifierPsiElement.getLeafAtIndex(index - 1);
            DBObject object = parentPsiElement.getUnderlyingObject();
            if (object == null || object == this) return;
            if (!object.getObjectType().matches(DATASET)) return;

            List<DBObject> columns = object.collectChildObjects(COLUMN);
            if (columns.size() > columnIndex) objects.add(columns.get(columnIndex));
        } else {
            BasePsiElement underlyingPsiElement = nn(getUnderlyingPsiElement());
            DATASET_LOOKUP_ADAPTER.collectInElement(underlyingPsiElement, basePsiElement -> {
                DBObject object = basePsiElement.getUnderlyingObject();
                if (object == null || object == this) return;
                if (!object.getObjectType().matches(DATASET)) return;

                List<DBObject> columns = object.collectChildObjects(COLUMN);
                if (columns.size() > columnIndex) objects.add(columns.get(columnIndex));
            });
        }
    }

    @Override
    public String getQualifiedNameWithType() {
        return getName();
    }

    @Override
    @NotNull
    public ConnectionHandler getConnection() {
        BasePsiElement underlyingPsiElement = nd(getUnderlyingPsiElement());
        DBLanguagePsiFile file = underlyingPsiElement.getFile();
        ConnectionHandler connection = file.getConnection();
        if (connection != null) return connection;

        ConnectionManager connectionManager = ConnectionManager.getInstance(getProject());
        return connectionManager.getConnectionBundle().getVirtualConnection(ConnectionId.VIRTUAL_ORACLE);
    }

    public void setParentObject(DBVirtualObject virtualObject) {
        ref.setParent(DBObjectRef.of(virtualObject));
    }

    @Override
    @NotNull
    public Project getProject() {
        PsiElement underlyingPsiElement = nn(getUnderlyingPsiElement());
        return underlyingPsiElement.getProject();
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return ref.getObjectType();
    }

    @Override
    public void navigate(boolean requestFocus) {
        PsiFile containingFile = getContainingFile();
        if (containingFile == null) return;

        VirtualFile virtualFile = containingFile.getVirtualFile();
        BasePsiElement relevantPsiElement = getRelevantPsiElement();
        if (virtualFile instanceof DBContentVirtualFile) {
            Document document = getDocument(containingFile);
            if (document == null) return;

            Editor[] editors = getEditors(document);
            OpenFileDescriptor descriptor = (OpenFileDescriptor) EditSourceUtil.getDescriptor(relevantPsiElement);
            if (descriptor == null) return;

            descriptor.navigateIn(editors[0]);
        } else {
            relevantPsiElement.navigate(requestFocus);
        }
    }
    
    @Override
    public PsiFile getContainingFile() throws PsiInvalidElementAccessException {
        BasePsiElement relevantPsiElement = getRelevantPsiElement();
        return relevantPsiElement.isValid() ? relevantPsiElement.getContainingFile() : null;
    }

    /*********************************************************
     *                       PsiReference                    *
     *********************************************************/
    @NotNull
    @Override
    public PsiElement getElement() {
        return getRelevantPsiElement();
    }

    @NotNull
    @Override
    public TextRange getRangeInElement() {
        return new TextRange(0, getName().length());
    }

    @Override
    @Nullable
    public PsiElement resolve() {
        return getUnderlyingPsiElement();
    }

    public BasePsiElement getUnderlyingPsiElement() {
        return underlyingPsiElements.ensure(this);
    }

    private BasePsiElement getRelevantPsiElement() {
        BasePsiElement relevant = relevantPsiElements.get(this);
        BasePsiElement underlying = underlyingPsiElements.ensure(this);
        return nvl(relevant, underlying);
    }

    @Override
    @NotNull
    public String getCanonicalText() {
        return getName();
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        return null;
    }

    @Override
    public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
        return null;
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        return getUnderlyingPsiElement() == element;
    }

    @Override
    @NotNull
    public Object[] getVariants() {
        return new Object[0];
    }

    @Override
    public boolean isSoft() {
        return false;
    }

    @Override
    public boolean canNavigateToSource() {
        return false;
    }

    @Override
    public int compareTo(@NotNull Object o) {
        DBObject that = (DBObject) o;
        int result = compareType(this, that);
        if (result == 0) {
            return compareName(this, that);
        }
        return result;
    }
}
