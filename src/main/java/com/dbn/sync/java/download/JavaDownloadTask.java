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
import com.dbn.object.DBJavaResource;
import com.dbn.object.common.DBObject;
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
    private DBObjectRef<DBJavaClass> javaClass;
    private DBObjectRef<DBJavaResource> javaResource = null;
    private VirtualFile targetFolder;
    private VirtualFile targetFile;
    private byte[] content;

    public JavaDownloadTask(DBJavaClass javaClass) {
        this(DBObjectRef.of(javaClass));
    }

    public JavaDownloadTask(DBObjectRef javaObject) {
        if(javaObject.getObjectType() == DBObjectType.JAVA_CLASS)
            this.javaClass = javaObject;
        else
            this.javaResource = javaObject;
    }

    public JavaDownloadTask(DBJavaResource javaResource) {
        this(DBObjectRef.of(javaResource));
    }


    public DBObject getObject() {
        if(this.javaResource == null) return javaClass.ensure();
        return javaResource.ensure();
    }

    public String getJavaClassName() {
        if(this.javaResource == null)
            return DBJavaNameCache.getCanonicalName(javaClass);
        return javaResource.getFileName();
    }

    public String getJavaResourceName() {
        return javaResource.getFileName();
    }


    public String getJavaFileName() {
        if(this.javaResource == null)
            return DBJavaNameCache.getSimpleName(javaClass) + ".java";
        else {
            String packageName = String.join("/", getPackageNameTokens());
            if(packageName.isEmpty())
                return javaResource.getFileName();
            return javaResource.getFileName().substring(packageName.length() + 1); // last separator token
        }
    }

    public String[] getPackageNameTokens() {
        if(this.javaResource == null) {
            String packageName = ((DBJavaClass) getObject()).getPackageName();
            return packageName == null ? new String[0] : packageName.split("\\.");
        } else {
            String[] fileNameTokens = this.javaResource.getFileName().split("/");
            String[] packageTokens = new String[fileNameTokens.length - 1];
			System.arraycopy(fileNameTokens, 0, packageTokens, 0, fileNameTokens.length - 1);
            return packageTokens;
        }
    }

    public String getSchemaName() {
        if( this.javaResource == null)
            return javaClass.getSchemaName();
        else
            return javaResource.getSchemaName();
    }


    @NotNull
    @Override
    public String getName() {
        if( this.javaResource == null)
            return getJavaClassName() + " (" + getSchemaName() + ")";
        else
            return getJavaResourceName();
    }

    @Override
    public Object getSubject() {
        return getJavaClass();
    }

    @Override
    public @Nullable Icon getIcon() {

        if(this.javaResource == null)
            return isEnabled() ? getObject().getIcon() : Icons.DBO_JAVA_CLASS;
        else
            return ((DBJavaResource) getObject()).getFileType().getIcon();
    }
}
