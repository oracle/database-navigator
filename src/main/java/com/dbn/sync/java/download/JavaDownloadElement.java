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

import com.dbn.common.icon.Icons;
import com.dbn.common.ui.list.Enableable;
import com.dbn.common.ui.list.Selectable;
import com.dbn.object.DBJavaClass;
import com.dbn.object.lookup.DBJavaNameCache;
import com.dbn.object.lookup.DBObjectRef;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

@Data
public class JavaDownloadElement implements Selectable<JavaDownloadElement>, Enableable<JavaDownloadElement> {
    private DBObjectRef<DBJavaClass> javaClass;
    private boolean enabled;
    private boolean selected;

    public JavaDownloadElement(DBObjectRef<DBJavaClass> javaClass) {
        this.javaClass = javaClass;
    }

    public DBJavaClass getJavaClass() {
        return javaClass.ensure();
    }

    public String getJavaClassName() {
        return DBJavaNameCache.getCanonicalName(javaClass);
    }

    public String getJavaFileName() {
        return DBJavaNameCache.getSimpleName(javaClass) + ".java";
    }

    public String[] getPackageNameTokens() {
        String packageName = getJavaClass().getPackageName();
        return packageName == null ? new String[0] : packageName.split("\\.");
    }

    public String getSchemaName() {
        return javaClass.getSchemaName();
    }


    @NotNull
    @Override
    public String getName() {
        return getJavaClassName() + " (" + getSchemaName() + ")";
    }

    @Override
    public @Nullable Icon getIcon() {
        return isEnabled() ? getJavaClass().getIcon() : Icons.DBO_JAVA_CLASS;
    }
}
