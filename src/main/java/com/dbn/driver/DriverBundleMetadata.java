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

package com.dbn.driver;

import com.dbn.common.checksum.Checksum;
import com.dbn.common.checksum.ChecksumType;
import com.dbn.common.state.PersistentStateElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;


@Slf4j
@Getter
@Setter
@NoArgsConstructor
public class DriverBundleMetadata implements PersistentStateElement {
    private File library;
    private String checksum;
    private Set<String> driverClassNames = new HashSet<>();

    public DriverBundleMetadata(File library) {
        this.library = library;
        this.checksum = Checksum.fromFileAttributes(library, ChecksumType.SHA_256);
    }

    public boolean matchesSignature(DriverBundleMetadata metadata) {
        return Objects.equals(this.checksum, metadata.checksum);
    }

    public boolean isValid() {
        return library.exists();
    }

    public boolean isEmpty() {
        return driverClassNames.isEmpty();
    }

    public boolean isDriverClass(String className) {
        return driverClassNames.contains(className);
    }

    @Override
    public void readState(Element element) {
        this.library = new File(stringAttribute(element, "path"));
        this.checksum = stringAttribute(element, "checksum");
        String[] classNames = stringAttribute(element, "driver-classes").split(",");
        this.driverClassNames.addAll(Arrays.asList(classNames));
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "checksum", checksum);
        setStringAttribute(element, "path", library.getAbsolutePath());
        setStringAttribute(element, "driver-classes", String.join(",", driverClassNames));

    }
}
