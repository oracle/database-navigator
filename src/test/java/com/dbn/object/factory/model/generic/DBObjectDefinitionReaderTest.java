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

package com.dbn.object.factory.model.generic;

import com.dbn.common.util.Lists;
import com.dbn.common.util.XmlContents;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.factory.model.DBObjectSpecReader;
import com.dbn.object.type.DBObjectType;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DBObjectDefinitionReaderTest {
    @NonNls
    private static final String TABLE_DEFINITION = """
            <definition>
                <attribute name="OBJECT_TYPE"   value="TABLE"/>
                <attribute name="OBJECT_NAME"   value="FILE_CONTENTS"/>
                <attribute name="OBJECT_DETAIL" value="lob(FILE_CONTENT) store as securefile (nocache filesystem_like_logging)"/>
                <children readonly="Y">
                    <definition readonly="Y">
                        <attribute name="OBJECT_TYPE"    value="COLUMN"/>
                        <attribute name="OBJECT_NAME"    value="ID"/>
                        <attribute name="DATA_TYPE"      value="varchar2(50)"/>
                        <attribute name="IS_NOT_NULL"    value="Y"/>
                        <attribute name="IS_PRIMARY_KEY" value="Y"/>
                    </definition>
                    <definition readonly="Y">
                        <attribute name="OBJECT_TYPE"    value="COLUMN"/>
                        <attribute name="OBJECT_NAME"    value="FILE_SIZE"/>
                        <attribute name="DATA_TYPE"      value="number(19)"/>
                        <attribute name="IS_NOT_NULL"    value="Y"/>
                    </definition>
                    <definition readonly="Y">
                        <attribute name="OBJECT_TYPE"    value="COLUMN"/>
                        <attribute name="OBJECT_NAME"    value="FILE_HASH"/>
                        <attribute name="DATA_TYPE"      value="varchar2(64)"/>
                        <attribute name="IS_NOT_NULL"    value="Y"/>
                    </definition>
                    <definition readonly="Y">
                        <attribute name="OBJECT_TYPE"    value="COLUMN"/>
                        <attribute name="OBJECT_NAME"    value="FILE_CONTENT"/>
                        <attribute name="DATA_TYPE"      value="blob"/>
                    </definition>
                    <definition readonly="Y">
                        <attribute name="OBJECT_TYPE"    value="COLUMN"/>
                        <attribute name="OBJECT_NAME"    value="METADATA"/>
                        <attribute name="DATA_TYPE"      value="json"/>
                    </definition>
                    <definition readonly="Y">
                        <attribute name="OBJECT_TYPE"        value="CONSTRAINT"/>
                        <attribute name="CONSTRAINT_TYPE"    value="unique"/>
                        <attribute name="CONSTRAINT_COLUMNS" value="FILE_SIZE, FILE_HASH"/>
                    </definition>
                </children>
            </definition>
            """;

    @Test
    @SneakyThrows
    public void read() {
        Element element = XmlContents.streamToElement(new ByteArrayInputStream(TABLE_DEFINITION.getBytes()));
        DBObjectSpec definition = DBObjectSpecReader.read(element);

        assertEquals(DBObjectType.TABLE, definition.getObjectType());
        List<DBObjectSpec> columns = definition.getChildren(DBObjectType.COLUMN);


        assertEquals(5, columns.size());
        List<String> columnNames = Lists.convert(columns, c -> c.getObjectName());

        assertTrue(columnNames.containsAll(Set.of("ID", "FILE_SIZE", "FILE_HASH", "FILE_CONTENT", "METADATA")));


        List<DBObjectSpec> constraints = definition.getChildren(DBObjectType.CONSTRAINT);
        assertEquals(1, constraints.size());
    }
}