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

package com.dbn.connection.config;

import com.dbn.connection.config.provider.CloudConfigProviderAuthentication;
import com.dbn.oci.config.OciConfigFileUtil;
import lombok.experimental.UtilityClass;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.dbn.common.util.Strings.isNotEmpty;

@UtilityClass
public class OciConfigProviderParameters {
    public static Map<String, String> build(
            CloudConfigProviderAuthentication authentication,
            String configFile,
            String profile) {
        Map<String, String> parameters = new LinkedHashMap<>();
        if (authentication == null) return parameters;

        parameters.put("AUTHENTICATION", authentication.getParameterValue());

        if (authentication != CloudConfigProviderAuthentication.OCI_DEFAULT) {
            return parameters;
        }

        if (isNotEmpty(profile)) {
            parameters.put("OCI_PROFILE", profile);
        }

        if (isNotEmpty(configFile) && isNotEmpty(profile)) {
            Map<String, String> profileValues = OciConfigFileUtil.getConfigProfileValues(configFile, profile);
            put(parameters, "OCI_TENANCY", profileValues.get("tenancy"));
            put(parameters, "OCI_USER", profileValues.get("user"));
            put(parameters, "OCI_FINGERPRINT", profileValues.get("fingerprint"));
            put(parameters, "OCI_KEY_FILE", profileValues.get("key_file"));
            put(parameters, "OCI_PASS_PHRASE", profileValues.get("pass_phrase"));
        }

        return parameters;
    }

    private static void put(Map<String, String> parameters, String key, String value) {
        if (isNotEmpty(value)) {
            parameters.put(key, value);
        }
    }
}
