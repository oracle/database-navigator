/*
 * Copyright 2026 Oracle and/or its affiliates
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

import com.dbn.common.checksum.ChecksumType;
import com.dbn.common.state.PersistentStateElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jdom.Element;

import java.util.Locale;

import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;

@Getter
@NoArgsConstructor
public class LibraryChecksum implements PersistentStateElement {
    private ChecksumType type;
    private String value;
    private String url;

    public LibraryChecksum(ChecksumType type, String value) {
        this(type, value, null);
    }

    public LibraryChecksum(ChecksumType type, String value, String url) {
        this.type = type;
        this.value = normalize(value);
        this.url = url;
    }

    public boolean hasValue() {
        return isNotEmptyOrSpaces(value);
    }

    public boolean hasUrl() {
        return isNotEmptyOrSpaces(url);
    }

    public boolean isStrong() {
        return type != null && type.isStrong();
    }

    @Override
    public void readState(Element element) {
        this.type = enumAttribute(element, "algorithm", ChecksumType.class);
        this.value = normalize(stringAttribute(element, "value"));
        this.url = stringAttribute(element, "url");
    }

    @Override
    public void writeState(Element element) {
        setEnumAttribute(element, "algorithm", type);
        setStringAttribute(element, "value", value);
        setStringAttribute(element, "url", url);
    }

    private static String normalize(String value) {
        return isNotEmptyOrSpaces(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }
}
