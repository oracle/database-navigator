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

package com.dbn.ddl.options;

import com.dbn.common.options.BasicProjectConfiguration;
import com.dbn.common.util.Strings;
import com.dbn.ddl.DDLFileType;
import com.dbn.ddl.DDLFileTypeId;
import com.dbn.ddl.options.ui.DDLFileExtensionSettingsForm;
import com.dbn.language.psql.PSQLFileType;
import com.dbn.language.sql.SQLFileType;
import lombok.Getter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.editor.DBContentType.CODE;
import static com.dbn.editor.DBContentType.CODE_BODY;
import static com.dbn.editor.DBContentType.CODE_SPEC;
import static com.dbn.editor.DBContentType.CODE_SPEC_AND_BODY;

@Getter
public class DDLFileExtensionSettings extends BasicProjectConfiguration<DDLFileSettings, DDLFileExtensionSettingsForm> {

    private final List<DDLFileType> fileTypes = Arrays.asList(
            new DDLFileType(DDLFileTypeId.VIEW, "DDL File - View", "vw", SQLFileType.INSTANCE, CODE),
            new DDLFileType(DDLFileTypeId.TRIGGER, "DDL File - Trigger", "trg", PSQLFileType.INSTANCE, CODE),
            new DDLFileType(DDLFileTypeId.PROCEDURE, "DDL File - Procedure", "prc", PSQLFileType.INSTANCE, CODE),
            new DDLFileType(DDLFileTypeId.FUNCTION, "DDL File - Function", "fnc", PSQLFileType.INSTANCE, CODE),
            new DDLFileType(DDLFileTypeId.PACKAGE, "DDL File - Package", "pkg", PSQLFileType.INSTANCE, CODE_SPEC_AND_BODY),
            new DDLFileType(DDLFileTypeId.PACKAGE_SPEC, "DDL File - Package Spec", "pks", PSQLFileType.INSTANCE, CODE_SPEC),
            new DDLFileType(DDLFileTypeId.PACKAGE_BODY, "DDL File - Package Body", "pkb", PSQLFileType.INSTANCE, CODE_BODY),
            new DDLFileType(DDLFileTypeId.TYPE, "DDL File - Type", "tpe", PSQLFileType.INSTANCE, CODE_SPEC_AND_BODY),
            new DDLFileType(DDLFileTypeId.TYPE_SPEC, "DDL File - Type Spec", "tps", PSQLFileType.INSTANCE, CODE_SPEC),
            new DDLFileType(DDLFileTypeId.TYPE_BODY, "DDL File - Type Body", "tpb", PSQLFileType.INSTANCE, CODE_BODY)
    );

    DDLFileExtensionSettings(DDLFileSettings parent) {
        super(parent);
    }

    @Override
    public String getDisplayName() {
        return txt("cfg.ddlFiles.title.DdlFileExtensionSettings");
    }

    public DDLFileType getFileType(DDLFileTypeId fileTypeId) {
        for (DDLFileType fileType : fileTypes) {
            if (fileType.getId() == fileTypeId) {
                return fileType;
            }
        }
        return null;
    }

    public DDLFileType getFileTypeForExtension(String extension) {
        for (DDLFileType fileType : fileTypes) {
            if (fileType.getExtensions().contains(extension)) {
                return fileType;
            }
        }
        return null;
    }

    /*********************************************************
     *                      Configuration                    *
     *********************************************************/
    @Override
    @NotNull
    public DDLFileExtensionSettingsForm createConfigurationEditor() {
        return new DDLFileExtensionSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "extensions";
    }

    @Override
    public void readConfiguration(Element element) {
        for (Element child : element.getChildren()) {
            DDLFileTypeId fileTypeId = enumAttribute(child, "file-type-id", DDLFileTypeId.class);
            String extensions = child.getAttributeValue("extensions");

            DDLFileType fileType = getFileType(fileTypeId);
            List<String> tokens = Strings.tokenize(extensions, ",");
            fileType.setExtensions(tokens);
        }
    }

    @Override
    public void writeConfiguration(Element element) {
        for (DDLFileType fileType : fileTypes) {
            Element fileTypeElement = newElement(element, "mapping");
            setEnumAttribute(fileTypeElement, "file-type-id", fileType.getId());
            String extensions = Strings.concatenate(fileType.getExtensions(), ",");
            fileTypeElement.setAttribute("extensions", extensions);
        }
    }
}
