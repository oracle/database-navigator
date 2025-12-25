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

package com.dbn.ml.model.source;

import com.dbn.ml.model.MLConfig;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;

/**
 * Parent configuration for ML data sources.
 * Follows VectorToolbox pattern (SourceConfig).
 * 
 * Contains sourceType selector and child configs for each source type.
 */
@Getter
@Setter
public class MLSourceConfig extends MLConfig {
    private MLSourceType sourceType = MLSourceType.DATABASE_TABLE;
    private final MLTableSourceConfig tableSourceConfig = new MLTableSourceConfig();
    private final MLFileSourceConfig fileSourceConfig = new MLFileSourceConfig();

    @Override
    public void readState(Element element) {
        if (element == null) return;
        super.readState(element);
        
        sourceType = enumAttribute(element, "source-type", sourceType);
        
        Element tableSourceElement = element.getChild("table-source");
        Element fileSourceElement = element.getChild("file-source");
        tableSourceConfig.readState(tableSourceElement);
        fileSourceConfig.readState(fileSourceElement);
    }

    @Override
    public void writeState(Element element) {
        super.writeState(element);
        
        setEnumAttribute(element, "source-type", sourceType);
        
        Element tableSourceElement = newElement(element, "table-source");
        Element fileSourceElement = newElement(element, "file-source");
        tableSourceConfig.writeState(tableSourceElement);
        fileSourceConfig.writeState(fileSourceElement);
    }
}
