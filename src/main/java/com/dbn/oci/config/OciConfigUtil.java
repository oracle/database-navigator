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

import com.dbn.common.Reflection;
import com.dbn.common.compatibility.Experimental;
import com.dbn.common.compatibility.Workaround;
import com.dbn.diagnostics.Diagnostics;
import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimplePrivateKeySupplier;
import com.oracle.bmc.generativeai.GenerativeAiClient;
import com.oracle.bmc.generativeai.requests.ListModelsRequest;
import com.oracle.bmc.generativeai.responses.ListModelsResponse;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.requests.ListRegionsRequest;
import com.oracle.bmc.identity.responses.ListRegionsResponse;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.dbn.common.util.Classes.withClassLoader;
import static com.dbn.common.util.Strings.isEmpty;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

public class OciConfigUtil {

    @Workaround
    public static List<String> getConfigProfileNames(String configFilePath) {
        if (isEmpty(configFilePath)) return emptyList();

        var config = readConfiguration(configFilePath);
        return new ArrayList<>(config.keySet());
    }

    @NonNls
    public static Map<String, String> getConfigProfileValues(String configFilePath, String profileName) {
        if (isEmpty(configFilePath)) return emptyMap();
        if (isEmpty(profileName)) return emptyMap();

        var config = readConfiguration(configFilePath);
        var profileConfig = config.get(profileName);
        return profileConfig == null ? emptyMap() : profileConfig;
    }

    private static Map<String, Map<String, String>> readConfiguration(String configFilePath) {
        try {
            ConfigFileReader.ConfigFile configFile = ConfigFileReader.parse(configFilePath);

            //configFile.accumulator.configurationsByProfile
            Object accumulator = Reflection.getFieldValue(configFile, "accumulator");
            return Reflection.getFieldValue(accumulator, "configurationsByProfile");
        } catch (Throwable t) {
            // TODO propagate exception to consumer
            Diagnostics.conditionallyLog(t);
            return emptyMap();
        }
    }


    @Experimental
    public static List<String> getRegionNames(OciConfig config) throws IOException {
        return withClassLoader(OciConfigUtil.class, () -> {
            AuthenticationDetailsProvider authProvider = createAuthProvider(config);
            try (IdentityClient client = IdentityClient.builder().build(authProvider)) {
                ListRegionsRequest request = ListRegionsRequest.builder().build();
                ListRegionsResponse response = client.listRegions(request);
                return response
                        .getItems()
                        .stream()
                        .map(r -> r.getName())
                        .collect(toList());
            }
        });
    }

    @Experimental
    public static List<String> getModelNames(OciConfig config) throws IOException {
        return withClassLoader(OciConfigUtil.class, () -> {
            AuthenticationDetailsProvider authProvider = createAuthProvider(config);
            try (GenerativeAiClient client = GenerativeAiClient.builder().build(authProvider)) {
                client.setRegion(Region.fromRegionCodeOrId(config.getRegionId()));
                ListModelsRequest request = ListModelsRequest.builder().compartmentId(config.getCompartmentId()).build();
                ListModelsResponse response = client.listModels(request);
                return response
                        .getModelCollection()
                        .getItems()
                        .stream()
                        .map(m -> m.getDisplayName())
                        .collect(toList());
            }
        });
    }

    public static @NotNull AuthenticationDetailsProvider createAuthProvider(OciConfig config) throws IOException {
        OciConfigType configType = config.getType();
        if (configType == OciConfigType.FILE) {
            String configFile = config.getConfigFile();
            String configProfile = config.getConfigProfile();

            return new ConfigFileAuthenticationDetailsProvider(configFile, configProfile);
        }

        if (configType == OciConfigType.CUSTOM) {
            return SimpleAuthenticationDetailsProvider
                    .builder()
                    .userId(config.getUserId())
                    .tenantId(config.getTenancyId())
                    .fingerprint(config.getFingerprint())
                    //.region(Region.fromRegionCodeOrId(config.getRegion()))
                    .privateKeySupplier(new SimplePrivateKeySupplier(config.getPrivateKeyFile()))
                    .build();

        }

        throw new IllegalArgumentException("Unsupported config type: " + configType);
    }
}
