package com.dbn.database.interfaces;

import com.dbn.connection.DatabaseType;
import com.dbn.database.common.DatabaseNativeDataTypes;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguageDialect;
import org.jetbrains.annotations.Nullable;

public interface DatabaseInterfaces {

    DatabaseType getDatabaseType();

    @Nullable
    DBLanguageDialect getLanguageDialect(DBLanguage<?> language);

    DatabaseNativeDataTypes getNativeDataTypes();

    DatabaseMessageParserInterface getMessageParserInterface();

    DatabaseEnvironmentInterface getEnvironmentInterface();

    DatabaseCompatibilityInterface getCompatibilityInterface();

    DatabaseMetadataInterface getMetadataInterface();

    DatabaseDataDefinitionInterface getDataDefinitionInterface();

    DatabaseExecutionInterface getExecutionInterface();

    default DatabaseDebuggerInterface getDebuggerInterface() {
        return null;
    }

    default DatabaseAssistantInterface getAssistantInterface() {
        return null;
    }
    void reset();
}
