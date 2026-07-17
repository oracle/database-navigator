package com.dbn.editor.code.source;

import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.extension.DBObjectExtensionPointCache;
import com.dbn.object.type.DBObjectType;

public class DBObjectSourceCodeAdapters extends DBObjectExtensionPointCache<DBObjectSourceCodeAdapter> {
    private static final DBObjectSourceCodeAdapters INSTANCE = new DBObjectSourceCodeAdapters();

    private DBObjectSourceCodeAdapters() {
        super(DBObjectSourceCodeAdapter.EP);
    }

    public static <T extends DBSchemaObject> DBObjectSourceCodeAdapter<T> get(DBObjectType objectType) {
        return INSTANCE.find(objectType);
    }
}
