package com.dbn.object.lookup;

import com.dbn.common.Reference;
import com.dbn.common.compatibility.Compatibility;
import com.dbn.common.dispose.Checks;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.string.StringDeBuilder;
import com.dbn.common.util.Lists;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.connection.context.DatabaseContextBase;
import com.dbn.nls.NlsResources;
import com.dbn.object.DBSchema;
import com.dbn.object.DBSynonym;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.object.type.DBObjectType;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.dispose.Failsafe.nn;
import static com.dbn.common.options.setting.Settings.connectionIdAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.object.type.DBObjectType.ANY;
import static com.dbn.object.type.DBObjectType.SYNONYM;
import static com.dbn.vfs.DatabaseFileSystem.PS;
import static com.dbn.vfs.DatabaseFileSystem.PSS;

@Slf4j
@Getter
@Setter
public class DBObjectRef<T extends DBObject> implements Comparable<DBObjectRef<?>>, Reference<T>, PersistentStateElement, DatabaseContextBase, ItemPresentation {
    private static final Pattern PATH_TOKENIZER = Pattern.compile("[^/\"]+|\"([^\"]*)\"");
    private static final String QUOTE = "\"";

    private Object parent; // can hold connection id or an actual DBObjectRef (memory optimisation)
    private String objectName;
    private DBObjectType objectType;
    private short overload;

    private WeakRef<T> reference;
    private int hashCode = -1;

    public DBObjectRef(ConnectionId connectionId, String identifier) {
        deserialize(connectionId, identifier);
    }

    public DBObjectRef(T object, String name) {
        this(object, object.getObjectType(), name);
    }
    public DBObjectRef(T object, DBObjectType objectType, String objectName) {
        this.reference = WeakRef.of(object);
        this.objectName = objectName.intern();
        this.objectType = objectType;
        this.overload = object.getOverload();

/*
        DBObject parentObj = object.getParentObject();
        if (parentObj != null) {
            this.parent = parentObj.ref();
        } else if (!(object instanceof DBVirtualObject)){
            this.parent = object.getConnectionId();
        }
*/
    }

    public DBObjectRef(DBObjectRef<?> parent, DBObjectType objectType, String objectName) {
        this.parent = parent;
        this.objectType = objectType;
        this.objectName = objectName.intern();
    }

    public DBObjectRef(ConnectionId connectionId, DBObjectType objectType, String objectName) {
        this.parent = connectionId;
        this.objectType = objectType;
        this.objectName = objectName.intern();
    }

    public void setParent(Object parent) {
        if (parent == null) return;

        if (parent instanceof DBObject) {
            DBObject object = (DBObject) parent;
            this.parent = object.ref();

        } else if (parent instanceof DBObjectRef) {
            this.parent = parent;

        } else if (parent instanceof ConnectionId) {
            this.parent =  parent;

        } else if (parent instanceof DatabaseContext) {
            DatabaseContext databaseContext = (DatabaseContext) parent;
            this.parent = databaseContext.getConnectionId();
        } else {
            throw new IllegalArgumentException(parent + " is not supported as parent of database object");
        }
    }

    public DBObjectRef() {

    }

    @Nullable
    @Override
    public Project getProject() {
        T object = reference == null ? null : reference.get();
        if (object != null) return object.getProject();

        ConnectionHandler connection = getConnection();
        if (connection != null) return connection.getProject();

        return null;
    }

    @Nullable
    public <P extends DBObject> P getParentObject(DBObjectType objectType) {
        DBObjectRef<P> parentRef = getParentRef(objectType);
        return DBObjectRef.get(parentRef);
    }

    @Nullable
    public <P extends DBObject> DBObjectRef<P> getParentRef(DBObjectType objectType) {
        DBObjectRef<?> element = this;
        while (element != null) {
            if (element.objectType != ANY && element.objectType.matches(objectType)) {
                return cast(element);
            }

            element = element.getParentRef();

        }
        return null;
    }

    public DBObjectRef<?> getParentRef() {
        return parent instanceof DBObjectRef ? (DBObjectRef) parent : null;
    }

    public static <T extends DBObject> DBObjectRef<T> from(Element element) {
        String objectIdentifier = stringAttribute(element, "object-ref");
        if (Strings.isNotEmpty(objectIdentifier)) {
            try {
                DBObjectRef<T> objectRef = new DBObjectRef<>();
                objectRef.readState(element);
                return objectRef;
            } catch (Exception e) {
                conditionallyLog(e);
                // deserialization exception already logged
            }
        }
        return null;
    }

    @Override
    public void readState(Element element) {
        if (element != null) {
            ConnectionId connectionId = connectionIdAttribute(element, "connection-id");
            String objectIdentifier = stringAttribute(element, "object-ref");
            deserialize(connectionId, objectIdentifier);
        }
    }


    @Override
    public void writeState(Element element) {
        String value = serialize();

        ConnectionId connectionId = getConnectionId();
        element.setAttribute("connection-id", connectionId == null ? "null" : connectionId.id());
        element.setAttribute("object-ref", value);
    }

    private void deserialize(ConnectionId connectionId, String objectIdentifier) {
        try {
            List<String> tokens = tokenizePath(objectIdentifier);

            DBObjectRef<?> objectRef = null;
            DBObjectType objectType = null;
            int tokenCount = tokens.size();
            for (int i = 0; i< tokenCount; i++) {
                String token = tokens.get(i);
                if (objectType == null) {
                    if (i == tokenCount -1) {
                        // last optional "overload" numeric token
                        this.overload = Short.parseShort(token);
                    } else {
                        objectType = DBObjectType.forListName(token, objectRef == null ? null : objectRef.objectType);
                    }
                } else {
                    if (i < tokenCount - 2) {
                        objectRef = objectRef == null ?
                                new DBObjectRef<>(connectionId, objectType, token) :
                                new DBObjectRef<>(objectRef, objectType, token);
                    } else {
                        this.parent = objectRef == null ? connectionId :  objectRef;
                        this.objectType = objectType;
                        this.objectName = token.intern();
                    }
                    objectType = null;
                }
            }
        } catch (Exception e) {
            conditionallyLog(e);
            log.error("Failed to deserialize object {}", objectIdentifier, e);
            throw e;
        }
    }

    private static List<String> tokenizePath(String objectIdentifier) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = PATH_TOKENIZER.matcher(objectIdentifier);
        while (matcher.find()) {
            String token = matcher.group(0);
            if (token.startsWith(QUOTE)) {
                token = token.substring(1, token.length() - 1);
            }
            tokens.add(token);
        }
        return tokens;
    }

    private static String quotePathElement(String pathElement) {
        if (pathElement.contains(PSS)) {
            return QUOTE + pathElement + QUOTE;
        }
        return pathElement;
    }


    @NotNull
    public String serialize() {
        StringDeBuilder builder = new StringDeBuilder();
        builder.append(objectType.getListName());
        builder.append(PS);
        builder.append(quotePathElement(objectName));

        DBObjectRef<?> parent = getParentRef();
        while (parent != null) {
            builder.prepend(PS);
            builder.prepend(quotePathElement(parent.objectName));
            builder.prepend(PS);
            builder.prepend(parent.objectType.getListName());
            parent = parent.getParentRef();
        }

        if (overload > 0) {
            builder.append(PS);
            builder.append(overload);
        }

        return builder.toString();
    }

    public String getPath() {
        DBObjectRef<?> parent = getParentRef();
        if (parent == null) {
            return objectName;
        } else {
            StringDeBuilder builder = new StringDeBuilder();
            builder.append(objectName);
            while(parent != null) {
                builder.prepend('.');
                builder.prepend(parent.objectName);
                parent = parent.getParentRef();
            }
            return builder.toString();
        }
    }    

    public String getQualifiedName() {
        return getPath();
    }

    public String getQualifiedObjectName() {
        DBObjectRef<?> parent = getParentRef();
        if (parent == null || parent.objectType == DBObjectType.SCHEMA) {
            return objectName;
        } else {
            StringDeBuilder builder = new StringDeBuilder();
            builder.append(objectName);
            while(parent != null && parent.objectType != DBObjectType.SCHEMA) {
                builder.prepend('.');
                builder.prepend(parent.objectName);
                parent = parent.getParentRef();
            }
            return builder.toString();
        }
    }
    

    public String getQualifiedNameWithType() {
        return NlsResources.txt("app.object.label.QualifiedNameWithType", objectType.getName(), getPath());
    }

    @Nullable
    public ConnectionId getConnectionId() {
        if (parent instanceof ConnectionId) {
            return (ConnectionId) parent;
        }

        if (parent instanceof DBObjectRef)  {
            DBObjectRef parentRef = (DBObjectRef) parent;
            return parentRef.getConnectionId();
        }

        T object = getObject();
        return object == null ? null : object.getConnectionId();
    }

    public boolean is(@NotNull DBObject object) {
        return Objects.equals(object.ref(), this);
    }

    @Contract("null -> null;!null -> !null;")
    public static <T extends DBObject> DBObjectRef<T> of(@Nullable T object) {
        return object == null ? null : cast(object.ref());
    }

    public static <T extends DBObject> String serialised(@Nullable T object) {
        DBObjectRef ref = DBObjectRef.of(object);
        return ref == null ? null : ref.serialize();
    }

    @Nullable
    public static <T extends DBObject> T get(@Nullable DBObjectRef<T> ref) {
        return ref == null ? null : ref.get();
    }

    @NotNull
    public static <T extends DBObject> T ensure(@Nullable DBObjectRef<T> ref) {
        return nn(get(ref));
    }

    @NotNull
    public static <T extends DBObject> List<T> get(@NotNull List<DBObjectRef<T>> refs) {
        return Lists.convert(refs, ref -> get(ref));
    }

    @NotNull
    public static <T extends DBObject> List<T> ensure(@NotNull List<DBObjectRef<T>> refs) {
        return Lists.convert(refs, ref -> ensure(ref));
    }

    @NotNull
    public static <T extends DBObject> List<DBObjectRef<T>> from(@NotNull List<T> objects) {
        return Lists.convert(objects, obj -> of(obj));
    }

    @Override
    @Nullable
    public T get() {
        return load();
    }

    @Nullable
    public T value() {
        return WeakRef.get(reference);
    }

    public T ensure(){
        return nn(get());
    }

    private T load() {
        T object = getObject();
        if (object != null) return object;

        clearReference();
        ConnectionHandler connection = getConnection();
        if (isNotValid(connection)) return object;
        if (!connection.isEnabled()) return object;

        object = lookup(connection);
        if (object == null) return null;

        reference = WeakRef.of(object);

        // update the ref-metadata with more qualified resolved object
        objectType = object.getObjectType();
        objectName = object.getName();
        overload = object.getOverload();
        DBObject parentObject = object.getParentObject();
        if (parentObject != null) parent = parentObject.ref();

        return object;
    }

    private T getObject() {
        try {
            if (reference == null) return null;

            T object = reference.get();
            if (object == null) return null;
            if (object.isDisposed()) return null;

            return object;
        } catch (Exception e) {
            conditionallyLog(e);
            return null;
        }
    }

    private void clearReference() {
        WeakRef<T> reference = this.reference;
        if (reference != null) {
            reference.clear();
            this.reference = null;
        }
    }


    @Nullable
    private T lookup(@NotNull ConnectionHandler connection) {
        DBObject object = null;
        DBObjectRef<?> parent = getParentRef();
        if (parent == null) {
            DBObjectBundle objectBundle = connection.getObjectBundle();
            object = objectBundle.getObject(objectType, objectName, overload);
        } else {
            DBObject parentObject = parent.get();
            if (parentObject != null) {
                object = parentObject.getChildObject(objectType, objectName, overload, true);
                DBObjectType genericType = objectType.getGenericType();
                if (object == null && genericType != objectType) {
                    object = parentObject.getChildObject(genericType, objectName, overload, true);
                }

                object = unpackSynonym(object);
            }
        }
        return cast(object);
    }

    @Nullable
    private DBObject unpackSynonym(DBObject object) {
        if (object instanceof DBSynonym) {
            if (objectType == SYNONYM) return object;

            DBSynonym synonym = (DBSynonym) object;
            object = synonym.getUnderlyingObject();
            if (object == null) return null;
            if (!object.matches(objectType)) return null;
        }
        return object;
    }

    @Nullable
    @Override
    public ConnectionHandler getConnection() {
        return ConnectionHandler.get(getConnectionId());
    }

    public DBSchema getSchema() {
        return getParentObject(DBObjectType.SCHEMA);
    }

    @Nullable
    @Override
    public SchemaId getSchemaId() {
        String schemaName = getSchemaName();
        return SchemaId.get(schemaName);
    }

    public String getSchemaName() {
        DBObjectRef<DBObject> schema = getParentRef(DBObjectType.SCHEMA);
        return schema == null ? null : schema.getObjectName();
    }

    public String getFileName() {
        if (overload == 0) {
            return objectName;
        } else {
            return objectName + PS + overload;
        }
    }

    public boolean isOfType(DBObjectType objectType) {
        return this.objectType.matches(objectType);
    }

    public boolean isLoaded() {
        DBObjectRef<?> parent = getParentRef();
        return (parent == null || parent.isLoaded()) && reference != null && reference.get() != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DBObjectRef<?> that = (DBObjectRef<?>) o;
        return deepEqual(this, that);
    }

    private static boolean deepEqual(DBObjectRef local, DBObjectRef remote) {
        if (local == null && remote == null) {
            return true;
        }

        if (local == null || remote == null) {
            return false;
        }

        if (local == remote) {
            return true;
        }

        if (local.getObjectType() != remote.getObjectType()) {
            return false;
        }

        if (local.getOverload() != remote.getOverload()) {
            return false;
        }

        if (local.getConnectionId() != remote.getConnectionId()) {
            return false;
        }

        if (!Objects.equals(local.getObjectName(), remote.getObjectName())) {
            return false;
        }

        return deepEqual(local.getParentRef(), remote.getParentRef());
    }

    @Override
    public int hashCode() {
        if (hashCode == -1) {
            hashCode = (getConnectionId() + PSS + serialize()).hashCode();
        }
        return hashCode;
    }

    @Override
    public int compareTo(@NotNull DBObjectRef<?> that) {
        ConnectionId thisConnectionId = nvl(this.getConnectionId(), ConnectionId.UNKNOWN);
        ConnectionId thatConnectionId = nvl(that.getConnectionId(), ConnectionId.UNKNOWN);

        int result = thisConnectionId.compareTo(thatConnectionId);
        if (result != 0) return result;

        DBObjectRef<?> thisParent = this.getParentRef();
        DBObjectRef<?> thatParent = that.getParentRef();
        if (thisParent != null && thatParent != null) {
            if (Objects.equals(thisParent, thatParent)) {
                result = this.objectType.compareTo(that.objectType);
                if (result != 0) return result;

                int nameCompare = this.objectName.compareToIgnoreCase(that.objectName);
                return nameCompare == 0 ? this.overload - that.overload : nameCompare;
            } else {
                return thisParent.compareTo(thatParent);
            }
        } else if(thisParent == null && thatParent == null) {
            result = this.objectType.compareTo(that.objectType);
            if (result != 0) return result;

            return this.objectName.compareToIgnoreCase(that.objectName);
        } else if (thisParent == null) {
            return -1;
        } else {
            return 1;
        }
    }

    @Override
    public String toString() {
        return objectName;
    }

    public boolean isValid() {
        T object = WeakRef.get(reference);
        return Checks.isValid(object);
    }

    @Override
    @Nullable
    public String getPresentableText() {
        return getObjectName();
    }

    @Override
    @Nullable
    @Compatibility
    public String getLocationString() {
        return null;
    }

    @Override
    public @Nullable Icon getIcon(boolean unused) {
        return getObjectType().getIcon();
    }
}
