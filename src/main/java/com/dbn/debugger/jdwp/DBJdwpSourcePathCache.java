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

package com.dbn.debugger.jdwp;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBMethod;
import com.dbn.object.DBProgram;
import com.dbn.object.DBSchema;
import com.dbn.object.DBSynonym;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.vfs.file.DBContentVirtualFile;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.ClassUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class DBJdwpSourcePathCache {
    private final Map<DBJdwpSourcePath, VirtualFile> cache = new ConcurrentHashMap<>();

    @Nullable
    public VirtualFile getSourceFile(DBJdwpSourcePath sourcePath, ConnectionHandler connection, SchemaId schemaId) {
        return cache.computeIfAbsent(sourcePath, sp -> resolveSourceFile(sp, connection, schemaId));
    }


    @Nullable
    private static VirtualFile resolveSourceFile(DBJdwpSourcePath sourcePath, ConnectionHandler connection, SchemaId schemaId) {
        if (sourcePath.isAnonymousBlock()) return null;
        if (sourcePath.isDatabaseProgram()) return resolveDatabaseFile(sourcePath, connection);
        if (sourcePath.isJavaProgram()) return resolveJavaFile(sourcePath, connection, schemaId);
        return null;
    }

    @Nullable
    private static DBContentVirtualFile resolveDatabaseFile(DBJdwpSourcePath sourcePath, ConnectionHandler connection) {
        DBProgram program = resolveDatabaseProgram(sourcePath, connection);
        if (program != null) {
            DBEditableObjectVirtualFile objectFile = program.getEditableVirtualFile();
            DBContentType contentType = sourcePath.isProgramBody() ?
                    DBContentType.CODE_BODY :
                    DBContentType.CODE_SPEC;
            return objectFile.getContentFile(contentType);
        }

        DBMethod method = resolveDatabaseMethod(sourcePath, connection);
        if (method != null) {
            DBEditableObjectVirtualFile objectFile = method.getEditableVirtualFile();
            return objectFile.getContentFile(DBContentType.CODE);
        }

        return null;
    }

    @Nullable
    private static DBProgram resolveDatabaseProgram(DBJdwpSourcePath sourcePath, ConnectionHandler connection) {
        DBObjectBundle objectBundle = connection.getObjectBundle();
        String programOwner = sourcePath.getProgramOwner();
        DBSchema schema = objectBundle.getSchema(programOwner);
        if (schema == null) return null;

        String programName = sourcePath.getProgramName();
        return schema.getProgram(programName);
    }

    @Nullable
    private static DBMethod resolveDatabaseMethod(DBJdwpSourcePath sourcePath, ConnectionHandler connection) {
        DBObjectBundle objectBundle = connection.getObjectBundle();
        String programOwner = sourcePath.getProgramOwner();
        DBSchema schema = objectBundle.getSchema(programOwner);
        if (schema == null) return null;

        String programName = sourcePath.getProgramName();
        return schema.getMethod(programName, (short) 0);
    }

    @Nullable
    private static VirtualFile resolveJavaFile(DBJdwpSourcePath sourcePath, ConnectionHandler connection, SchemaId schemaId) {
        String programName = sourcePath.getProgramName();
        DBJavaClass javaClass = resolveJavaClass(programName, connection, schemaId);

        if (javaClass != null && javaClass.isSource()) {
            DBEditableObjectVirtualFile editableVirtualFile = javaClass.getEditableVirtualFile();
            DBContentType contentType = DBContentType.CODE;
            return editableVirtualFile.getContentFile(contentType);
        }

        VirtualFile localFile = resolveLocalFile(programName, connection.getProject());
        if (localFile != null) return localFile;

        if (javaClass != null) {
            DBEditableObjectVirtualFile editableVirtualFile = javaClass.getEditableVirtualFile();
            DBContentType contentType = DBContentType.CODE;
            return editableVirtualFile.getContentFile(contentType);
        }
        return null;
    }

    @Nullable
    private static DBJavaClass resolveJavaClass(String programName, ConnectionHandler connection, SchemaId schemaId) {
        String objectName = programName.split("\\.")[0];

        DBObjectBundle objectBundle = connection.getObjectBundle();
        DBSchema schema = objectBundle.getSchema(schemaId.getName());
        if (schema == null) return null;

        DBJavaClass javaClass = schema.getJavaClass(objectName);
        if (javaClass != null) return javaClass;

        DBSynonym synonym = schema.getSynonym(objectName);
        DBObject object = DBSynonym.unwrap(synonym);
        if (object instanceof DBJavaClass) {
            return (DBJavaClass) object;
        }

        return javaClass;
    }

    @Nullable
    private static VirtualFile resolveLocalFile(String programName, Project project) {
        String className = programName.replace("/", ".");

        PsiManager psiManager = PsiManager.getInstance(project);
        PsiClass psiClass = ClassUtil.findPsiClass(psiManager, className);
        if (psiClass == null) return null;

        return psiClass.getContainingFile().getVirtualFile();
    }
}
