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

package com.dbn.common.state;


import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import static com.dbn.common.options.setting.Settings.newElement;

public interface PersistentStateElement {

    /**
     * Read the state from the given jdom element
     * @param element {@link Element} to read the state from
     */
    void readState(@NonNls Element element);
    /**
     * Write the state into the given jdom element
     * @param element {@link Element} to write the state into
     */
    void writeState(@NonNls Element element);

    static <T extends PersistentStateElement> T cloneElement(T source, T target) {
        Element element = newElement("Element");
        source.writeState(element);
        target.readState(element);
        return target;
    }
}
