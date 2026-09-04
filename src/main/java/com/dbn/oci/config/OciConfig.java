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

package com.dbn.oci.config;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Cloneable;
import lombok.Data;
import lombok.SneakyThrows;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.getEnum;
import static com.dbn.common.options.setting.Settings.getString;
import static com.dbn.common.options.setting.Settings.setEnum;
import static com.dbn.common.options.setting.Settings.setString;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.common.util.Unsafe.cast;

@Data
public class OciConfig implements PersistentStateElement, Cloneable<OciConfig> {
    private OciConfigType type = OciConfigType.FILE;
    private String userId;
    private String tenancyId;
    private String compartmentId;
    private String privateKeyFile;
    private String fingerprint;
    private String configFile;
    private String configProfile;
    private String regionId = "us-chicago-1";  // fallback to the initially supported region

    public boolean isProvided() {
        if (type == OciConfigType.FILE) {
            return
                isNotEmpty(configFile) &&
                isNotEmpty(configProfile) &&
                isNotEmpty(compartmentId);
        } else {
            return
                isNotEmpty(userId) &&
                isNotEmpty(tenancyId) &&
                isNotEmpty(compartmentId) &&
                isNotEmpty(privateKeyFile) &&
                isNotEmpty(fingerprint);
        }
    }

    @Override
    public void readState(Element element) {
        if (element == null) return;

        type = getEnum(element, "type", type);
        userId = getString(element, "user-id", userId);
        tenancyId = getString(element, "tenancy-id", tenancyId);
        compartmentId = getString(element, "compartment-id", compartmentId);
        regionId = getString(element, "region-id", regionId);

        privateKeyFile = getString(element, "private-key-file", privateKeyFile);
        fingerprint = getString(element, "fingerprint", fingerprint);

        configFile = getString(element, "config-file", configFile);
        configProfile = getString(element, "config-profile", configProfile);
    }

    @Override
    public void writeState(Element element) {
        setEnum(element, "type", type);

        setString(element, "user-id", userId);
        setString(element, "tenancy-id", tenancyId);
        setString(element, "compartment-id", compartmentId);
        setString(element, "region-id", regionId);

        setString(element, "private-key-file", privateKeyFile);
        setString(element, "fingerprint", fingerprint);

        setString(element, "config-file", configFile);
        setString(element, "config-profile", configProfile);
    }

    @Override
    @SneakyThrows
    public OciConfig clone() {
        return cast(super.clone());
    }
}
