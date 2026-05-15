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

package com.dbn.driver.download.metadata;

import com.dbn.common.state.PersistentStateElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@Getter
@NoArgsConstructor
public class Developer implements PersistentStateElement {
    private String name;
    private String url;


    public Developer(String name, String url) {
        this.name = name;
        this.url = url;
    }

    @Override
    public String toString() {
        return String.format("Author [name=%s, url=%s]", name, url);
    }

    @Override
    public void readState(Element element) {
        if (element == null) return;

        name = stringAttribute(element, "name");
        url = stringAttribute(element, "url");
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "name", name);
        setStringAttribute(element, "url", url);
    }
}
