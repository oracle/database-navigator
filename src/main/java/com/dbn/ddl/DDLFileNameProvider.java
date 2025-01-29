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

package com.dbn.ddl;

import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;

import static com.dbn.common.util.Strings.toLowerCase;

@Getter
public class DDLFileNameProvider {
    private final DBObjectRef object;
    private final DDLFileType ddlFileType;
    private final String extension;

    public DDLFileNameProvider(DBObjectRef object, DDLFileType ddlFileType, String extension) {
        this.object = object;
        this.ddlFileType = ddlFileType;
        this.extension = extension;
    }

    public DBObjectType getObjectType() {
        return object.getObjectType();
    }

    public DBObject getObject() {
        return object.get();
    }

    private String getAdjustedFileName(){
        String fileName = object.getFileName();

        if(object.getObjectType() == DBObjectType.JAVA_CLASS){
            return fileName.replace("/",".");
        }
        return toLowerCase(fileName); // TODO allow file name case configuration
    }

    public String getFileName() {
        return getAdjustedFileName() + '.' + extension;
    }

    public String getFilePattern() {
        return "*" + getAdjustedFileName() + "*." + extension;  // TODO allow more qualified suffix and postfix definitions
    }

}
