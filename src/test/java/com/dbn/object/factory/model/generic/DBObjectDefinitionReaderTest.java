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
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DBObjectDefinitionReaderTest {

    @Test
    @SneakyThrows
    public void read() {
        Element element = XmlContents.fileToElement(DBObjectDefinitionReaderTest.class, "staging-table-definition.xml");
        DBObjectSpec definition = DBObjectSpecReader.read(element);

        assertEquals(DBObjectType.TABLE, definition.getObjectType());
        List<DBObjectSpec> children = definition.getChildren(DBObjectType.COLUMN);


        assertEquals(5, children.size());
        List<String> columnNames = Lists.convert(children, c -> c.getObjectName());

        assertTrue(columnNames.containsAll(Set.of("ID", "FILE_SIZE", "FILE_HASH", "FILE_CONTENT", "METADATA")));
    }
}