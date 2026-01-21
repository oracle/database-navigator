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
            <definition type="TABLE" name="FILE_CONTENTS">
                <attributes>
                    <attr name="OBJECT_DETAIL" value="lob(FILE_CONTENT) store as securefile (nocache filesystem_like_logging)"/>
                </attributes>
                <children readonly="Y">
                    <definition type="COLUMN" name="ID" readonly="Y">
                        <attributes>
                            <attr name="DATA_TYPE" value="varchar2(50)"/>
                            <attr name="IS_NOT_NULL" value="Y"/>
                            <attr name="IS_PRIMARY_KEY" value="Y"/>
                        </attributes>
                    </definition>
                    <definition type="COLUMN" name="FILE_SIZE" readonly="Y">
                        <attributes>
                            <attr name="DATA_TYPE" value="number(19)"/>
                            <attr name="IS_NOT_NULL" value="Y"/>
                        </attributes>
                    </definition>
                    <definition type="COLUMN" name="FILE_HASH" readonly="Y">
                        <attributes>
                            <attr name="DATA_TYPE" value="varchar2(64)"/>
                            <attr name="IS_NOT_NULL" value="Y"/>
                        </attributes>
                    </definition>
                    <definition type="COLUMN" name="FILE_CONTENT" readonly="Y">
                        <attributes>
                            <attr name="DATA_TYPE" value="blob"/>
                        </attributes>
                    </definition>
                    <definition type="COLUMN" name="METADATA" readonly="Y">
                        <attributes>
                            <attr name="DATA_TYPE" value="json"/>
                        </attributes>
                    </definition>
                    <definition type="CONSTRAINT" readonly="Y">
                        <attributes>
                            <attr name="CONSTRAINT_TYPE" value="unique"/>
                            <attr name="CONSTRAINT_COLUMNS" value="FILE_SIZE, FILE_HASH"/>
                        </attributes>
                    </definition>
                </children>
            </definition>""";

    @Test
    @SneakyThrows
    public void read() {
        Element element = XmlContents.streamToElement(new ByteArrayInputStream(TABLE_DEFINITION.getBytes()));
        DBObjectSpec definition = DBObjectSpecReader.read(element);

        assertEquals(DBObjectType.TABLE, definition.getObjectType());
        List<DBObjectSpec> children = definition.getChildren(DBObjectType.COLUMN);


        assertEquals(5, children.size());
        List<String> columnNames = Lists.convert(children, c -> c.getObjectName());

        assertTrue(columnNames.containsAll(Set.of("ID", "FILE_SIZE", "FILE_HASH", "FILE_CONTENT", "METADATA")));
    }
}