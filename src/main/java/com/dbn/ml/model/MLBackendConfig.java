/*
 * Copyright 2024-2025 Oracle and/or its affiliates
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

package com.dbn.ml.model;

import com.dbn.common.options.setting.Settings;
import com.dbn.ml.backend.MLBackendType;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

/**
 * Configuration for ML backend selection and backend-specific settings.
 *
 * @author ayoub allali
 */
@Getter
@Setter
public class MLBackendConfig extends MLConfig {

    /** Selected backend type (defaults to Tribuo ) */
    private MLBackendType backendType = MLBackendType.TRIBUO;

    /** Auto-cleanup staging tables after training (DBMS backend only) */
    private boolean autoCleanupStagingTables = true;

    /** Preferred schema for staging tables (DBMS backend only) */
    private String preferredSchema;

    @Override
    public void readState(Element element) {
        if (element == null) return;
        super.readState(element);

        String backendTypeStr = Settings.stringAttribute(element, "backend-type", backendType.name());
        try {
            backendType = MLBackendType.valueOf(backendTypeStr);
        } catch (IllegalArgumentException e) {
            // If invalid backend type, default to Tribuo
            backendType = MLBackendType.TRIBUO;
        }

        autoCleanupStagingTables = Settings.booleanAttribute(element, "auto-cleanup", autoCleanupStagingTables);
        preferredSchema = Settings.stringAttribute(element, "preferred-schema", preferredSchema);
    }

    @Override
    public void writeState(Element element) {
        super.writeState(element);

        Settings.setStringAttribute(element, "backend-type", backendType.name());
        Settings.setBooleanAttribute(element, "auto-cleanup", autoCleanupStagingTables);
        Settings.setStringAttribute(element, "preferred-schema", preferredSchema);
    }
}
