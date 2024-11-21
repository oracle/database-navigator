/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.connection.config.file;

import com.dbn.common.latent.Latent;
import com.dbn.common.util.Cloneable;
import com.dbn.common.util.Files;
import com.dbn.common.util.Strings;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;

@Data
public class DatabaseFile implements Cloneable<DatabaseFile> {
    private String path;
    private String schema;
    private Latent<File> file = Latent.mutable(
            () -> getPath(),
            () -> Strings.isEmpty(path) ? null : new File(path));

    public DatabaseFile() {}

    public DatabaseFile(String path) {
        this(path, Files.getFileName(path));
    }

    public DatabaseFile(String path, String schema) {
        this.path = path;
        this.schema = schema;
    }

    public String getFileName() {
        return Files.getFileName(path);
    }

    @Nullable
    public File getFile() {
        return file.get();
    }

    public boolean isValid() {
        File file = getFile();
        if (file == null) return false;
        if (file.isDirectory()) return false;

        return true;
    }

    public boolean isMain() {
        return Objects.equals(schema, "main");
    }

    public boolean isPresent() {
        File file = getFile();
        return isValid() && file != null  && file.exists();
    }

    @Override
    public DatabaseFile clone() {
        return new DatabaseFile(path, schema);
    }

}
