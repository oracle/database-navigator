/*
 * Copyright 2025 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.assistant.tool;

import com.dbn.common.component.ConnectionComponent;
import com.dbn.connection.ConnectionHandler;
import com.dbn.editor.DBContentType;
import com.dbn.editor.DatabaseFileEditorManager;
import com.dbn.editor.EditorProviderId;
import com.dbn.editor.code.SourceCodeManager;
import com.dbn.editor.code.content.SourceCodeContent;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Predicate;


@Getter
public abstract class AssistantToolBase extends ConnectionComponent implements AssistantTool{
    private AssistantToolType type;
    private AssistantToolCategory category;
    private String name;
    private String description;
    private boolean interactive;

    public void initialize(ConnectionHandler connection, String name, String description, AssistantToolType type, AssistantToolCategory category, boolean interactive) {
        initialize(connection);
        this.name = name;
        this.description = description;
        this.type = type;
        this.category = category;
        this.interactive = interactive;
    }

    protected static <T extends DBObject> List<String> getObjectNames(List<T> objects, boolean qualified) {
        return getObjectNames(objects, qualified, o -> true);
    }

    protected static <T extends DBObject> List<String> getObjectNames(List<T> objects, boolean qualified, Predicate<T> filter) {
        return objects
                .stream()
                .filter(filter)
                .map(o -> qualified ? o.getQualifiedName() : o.getName())
                .toList();
    }

    @NotNull
    protected DBSchema getSchema(String schemaName) {
        DBSchema schema = getConnection().getObjectBundle().getSchema(schemaName);
        verify(schema, DBObjectType.SCHEMA, schemaName);
        return schema;
    }

    protected static <T extends DBObject> void verify(T object, DBObjectType objectType, String objectName) {
        if (object == null) throw new IllegalArgumentException(objectType.getTitleCasedName() + " not found: " + objectName);
    }

    protected static void verify(Object object, String message) {
        if (object == null) throw new IllegalStateException(message);
    }

    protected static <T extends DBObject> T undisposed(T object) {
        DBObjectRef<T> ref = DBObjectRef.of(object);
        return ref.get();
    }

    protected static String loadObjectSourceCode(DBSchemaObject object, DBContentType contentType) throws SQLException {
        SourceCodeManager sourceCodeManager = SourceCodeManager.getInstance(object.getProject());
        SourceCodeContent sourceCode = sourceCodeManager.loadSourceFromDatabase(object, contentType);
        return sourceCode.getRawContent();
    }

    protected static void openEditor(DBObject object) {
        Project project = object.getProject();
        DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(project);
        editorManager.connectAndOpenEditor(object, EditorProviderId.DATA, true, true);
    }

    @Override
    public String toString() {
        return "AssistantTool{" +
                "type=" + type + ", " +
                "category=" + category + ", " +
                "description='" + description + '\'' +
                '}';
    }
}
