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
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.newElement;

@Getter
@Setter
public class EmbeddingTableSources extends EmbeddingSourceList<EmbeddingTableSource> implements PersistentStateElement {
    private boolean autoSync;

    @Override
    public void readState(Element element) {
        Element sourcesElement = element.getChild("table-sources");
        for (Element childElement : childrenOf(sourcesElement, "source")) {
            EmbeddingTableSource source = new EmbeddingTableSource();
            source.readState(childElement);
            addElement(source);
        }
    }

    @Override
    public void writeState(Element element) {
        Element sourcesElement = newElement(element, "table-sources");
        for (EmbeddingTableSource source : getElements()) {
            Element childElement = newElement(sourcesElement, "source");
            source.writeState(childElement);
        }
    }
}
