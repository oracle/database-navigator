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

package com.dbn.common.options;

import com.dbn.nls.NlsSupport;
import com.intellij.openapi.options.ConfigurationException;
import org.jdom.Element;

import static com.dbn.common.options.ConfigActivity.APPLYING;
import static com.dbn.common.options.ConfigActivity.CLONING;

public interface PersistentConfiguration extends NlsSupport {
    void readConfiguration(Element element);
    void writeConfiguration(Element element);

    default void validate() throws ConfigurationException {};

    default void applyTo(PersistentConfiguration configuration) {
        Element element = new Element("configuration");
        writeConfiguration(element);
        configuration.readConfiguration(element);
    }

    default boolean isTransientContext() {
        return ConfigMonitor.is(CLONING) || ConfigMonitor.is(APPLYING);
    }
}