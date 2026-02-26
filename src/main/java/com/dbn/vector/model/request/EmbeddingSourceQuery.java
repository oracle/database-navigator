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

package com.dbn.vector.model.request;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Cloneable;
import com.dbn.common.util.UUIDs;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.readCdata;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.options.setting.Settings.writeCdata;
import static com.dbn.common.util.Strings.truncateWithEllipsis;
import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.vector.model.request.EmbeddingSourceType.DATABASE_QUERY;

@Getter
@Setter
public class EmbeddingSourceQuery implements EmbeddingSource, PersistentStateElement, Cloneable<EmbeddingSourceQuery> {
    private String identifier = UUIDs.compact();
    private String schemaName;
    private String selectStatement;
    private transient String selectStatementPreview;

    @Override
    public EmbeddingSourceType getType() {
        return DATABASE_QUERY;
    }

    public String getSelectStatementPreview() {
        if (selectStatementPreview != null) return selectStatementPreview;

        selectStatementPreview = truncateWithEllipsis(selectStatement.replaceAll("\\s+", " "), 40);
        return selectStatementPreview;
    }

    public void setSelectStatement(String selectStatement) {
        this.selectStatement = selectStatement;
        this.selectStatementPreview = null;
    }

    @Override
    public void readState(Element element) {
        if (element == null) return;

        identifier = stringAttribute(element, "identifier");
        schemaName = stringAttribute(element, "schema");
        selectStatement = readCdata(element);
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "identifier", identifier);
        setStringAttribute(element, "schema", schemaName);
        writeCdata(element, selectStatement);
    }

    @Override
    @SneakyThrows
    public EmbeddingSourceQuery clone() {
        return cast(super.clone());
    }
}
