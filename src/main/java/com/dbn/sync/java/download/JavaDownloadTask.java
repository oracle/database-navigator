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

package com.dbn.sync.java.download;

import com.dbn.batch.impl.BatchTaskBase;
import com.dbn.common.icon.Icons;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaEntity;
import com.dbn.object.DBJavaResource;
import com.dbn.object.lookup.DBJavaNameCache;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

@Getter
@Setter
public class JavaDownloadTask extends BatchTaskBase {
    private DBObjectRef<DBJavaEntity> entity;
    private VirtualFile targetFolder;
    private VirtualFile targetFile;
    private byte[] content;

    public JavaDownloadTask(DBJavaClass javaClass) {
        this(DBObjectRef.of(javaClass));
    }

    public JavaDownloadTask(DBJavaResource javaResource) {
        this(DBObjectRef.of(javaResource));
    }

    public JavaDownloadTask(DBObjectRef<DBJavaEntity> javaEntity) {
        this.entity = javaEntity;
    }

    public DBJavaEntity getEntity() {
        return entity.ensure();
    }

    public String getEntityName() {
        return entity.getFileName();
    }

    public String getEntityFileName() {
        DBObjectType entityType = entity.getObjectType();
        if (entityType == DBObjectType.JAVA_CLASS) {
            return DBJavaNameCache.getSimpleName(entity) + ".java";
        }
        return DBJavaNameCache.getSimpleName(entity);
    }

    public String[] getEntityPathTokens() {
        String[] tokens = entity.getFileName().split("/");
        String[] pathTokens = new String[tokens.length - 1];
        System.arraycopy(tokens, 0, pathTokens, 0, tokens.length - 1);
        return pathTokens;
    }

    public String getSchemaName() {
        return entity.getSchemaName();
    }

    @NotNull
    @Override
    public String getName() {
        return getEntityName() + " (" + getSchemaName() + ")";
    }

    @Override
    public Object getSubject() {
        return getEntity();
    }

    @Override
    public @Nullable Icon getIcon() {
        DBObjectType objectType = entity.getObjectType();
        if(objectType == DBObjectType.JAVA_CLASS) {
            return isEnabled() ? getEntity().getIcon() : Icons.DBO_JAVA_CLASS;
        }

        if(objectType == DBObjectType.JAVA_RESOURCE) {
            DBJavaResource resource = (DBJavaResource) getEntity();
            return resource.getIcon();
        }
        return null;
    }
}
